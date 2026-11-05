# Token-first Git gutter experiment

Experiment for [rstudio/rstudio#19218](https://github.com/rstudio/rstudio/issues/19218).
This directory contains a runnable prototype and comparison harness. It does not
change the shipped editor or vendored Ace files.

## Run

From the repository root, with Node.js and Git available:

```sh
node --test src/gwt/tools/git-diff-token-experiment/experiment.test.cjs
node src/gwt/tools/git-diff-token-experiment/run.cjs --samples 5
```

No npm packages or network access are needed. The historical commits in
`history.json` must be present locally; a shallow checkout may need more history.
The runner writes detailed samples, hunk coordinates, timeout phases, and summary
counts to `.cache/git-diff-token-experiment/`. Use `--output <directory>` to change
the destination. A successful exit means the experiment completed and coverage
checks passed; it does **not** mean a candidate is suitable to ship.

## Prototype

`engine.cjs` loads the bundled Ace and the actual `git_diff_gutter.js` in a Node
VM, enables strict CSP, then inserts the candidate into `computeChunks` in memory.
The production `update` path still handles whitespace preferences, and the real
`computeChunks` still trims common prefixes/suffixes and handles empty sides.
Exact-match instrumentation sites fail loudly if the source changes.

The candidate encodes the remaining lines before the first provider call, drops
character ranges that describe tokens, and retains the existing elapsed-time /
whole-range failure guard. It measures tokenization as part of the elapsed time.
With a side-by-side review view attached it uses the current raw-text path, so
character highlights remain available. This branch is exercised without a DOM;
view attachment, rendering, and switching between modes are not implemented or
validated in the IDE.

| Strategy | Representation |
| --- | --- |
| current | Current raw-text diff with its token fallback |
| bare | Current fallback's per-line CJK identifiers, used immediately |
| indent | Original leading spaces/tabs followed by the identifier |
| padded | Identifier padded with periods to at least `min(5, non-whitespace length)`; blank lines preserved |
| indent-padded | Padding plus original leading spaces/tabs |
| structured | Identifiers keyed on trimmed text, with original leading/trailing whitespace and padding |

The structured variant preserves both exact equality and the provider's initial
trimmed-line equality, as well as indentation and the short-line threshold for
single-character identifiers. It still changes length-weighted matching and
character-refinement heuristics. Above 20,992 unique lines identifiers grow to
multiple characters, so very short lines may also grow. Identity remains exact;
the tests check uniqueness across this boundary.

## Measurement and checks

The corpus contains 16 synthetic cases and 13 pinned historical file changes
across R, JavaScript, C++, and Java. The manifest records the main-branch commit
used to select the history. The engine itself always comes from this checkout.

- Accuracy comparisons give raw text and each candidate a 5,000 ms budget.
  If either computation times out, their equality result is null rather than a
  claimed match or mismatch.
- Performance samples use the editor's 250 ms budget, one excluded warm-up per
  strategy and case, and rotating strategy order. The report keeps every sample,
  including failure and alignment/refinement timeout flags.
- Every completed accuracy diff and every non-failed timing result must have
  ordered, bounded chunks, equal unchanged gaps, and restore the complete base
  when all chunks are reverted. These checks do not establish that individual
  chunks correspond to the changes a user would want to revert separately.
- Git comparisons use `git diff --no-index --unified=0 --diff-algorithm=myers
  --indent-heuristic` on the same normalized text. Final-newline changes are
  excluded from exact Git comparison because Ace models the final empty row
  differently. Different valid diff algorithms can select different boundaries;
  disagreement with Git alone is not a correctness failure.
- Focused tests cover identity, padding, a meaningful unchanged separator,
  character highlights with review attached, whitespace preferences, empty
  inputs, EOF changes, diff coverage, and Git hunk parsing.

Absolute timings depend on machine load and the Node VM; they are not browser
latency guarantees. No wall-clock threshold is asserted in the tests. No IDE
build or E2E run is required for this isolated experiment, and neither was run.

## Results

Measured on an Apple M4 Pro, macOS arm64, Node v26.13.0, using
the bundled engine at `9bebbdec74` and five measured samples
per strategy/case. All 13 historical raw references completed within the larger
accuracy budget. All coverage checks passed.

| Strategy | Historical hunk mismatches vs raw (of 13) | 100-line replacement | 300-line replacement | 800-line replacement |
| --- | ---: | ---: | ---: | ---: |
| current | 0 | 40.1 ms | 308.9 ms | 500.1 ms (no markers) |
| bare | 8 | 11.3 ms | 54.7 ms | 250.0 ms (no markers) |
| indent | 4 | 16.9 ms | 60.9 ms | 250.1 ms (no markers) |
| padded | 5 | 6.9 ms | 62.1 ms | 251.1 ms (no markers) |
| indent-padded | 2 | 9.0 ms | 62.8 ms | 250.1 ms (no markers) |
| structured | 2 | 11.6 ms | 66.8 ms | 249.9 ms (no markers) |

Bare and indentation-only tokens merge two three-line edits separated by
`keep_this_meaningful_statement()`, while raw text and Git keep them separate.
Padding avoids that particular regression.

Both combined strategies differ from raw text on two historical changes:

- `297730de99`, `git_diff_gutter.js`: they split around an unchanged `};`.
  Those two candidate hunks agree with Git and are plausibly preferable.
- `8a832cdbfb`, `TextEditingTarget.java`: they split around an unchanged `{`
  between a replaced function signature and its body. The raw diff groups that
  replacement together. This is a user-visible change in navigation/revert scope,
  not just a different character highlight.

The 800-line replacement exhausted character refinement on every timing sample,
for every strategy; line alignment did not time out. The existing failure guard
then discarded the result. Tokens therefore do not make the elapsed-time test
an unambiguous indication of alignment failure. They halve the worst-case wait
here by avoiding the first raw-text pass, but still leave no markers.

## Decision

The speedup is promising for moderate bulk replacements. Bare tokens are not a
suitable default. Structured tokens are a better candidate but still change
chunk grouping and retain refinement timeouts. Switching to the raw path for
review also means opening the review view can change hunk boundaries; the
headless character-highlight check does not resolve that interaction.

Keep this as an experiment, without a production change or PR. Before promoting
it, choose the intended chunk-boundary behavior and validate view switching,
peek/navigation, and individual reverts in the IDE. A provider option that keeps
original line metadata while skipping character refinement, with explicit
alignment/refinement timeout reporting, remains the more direct approach to the
underlying problem. That approach is not implemented by this experiment.
