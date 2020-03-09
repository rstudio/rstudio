## TODO

 Discuss tokens / escaping / etc. with Joe

Images that link to things need to be supported

Images should support width/align for gfm (write raw\_html) (we'd need to do the image tag parsing for this to work). May be as simple as just letting it through (as pandoc seems to automatically write html tags if raw\_html is supported but link\_attributes aren't) Alternatively, we could send inline html through the schema dom parser to see if it has an internal representation

Sizing/alignment inline for images

Clipboard / DragDrop support for images

Surface attributes

Better direct manipulation for tables

Unit testing for core panmirror code

Make character escaping configurable

Link input rule doesn't work for enter after link (<https://discuss.prosemirror.net/t/trigger-inputrule-on-enter/1118/2>)

Cursor location for insert yaml in the middle of paragraph

Tables with a large number of columns are written as HTML when variable column widths are presented (presumably b/c it can't represent the percentage granularity w/ markdown) Perhaps don't set widths on all of the columns (only ones explicitly sized?)

improve scrolling with: <https://github.com/cferdinandi/smooth-scroll>

multimarkdown support is incomplete: -mmd\_title\_block -mmd\_link\_attributes (not written, likely limitation of pandoc) -mmd\_header\_identifiers (work fine, but we currently allow edit of classes + keyvalue for markdown\_mmd)

no support for +pandoc\_title\_block

We currently can't round-trip reference links (as pandoc doesn't seem to write them, this is not disimillar from the situation w/ inline footnotes so may be fine)

showPandocWarnings treatment for RStudio IDE (including localization)

No editing support for fancy list auto-numbering (\#. as list item that is auto-numbered)

handling for div with only an id (shading treatment a bit much?)

Direct parsing of citations (get rid of special post-processing + supported nested) (note: for nested we need excludes: '')

MathQuill/MathJax: <https://pboysen.github.io/> <https://discuss.prosemirror.net/t/odd-behavior-with-nodeview-and-atom-node/1521>

critic markup: <http://criticmarkup.com/>

pandoc scholar: <https://pandoc-scholar.github.io/> pandoc jats: <https://github.com/mfenner/pandoc-jats>

pandoc schema: <https://github.com/jgm/pandoc-types/blob/master/Text/Pandoc/Definition.hs#L94>

Notes on preformance implications of scanning the entire document + some discussion of the tricky nature of doing step by step inspection: <https://discuss.prosemirror.net/t/changed-part-of-document/992> <https://discuss.prosemirror.net/t/reacting-to-node-adding-removing-changing/676> <https://discuss.prosemirror.net/t/undo-and-cursor-position/677/5>

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