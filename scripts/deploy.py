#!/usr/bin/env python3
"""
deploy.py — Triggers the SmartOps Hub CI/CD pipeline via GitHub Actions API.
Requires: GITHUB_TOKEN environment variable set.
Usage: python deploy.py [--branch main]
"""

import argparse
import json
import os
import sys
import urllib.request
import urllib.error


GITHUB_OWNER = "mbiondi18"
GITHUB_REPO = "smartops-hub"


def trigger_workflow(branch: str, token: str) -> None:
    url = (
        f"https://api.github.com/repos/{GITHUB_OWNER}/{GITHUB_REPO}"
        f"/actions/workflows/ci-cd.yml/dispatches"
    )
    payload = json.dumps({"ref": branch}).encode()
    headers = {
        "Authorization": f"Bearer {token}",
        "Accept": "application/vnd.github+json",
        "Content-Type": "application/json",
        "X-GitHub-Api-Version": "2022-11-28",
    }

    req = urllib.request.Request(url, data=payload, headers=headers, method="POST")
    try:
        with urllib.request.urlopen(req) as response:
            # 204 No Content = success
            print(f"Pipeline triggered on branch '{branch}'. HTTP {response.status}")
            print(f"Check progress at: https://github.com/{GITHUB_OWNER}/{GITHUB_REPO}/actions")
    except urllib.error.HTTPError as e:
        body = e.read().decode()
        print(f"ERROR — HTTP {e.code}: {body}")
        sys.exit(1)
    except urllib.error.URLError as e:
        print(f"ERROR — {e.reason}")
        sys.exit(1)


def main():
    parser = argparse.ArgumentParser(description="Trigger SmartOps Hub GitHub Actions pipeline")
    parser.add_argument("--branch", default="main", help="Branch to deploy (default: main)")
    args = parser.parse_args()

    token = os.environ.get("GITHUB_TOKEN")
    if not token:
        print("ERROR: GITHUB_TOKEN environment variable not set.")
        print("Set it with: $env:GITHUB_TOKEN = 'your-token'")
        sys.exit(1)

    trigger_workflow(args.branch, token)


if __name__ == "__main__":
    main()
