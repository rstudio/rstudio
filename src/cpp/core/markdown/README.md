# Markdown renderer migration

[Issue #18904](https://github.com/rstudio/rstudio/issues/18904) tracks replacing
Sundown. cmark-gfm is the default renderer; configure with
`-DRSTUDIO_USE_CMARK_GFM=OFF` to link the vendored Sundown renderer instead, for
comparing renderer output. Only the selected renderer is linked. The flag swaps
the renderer only: the MathJax, metadata and `htmlPreserve` preprocessing in this
directory is shared by both builds and has changed with the migration, so the
`OFF` build does not reproduce pre-migration output byte for byte. The temporary
fallback and its vendored sources can be removed after platform and IDE-level
validation.

## Dependency

`src/cpp/ext/CMakeLists.txt` declares cmark-gfm with the standard
`dependency()` / `fetch()` mechanism. It pins
[`27d942c8b0a62d192f616e5bf3578f4b6a89e180`](https://github.com/github/cmark-gfm/commit/27d942c8b0a62d192f616e5bf3578f4b6a89e180),
a commit after the `0.29.0.gfm.13` release that includes subsequent fixes,
including the autolink pointer-overflow fix at that revision.

The dependency uses `CUSTOM TRUE` to build only the static runtime library,
without upstream's CLI, installation rules or global compiler settings.
`ALWAYS_FETCH TRUE` builds the pinned revision rather than an arbitrary system
version, since the adapter depends on the extension API and on renderer details
such as the table extension's cell contents. The standard
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
| `x^2`, `x^(a + b)` | RStudio inline extension built on the public extension API. `^(` and `)` are paired through the delimiter stack, like strikethrough, so their contents are parsed by cmark alongside emphasis, links and reference definitions; bare superscripts resolve in an AST pass after parsing. Code and escaped carets stay literal, and carets inside image descriptions remain literal in the `alt` text. Chains of bare superscripts are bounded at 16 levels; unmatched delimiters do not repeatedly scan the remaining input. |
| Smart punctuation, hard wrapping | `CMARK_OPT_SMART`, `CMARK_OPT_HARDBREAKS`. Smart punctuation now also curls apostrophes in contractions. Sundown's `(c)`, `(r)`, `(tm)`, `1/2`, `1/4` and `3/4` replacements are applied to text nodes outside code and image descriptions. |
| MathJax, YAML/Pandoc metadata, `htmlPreserve` | RStudio preprocessing remains shared. Code exclusion recognizes matching backtick runs on one line and closing fences at least as long as the opener. Math restoration respects HTML escaping. |
| Authored HTML in previews, tutorials, presentations | Retained with `CMARK_OPT_UNSAFE`, preserving the existing caller contract. This migration does **not** make those surfaces HTML sanitizers. |
| Help descriptions requesting escaping | Escape raw HTML nodes, remove Markdown images, and preserve generated Markdown formatting. An HTML block becomes a paragraph of escaped text; unlike Sundown, Markdown between its tags is not rendered, because CommonMark recognizes the block before inline parsing. Keep the existing strict URL allowlist. Rejected links keep the formatted label, rather than the original Markdown source. `safelink` only filters link targets, as `HTML_SAFELINK` did; raw HTML is governed by `escape` and `skipHTML`. |
| Code highlighting | cmark emits `language-r` / `language-cpp`; the preview detector and bundled stylesheets accept those as well as the old classes. Braced info strings such as ```` ```{r} ```` or ```` ```{r, echo=FALSE} ```` are reduced to the language, as Sundown did, so `class="language-r"` is emitted. |
| Presentation image sizing and columns | Media classification and column splitting accept the space before `/>` emitted by cmark. |
| Unused legacy options | `toc`, `skipStyle`, `useXHTML=false`, or disabling `noIntraEmphasis`, `fencedCode`, `laxSpacing`, or `spaceHeaders` returns `not_supported`. There are no such settings in the audited RStudio or local Workbench callers. |

`noIntraEmphasis=true` no longer promises Sundown's rules for asterisks: for
example, `a*b*c` becomes `a<em>b</em>c`, while `snake_case_name` stays literal.
CommonMark also preserves ordered-list starting numbers, handles nested/loose
lists differently, uses only the first word of a code fence's info string, and
resolves the innermost of nested links, so `[x^([label][ref])](/outer)` links
`label` rather than the enclosing text.

cmark's built-in safe URL mode is not Sundown's allowlist: it allows relative
URLs and unknown schemes. `safelink` therefore also filters link AST nodes using
the existing `/`, `http://`, `https://`, `ftp://`, and `mailto:` rules, including
the first-character check. No tagfilter or task-list extension is enabled,
because the old renderer did not enable their behavior.

The wrapper serializes rendering because upstream registration **and** inline
parsing mutate global tables. The adapter includes only the public `cmark-gfm.h`,
`cmark-gfm-core-extensions.h` and `cmark-gfm-extension_api.h` headers. Upstream
source remains untouched.

MathJax continues using RStudio's existing pattern-based processor. It shields
column-zero backtick fences; math inside tilde fences is not protected. Other
inherited limits include multiline inline code, fences nested in quotes/lists,
fence-like backticks in raw HTML, and backticks spanning separate table cells.
Use top-level backtick fences when literal code contains math delimiters. A
complete CommonMark-aware math processor is outside this renderer migration.

The retained improvements cover matching backtick runs on one line, backslash
escapes before a backtick, the pinned parser's 80-backtick inline limit and
255-character effective fence limit, HTML escaping during math restoration, and
metadata removal before math processing. Exclusion searches cache future matches
and select the earliest opener before scanning for its closer. Placeholders have
a fixed `0x` prefix so their HTML-tag and ordered-list classification cannot
depend on UUID contents.

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

## Validation

The pull request that introduced the migration records the test runs,
sanitizer checks, corpus comparison and upstream static-analysis notes.

Before removing the fallback, exercise previews, custom completion help, and
tutorials in the IDE on Windows, Linux, and macOS. R Presentations are soft
deprecated in favor of R Markdown and Quarto presentations, so retain a basic
presentation smoke test without requiring exact Sundown output parity. This
change does not modify Pandoc, knitr, Quarto, or the visual editor's renderer.
