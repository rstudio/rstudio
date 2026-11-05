# Markdown renderer migration

[Issue #18904](https://github.com/rstudio/rstudio/issues/18904) tracks replacing
Sundown. cmark-gfm is the default renderer; configure with
`-DRSTUDIO_USE_CMARK_GFM=OFF` to build the old renderer for comparison or rollback.
Only the selected renderer is linked. The temporary fallback and its vendored
sources can be removed after platform and IDE-level validation.

## Dependency

`cmark-gfm/` contains unmodified upstream `src/` and `extensions/` directories,
the upstream version declaration, README, complete license, and a `REVISION`
file. The pin is
[`27d942c8b0a62d192f616e5bf3578f4b6a89e180`](https://github.com/github/cmark-gfm/commit/27d942c8b0a62d192f616e5bf3578f4b6a89e180).
Upstream's most recent release is still
[`0.29.0.gfm.13`](https://github.com/github/cmark-gfm/releases/tag/0.29.0.gfm.13)
(July 2023), but development has continued; this pin includes subsequent fixes,
including the autolink pointer-overflow fix at that revision. It is a commit
after that release, not a new tagged release.

Run `src/cpp/tools/sync-cmark-gfm` from any working directory to reproduce the
vendor tree, or pass `--revision <full-commit-sha>` to update it. The script uses
`gh api` and Python's standard library. Review the source list and version in
`CmarkGfm.cmake`, `dependencies/metadata/general.txt`, and `NOTICE` on updates.
No download, re2c, Python, or upstream CLI is required to build RStudio: the
generated C scanners are included. The vendor directory is a source subset;
use the complete upstream checkout to run upstream's own CMake test project.

`CmarkGfm.cmake` builds a static C99 library, generates platform/export/version
headers in the build tree, and avoids upstream install rules and project-wide
compiler changes. Runtime source licenses are BSD-2-Clause and MIT; retain all
of `COPYING`, including its component attributions.

## Compatibility decisions

| Behavior | cmark-gfm integration |
| --- | --- |
| Tables, strikethrough, bare autolinks | Upstream GFM extensions, individually configurable. Require double tildes for strikethrough, matching Sundown. |
| Fenced/indented code, emphasis, lists, HTML blocks | CommonMark parsing rules. These are intentional grammar changes. |
| `x^2`, `x^(a + b)` | RStudio inline extension, including inline formatting and reference links. Code and escaped carets stay literal. Nesting is bounded at 16; unmatched parenthesized superscripts do not repeatedly scan the remaining input. |
| Smart punctuation, hard wrapping | `CMARK_OPT_SMART`, `CMARK_OPT_HARDBREAKS`. Smart punctuation now also curls apostrophes in contractions. |
| MathJax, YAML/Pandoc metadata, `htmlPreserve` | Existing RStudio preprocessing/restoration remains shared. Code exclusion also covers tilde fences and up to three leading spaces. |
| Authored HTML in previews, tutorials, presentations | Retained with `CMARK_OPT_UNSAFE`, preserving the existing caller contract. This migration does **not** make those surfaces HTML sanitizers. |
| Help descriptions requesting escaping | Escape raw HTML nodes, remove Markdown images, and preserve generated Markdown formatting. Keep the existing strict URL allowlist. Rejected links keep the formatted label, rather than the original Markdown source. |
| Code highlighting | cmark emits `language-r` / `language-cpp`; the preview detector accepts those as well as the old classes. |
| Presentation image sizing | The media classifier accepts the space before `/>` emitted by cmark. |
| Unused legacy options | `toc`, `skipStyle`, `useXHTML=false`, or disabling `noIntraEmphasis`, `fencedCode`, `laxSpacing`, or `spaceHeaders` returns `not_supported`. There are no such settings in the audited RStudio or local Workbench callers. |

`noIntraEmphasis=true` no longer promises Sundown's rules for asterisks: for
example, `a*b*c` becomes `a<em>b</em>c`, while `snake_case_name` stays literal.
CommonMark also preserves ordered-list starting numbers, handles nested/loose
lists differently, and uses only the first word of a code fence's info string.
Raw R chunk headers such as `{r, echo=FALSE}` consequently render differently if
sent directly to the Markdown parser. R Presentations normally knit these
headers away before this stage.

cmark's built-in safe URL mode is not Sundown's allowlist: it allows relative
URLs and unknown schemes. `safelink` therefore also filters link AST nodes using
the existing `/`, `http://`, `https://`, `ftp://`, and `mailto:` rules, including
the first-character check. No tagfilter or task-list extension is enabled,
because the old renderer did not enable their behavior.

The wrapper serializes rendering because upstream registration **and** inline
parsing mutate global tables. The superscript adapter uses the pinned upstream
inline parser API and parser structure; recheck that interface during upgrades.
Vendor source remains untouched.

## Reproducing comparisons

With unit tests enabled, the `rstudio-markdown` executable reads UTF-8 Markdown
from stdin and emits HTML through the complete `core::markdown` pipeline.
`--safe` applies the settings used for custom help descriptions. It is not
installed with RStudio.

Build one executable with `RSTUDIO_USE_CMARK_GFM=OFF` and one with it `ON` in
separate build directories, then run, for example:

```sh
src/cpp/tools/compare-markdown \
  build-sundown/core/rstudio-markdown \
  build-cmark/core/rstudio-markdown \
  --output build-cmark/comparison \
  README.md src/cpp/session/resources/templates/r_presentation.Rpres
```

The paths above assume configuring from `src/cpp`. Root configurations put
these executables under `src/cpp/core` in the build directory instead. The
comparison tool preserves both HTML outputs and emits token diffs. It ignores
serialization whitespace outside `pre`, entity spelling, attribute ordering,
void-tag slashes, and the known `language-` class prefix. It is a triage aid,
not a browser-layout equivalence test; text whitespace can still be significant.

For presentation comparisons, first knit the shipped template and
`fixtures/presentation.Rpres` to temporary Markdown files with `knitr::knit()`.
Keep generated plots in the temporary directory and compare those Markdown
files. The added fixture covers formatted text, links, lists, tables, code,
plots, superscripts, MathJax, and authored HTML/video.

## Investigation results

Local validation on macOS arm64:

- All nine upstream test suites passed at the pinned revision.
- All 74 vendored files reproduce the upstream bytes exactly.
- The 13 Markdown integration tests passed with cmark; the nine shared tests
  also passed with Sundown. They cover the actual help, preview and presentation
  contracts, extension toggles, concurrent parsers, and bounded superscripts.
- The complete cmark core test run passed 821 tests, with 12 environment/platform
  skips and three disabled tests. The first sandboxed run could not register
  filesystem monitors; the successful run used normal OS access.
- Both changed session translation units passed compilation checks; the actual
  preview/highlighting and presentation media classifiers accepted the old and
  new HTML in a focused harness.
- AddressSanitizer and UndefinedBehaviorSanitizer checks passed with the
  adapter, preprocessing and 34 vendored C translation units instrumented,
  including the knitted presentations, invalid UTF-8, unsafe links, images
  containing markup, and deeply nested/unmatched superscripts.
- Compared the 73 tracked `.md` / `.Rpres` inputs at the starting revision:
  16 were equivalent after the tool's normalization; 52 were equivalent when
  also allowing smart-punctuation differences. Remaining differences included
  emphasis around code spans, nested/loose lists, ordered-list start values,
  HTML comments/blocks, fence info strings, and URL percent-encoding. The corpus
  is not byte-identical and is not a claim of complete visual equivalence.
- The knitted shipped presentation template was equivalent after normalization.
  The expanded presentation fixture differed only in the apostrophe in `don't`.

Before removing the fallback, exercise previews, custom completion help, and
tutorials in the IDE on Windows, Linux, and macOS. R Presentations are soft
deprecated in favor of R Markdown and Quarto presentations, so retain a basic
presentation smoke test without requiring exact Sundown output parity. This
change does not modify Pandoc, knitr, Quarto, or the visual editor's renderer.
