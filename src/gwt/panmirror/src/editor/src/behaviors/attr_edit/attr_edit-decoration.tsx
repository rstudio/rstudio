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

import { Plugin, PluginKey, Transaction, EditorState } from 'prosemirror-state';
import { DecorationSet, EditorView, Decoration } from 'prosemirror-view';

import { findParentNodeOfType } from 'prosemirror-utils';

import * as React from 'react';

import { EditorUI } from '../../api/ui';
import { AttrEditOptions } from "../../api/attr_edit";
import { CommandFn } from '../../api/command';
import { AttrProps } from '../../api/ui';
import { WidgetProps, reactRenderForEditorView } from '../../api/widgets/react';
import { nodeDecoration } from '../../api/decoration';

import { kEditAttrShortcut } from './attr_edit';
import { attrEditCommandFn } from './attr_edit-command';

import './attr_edit-decoration.css';

interface AttrEditDecorationProps extends WidgetProps {
  tags: string[];
  attrs: AttrProps;
  editFn: CommandFn;
  view: EditorView;
  ui: EditorUI;
}

const AttrEditDecoration: React.FC<AttrEditDecorationProps> = props => {
  
  const buttonTitle = `${props.ui.context.translateText('Edit Attributes')} (${kEditAttrShortcut})`;
  
  const onClick = (e: React.MouseEvent) => {
    props.editFn(props.view.state, props.view.dispatch, props.view);
  };

  return (
    <div className="pm-attr-edit-decoration pm-surface-widget-text-color " style={props.style}>
      {props.tags.length ? 
        props.tags.map(tag => {
          return (
            <div key={tag} className="attr-edit-tag attr-edit-widget pm-block-border-color pm-border-background-color">
              <div>{tag}</div>
            </div> 
          );
        })
        : null
      } 
      <div 
        className="attr-edit-button attr-edit-widget pm-block-border-color pm-border-background-color" 
        title={buttonTitle}
        onClick={onClick}
      >
        <div style={{visibility: 'hidden'}}>....</div>
        <div className="attr-edit-button-ellipses">...</div>
      </div>
    </div>
  );
};

const key = new PluginKey<DecorationSet>('attr_edit_decoration');

class AttrEditDecorationPlugin extends Plugin<DecorationSet> {
  constructor(ui: EditorUI, editors: AttrEditOptions[]) {
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
          const schema = newState.schema;
          const nodeTypes = editors.map(editor => editor.type(schema));

          // provide decoration if selection is contained within a heading, div, or code block
          const parentWithAttrs = findParentNodeOfType(nodeTypes)(tr.selection);
          if (parentWithAttrs) {

            // get editor options + provide defaults
            const editor = editors.find(ed=> ed.type(schema) === parentWithAttrs.node.type)!;
            editor.tags = editor.tags || ((editorNode) => {
              const attrTags = [];
              if (editorNode.attrs.id) {
                attrTags.push(`#${editorNode.attrs.id}`);
              }
              if (editorNode.attrs.classes && editorNode.attrs.classes.length) {
                attrTags.push(`.${editorNode.attrs.classes[0]}`);
              }
              return attrTags;
            });
            editor.editFn = editor.editFn || attrEditCommandFn;
            editor.offset = editor.offset || (() => 0);

            // get attrs/tags
            const node = parentWithAttrs.node;
            const attrs = node.attrs;
            const tags = editor.tags(node);

            // create a unique key to avoid recreating the decorator when the selection changes
            const specKey = `attr_edit_decoration_pos:${parentWithAttrs.pos}tags:${tags.join('/')}`;
        
            // if the old popup already has a decoration for this key then just use it
            if (old.find(undefined, undefined, spec => spec.key === specKey).length) {
              return old.map(tr.mapping, tr.doc);
            }

            // does the offsetParent have any right padding we need to offset for?
            // we normally use right: 5px for positioning but that is relative to
            // the edge of the offsetParent. However, some offset parents (e.g. a 
            // td or a nested div) have their own internal padding to account for
            // so we look for it here
            let rightPaddingOffset = 0;
            const attrsNode = editorView.nodeDOM(parentWithAttrs.pos);
            if (attrsNode) {
              const attrsEl = attrsNode as HTMLElement;
              if (attrsEl.offsetParent) {
                const offsetParentStyle = window.getComputedStyle(attrsEl.offsetParent);
                rightPaddingOffset = -parseInt(offsetParentStyle.paddingRight!, 10) || 0;
              }
            }
          
            // cacculate position offsets
            const offset = editor.offset();
            const xOffset = rightPaddingOffset + offset;
            const yOffset = (13 / 2) + 1 + offset; // 13 is from height defined in attr_edit-decoration.css
            const cssProps: React.CSSProperties = {
              transform: `translate(${xOffset}px,-${yOffset}px)`
            };

            // create attr edit react component
            const attrEdit = (
              <AttrEditDecoration
                tags={tags}
                attrs={attrs}
                editFn={editor.editFn(ui)}
                view={editorView}
                ui={ui}
                style={cssProps}
              />
            );

            // create decorator and render attr editor into it
            const decoration = window.document.createElement('div');
            reactRenderForEditorView(attrEdit, decoration, editorView);

            // decorations to return
            const decorations: Decoration[] = [];

            // add classes 
            if (editor.classes) {
              decorations.push(nodeDecoration(
                parentWithAttrs, 
                { class: editor.classes().join(' ')}
              ));
            }
            
            // attr_edit controls
            decorations.push(Decoration.widget(parentWithAttrs.pos, decoration, 
              { 
                key: specKey,
                ignoreSelection: true,
                stopEvent: () => {
                  return true;
                }
              })
            );

            // return decorations
            return DecorationSet.create(tr.doc, decorations);

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


export function attrEditDecorationPlugin(ui: EditorUI, editors: AttrEditOptions[]) {
  return new AttrEditDecorationPlugin(ui, editors);
}





