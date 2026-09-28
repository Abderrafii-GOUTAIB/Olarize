#!/usr/bin/env python3
"""Check PR publication using mocked Git and GitHub; never push or publish."""
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('publisher', Path(__file__).with_name('publish-pr.py'))
publisher = importlib.util.module_from_spec(spec)
spec.loader.exec_module(publisher)


class PublicationTests(unittest.TestCase):
    def setUp(self):
        self.folder = tempfile.TemporaryDirectory(prefix='.publish-test-', dir=publisher.ROOT)
        self.addCleanup(self.folder.cleanup)
        self.root = Path(self.folder.name)
        directory = self.root / '.github/jira'
        directory.mkdir(parents=True)
        (directory / 'OLARIZE-42.json').write_text(json.dumps({
            'key': 'OLARIZE-42', 'summary': 'Exact Jira title',
            'url': 'https://goutaibabderrafii.atlassian.net/browse/OLARIZE-42'}))
        self.root_patch = patch.object(publisher, 'ROOT', self.root)
        self.root_patch.start()
        self.addCleanup(self.root_patch.stop)

    def git(self, *args, **kwargs):
        if args[0] == 'symbolic-ref':
            return 'fix/OLARIZE-42'
        if args[0] == 'remote':
            return 'https://github.com/example/project.git'
        return ''

    def test_creates_pr_with_actual_ticket_title_and_link(self):
        with patch.object(publisher, 'git', side_effect=self.git) as git, patch.object(publisher, 'credentials', return_value='synthetic'), patch.object(publisher, 'github', side_effect=[[], {'html_url': 'https://github.com/example/project/pull/1'}]) as api:
            publisher.publish()
            payload = api.call_args[0][-1]
            self.assertEqual(payload['title'], 'OLARIZE-42: Exact Jira title')
            self.assertEqual(payload['base'], 'develop')
            self.assertIn('/browse/OLARIZE-42', payload['body'])
            git.assert_any_call('push', '--set-upstream', 'origin', 'HEAD:refs/heads/fix/OLARIZE-42', timeout=120)

    def test_existing_pr_is_reused(self):
        with patch.object(publisher, 'git', side_effect=self.git), patch.object(publisher, 'credentials', return_value='synthetic'), patch.object(publisher, 'github', return_value=[{'html_url': 'https://github.com/example/project/pull/1'}]) as api:
            publisher.publish()
            self.assertEqual(api.call_count, 1)

    def test_missing_metadata_stops_before_push(self):
        with patch.object(publisher, 'git', return_value='feature/OLARIZE-99') as git:
            with self.assertRaisesRegex(ValueError, 'Missing Jira'):
                publisher.publish()
            self.assertEqual(git.call_count, 1)

    def test_api_failure_does_not_push(self):
        with patch.object(publisher, 'git', side_effect=self.git) as git, patch.object(publisher, 'credentials', return_value='synthetic'), patch.object(publisher, 'github', side_effect=ValueError('unavailable')):
            with self.assertRaises(ValueError):
                publisher.publish()
            self.assertFalse(any(call[0][0] == 'push' for call in git.call_args_list))


if __name__ == '__main__':
    unittest.main()
