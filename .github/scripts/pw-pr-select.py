#!/usr/bin/env python3
"""Compute the Playwright test selector for a PR-triggered workflow.

Reads .github/pw-test-map.yml and a list of changed paths from stdin
(one per line). Considers PR title overrides ([full-pw], [skip-pw]).
Prints `key=value` lines to stdout; the calling workflow tees stdout
into `$GITHUB_OUTPUT` to set step outputs.

Usage (inside a GitHub Actions step):

    git diff --name-only "origin/$GITHUB_BASE_REF...HEAD" \\
        | python3 .github/scripts/pw-pr-select.py \\
            --map .github/pw-test-map.yml \\
            --title "$PR_TITLE" \\
        | tee -a "$GITHUB_OUTPUT"

Outputs (printed as `key=value` on stdout):
    selector  Space-separated Playwright test paths (empty when full=true).
    skip      "true" if the workflow should be skipped entirely.
    full      "true" if the full suite should run (selector is empty).

Selection logic (first match wins):
    1. PR title contains `[skip-pw]`  -> skip.
    2. PR title contains `[full-pw]`  -> full suite.
    3. Empty diff                     -> skip with a stderr warning.
    4. All changed files match `skip_if_only` patterns -> skip.
    5. Union of: each YAML rule whose `paths` match any changed file
       contributes its `tests`, AND each changed `.test.ts` file under
       `e2e/rstudio/tests/` contributes itself.
    6. If the union from step 5 is empty, use `fallback`.
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

import yaml


E2E_TESTS_PREFIX = "e2e/rstudio/tests/"


class GlobError(ValueError):
    """Raised when a glob pattern is malformed."""


def glob_to_regex(pattern: str) -> re.Pattern[str]:
    """Translate a gitignore-style glob to a regex.

    Semantics (whole-segment `**`):
    - `**/`  at the start matches zero or more leading path segments.
    - `/**/` between segments matches zero or more whole segments.
    - `/**`  at the end matches the rest of the path (any depth, or nothing).
    - `*`    matches anything except `/`.
    - `?`    matches a single character except `/`.
    All other characters are matched literally.

    A `**` outside the three positions above (e.g. `src/**foo`) is
    malformed and raises GlobError -- catching typos early is more
    valuable than guessing the author's intent.
    """
    out: list[str] = []
    i = 0
    n = len(pattern)

    if pattern.startswith("**/"):
        out.append("(?:.*/)?")
        i = 3

    while i < n:
        c = pattern[i]
        if c == "/" and pattern[i + 1:i + 3] == "**":
            # `/**` at end of pattern, or `/**/` between segments.
            if i + 3 == n:
                out.append("(?:/.*)?")
                i = n
            elif pattern[i + 3] == "/":
                out.append("(?:/.*)?/")
                i += 4
            else:
                raise GlobError(
                    f"`**` must stand alone as a path segment in {pattern!r}"
                )
        elif c == "*" and i + 1 < n and pattern[i + 1] == "*":
            raise GlobError(
                f"`**` must stand alone as a path segment in {pattern!r}"
            )
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


def matches_any(path: str, compiled: list[re.Pattern[str]]) -> bool:
    return any(p.match(path) for p in compiled)


def emit(name: str, value: str) -> None:
    # Print key=value to stdout. The caller (GitHub Actions workflow)
    # is responsible for routing stdout into $GITHUB_OUTPUT. Keeping
    # the env-var read out of Python avoids a tainted-path warning
    # from static analyzers and makes the script easier to test
    # standalone.
    print(f"{name}={value}")


def list_field(d: dict, key: str) -> list:
    """Return d[key] as a list, treating missing or None as []."""
    return d.get(key) or []


def e2e_test_selector(path: str) -> str | None:
    """If `path` is a Playwright `.test.ts` file under e2e/rstudio/tests/,
    return the selector to run it (relative to e2e/rstudio/). Otherwise
    return None.

    This sits outside the YAML rules because the mapping is identity
    (a changed test file maps to itself); encoding it as data would
    require a self-referential rule per directory.

    Non-`.test.ts` files under tests/ (fixtures, helpers) are not
    selected by this function and currently fall through to the
    fallback set.
    """
    if not path.startswith(E2E_TESTS_PREFIX):
        return None
    if not path.endswith(".test.ts"):
        return None
    return path[len("e2e/rstudio/"):]


def annotate_error(file: str, message: str) -> None:
    """Emit a GitHub Actions error annotation tied to a file."""
    print(f"::error file={file}::{message}", file=sys.stderr)


def run_self_test() -> int:
    glob_cases = [
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
    e2e_cases = [
        ("e2e/rstudio/tests/panes/console/foo.test.ts", "tests/panes/console/foo.test.ts"),
        ("e2e/rstudio/tests/sandbox.test.ts", "tests/sandbox.test.ts"),
        ("e2e/rstudio/tests/projects/create_projects.test.ts", "tests/projects/create_projects.test.ts"),
        ("e2e/rstudio/tests/panes/console/helpers.ts", None),
        ("e2e/rstudio/playwright.config.ts", None),
        ("src/cpp/foo.cpp", None),
    ]
    malformed_patterns = [
        "src/**foo",       # `**` not segment-aligned
        "foo**bar",        # `**` mid-segment
        "src/**foo/bar",   # `**` glued to following text
    ]

    failed = 0
    for pat, path, expected in glob_cases:
        got = bool(glob_to_regex(pat).match(path))
        if got != expected:
            failed += 1
            print(f"FAIL: pattern={pat!r} path={path!r} expected={expected} got={got}", file=sys.stderr)
    for path, expected in e2e_cases:
        got = e2e_test_selector(path)
        if got != expected:
            failed += 1
            print(f"FAIL: e2e_test_selector({path!r}) expected={expected!r} got={got!r}", file=sys.stderr)
    for pat in malformed_patterns:
        try:
            glob_to_regex(pat)
        except GlobError:
            continue
        failed += 1
        print(f"FAIL: expected GlobError for malformed pattern {pat!r}", file=sys.stderr)

    total = len(glob_cases) + len(e2e_cases) + len(malformed_patterns)
    if failed:
        print(f"{failed} of {total} self-test case(s) failed", file=sys.stderr)
        return 1
    print(f"All {total} self-test cases passed")
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

    map_path = str(args.map)
    try:
        config = yaml.safe_load(args.map.read_text(encoding="utf-8")) or {}
    except yaml.YAMLError as e:
        annotate_error(map_path, f"Failed to parse YAML: {e}")
        return 1
    if not isinstance(config, dict):
        annotate_error(map_path, "Top-level YAML must be a mapping")
        return 1

    try:
        skip_if_only = [glob_to_regex(p) for p in list_field(config, "skip_if_only")]
        compiled_rules = [
            (
                [glob_to_regex(p) for p in list_field(rule, "paths")],
                list_field(rule, "tests"),
            )
            for rule in list_field(config, "rules")
        ]
    except GlobError as e:
        annotate_error(map_path, str(e))
        return 1
    fallback = list_field(config, "fallback")

    changed = [line.strip() for line in sys.stdin if line.strip()]

    if not changed:
        # PR with zero changed files is unusual (GitHub generally won't open
        # one); log loudly so the cause is visible if it ever happens.
        print("WARNING: no changed files in diff; skipping Playwright run", file=sys.stderr)
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
    for patterns, tests in compiled_rules:
        if any(matches_any(f, patterns) for f in changed):
            selected.extend(tests)
    for f in changed:
        sel = e2e_test_selector(f)
        if sel is not None:
            selected.append(sel)

    selected = list(dict.fromkeys(selected))  # order-preserving dedupe

    if not selected:
        selected = list(fallback)

    emit("skip", "false")
    emit("full", "false")
    emit("selector", " ".join(selected))
    return 0


if __name__ == "__main__":
    sys.exit(main())
