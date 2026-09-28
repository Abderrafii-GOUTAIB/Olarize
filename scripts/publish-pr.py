#!/usr/bin/env python3
"""Push the current ticket branch and create its pull request into develop."""
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import urllib.parse

ROOT = Path(__file__).resolve().parents[1]


def git(*args, **kwargs):
    return subprocess.run(['git', *args], cwd=ROOT, text=True, capture_output=True,
                          check=True, **kwargs).stdout.strip()


def github(method, repository, path, token, payload=None):
    # Use the OS HTTPS trust store through curl; never disable TLS verification.
    # Pass authorization on stdin so credentials never appear in process arguments.
    command = ['curl', '--silent', '--show-error', '--fail', '--max-time', '30',
               '--config', '-', '--request', method,
               '--header', 'Accept: application/vnd.github+json',
               '--header', 'Content-Type: application/json',
               '--user-agent', 'Olarize-ticket-publisher',
               '--url', 'https://api.github.com/repos/' + repository + path]
    if payload is not None:
        command.extend(['--data-binary', json.dumps(payload)])
    result = subprocess.run(command, input='header = ' + json.dumps('Authorization: Bearer ' + token) + '\n',
                            text=True, capture_output=True, timeout=35)
    if result.returncode:
        raise ValueError('GitHub request failed (HTTPS client code %s). Check connectivity and repository access, then retry.' % result.returncode)
    return json.loads(result.stdout)


def credentials():
    token = os.environ.get('GITHUB_TOKEN') or os.environ.get('GH_TOKEN')
    if token:
        return token
    env = dict(os.environ, GIT_TERMINAL_PROMPT='0')
    try:
        output = git('credential', 'fill', input='protocol=https\nhost=github.com\n\n', env=env, timeout=15)
    except (subprocess.SubprocessError, OSError):
        raise ValueError('GitHub authentication is missing. Configure Git credentials or GITHUB_TOKEN, then retry.') from None
    fields = dict(line.split('=', 1) for line in output.splitlines() if '=' in line)
    if not fields.get('password'):
        raise ValueError('The Git credential helper supplied no GitHub token.')
    return fields['password']


def publish():
    branch = git('symbolic-ref', '--quiet', '--short', 'HEAD')
    if not re.fullmatch(r'(feature|fix)/OLARIZE-[1-9][0-9]*', branch):
        raise ValueError('Publication requires feature/OLARIZE-<number> or fix/OLARIZE-<number>.')
    ticket = branch.split('/')[1]
    metadata_path = ROOT / '.github' / 'jira' / (ticket + '.json')
    try:
        metadata = json.loads(metadata_path.read_text())
    except (OSError, ValueError):
        raise ValueError('Missing Jira title/link for %s. Sync .github/jira/%s.json from Jira before retrying.' % (ticket, ticket)) from None
    if (metadata.get('key') != ticket or not isinstance(metadata.get('summary'), str)
            or not metadata['summary'].strip()
            or metadata.get('url') != 'https://goutaibabderrafii.atlassian.net/browse/' + ticket):
        raise ValueError('Invalid Jira metadata for ' + ticket)
    origin = git('remote', 'get-url', '--push', 'origin')
    match = re.fullmatch(r'(?:https://github\.com/|git@github\.com:)([A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+?)(?:\.git)?', origin)
    if not match:
        raise ValueError('origin must point to a github.com repository without embedded credentials.')
    repository = match.group(1)
    token = credentials()
    title = ticket + ': ' + metadata['summary']
    body = 'Jira: [%s — %s](%s)\n\nImplements the linked Jira ticket.\n\nValidation results are recorded in the branch documentation and GitHub checks.' % (ticket, metadata['summary'], metadata['url'])
    query = urllib.parse.urlencode({'state': 'open', 'head': repository.split('/')[0] + ':' + branch, 'base': 'develop'})
    existing = github('GET', repository, '/pulls?' + query, token)
    # Normal push only: rewritten history must be handled explicitly by the user.
    git('push', '--set-upstream', 'origin', 'HEAD:refs/heads/' + branch, timeout=120)
    if existing:
        pr = existing[0]
        print('Olarize: pushed changes to existing PR ' + pr['html_url'])
    else:
        pr = github('POST', repository, '/pulls', token,
                    {'title': title, 'head': branch, 'base': 'develop', 'body': body})
        print('Olarize: created PR ' + pr['html_url'])
    return pr['html_url']


if __name__ == '__main__':
    try:
        publish()
    except (ValueError, OSError, subprocess.SubprocessError) as error:
        detail = str(error) if isinstance(error, ValueError) else 'Git operation failed; check authentication, connectivity and branch history.'
        print('Olarize: ' + detail, file=sys.stderr)
        print('Your local commit is preserved. Retry with: python3 scripts/publish-pr.py', file=sys.stderr)
        sys.exit(1)
