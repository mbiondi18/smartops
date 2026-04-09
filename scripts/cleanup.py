#!/usr/bin/env python3
"""
cleanup.py — Deletes old images from GCP Artifact Registry, keeping the N most recent.
Requires: gcloud CLI authenticated and in PATH.
Usage: python cleanup.py [--keep 5] [--dry-run]
"""

import argparse
import subprocess
import json
import sys
import platform


PROJECT = "smartops-hub-project"
LOCATION = "europe-west1"
REPOSITORY = "smartops-hub-project-dev"
IMAGE = "smartops-hub"


def list_images() -> list[dict]:
    cmd = [
        "gcloud", "artifacts", "docker", "images", "list",
        f"{LOCATION}-docker.pkg.dev/{PROJECT}/{REPOSITORY}/{IMAGE}",
        "--include-tags",
        "--sort-by=~CREATE_TIME",
        "--format=json",
        f"--project={PROJECT}",
    ]
    result = subprocess.run(cmd, capture_output=True, text=True, shell=platform.system() == "Windows")
    if result.returncode != 0:
        print(f"ERROR listing images: {result.stderr}")
        sys.exit(1)
    return json.loads(result.stdout)


def delete_image(digest: str, dry_run: bool) -> None:
    image_ref = (
        f"{LOCATION}-docker.pkg.dev/{PROJECT}/{REPOSITORY}/{IMAGE}@{digest}"
    )
    if dry_run:
        print(f"  [dry-run] would delete {image_ref}")
        return

    cmd = [
        "gcloud", "artifacts", "docker", "images", "delete",
        image_ref,
        "--quiet",
        f"--project={PROJECT}",
    ]
    result = subprocess.run(cmd, capture_output=True, text=True, shell=platform.system() == "Windows")
    if result.returncode != 0:
        print(f"  ERROR deleting {image_ref}: {result.stderr}")
    else:
        print(f"  Deleted {image_ref}")


def main():
    parser = argparse.ArgumentParser(description="Clean up old Artifact Registry images")
    parser.add_argument("--keep", type=int, default=5, help="Number of images to keep (default: 5)")
    parser.add_argument("--dry-run", action="store_true", help="Print what would be deleted without deleting")
    args = parser.parse_args()

    print(f"Fetching image list for {IMAGE}...")
    images = list_images()
    print(f"Found {len(images)} image(s). Keeping {args.keep} most recent.")

    to_delete = images[args.keep:]
    if not to_delete:
        print("Nothing to delete.")
        return

    print(f"Deleting {len(to_delete)} image(s){'  [dry-run]' if args.dry_run else ''}:")
    for image in to_delete:
        digest = image.get("version") or image.get("digest", "")
        delete_image(digest, args.dry_run)

    print("Done.")


if __name__ == "__main__":
    main()
