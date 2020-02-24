


## TODO

Docs on Math and inline html/latex

Add docs on escaping (derived from inlines, can escape $, \, and <)

Docs on more advanced constructs (insert menu w/ yaml, code chunks, etc.)

  
Observed a situation where a pandoc markdown doc with a footnote was converted (via magic comment) to a gfm
document. Then, deleting the footnote caused the entire doc to be deleted with no ability to undo!

Observed a situation with a table above another table, where attemptign to select and delete the second table (as part of a selection encompassing content before and after the table) resulted in nearly the entire document being removed.

Backspace key seems to be the culprit for both of the above

DeleteRows DeleteCols gestures for table
Insert Multiple Rows for table?

## Enhancements

Images should support width/align for gfm (write raw_html)

Dialog/picker for images

Sizing/alignment inline for images

Surface attributes

Better direct manipulation for tables

Unit testing for core panmirror code

Improved treatment for relative image paths that don't resolve

FindUX in standalone
Magic comments in standalone
Dynamic commands/format for standalone front-end

Cursor location for insert yaml in the middle of paragraph

Provide some extra vertical space at the bottom when typing at the bottom

Should we consider scanning for heading links the way we scan for citations?
Or should we just get rid of input rules for heading links (and perhaps other 
links outside of plain links) entirely b/c the behavior is so hard to predict
and reason about (and this could be worse than no behavior at all). generally,
what sorts of things should be input rules and what should be full on scanners

Mark input rules should screen whether the mark is valid. Or do they even need to?
(i.e. the mark will be prevented)

improve scrolling with: https://github.com/cferdinandi/smooth-scroll

multimarkdown support is incomplete:
   -mmd_title_block
   -mmd_link_attributes (not written, likely limitation of pandoc)
   -mmd_header_identifiers (work fine, but we currently allow edit of classes + keyvalue for markdown_mmd) 

no support for +pandoc_title_block

cmd+click for links (or gdocs style popup)

We currently can't round-trip reference links (as pandoc doesn't seem to write them, this is
not disimillar from the situation w/ inline footnotes so may be fine)

Improve EditorPane.showPandocWarnings treatment (including localization)

No editing support for fancy list auto-numbering (#. as list item that is auto-numbered)

EditorUI.translate call for translatable text

handling for div with only an id (shading treatment a bit much?)

internal links / external links via cmd+click

Direct parsing of citations (get rid of special post-processing + supported nested)
 (note: for nested we need excludes: '')

MathQuill/MathJax: 
   https://pboysen.github.io/
   https://discuss.prosemirror.net/t/odd-behavior-with-nodeview-and-atom-node/1521

critic markup: http://criticmarkup.com/

pandoc scholar: https://pandoc-scholar.github.io/
pandoc jats:    https://github.com/mfenner/pandoc-jats

pandoc schema: https://github.com/jgm/pandoc-types/blob/master/Text/Pandoc/Definition.hs#L94

## Project/Build

Can we minify the typescript library?

https://fuse-box.org/docs/getting-started/typescript-project

Can we combine the editor library w/ the prosemirror/orderedmap dependencies?
https://github.com/fathyb/parcel-plugin-typescript


https://stackoverflow.com/questions/44893654/how-do-i-get-typescript-to-bundle-a-3rd-party-lib-from-node-modules

https://www.typescriptlang.org/docs/handbook/gulp.html
https://www.npmjs.com/package/@lernetz/gulp-typescript-bundle

https://webpack.js.org/guides/typescript/

https://github.com/gulp-community/gulp-concat


may need to make use of project references (allows mutliple tsconfig.json files
that all reference eachother)
   https://www.typescriptlang.org/docs/handbook/project-references.html
will ultimately need something like lerna:
   https://blog.logrocket.com/setting-up-a-monorepo-with-lerna-for-a-typescript-project-b6a81fe8e4f8/

create-react-app currently doesn't support project references:
   https://github.com/facebook/create-react-app/issues/6799

simple explanation:
   https://stackoverflow.com/questions/51631786/how-to-use-project-references-in-typescript-3-0
   https://gitlab.com/parzh/re-scaled/commit/ca47c1f6195b211ed5d61d2821864c8cecd86bad
   https://www.typescriptlang.org/docs/handbook/project-references.html#structuring-for-relative-modules