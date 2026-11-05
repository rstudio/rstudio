# Git gutter diff experiments

Experiment for [rstudio/rstudio#19218](https://github.com/rstudio/rstudio/issues/19218).
This directory contains runnable prototypes and a comparison harness. It does not
change the shipped editor or vendored Ace files.

## Run

From the repository root, with Node.js and Git available:

```sh
node --test src/gwt/tools/git-diff-token-experiment/experiment.test.cjs
node src/gwt/tools/git-diff-token-experiment/run.cjs --samples 5
# Use a separate output directory to keep an earlier run:
node src/gwt/tools/git-diff-token-experiment/run.cjs --samples 5 --output .cache/git-diff-bounded-experiment
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

The token candidates encode the remaining lines before the first provider call, drop
character ranges that describe tokens, and retain the existing elapsed-time /
whole-range failure guard. They measure tokenization as part of the elapsed time.
The token and line-only candidates use the current raw-text path when review is
attached. The bounded candidate shares its hunk policy across both modes and
adds separately budgeted review highlights without changing those hunks.
This branch is exercised without a DOM;
rendering and mode switching are not validated in the IDE. For the provider
option, a headless test also exercises the real view-attachment and update-flush
methods with event and DOM plumbing stubbed.

| Strategy | Representation |
| --- | --- |
| current | Current raw-text diff with its token fallback |
| bare | Current fallback's per-line CJK identifiers, used immediately |
| indent | Original leading spaces/tabs followed by the identifier |
| padded | Identifier padded with periods to at least `min(5, non-whitespace length)`; blank lines preserved |
| indent-padded | Padding plus original leading spaces/tabs |
| structured | Identifiers keyed on trimmed text, with original leading/trailing whitespace and padding |
| line-only | Original lines with the provider's experimental `skipCharacterRefinement` option |
| bounded | Original line heuristics with limited refinement for small or ambiguous groups; fixed hunks in review |

The structured variant preserves both exact equality and the provider's initial
trimmed-line equality, as well as indentation and the short-line threshold for
single-character identifiers. It still changes length-weighted matching and
character-refinement heuristics. Above 20,992 unique lines identifiers grow to
multiple characters, so very short lines may also grow. Identity remains exact;
the tests check uniqueness across this boundary.

The line-only provider option is inserted into `DefaultLinesDiffComputer.refineDiff` in
memory. It returns the existing full line-range mapping before constructing
character sequences or running the character diff algorithm. Original line
hashes, weighted matching, boundary placement, short-line joining, and whitespace
scanning are unchanged. Coarse internal mappings still go through the existing
line-range conversion; the public result omits character changes when the option
is enabled. The option defaults off and stays off while review is attached.
Opening or closing review marks the cached diff for recomputation and flushes
it through the existing update path. Without this, opening review with no
pending edit would reuse cached line-only chunks and show no character ranges.
The transition test checks open, close, and reopen without changing the document,
including the exact chunks and character ranges delivered to the mock view.
The line-only strategy retains the existing elapsed-time/whole-range failure
guard. The bounded strategy instead uses the provider's explicit timeout status,
as described below.

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
  exact character ranges with review attached, whitespace preferences, empty
  inputs, EOF changes, diff coverage, and Git hunk parsing. They also verify the
  provider's intermediate line alignments and expose cases where removing
  refinement changes the final hunk grouping.
- For the provider option, the full corpus also compares the review-mode hunks
  and character ranges with raw text. `reviewBoundaryChanges` records cases
  where enabling review changes the final hunks even though the line alignment
  heuristics are unchanged. This is a headless mode comparison, not a UI test.
- For bounded refinement, each case also exercises open/close/reopen and asserts
  that all successful transitions retain the gutter hunks. Paired 250 ms gutter
  and review samples assert the same boundaries, with alternating measurement
  order. Per-group and aggregate allocation/access budgets are checked too.

Absolute timings depend on machine load and the Node VM; they are not browser
latency guarantees. No wall-clock threshold is asserted in the tests. No IDE
build or E2E run is required for this isolated experiment, and neither was run.

## Token-first results

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

The 800-line replacement exhausted character refinement on every timing sample
in this original token-first run; line alignment did not time out. The existing failure guard
then discarded the result. Tokens therefore do not make the elapsed-time test
an unambiguous indication of alignment failure. They halve the worst-case wait
here by avoiding the first raw-text pass, but still leave no markers.

## Token-first assessment

The speedup is promising for moderate bulk replacements. Bare tokens are not a
suitable default. Structured tokens are a better candidate but still change
chunk grouping and retain refinement timeouts. Switching to the raw path for
review also means opening the review view can change hunk boundaries; the
headless character-highlight check does not resolve that interaction.

These results motivated the provider-level experiment below.

## Provider-level results

The same 29 cases were rerun with five measured samples per strategy, on the
same machine and Node version. The table uses medians from this run, so the
provider option and token variants share the same measurement conditions.
Detailed output is in `.cache/git-diff-provider-experiment/` for this checkout.

| Strategy | Historical hunk mismatches vs raw (of 13) | 100-line replacement | 300-line replacement | 800-line replacement |
| --- | ---: | ---: | ---: | ---: |
| current | 0 | 25.4 ms | 290.1 ms | 499.6 ms (no markers) |
| bare | 8 | 8.5 ms | 40.8 ms | 250.0 ms (no markers) |
| structured | 2 | 5.7 ms | 44.5 ms | 250.1 ms (no markers) |
| line-only | 1 | 1.7 ms | 15.1 ms | 104.8 ms |

For the provider option:

- All 29 intermediate line alignments matched the unskipped raw-text provider.
  No character refinement algorithms ran with the option enabled.
- There were no failed timing samples. In particular, all five 800-line
  replacement samples retained the expected full replacement hunk.
- At the 5,000 ms validation budget, attaching review preserved the raw provider's
  exact hunks and character ranges in all 29 cases. The optimization applies
  only while review is closed; review retains the existing refinement cost and
  timeout behavior at the normal 250 ms budget.
- Final hunks differed in one of 13 historical file changes and three synthetic
  cases. The historical difference is `8a832cdbfb` in `TextEditingTarget.java`:
  three pairs of changes become three larger hunks. In the synthetic cases,
  changes separated by an unchanged `}`, empty line, or whitespace-only line
  become one hunk instead of two. The meaningful-statement separator stays
  separate. Coverage and complete-base reconstruction checks pass throughout.

The intermediate line heuristics already join these short-line groups. The
character pass subsequently splits some of them back apart. Preserving the line
heuristics therefore does not guarantee identical final hunks when refinement
is skipped. The tests explicitly retain this counterexample, and the report
records the resulting boundary changes between gutter-only and review modes.

## Line-only assessment

The provider option is the strongest performance candidate so far: it removes
the expensive character pass rather than shortening its input, preserves the
original line alignment, and fixes the tested 800-line gutter failure. It also
preserves character highlights by leaving review on the existing path.

The remaining design decision is hunk granularity. Accepting the coarser hunks
also accepts changes to navigation/revert grouping when review opens. If exact
current hunks are required, a pure refinement bypass is insufficient; bounded or
selective refinement would need its own experiment. The timing guard still
infers failure from elapsed time, and large line-alignment problems can still
exhaust the budget. Public timeout reporting remains follow-up work.

This remains a headless experiment. Before a production PR, choose the hunk
contract and validate mode switching, peeks, navigation, and individual reverts
in the IDE. No shipped source or generated Ace bundle has been modified.

## Bounded refinement

`bounded.cjs` installs the experimental policy around the provider's original
refinement function. Original line alignment, indentation placement, short-line
joining, and whitespace scanning are unchanged. A group is eligible when it has
at most 32 changed lines across both sides, or at most 128 and a trimmed line
shared by the two sides (an ambiguous alignment). Eligible groups must also fit
within 4,096 characters across both mapped ranges, including boundary newlines.

| Budget | Hunk selection | Additional review highlights |
| --- | ---: | ---: |
| Maximum combined lines per group | 32, or 128 when ambiguous | 128 |
| Maximum combined characters per group | 4,096 | 8,192 |
| Character-sequence reads per group | 200,000 | 100,000 |
| Character-sequence reads per update | 2,000,000 | 300,000 |
| Characters submitted per update | 65,536 | 16,384 |

Both views use the same hunk-selection policy. An oversized group or exhausted
budget returns its original coarse line mapping. Review can refine characters
inside that mapping under its own budget, but the resulting character ranges
are decorations only: they never participate in final hunk conversion. Groups
that cannot be decorated retain whole-line highlighting.

The access budget counts `LinesSliceCharSequence.getElement` calls, including
calls from the provider's refinement heuristics. The size/allocation caps bound
remaining work that accesses sequence storage directly. These are deterministic
work limits, not hard wall-clock deadlines. Independent hunk and highlight
budgets ensure that opening review cannot consume work reserved for a later
hunk or choose a different partition just because the machine ran more slowly.
Tests cover both per-group and shared-budget exhaustion, including a zero
allocation budget, and verify that review still cannot repartition the result.
An additional check uses Ace's actual position mapping to verify selections in
coarse review hunks after an insertion. Missing character ranges must be
`undefined`, not an empty array, to preserve its line-position fallback.

Line alignment retains its 250 ms timeout. The harness preserves the provider's
existing `hitTimeout` flag on the returned chunk array. The bounded gutter checks
that flag instead of inferring failure from elapsed time and a whole-file hunk.
Character-budget exhaustion yields coarse mappings rather than setting that
flag. Overall wall time can exceed 250 ms because the character work has a
separate budget. A production API should expose this status explicitly.

## Bounded results

The same 29 cases and five samples per strategy were used, on the same Apple M4
Pro / Node v26.13.0 setup. This run also times the bounded review path. Medians:

| Case | Current gutter | Structured tokens | Line-only gutter | Bounded gutter | Bounded review |
| --- | ---: | ---: | ---: | ---: | ---: |
| 100-line replacement | 25.8 ms | 5.9 ms | 1.7 ms | 1.7 ms | 1.7 ms |
| 300-line replacement | 291.1 ms | 44.8 ms | 15.2 ms | 15.0 ms | 15.1 ms |
| 800-line replacement | 499.6 ms (no markers) | 250.0 ms (no markers) | 107.5 ms | 108.1 ms | 108.6 ms |
| Large historical Java change | 253.1 ms | 268.8 ms | 102.5 ms | 228.0 ms | 242.5 ms |

- Final bounded hunks matched the completed raw reference in **all 29 cases**,
  including all 13 historical changes and the short-separator cases that the
  line-only option grouped together. Intermediate line alignments also matched.
- There were **zero gutter/review boundary differences** in all 145 paired
  timing checks. All 29 open/close/reopen cases preserved their hunks and
  delivered the expected character ranges to the mock review view.
- No bounded gutter or review timing sample failed. Allocation and read-budget
  checks passed. The largest canonical allocation was 31,304 characters; the
  large Java change exhausted the 2,000,000-read budget, exercising fallback.
- Review character ranges exactly matched the raw reference in 25 of 29 cases.
  The three bulk replacements use whole-line highlighting. The large historical
  Java change retains fewer character highlights after its budgets are consumed.
  Hunk boundaries remain identical in those cases.

The bounded policy preserves the current granularity on this corpus while
keeping the large-replacement speedup and a consistent revert scope across
views. This is stronger evidence than the previous two strategies. It does not
establish exact raw-diff parity for arbitrary files: forced exhaustion tests
intentionally produce coarser hunks, consistently in both views.

This is still a local, headless experiment. A production change would need a
supported provider option/status API, the review-refresh integration, and IDE
verification of rendering, navigation, peeks, individual reverts, and mode
switching. No production source or generated Ace bundle has been modified.
