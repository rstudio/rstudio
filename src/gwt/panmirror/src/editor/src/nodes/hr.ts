/*
 * hr.ts
 *
 * Copyright (C) 2019-20 by RStudio, Inc.
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

import { Schema } from 'prosemirror-model';
import { InputRule } from 'prosemirror-inputrules';
import { findParentNodeOfType } from 'prosemirror-utils';
import { EditorState } from 'prosemirror-state';

import { ProsemirrorCommand, insertNode, EditorCommandId } from '../api/command';
import { Extension } from '../api/extension';
import { PandocOutput, PandocTokenType } from '../api/pandoc';

import './hr-styles.css';

const extension: Extension = {
  nodes: [
    {
      name: 'horizontal_rule',
      spec: {
        group: 'block',
        parseDOM: [{ tag: 'hr' }],
        toDOM() {
          return ['div', ['hr', { class: 'pm-border-background-color' }]];
        },
      },
      pandoc: {
        readers: [
          {
            token: PandocTokenType.HorizontalRule,
            node: 'horizontal_rule',
          },
        ],
        writer: (output: PandocOutput) => {
          output.writeToken(PandocTokenType.HorizontalRule);
        },
      },
    },
  ],

  commands: (schema: Schema) => {
    return [
      new ProsemirrorCommand(EditorCommandId.HorizontalRule, ['Mod-_'], insertNode(schema.nodes.horizontal_rule)),
    ];
  },

  inputRules: (_schema: Schema) => {
    return [
      new InputRule(/^\*{3}$/, (state: EditorState, match: string[], start: number, end: number) => {
        const schema = state.schema;
        const paraNode = findParentNodeOfType(schema.nodes.paragraph)(state.selection);
        if (paraNode && state.selection.$anchor.depth === 2) {
          // only in top-level paragraphs
          return state.tr.replaceRangeWith(start, end, schema.nodes.horizontal_rule.create());
        } else {
          return null;
        }
      }),
    ];
  },
};

export default extension;
