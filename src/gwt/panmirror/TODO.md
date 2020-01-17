


## TODO

- Eliminate some toolbar buttons when toolbar gets more narrow

- Cursor location for insert yaml in the middle of paragraph


## Enhancements

Provide some extra vertical space at the bottom when typing at the bottom

Should we consider scanning for heading links the way we scan for citations?
Or should we just get rid of input rules for heading links (and perhaps other 
links outside of plain links) entirely b/c the behavior is so hard to predict
and reason about (and this could be worse than no behavior at all)

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

find/replace
  https://tiptap.scrumpy.io/search-and-replace 
  https://github.com/mattberkowitz/prosemirror-find-replace

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
