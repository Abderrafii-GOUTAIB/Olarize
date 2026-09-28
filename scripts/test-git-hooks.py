#!/usr/bin/env python3
"""Exercise real Git commits with the project hooks in a disposable repository."""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]


def main():
    env = {key: value for key, value in os.environ.items() if not key.startswith("GIT_")}
    env.update(GIT_CONFIG_NOSYSTEM="1", GIT_CONFIG_GLOBAL=os.devnull)
    with tempfile.TemporaryDirectory(prefix=".hook-test-", dir=ROOT) as folder:
        repo = Path(folder)

        def run(*args, success=True):
            result = subprocess.run(args, cwd=repo, env=env, text=True, capture_output=True)
            assert (result.returncode == 0) == success, result.stdout + result.stderr
            return result

        run("git", "init", "--quiet")
        run("git", "config", "user.name", "Synthetic Hook Test")
        run("git", "config", "user.email", "hook-test@example.invalid")
        run("git", "config", "commit.gpgsign", "false")
        shutil.copytree(ROOT / ".githooks", repo / ".githooks")
        (repo / "scripts").mkdir()
        shutil.copy2(ROOT / "scripts/install-hooks.sh", repo / "scripts/install-hooks.sh")
        run("sh", "scripts/install-hooks.sh")
        run("sh", "scripts/install-hooks.sh")  # Installation is repeatable.
        run("git", "symbolic-ref", "HEAD", "refs/heads/feature/OLARIZE-18")
        (repo / "sample.txt").write_text("synthetic data\n")
        run("git", "add", "sample.txt")
        for message in ("message", "OLARIZE-42: wrong ticket", "OLARIZE-18:", "OLARIZE-18:   "):
            result = run("git", "commit", "-m", message, success=False)
            assert "commit subject must start" in result.stderr
        run("git", "commit", "-m", "OLARIZE-18: add synthetic fixture")

        for branch in ("OLARIZE-18", "feat/OLARIZE-18", "feature/OLARIZE-0", "feature/OLARIZE-18-extra", "develop", "master"):
            run("git", "checkout", "-b", branch)
            result = run("git", "commit", "--allow-empty", "-m", "OLARIZE-18: rejected", success=False)
            assert "commits require a branch" in result.stderr
        run("git", "checkout", "--detach")
        run("git", "commit", "--allow-empty", "-m", "OLARIZE-18: detached", success=False)

        for branch, ticket in (("fix/OLARIZE-42", "OLARIZE-42"), ("feature/OLARIZE-123", "OLARIZE-123")):
            run("git", "checkout", "-b", branch)
            run("git", "commit", "--allow-empty", "-m", "OLARIZE-18: wrong ticket", success=False)
            run("git", "commit", "--allow-empty", "-m", ticket + ": valid change\n\nOptional body.")

        for content in ("bad whitespace \n", "<<<<<<< ours\nsynthetic\n=======\nother\n>>>>>>> theirs\n"):
            (repo / "sample.txt").write_text(content)
            run("git", "add", "sample.txt")
            result = run("git", "commit", "-m", "OLARIZE-123: invalid content", success=False)
            assert "staged whitespace/conflict markers" in result.stderr
        (repo / "sample.txt").write_text("clean staged data\n")
        run("git", "add", "sample.txt")
        (repo / "sample.txt").write_text("unstaged whitespace \n")
        run("git", "commit", "-m", "OLARIZE-123: check staged content only")
        assert (repo / "sample.txt").read_text() == "unstaged whitespace \n"

        run("git", "config", "--unset", "core.hooksPath")
        (repo / ".git/hooks/commit-msg").write_text("#!/bin/sh\nexit 0\n")
        run("sh", "scripts/install-hooks.sh", success=False)
        run("git", "config", "core.hooksPath", "other-hooks")
        run("sh", "scripts/install-hooks.sh", success=False)
    print("Git hook checks passed: branches, matching tickets, whitespace, conflict markers, partial staging and installer safeguards.")


if __name__ == "__main__":
    main()
