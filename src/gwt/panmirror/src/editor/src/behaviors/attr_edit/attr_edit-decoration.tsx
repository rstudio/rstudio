
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

import './attr_edit-decoration.css';
import { kEditAttrShortcut } from './attr_edit';
import { editRawBlockCommand } from '../../api/raw';


interface AttrEditButtonProps extends WidgetProps {
  title: string;
  onClick: VoidFunction;
}

const AttrEditButton: React.FC<AttrEditButtonProps> = props => {
  
  const onMouseDown = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
  };

  const onClick = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    props.onClick();
  };
  
  return (
    <div className="button-container" title={props.title} onMouseDown={onMouseDown} onClick={onClick}>
      <div className="pm-border-background-color button-background"></div>
      <div className="pm-inverted-text-color button-ellipse">...</div>
    </div>
  );
};

interface AttrEditDecorationProps extends WidgetProps {
  attrs: AttrProps;
  editAttrFn: CommandFn;
  view: EditorView;
  ui: EditorUI;
}

const AttrEditDecoration: React.FC<AttrEditDecorationProps> = props => {
  
  const buttonTitle = `${props.ui.context.translateText('Edit Attributes')} (${kEditAttrShortcut})`;

  const onEditAttrClick = () => {
    props.editAttrFn(props.view.state, props.view.dispatch, props.view);
  };
  
  return (
    <div className="pm-attr-edit-decoration" style={props.style}>
       <AttrEditButton title={buttonTitle} onClick={onEditAttrClick}/>
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
        apply: (tr: Transaction, _old: DecorationSet, _oldState: EditorState, newState: EditorState) => {
          
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
            const attrs = node.attrs as AttrProps;

            // raw blocks have their own edit function
            if (node.type === schema.nodes.raw_block) {
              editAttrFn = editRawBlockCommand(ui);
            }

            // headings use an outline rather than a border, so offset for it (it's hard-coded to 6px in heading.css
            // so if this value changes the css must change as well)
            const outlineOffset = node.type === schema.nodes.heading ? 6 : 0;

            // node decorator position
            const decorationPosition = nodeDecorationPosition(
              editorView, 
              parentWithAttrs,
              { // offsets
                top: -9 - outlineOffset,
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
                attrs={attrs}
                editAttrFn={editAttrFn}
                view={editorView}
                ui={ui}
                style={decorationPosition.style}
              />
            );

            // create decorator and render attr editor into it
            const decoration = window.document.createElement('div');
            reactRenderForEditorView(attrEdit, decoration, editorView);

            // return decoration
            return DecorationSet.create(tr.doc, [Decoration.widget(decorationPosition.pos, decoration)]);

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
