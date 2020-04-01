## Info

pandoc schema: <https://github.com/jgm/pandoc-types/blob/master/Text/Pandoc/Definition.hs#L94>

## Feedback

## TODO

- Add a latching state for RStudio formatting toolbars

- Single line of tex brought in as tex block (convert to inline)

- Clear Raw command for converting raw_inline to text node

- Account for placing latex commands inside code marks (awkward right now)

- Handle insert assignment operator (Alt + -). Note behaves differnetly
  depending on language

TeX handling

- Eliminate current \ escaping special cases
- Break out a special mark type for inline latex
- Create a \[A-Za-z] input rule that gets you into a Tex command
- Create an input rule that fires if you add a second backslash at the beginning of the command
  (the rule would remove the backslash as well as the Tex command formatting)
- Use inclusive: true (and then create an appendTransaction handler that clear the stored mark)

HTML handling

Math handing

- Require an explicit gesture (no scanning) and implement math editor / previewer


Take the Fragment the represents the Cite (Citation+) and parse out the Pandoc tokens. Delimiters are `[]@;` and we need to only respect unescaped versions of those. See about handling citations within citations?

Look into correct handling of non-bracket citations (currently brackets added on intake)

Update docs on Raw

Doc with only YAML block needs paragraph at end

Surface attributes handling for div with only an id (shading treatment a bit much?)

Better direct manipulation for tables

improve scrolling with: <https://github.com/cferdinandi/smooth-scroll>

Consider moving widgets to React now that they are outside the PM dom.

MathJax preview. When containing the selection, the math will show both the code and the preview. When not containing the selection will show the preview. (so probably require a node view for this). Consider a “done” gesture for display math.

Reveal codes / typora behavior

Updated dark mode node selection color from Paul (pending Maria)

## Future

Unit testing for core panmirror code

Insert special character UX

multimarkdown support is incomplete: -mmd\_title\_block -mmd\_link\_attributes (not written, likely limitation of pandoc) -mmd\_header\_identifiers (work fine, but we currently allow edit of classes + keyvalue for markdown\_mmd)

no support for +pandoc\_title\_block

Example lists don't round trip through the AST:
  - (@good) referenced elsewhere via (@good) just becomes a generic example (@) with a literal numeric reference.
  - The writer doesn't preserve the (@) or the (@good) when writing

We currently can't round-trip reference links (as pandoc doesn't seem to write them, this is not disimillar from the situation w/ inline footnotes so may be fine)

allow more control over markdown output, particularly list indenting (perhaps get a PR into pandoc to make it flexible)

as with above, make character escaping configurable

No editing support for fancy list auto-numbering (\#. as list item that is auto-numbered)

Consider special Knit behavior from Visual Mode: execute w/ keep_md and only re-executes R code when the code chunks have actually changed.

MathQuill/MathJax: <https://pboysen.github.io/> <https://discuss.prosemirror.net/t/odd-behavior-with-nodeview-and-atom-node/1521>

critic markup: <http://criticmarkup.com/>

pandoc scholar: <https://pandoc-scholar.github.io/> pandoc jats: <https://github.com/mfenner/pandoc-jats>

Notes on preformance implications of scanning the entire document + some discussion of the tricky nature of doing step by step inspection: <https://discuss.prosemirror.net/t/changed-part-of-document/992> <https://discuss.prosemirror.net/t/reacting-to-node-adding-removing-changing/676> <https://discuss.prosemirror.net/t/undo-and-cursor-position/677/5>

## Known issues:

- When dragging and dropping an image to a place in the document above the original position the shelf sometimes
  stays in it's original position (until you scroll)

- Tables with a large number of columns are written as HTML when variable column widths are presented (presumably b/c it can't represent the percentage    
  granularity w/ markdown) Perhaps don't set widths on all of the columns (only ones explicitly sized?)

- Clear Formatting doesn't play well with table selections (only one of the cells is considered part of the "selected nodes")

## Project/Build

Can we minify the typescript library?

<https://fuse-box.org/docs/getting-started/typescript-project>

Can we combine the editor library w/ the prosemirror/orderedmap dependencies? <https://github.com/fathyb/parcel-plugin-typescript>

<https://stackoverflow.com/questions/44893654/how-do-i-get-typescript-to-bundle-a-3rd-party-lib-from-node-modules>

<https://www.typescriptlang.org/docs/handbook/gulp.html> <https://www.npmjs.com/package/@lernetz/gulp-typescript-bundle>

<https://webpack.js.org/guides/typescript/>

<https://github.com/gulp-community/gulp-concat>

may need to make use of project references (allows mutliple tsconfig.json files that all reference eachother) <https://www.typescriptlang.org/docs/handbook/project-references.html> will ultimately need something like lerna: <https://blog.logrocket.com/setting-up-a-monorepo-with-lerna-for-a-typescript-project-b6a81fe8e4f8/>

create-react-app currently doesn't support project references: <https://github.com/facebook/create-react-app/issues/6799>

simple explanation: <https://stackoverflow.com/questions/51631786/how-to-use-project-references-in-typescript-3-0> <https://gitlab.com/parzh/re-scaled/commit/ca47c1f6195b211ed5d61d2821864c8cecd86bad> <https://www.typescriptlang.org/docs/handbook/project-references.html#structuring-for-relative-modules>