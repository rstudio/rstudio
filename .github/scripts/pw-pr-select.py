#!/usr/bin/env python3
"""Compute the Playwright test selector for a PR-triggered workflow.

Reads .github/pw-test-map.yml and a list of changed paths from stdin
(one per line). Considers PR title overrides ([full-pw], [skip-pw]).
Emits GITHUB_OUTPUT lines for `selector`, `skip`, and `full`.

Usage (inside a GitHub Actions step):

    git diff --name-only "origin/$GITHUB_BASE_REF...HEAD" \\
        | python3 .github/scripts/pw-pr-select.py \\
            --map .github/pw-test-map.yml \\
            --title "$PR_TITLE"

Outputs:
    selector  Space-separated Playwright test paths (empty when full=true).
    skip      "true" if the workflow should be skipped entirely.
    full      "true" if the full suite should run (selector is empty).
"""

from __future__ import annotations

import argparse
import os
import re
import sys
from pathlib import Path

import yaml


def glob_to_regex(pattern: str) -> re.Pattern[str]:
    """Translate a gitignore-style glob to a regex.

    - `**` matches any number of path segments (including zero).
    - `*` matches anything except `/`.
    - `?` matches a single character except `/`.
    All other characters are matched literally.
    """
    out = []
    i = 0
    while i < len(pattern):
        c = pattern[i]
        if c == "*":
            if i + 1 < len(pattern) and pattern[i + 1] == "*":
                out.append(".*")
                i += 2
                if i < len(pattern) and pattern[i] == "/":
                    i += 1
            else:
                out.append("[^/]*")
                i += 1
        elif c == "?":
            out.append("[^/]")
            i += 1
        else:
            out.append(re.escape(c))
            i += 1
    return re.compile("^" + "".join(out) + "$")


def matches_any(path: str, patterns: list[str]) -> bool:
    return any(glob_to_regex(p).match(path) for p in patterns)


def emit(name: str, value: str) -> None:
    output_path = os.environ.get("GITHUB_OUTPUT")
    line = f"{name}={value}"
    if output_path:
        with open(output_path, "a", encoding="utf-8") as fh:
            fh.write(line + "\n")
    print(line)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--map", required=True, type=Path)
    parser.add_argument("--title", default="", help="PR title, for override tokens")
    args = parser.parse_args()

    title = args.title or ""

    if "[skip-pw]" in title:
        emit("skip", "true")
        emit("full", "false")
        emit("selector", "")
        return 0

    if "[full-pw]" in title:
        emit("skip", "false")
        emit("full", "true")
        emit("selector", "")
        return 0

    config = yaml.safe_load(args.map.read_text(encoding="utf-8")) or {}
    skip_if_only = config.get("skip_if_only", []) or []
    fallback = config.get("fallback", []) or []
    rules = config.get("rules", []) or []

    changed = [line.strip() for line in sys.stdin if line.strip()]

    if not changed:
        # No diff = nothing to test. Skip to avoid burning runner minutes.
        emit("skip", "true")
        emit("full", "false")
        emit("selector", "")
        return 0

    if all(matches_any(f, skip_if_only) for f in changed):
        emit("skip", "true")
        emit("full", "false")
        emit("selector", "")
        return 0

    selected: list[str] = []
    seen: set[str] = set()
    for rule in rules:
        patterns = rule.get("paths", []) or []
        tests = rule.get("tests", []) or []
        if any(matches_any(f, patterns) for f in changed):
            for t in tests:
                if t not in seen:
                    seen.add(t)
                    selected.append(t)

    if not selected:
        selected = list(fallback)

    emit("skip", "false")
    emit("full", "false")
    emit("selector", " ".join(selected))
    return 0


if __name__ == "__main__":
    sys.exit(main())
