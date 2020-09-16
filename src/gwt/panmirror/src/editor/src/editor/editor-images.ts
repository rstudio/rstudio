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
import propertiesDecoImage from './images/properties_deco.png';
import propertiesDecoDarkImage from './images/properties_deco_dark.png';
import removelinkImage from './images/removelink.png';
import runchunkImage from './images/runchunk.png';
import runprevchunksImage from './images/runprevchunks.png';
import searchImage from './images/search.png';
import searchProgressImage from './images/search_progress.gif';

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
import codeBlockImage from './images/omni_insert/code_block.png';
import codeBlockDarkImage from './images/omni_insert/code_block_dark.png';
import footnoteImage from './images/omni_insert/footnote.png';
import footnoteDarkImage from './images/omni_insert/footnote_dark.png';
import citationImage from './images/omni_insert/citation.png';
import citationDarkImage from './images/omni_insert/citation_dark.png';
import crossReferenceImage from './images/omni_insert/cross_reference.png';
import crossReferenceDarkImage from './images/omni_insert/cross_reference_dark.png';
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
import pythonChunkImage from './images/omni_insert/python_chunk.png';
import sqlChunkImage from './images/omni_insert/sql_chunk.png';
import d3ChunkImage from './images/omni_insert/d3_chunk.png';
import stanChunkImage from './images/omni_insert/stan_chunk.png';
import bashChunkImage from './images/omni_insert/bash_chunk.png';
import bashChunkDarkImage from './images/omni_insert/bash_chunk_dark.png';
import rChunkImage from './images/omni_insert/r_chunk.png';
import rChunkDarkImage from './images/omni_insert/r_chunk_dark.png';
import rcppChunkImage from './images/omni_insert/rcpp_chunk.png';
import rcppChunkDarkImage from './images/omni_insert/rcpp_chunk_dark.png';

import articleImage from './images/citations/article.png';
import articleDarkImage from './images/citations/article_dark.png';
import bookImage from './images/citations/book.png';
import bookDarkImage from './images/citations/book_dark.png';
import broadcastImage from './images/citations/broadcast.png';
import broadcastDarkImage from './images/citations/broadcast_dark.png';
import dataImage from './images/citations/data.png';
import dataDarkImage from './images/citations/data_dark.png';
import entryImage from './images/citations/entry.png';
import entryDarkImage from './images/citations/entry_dark.png';
import imageImage from './images/citations/image.png';
import imageDarkImage from './images/citations/image_dark.png';
import legalImage from './images/citations/legal.png';
import legalDarkImage from './images/citations/legal_dark.png';
import mapImage from './images/citations/map.png';
import mapDarkImage from './images/citations/map_dark.png';
import movieImage from './images/citations/movie.png';
import movieDarkImage from './images/citations/movie_dark.png';
import otherImage from './images/citations/other.png';
import otherDarkImage from './images/citations/other_dark.png';
import songImage from './images/citations/song.png';
import songDarkImage from './images/citations/song_dark.png';
import webImage from './images/citations/web.png';
import webDarkImage from './images/citations/web_dark.png';
import zoteroOverlayImage from './images/citations/zotero-overlay.png';
import localSourcesImage from './images/citations/insert/local-sources.png';
import bibliographyImage from './images/citations/insert/bibliography.png';
import bibliographyFolderImage from './images/citations/insert/bibliography-folder.png';
import zoteroRootImage from './images/citations/insert/zotero-root.png';
import zoteroLibraryImage from './images/citations/insert/zotero-library.png';
import zoteroCollectionImage from './images/citations/insert/zotero-collection.png';
import doiImage from './images/citations/insert/doi.png';
import crossRefImage from './images/citations/insert/crossref.png';
import pubmedImage from './images/citations/insert/pubmed.png';
import dataciteImage from './images/citations/insert/datacite.png';

import tagDelete from './images/widgets/tag-delete.png';
import tagEdit from './images/widgets/tag-edit.png';

export function defaultEditorUIImages(): EditorUIImages {
  return {
    copy: copyImage,
    properties: propertiesImage,
    properties_deco: propertiesDecoImage,
    properties_deco_dark: propertiesDecoDarkImage,
    removelink: removelinkImage,
    runchunk: runchunkImage,
    runprevchunks: runprevchunksImage,
    search: searchImage,
    search_progress: searchProgressImage,
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
      code_block: codeBlockImage,
      code_block_dark: codeBlockDarkImage,
      footnote: footnoteImage,
      footnote_dark: footnoteDarkImage,
      citation: citationImage,
      citation_dark: citationDarkImage,
      cross_reference: crossReferenceImage,
      cross_reference_dark: crossReferenceDarkImage,
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
      python_chunk: pythonChunkImage,
      sql_chunk: sqlChunkImage,
      d3_chunk: d3ChunkImage,
      stan_chunk: stanChunkImage,
      bash_chunk: bashChunkImage,
      bash_chunk_dark: bashChunkDarkImage,
      r_chunk: rChunkImage,
      r_chunk_dark: rChunkDarkImage,
      rcpp_chunk: rcppChunkImage,
      rcpp_chunk_dark: rcppChunkDarkImage,
    },
    citations: {
      article: articleImage,
      article_dark: articleDarkImage,
      book: bookImage,
      book_dark: bookDarkImage,
      broadcast: broadcastImage,
      broadcast_dark: broadcastDarkImage,
      data: dataImage,
      data_dark: dataDarkImage,
      entry: entryImage,
      entry_dark: entryDarkImage,
      image: imageImage,
      image_dark: imageDarkImage,
      legal: legalImage,
      legal_dark: legalDarkImage,
      map: mapImage,
      map_dark: mapDarkImage,
      movie: movieImage,
      movie_dark: movieDarkImage,
      other: otherImage,
      other_dark: otherDarkImage,
      song: songImage,
      song_dark: songDarkImage,
      web: webImage,
      web_dark: webDarkImage,
      zoteroOverlay: zoteroOverlayImage,
      local_sources: localSourcesImage,
      bibligraphy: bibliographyImage,
      bibligraphy_folder: bibliographyFolderImage,
      zotero_library: zoteroLibraryImage,
      zotero_collection: zoteroCollectionImage,
      zotero_root: zoteroRootImage,
      doi: doiImage,
      crossref: crossRefImage,
      pubmed: pubmedImage,
      datacite: dataciteImage
    },
    widgets: {
      tag_delete: tagDelete,
      tag_edit: tagEdit
    }
  };
}
