/*
 * wrap.ts
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

import { Transaction } from "prosemirror-state";
import { Transform } from "prosemirror-transform";

import { split } from 'sentence-splitter';

import { trTransform } from "./transaction";
import { findChildrenByType } from "prosemirror-utils";

export function wrapSentences(tr: Transaction) {
  trTransform(tr, wrapSentencesTransform);
}

function wrapSentencesTransform(tr: Transform) {

  // find all paragraphs in doc
  const schema = tr.doc.type.schema;
  const paragraphs = findChildrenByType(tr.doc, schema.nodes.paragraph);

  // insert linebreaks in paragraphs (go backwards to preserve positions)
  paragraphs.reverse().forEach(paragraph => {

    // don't break sentences inside tight list items
    const $pos = tr.doc.resolve(paragraph.pos);
    const parent = $pos.node($pos.depth);
    if (parent.type === schema.nodes.list_item && $pos.node($pos.depth - 1).attrs.tight) {
      return;
    }

    // break sentences in text
    const parts = split(paragraph.node.textContent);
    parts.reverse().filter(part => part.type === "Sentence").forEach(sentence => {
      // don't break sentence if at least one mark is active
      if (tr.doc.resolve(paragraph.pos + sentence.range[1] + 1).marks().length === 0) {
        const hardBreak = schema.text("\n");
        const hardBreakPos = paragraph.pos + sentence.range[1] + 2;
        tr.insert(hardBreakPos, hardBreak);
      }
    });
  });
}
