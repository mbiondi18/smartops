#!/usr/bin/env python3
"""
health_check.py — Polls the SmartOps Hub health endpoint and prints status.
Usage: python health_check.py [--url http://<external-ip>]
"""

import argparse
import json
import sys
import urllib.request
import urllib.error
from datetime import datetime, timezone

DEFAULT_URL = "http://34.76.76.49"


def check_health(base_url: str) -> None:
    url = f"{base_url}/actuator/health"
    timestamp = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC")

    try:
        with urllib.request.urlopen(url, timeout=10) as response:
            body = json.loads(response.read().decode())
            status = body.get("status", "UNKNOWN")

            if status == "UP":
                print(f"[{timestamp}] OK  — SmartOps Hub is UP ({url})")
            else:
                print(f"[{timestamp}] WARN — status={status} ({url})")
                sys.exit(1)

    except urllib.error.HTTPError as e:
        print(f"[{timestamp}] ERROR — HTTP {e.code} from {url}")
        sys.exit(1)
    except urllib.error.URLError as e:
        print(f"[{timestamp}] ERROR — Could not reach {url}: {e.reason}")
        sys.exit(1)


def main():
    parser = argparse.ArgumentParser(description="SmartOps Hub health check")
    parser.add_argument("--url", default=DEFAULT_URL, help="Base URL of the app")
    args = parser.parse_args()
    check_health(args.url.rstrip("/"))


if __name__ == "__main__":
    main()
