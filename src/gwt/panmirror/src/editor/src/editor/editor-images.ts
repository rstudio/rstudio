/*
 * editor-images.ts
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

import { EditorUIImages } from '../api/ui-images';

import copyImage from './images/copy.png';
import propertiesImage from './images/properties.png';
import removelinkImage from './images/removelink.png';
import runchunkImage from './images/runchunk.png';
import runprevchunksImage from './images/runprevchunks.png';
import searchImage from './images/search.png';

import omniInsertGenericImage from './images/omni_insert/generic.png';
import heading1Image from './images/omni_insert/heading1.png';
import heading1DarkImage from './images/omni_insert/heading1_dark.png';
import heading2Image from './images/omni_insert/heading2.png';
import heading2DarkImage from './images/omni_insert/heading2_dark.png';
import heading3Image from './images/omni_insert/heading3.png';
import heading3DarkImage from './images/omni_insert/heading3_dark.png';
import heading4Image from './images/omni_insert/heading4.png';
import heading4DarkImage from './images/omni_insert/heading4_dark.png';
import bulletListImage from './images/omni_insert/bullet_list.png';
import bulletListDarkImage from './images/omni_insert/bullet_list_dark.png';
import orderedListImage from './images/omni_insert/ordered_list.png';
import orderedListDarkImage from './images/omni_insert/ordered_list_dark.png';
import blockquoteImage from './images/omni_insert/blockquote.png';
import blockquoteDarkImage from './images/omni_insert/blockquote_dark.png';
import mathInlineImage from './images/omni_insert/math_inline.png';
import mathInlineDarkImage from './images/omni_insert/math_inline_dark.png';
import mathDisplayImage from './images/omni_insert/math_display.png';
import mathDisplayDarkImage from './images/omni_insert/math_display_dark.png';
import htmlBlockImage from './images/omni_insert/html_block.png';
import htmlBlockDarkImage from './images/omni_insert/html_block_dark.png';
import lineBlockImage from './images/omni_insert/line_block.png';
import lineBlockDarkImage from './images/omni_insert/line_block_dark.png';
import emojiImage from './images/omni_insert/emoji.png';
import emojiDarkImage from './images/omni_insert/emoji_dark.png';
import commentImage from './images/omni_insert/comment.png';
import commentDarkImage from './images/omni_insert/comment_dark.png';
import divImage from './images/omni_insert/div.png';
import divDarkImage from './images/omni_insert/div_dark.png';
import footnoteImage from './images/omni_insert/footnote.png';
import footnoteDarkImage from './images/omni_insert/footnote_dark.png';
import symbolImage from './images/omni_insert/symbol.png';
import symbolDarkImage from './images/omni_insert/symbol_dark.png';
import tableImage from './images/omni_insert/table.png';
import tableDarkImage from './images/omni_insert/table_dark.png';
import definitionListImage from './images/omni_insert/definition_list.png';
import definitionListDarkImage from './images/omni_insert/definition_list_dark.png';
import horizontalRuleImage from './images/omni_insert/horizontal_rule.png';
import horizontalRuleDarkImage from './images/omni_insert/horizontal_rule_dark.png';
import imgImage from './images/omni_insert/image.png';
import imgDarkImage from './images/omni_insert/image_dark.png';
import linkImage from './images/omni_insert/link.png';
import linkDarkImage from './images/omni_insert/link_dark.png';
import paragraphImage from './images/omni_insert/paragraph.png';
import paragraphDarkImage from './images/omni_insert/paragraph_dark.png';
import rawBlockImage from './images/omni_insert/raw_block.png';
import rawBlockDarkImage from './images/omni_insert/raw_block_dark.png';
import rawInlineImage from './images/omni_insert/raw_inline.png';
import rawInlineDarkImage from './images/omni_insert/raw_inline_dark.png';
import texBlockImage from './images/omni_insert/tex_block.png';
import texBlockDarkImage from './images/omni_insert/tex_block_dark.png';
import yamlBlockImage from './images/omni_insert/yaml_block.png';
import yamlBlockDarkImage from './images/omni_insert/yaml_block_dark.png';

export function defaultEditorUIImages(): EditorUIImages {
  return {
    copy: copyImage,
    properties: propertiesImage,
    removelink: removelinkImage,
    runchunk: runchunkImage,
    runprevchunks: runprevchunksImage,
    search: searchImage,
    omni_insert: {
      generic: omniInsertGenericImage,
      heading1: heading1Image,
      heading1_dark: heading1DarkImage,
      heading2: heading2Image,
      heading2_dark: heading2DarkImage,
      heading3: heading3Image,
      heading3_dark: heading3DarkImage,
      heading4: heading4Image,
      heading4_dark: heading4DarkImage,
      bullet_list: bulletListImage,
      bullet_list_dark: bulletListDarkImage,
      ordered_list: orderedListImage,
      ordered_list_dark: orderedListDarkImage,
      blockquote: blockquoteImage,
      blockquote_dark: blockquoteDarkImage,
      math_inline: mathInlineImage,
      math_inline_dark: mathInlineDarkImage,
      math_display: mathDisplayImage,
      math_display_dark: mathDisplayDarkImage,
      html_block: htmlBlockImage,
      html_block_dark: htmlBlockDarkImage,
      line_block: lineBlockImage,
      line_block_dark: lineBlockDarkImage,
      emoji: emojiImage,
      emoji_dark: emojiDarkImage,
      comment: commentImage,
      comment_dark: commentDarkImage,
      div: divImage,
      div_dark: divDarkImage,
      footnote: footnoteImage,
      footnote_dark: footnoteDarkImage,
      symbol: symbolImage,
      symbol_dark: symbolDarkImage,
      table: tableImage,
      table_dark: tableDarkImage,
      definition_list: definitionListImage,
      definition_list_dark: definitionListDarkImage,
      horizontal_rule: horizontalRuleImage,
      horizontal_rule_dark: horizontalRuleDarkImage,
      image: imgImage,
      image_dark: imgDarkImage,
      link: linkImage,
      link_dark: linkDarkImage,
      paragraph: paragraphImage,
      paragraph_dark: paragraphDarkImage,
      raw_block: rawBlockImage,
      raw_block_dark: rawBlockDarkImage,
      raw_inline: rawInlineImage,
      raw_inline_dark: rawInlineDarkImage,
      tex_block: texBlockImage,
      tex_block_dark: texBlockDarkImage,
      yaml_block: yamlBlockImage,
      yaml_block_dark: yamlBlockDarkImage,
    },
  };
}
