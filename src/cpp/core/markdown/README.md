# Markdown renderer migration

[Issue #18904](https://github.com/rstudio/rstudio/issues/18904) tracks replacing
Sundown. cmark-gfm is the default renderer; configure with
`-DRSTUDIO_USE_CMARK_GFM=OFF` to build the old renderer for comparison or rollback.
Only the selected renderer is linked. The temporary fallback and its vendored
sources can be removed after platform and IDE-level validation.

## Dependency

`src/cpp/ext/CMakeLists.txt` declares cmark-gfm with the standard
`dependency()` / `fetch()` mechanism. It pins
[`27d942c8b0a62d192f616e5bf3578f4b6a89e180`](https://github.com/github/cmark-gfm/commit/27d942c8b0a62d192f616e5bf3578f4b6a89e180),
a commit after the `0.29.0.gfm.13` release that includes subsequent fixes,
including the autolink pointer-overflow fix at that revision.

The dependency uses `CUSTOM TRUE` to build only the static runtime library,
without upstream's CLI, installation rules or global compiler settings.
`ALWAYS_FETCH TRUE` keeps the adapter tied to the pinned internal parser API;
an arbitrary system version is not interchangeable. The standard
`FETCHCONTENT_SOURCE_DIR_CMARK-GFM` override can supply an existing checkout.
No cmark-gfm source files are stored in this repository.

The external target generates platform/export/version headers in its build
directory. Include paths and static-library definitions are confined to cmark
and its adapter, so upstream headers such as `utf8.h` cannot shadow other
dependencies in core consumers. Updating cmark requires changing its revision
in `src/cpp/ext/CMakeLists.txt`, reviewing the source list and adapter API, and
regenerating dependency notices. Runtime source licenses are BSD-2-Clause and
MIT; the full upstream `COPYING` file supplies the component attributions.

## Compatibility decisions

| Behavior | cmark-gfm integration |
| --- | --- |
| Tables, strikethrough, bare autolinks | Upstream GFM extensions, individually configurable. Require double tildes for strikethrough, matching Sundown. |
| Fenced/indented code, emphasis, lists, HTML blocks | CommonMark parsing rules. These are intentional grammar changes. |
| `x^2`, `x^(a + b)` | RStudio inline extension, including inline formatting and reference links. Bare superscripts resolve after enclosing emphasis and links. Code and escaped carets stay literal. Nesting is bounded at 16; unmatched or depth-limited superscripts do not repeatedly scan the remaining input. |
| Smart punctuation, hard wrapping | `CMARK_OPT_SMART`, `CMARK_OPT_HARDBREAKS`. Smart punctuation now also curls apostrophes in contractions. |
| MathJax, YAML/Pandoc metadata, `htmlPreserve` | RStudio preprocessing remains shared. Code exclusion recognizes matching backtick runs on one line and closing fences at least as long as the opener. Math restoration respects HTML escaping. |
| Authored HTML in previews, tutorials, presentations | Retained with `CMARK_OPT_UNSAFE`, preserving the existing caller contract. This migration does **not** make those surfaces HTML sanitizers. |
| Help descriptions requesting escaping | Escape raw HTML nodes, remove Markdown images, and preserve generated Markdown formatting. Keep the existing strict URL allowlist. Rejected links keep the formatted label, rather than the original Markdown source. |
| Code highlighting | cmark emits `language-r` / `language-cpp`; the preview detector and bundled stylesheets accept those as well as the old classes. |
| Presentation image sizing and columns | Media classification and column splitting accept the space before `/>` emitted by cmark. |
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
Upstream source remains untouched.

The shared MathJax filter retains its existing pattern-based code exclusions.
It does not fully implement CommonMark block and inline grammar: math in
multiline inline-code spans or fences nested in quotes/lists can still be
converted, and backticks in separate table cells can hide an intervening
equation. These cases also fail with the original Sundown pipeline. This
migration fixes matching backtick runs on one line without replacing the legacy
math processor. Fence openers must remain at column zero because the filter
cannot determine where an enclosing list ends. Use top-level fenced code blocks
when literal code contains math delimiters across lines. Metadata is removed
before math processing, and exclusion searches cache future matches to avoid
repeatedly scanning the document suffix after every code span. Only the
earliest fence opener searches for its closer, so overlapping candidates do
not repeatedly scan to the end of the document. When math, HTML and fences
occur together, a cmark prepass supplies top-level HTML block ranges so
fence-like content there cannot hide later equations. The ranges use the HTML
literal's consumed lines, account for CR/LF line endings, and are recomputed
with math masked at the same byte offsets so HTML inside equations does not
hide real code blocks. The final scan accepts a provisional math span only
when it starts before the next code range, discarding spans that begin inside
code before they can hide later fence openers. Both masking and restoration placeholders start with `0x`, avoiding
HTML-tag or ordered-list interpretation that could otherwise depend on UUID
contents. Exclusions honor the pinned parser's 255-character fence limit and
80-character inline-backtick limit.

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
plots, columns, superscripts, MathJax, and authored HTML/video.

## Investigation results

Local validation on macOS arm64:

- All nine upstream test suites passed at the pinned revision.
- The 24 Markdown integration tests passed with cmark; the 13 shared tests
  also passed with Sundown. They cover the actual help, preview and presentation
  contracts, extension toggles, concurrent parsers, and bounded superscripts.
- The complete cmark core test run passed 832 tests, with 12 environment/platform
  skips and three disabled tests. The first sandboxed run could not register
  filesystem monitors; the successful run used normal OS access.
- Both changed session translation units passed compilation checks; the actual
  preview/highlighting, presentation media classification and column splitting
  accepted the old and new HTML in a focused harness.
- AddressSanitizer and UndefinedBehaviorSanitizer checks passed with the
  adapter, preprocessing and 34 upstream C translation units instrumented,
  including the knitted presentations, invalid UTF-8, unsafe links, images
  containing markup, escaped HTML and formatted rejected links in tables,
  math containing HTML, long fences, and deeply nested/unmatched superscripts.
- Compared the 73 tracked `.md` / `.Rpres` inputs at the starting revision:
  16 were equivalent after the tool's normalization; 52 were equivalent when
  also allowing smart-punctuation differences. Remaining differences included
  emphasis around code spans, nested/loose lists, ordered-list start values,
  HTML comments/blocks, fence info strings, and URL percent-encoding. The corpus
  is not byte-identical and is not a claim of complete visual equivalence.
- The knitted shipped presentation template was equivalent after normalization.
  The expanded presentation fixture differed only in the apostrophe in `don't`.

An earlier scan, before switching to the external dependency declaration,
reported nine findings in the unmodified upstream sources. Four concern `src/main.c`, the upstream CLI, which is excluded
from `CMARK_SOURCES` and is not linked into RStudio. Five flag copies reached from
`src/blocks.c` (lines 748, 757, 761, 1462 and 1464 at the pinned revision).
Those paths bound their input slices by the supplied buffer length and copy
through `cmark_strbuf_put`, which grows the destination before `memmove`;
the UTF-8 validation path also uses this buffer helper. These appear to be
static-analysis false positives. The current PR code scan passes with the
external dependency layout; these upstream observations remain documented for
security review. No scan exclusions or upstream source changes were added.

Before removing the fallback, exercise previews, custom completion help, and
tutorials in the IDE on Windows, Linux, and macOS. R Presentations are soft
deprecated in favor of R Markdown and Quarto presentations, so retain a basic
presentation smoke test without requiring exact Sundown output parity. This
change does not modify Pandoc, knitr, Quarto, or the visual editor's renderer.
