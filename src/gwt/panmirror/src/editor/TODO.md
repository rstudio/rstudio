## Info

pandoc schema: <https://github.com/jgm/pandoc-types/blob/master/Text/Pandoc/Definition.hs#L94>

## Feedback

## TODO
 

- When a figure is converted into an image b/c a linkTo property was added the image is selected at full width
  (as opposed to just having the image be selected).

- When an image is converted into a figure b/c the linkTo property is removed the image doens't remain selected.
  (maybe want to do a delete then insert for this and the above)

- Put cursor in between image and period with this and you get a link popup, but when you edit it you get no href!
[![](https://rstudio.com/wp-content/uploads/2018/10/RStudio-Logo-Flat.png){width="40px"}.](https://www.rstudio.com)

Can't undo a delete of a node + text (as with above)


Check other uses of markdownOutputFilter (generally doesn't work with tables). example lists, heading links

Discuss tokens / escaping / etc. with Joe. HTML must be inserted manually. Backslashes need to
be escaped (already wrote the code to do this.). Math bears more discussion.....

Consider moving widgets to React now that they are outside the PM dom.

Surface attributes handling for div with only an id (shading treatment a bit much?)

Better direct manipulation for tables

Insert special character UX

Tables with a large number of columns are written as HTML when variable column widths are presented (presumably b/c it can't represent the percentage granularity w/ markdown) Perhaps don't set widths on all of the columns (only ones explicitly sized?)

Visual mode implications for addin API

improve scrolling with: <https://github.com/cferdinandi/smooth-scroll>

MathJax preview

Direct parsing of citations (get rid of special post-processing + supported nested) (note: for nested we need excludes: '').
Only do this if our current method breaks footnotes in citations (if that's even a thing)

## Future

Unit testing for core panmirror code

multimarkdown support is incomplete: -mmd\_title\_block -mmd\_link\_attributes (not written, likely limitation of pandoc) -mmd\_header\_identifiers (work fine, but we currently allow edit of classes + keyvalue for markdown\_mmd)

no support for +pandoc\_title\_block

allow more control over markdown output, particularly list indenting (perhaps get a PR into pandoc to make it flexible)

as with above, make character escaping configurable

We currently can't round-trip reference links (as pandoc doesn't seem to write them, this is not disimillar from the situation w/ inline footnotes so may be fine)

No editing support for fancy list auto-numbering (\#. as list item that is auto-numbered)

MathQuill/MathJax: <https://pboysen.github.io/> <https://discuss.prosemirror.net/t/odd-behavior-with-nodeview-and-atom-node/1521>

critic markup: <http://criticmarkup.com/>

pandoc scholar: <https://pandoc-scholar.github.io/> pandoc jats: <https://github.com/mfenner/pandoc-jats>

Notes on preformance implications of scanning the entire document + some discussion of the tricky nature of doing step by step inspection: <https://discuss.prosemirror.net/t/changed-part-of-document/992> <https://discuss.prosemirror.net/t/reacting-to-node-adding-removing-changing/676> <https://discuss.prosemirror.net/t/undo-and-cursor-position/677/5>

## Known issues:

- When dragging and dropping an image to a place in the document above the original position the shelf sometimes
  stays in it's original position (until you scroll)


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