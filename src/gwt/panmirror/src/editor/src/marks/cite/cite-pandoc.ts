import {
  PandocAst,
  PandocAstOutputFilterUtil,
  PandocToken,
  PandocTokenType,
  forEachToken,
  mapTokens,
  PandocOutput,
} from '../../api/pandoc';
import { Mark, Fragment } from 'prosemirror-model';
import { uuidv4 } from '../../api/util';

const kCiteInlinesClass = '396637E5-93B6-40D3-A07D-DE2F65569267';

export function citePandocWriter(output: PandocOutput, _mark: Mark, parent: Fragment) {
  output.writeToken(PandocTokenType.Span, () => {
    output.writeAttr('id-' + uuidv4(), [kCiteInlinesClass], []);
    output.writeArray(() => {
      output.writeInlines(parent);
    });
  });
}

export async function citePandocAstOutputFilter(ast: PandocAst, util: PandocAstOutputFilterUtil): Promise<PandocAst> {
  const CITATION_ATTR = 0;
  const CITATION_ATTR_ID = 0;
  const CITATION_ATTR_CLASSES = 1;
  const CITATION_INLINES = 1;

  function isCiteInlines(tok: PandocToken) {
    return tok.t === PandocTokenType.Span && tok.c[CITATION_ATTR][CITATION_ATTR_CLASSES].includes(kCiteInlinesClass);
  }

  // find all the citation inline content (we need to have pandoc parse
  // it as markdown to determine whehter it's still a valid ciation)
  const citations: { [key: string]: string } = {};
  forEachToken(ast.blocks, (tok: PandocToken) => {
    if (isCiteInlines(tok)) {
      citations[tok.c[CITATION_ATTR][CITATION_ATTR_ID]] = tok.c[CITATION_INLINES];
    }
  });

  // short-circuit return if there are no citations
  if (Object.keys(citations).length === 0) {
    return ast;
  }

  // translate citations into markdown. here we need to use a markdown
  // renderer w/o support for citations so the @ characters don't get
  // escaped within the citation
  const citationBlocks = Object.keys(citations).map(id => {
    return {
      t: PandocTokenType.Div,
      c: [[id, [], []], [{ t: PandocTokenType.Para, c: citations[id] }]],
    };
  });
  const citationsAst = { ...ast, blocks: citationBlocks };
  let citationsMarkdown = await util.astToMarkdown(citationsAst, '-citations');

  // remove escape characters from citations
  // end brackets
  citationsMarkdown = citationsMarkdown.replace(/\n\\\[/g, '\n[');
  citationsMarkdown = citationsMarkdown.replace(/\\\]\n/g, ']\n');
  // inline author suffixes - e.g.  @smith04 \[p. 33\]. note that
  // we only look for the beginning part b/c the replace above
  // would have already taken care of the end
  citationsMarkdown = citationsMarkdown.replace(/(\n@[^ ]+ )\\\[/g, '$1[');
  // escaped semicolons
  citationsMarkdown = citationsMarkdown.replace('\\\\;', '\\;');

  // parse the markdown via pandoc
  const citationsMarkdownAst = await util.markdownToAst(citationsMarkdown);

  // extract out the parsed ast tokens
  const citationsTokens: { [key: string]: PandocToken } = {};
  citationsMarkdownAst.blocks.forEach(block => {
    const id = block.c[0][0];
    const tokenContainer = block.c[1][0].c;
    // if it was a valid citation then it's a single 'cite' token
    if (tokenContainer.length === 1) {
      citationsTokens[id] = tokenContainer[0];
      // otherwise it's an array of inlines, wrap in a do-nothing span
      // so that it's still a single item
    } else {
      citationsTokens[id] = {
        t: PandocTokenType.Span,
        c: [['', [], []], tokenContainer],
      };
    }
  });

  // substitute citations
  const blocks = mapTokens(ast.blocks, (tok: PandocToken) => {
    if (isCiteInlines(tok)) {
      const id = tok.c[CITATION_ATTR][CITATION_ATTR_ID];
      return citationsTokens[id];
    } else {
      return tok;
    }
  });

  // return revised ast
  return {
    ...ast,
    blocks,
  };
}
