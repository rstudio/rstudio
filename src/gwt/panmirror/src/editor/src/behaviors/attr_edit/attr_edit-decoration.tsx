
/*
 * attr_edit-decoration.tsx
 *
 * Copyright (C) 2019-20 by RStudio, PBC
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
import { Plugin, PluginKey, Transaction, EditorState } from 'prosemirror-state';
import { DecorationSet, EditorView, Decoration } from 'prosemirror-view';

import { findParentNodeOfType } from 'prosemirror-utils';

import * as React from 'react';

import { CommandFn } from '../../api/command';
import { AttrProps, EditorUI } from '../../api/ui';
import { WidgetProps, reactRenderForEditorView } from '../../api/widgets/react';
import { nodeDecorationPosition } from '../../api/widgets/decoration';

import { kEditAttrShortcut } from './attr_edit';
import { editRawBlockCommand } from '../../api/raw';

import './attr_edit-decoration.css';

interface AttrEditDecorationProps extends WidgetProps {
  tag?: string;
  attrs: AttrProps;
  editFn: CommandFn;
  view: EditorView;
  ui: EditorUI;
}

const AttrEditDecoration: React.FC<AttrEditDecorationProps> = props => {
  
  const buttonTitle = `${props.ui.context.translateText('Edit Attributes')} (${kEditAttrShortcut})`;
  
  const suppressMouseDown = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
  };

  const onClick = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    props.editFn(props.view.state, props.view.dispatch, props.view);
  };

  return (
    <div className="pm-attr-edit-decoration pm-surface-widget-text-color " style={props.style}>
      {props.tag ? 
        <div className="attr-edit-tag attr-edit-widget pm-border-background-color">
          <div>
            {props.tag}
          </div>
        </div> 
        : null
      } 
      <div 
        className="attr-edit-button attr-edit-widget pm-border-background-color" 
        title={buttonTitle}
        onMouseDown={suppressMouseDown} 
        onClick={onClick}
      >
        <div>&nbsp;</div>
        <div className="attr-edit-button-ellipses">...</div>
      </div>
    </div>
  );
};


const key = new PluginKey<DecorationSet>('attr_edit_decoration');

export class AttrEditDecorationPlugin extends Plugin<DecorationSet> {
  constructor(schema: Schema, ui: EditorUI, editAttrFn: CommandFn, rawBlocks: boolean) {
    let editorView: EditorView;
    super({
      key,
      view(view: EditorView) {
        editorView = view;
        return {};
      },
      state: {
        init: (_config: { [key: string]: any }) => {
          return DecorationSet.empty;
        },
        apply: (tr: Transaction, old: DecorationSet, _oldState: EditorState, newState: EditorState) => {
          
          // node types
          const nodeTypes = [schema.nodes.heading, schema.nodes.code_block, schema.nodes.div];
          if (rawBlocks) {
            nodeTypes.push(schema.nodes.raw_block);
          }

          // provide decoration if selection is contained within a heading, div, or code block
          const parentWithAttrs = findParentNodeOfType(nodeTypes)(tr.selection);
          if (parentWithAttrs) {

            // get attrs
            const node = parentWithAttrs.node;
            const attrs = node.attrs;

            // create tag (if any)
            const tags = [];
            if (node.type === schema.nodes.raw_block) {
              tags.push(attrs.format);
            } else {
              if (attrs.id) {
                tags.push(`#${attrs.id}`);
              }
              if (attrs.classes && attrs.classes.length) {
                tags.push(`.${attrs.classes[0]}`);
              }
            }
            const tagText = tags.join(' ');

            // create a unique key to avoid recreating the decorator when the selection changes
            const specKey = `attr_edit_decoration_pos:${parentWithAttrs.pos};tag:${tagText}`;

          
            // if the old popup already has a decoration for this key then just use it
            if (old.find(undefined, undefined, spec => spec.key === specKey).length) {
              return old.map(tr.mapping, tr.doc);
            }
          
            // raw blocks have their own edit function
            const editFn = node.type === schema.nodes.raw_block ? editRawBlockCommand(ui) : editAttrFn;

            // headings use an outline rather than a border, so offset for it (it's hard-coded to 6px in heading.css
            // so if this value changes the css must change as well)
            const outlineOffset = node.type === schema.nodes.heading ? 8 : 0;

            // node decorator position
            const decorationPosition = nodeDecorationPosition(
              editorView, 
              parentWithAttrs,
              { // offsets
                top: -7 - outlineOffset,
                right: 5 - outlineOffset
              }
            );

            // no decorator if we couldn't get a position
            if (!decorationPosition) {
              return DecorationSet.empty;
            }

            // create attr edit react component
            const attrEdit = (
              <AttrEditDecoration
                tag={tagText}
                attrs={attrs}
                editFn={editFn}
                view={editorView}
                ui={ui}
                style={decorationPosition.style}
              />
            );

            // create decorator and render attr editor into it
            const decoration = window.document.createElement('div');
            reactRenderForEditorView(attrEdit, decoration, editorView);

            // return decoration
            return DecorationSet.create(tr.doc, [Decoration.widget(decorationPosition.pos, decoration, { key: specKey })]);

          } else {
            return DecorationSet.empty;
          }

          
         
        },
      },
      props: {
        decorations: (state: EditorState) => {
          return key.getState(state);
        },
      },
    });
  }
}
