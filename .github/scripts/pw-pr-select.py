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
import functools
import os
import re
import sys
from pathlib import Path

import yaml


def glob_to_regex(pattern: str) -> re.Pattern[str]:
    """Translate a gitignore-style glob to a regex.

    Semantics (whole-segment `**`):
    - `**/`  at the start matches zero or more leading path segments.
    - `/**/` between segments matches zero or more whole segments.
    - `/**`  at the end matches the rest of the path (any depth, or nothing).
    - `*`    matches anything except `/`.
    - `?`    matches a single character except `/`.
    All other characters are matched literally.
    """
    out: list[str] = []
    i = 0
    n = len(pattern)

    # Leading `**/`: zero or more segments at the start.
    if pattern.startswith("**/"):
        out.append("(?:.*/)?")
        i = 3

    while i < n:
        c = pattern[i]
        if c == "*" and i + 1 < n and pattern[i + 1] == "*":
            # `**` only valid as `/**/` between segments or `/**` at end.
            # The leading-`/` should already be in `out`; we replace it.
            if out and out[-1] == "/" and i + 2 < n and pattern[i + 2] == "/":
                out[-1] = "(?:/.*)?/"
                i += 3
            elif out and out[-1] == "/" and i + 2 == n:
                out[-1] = "(?:/.*)?"
                i += 2
            else:
                # Treat a bare or malformed `**` as `.*` (rare).
                out.append(".*")
                i += 2
        elif c == "*":
            out.append("[^/]*")
            i += 1
        elif c == "?":
            out.append("[^/]")
            i += 1
        elif c == "/":
            out.append("/")
            i += 1
        else:
            out.append(re.escape(c))
            i += 1
    return re.compile("^" + "".join(out) + "$")


@functools.lru_cache(maxsize=None)
def _compile(pattern: str) -> re.Pattern[str]:
    return glob_to_regex(pattern)


def matches_any(path: str, patterns: list[str]) -> bool:
    return any(_compile(p).match(path) for p in patterns)


def emit(name: str, value: str) -> None:
    output_path = os.environ.get("GITHUB_OUTPUT")
    line = f"{name}={value}"
    if output_path:
        with open(output_path, "a", encoding="utf-8") as fh:
            fh.write(line + "\n")
    print(line)


E2E_TESTS_PREFIX = "e2e/rstudio/tests/"


def e2e_test_selector(path: str) -> str | None:
    """If `path` is a Playwright test file under e2e/rstudio/tests/, return
    the selector to run it. Otherwise return None.

    Selectors are returned relative to e2e/rstudio/ (Playwright's working
    directory). Only `.test.ts` files are mapped; other files under
    tests/ (fixtures, helpers) are ignored here and may match map rules
    or fall through to the fallback.
    """
    if not path.startswith(E2E_TESTS_PREFIX):
        return None
    if not path.endswith(".test.ts"):
        return None
    return path[len("e2e/rstudio/"):]


def run_self_test() -> int:
    cases = [
        # (pattern, path, expected_match)
        ("src/**", "src/foo.cpp", True),
        ("src/**", "src/a/b/c.cpp", True),
        ("src/**", "srcfoo", False),
        ("src/gwt/**/views/chat/**", "src/gwt/foo/views/chat/x", True),
        ("src/gwt/**/views/chat/**", "src/gwt/views/chat/x", True),  # zero segments
        ("src/gwt/**/views/chat/**", "src/gwt/foo/aviews/chat/x", False),  # whole-segment
        ("src/cpp/session/modules/posit_assistant/**", "src/cpp/session/modules/posit_assistant/foo.cpp", True),
        ("**/*.md", "NEWS.md", True),
        ("**/*.md", "docs/api.md", True),
        ("**/*.md", "src/foo.cpp", False),
        ("docs/**", "docs/api.md", True),
        ("docs/**", "src/docs/api.md", False),
        ("src/cpp/session/modules/SessionChat*", "src/cpp/session/modules/SessionChat.cpp", True),
        ("src/cpp/session/modules/SessionChat*", "src/cpp/session/modules/SessionConsole.cpp", False),
    ]
    failed = 0
    for pat, path, expected in cases:
        got = bool(glob_to_regex(pat).match(path))
        if got != expected:
            failed += 1
            print(f"FAIL: pattern={pat!r} path={path!r} expected={expected} got={got}", file=sys.stderr)
    e2e_cases = [
        ("e2e/rstudio/tests/panes/console/foo.test.ts", "tests/panes/console/foo.test.ts"),
        ("e2e/rstudio/tests/sandbox.test.ts", "tests/sandbox.test.ts"),
        ("e2e/rstudio/tests/projects/create_projects.test.ts", "tests/projects/create_projects.test.ts"),
        ("e2e/rstudio/tests/panes/console/helpers.ts", None),  # not a .test.ts
        ("e2e/rstudio/playwright.config.ts", None),
        ("src/cpp/foo.cpp", None),
    ]
    for path, expected in e2e_cases:
        got = e2e_test_selector(path)
        if got != expected:
            failed += 1
            print(f"FAIL: e2e_test_selector({path!r}) expected={expected!r} got={got!r}", file=sys.stderr)
    if failed:
        print(f"{failed} self-test case(s) failed", file=sys.stderr)
        return 1
    print(f"All {len(cases) + len(e2e_cases)} self-test cases passed")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--map", type=Path, help="Path to pw-test-map.yml")
    parser.add_argument("--title", default="", help="PR title, for override tokens")
    parser.add_argument("--self-test", action="store_true", help="Run internal assertions and exit")
    args = parser.parse_args()

    if args.self_test:
        return run_self_test()

    if args.map is None:
        parser.error("--map is required unless --self-test is given")

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

    def add(t: str) -> None:
        if t not in seen:
            seen.add(t)
            selected.append(t)

    for rule in rules:
        patterns = rule.get("paths", []) or []
        tests = rule.get("tests", []) or []
        if any(matches_any(f, patterns) for f in changed):
            for t in tests:
                add(t)

    # Implicit rule: a changed Playwright test file maps to its own area.
    # Keeps the scope tight when only test code is edited.
    for f in changed:
        sel = e2e_test_selector(f)
        if sel is not None:
            add(sel)

    if not selected:
        selected = list(fallback)

    emit("skip", "false")
    emit("full", "false")
    emit("selector", " ".join(selected))
    return 0


if __name__ == "__main__":
    sys.exit(main())
