/*
 * attr_edit-decoration.tsx
 *
 * Copyright (C) 2021 by RStudio, PBC
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

import { Plugin, PluginKey, Transaction, EditorState, Selection, NodeSelection } from 'prosemirror-state';
import { DecorationSet, EditorView, Decoration, WidgetDecorationSpec } from 'prosemirror-view';
import { Node as ProsemirrorNode } from 'prosemirror-model';

import { findChildren } from 'prosemirror-utils';

import * as React from 'react';

import { EditorUI } from '../../api/ui';
import { AttrEditOptions } from '../../api/attr_edit';
import { ImageButton } from '../../api/widgets/button';
import { CommandFn, EditorCommandId } from '../../api/command';
import { AttrProps } from '../../api/ui-dialogs';
import { WidgetProps, reactRenderForEditorView } from '../../api/widgets/react';
import { PandocExtensions } from '../../api/pandoc';
import { transactionsAreTypingChange, forChangedNodes, kSetMarkdownTransaction } from '../../api/transaction';

import { kEditAttrShortcut } from './attr_edit';
import { attrEditNodeCommandFn } from './attr_edit-command';

import { pandocAttrAvailable } from '../../api/pandoc_attr';
import { selectionIsWithinRange } from '../../api/selection';
import './attr_edit-decoration.css';
import { setTextSelection } from 'prosemirror-utils';


interface AttrEditDecorationProps extends WidgetProps {
  tags: string[];
  attrs: AttrProps;
  editFn: CommandFn;
  getPos: () => number;
  view: EditorView;
  ui: EditorUI;
}

const AttrEditDecoration: React.FC<AttrEditDecorationProps> = props => {
  const buttonTitle = `${props.ui.context.translateText('Edit Attributes')} (${kEditAttrShortcut})`;

  const onClick = () => {
    // set selection before invoking function
    if (props.view.dispatch) {
      const pos = props.getPos();
      const node = props.view.state.doc.nodeAt(pos);
      if (node) {
        const tr = props.view.state.tr;
        if (node.type.spec.selectable) {
          tr.setSelection(new NodeSelection(tr.doc.resolve(pos)));
        } else {
          if (!selectionIsWithinRange(tr.selection, { from: pos, to: pos + node.nodeSize })) {
            setTextSelection(pos + 1)(tr);
          }
        }
        props.view.dispatch(tr);
      }
    }
    
    // perform edit
    props.editFn(props.view.state, props.view.dispatch, props.view);
  };

  return (
    <div className="pm-attr-edit-decoration pm-surface-widget-text-color " style={props.style}>
      {props.tags.length
        ? props.tags.map(tag => {
            return (
              <span
                key={tag}
                className="attr-edit-tag attr-edit-widget pm-block-border-color pm-border-background-color"
                onClick={onClick}
              >
                {tag}
              </span>
            );
          })
        : null}
      {props.editFn(props.view.state) ? (
        <ImageButton
          classes={['attr-edit-button']}
          image={props.ui.prefs.darkMode() ? props.ui.images.properties_deco_dark! : props.ui.images.properties_deco!}
          title={buttonTitle}
          tabIndex={-1}
          onClick={onClick}
        />
      ) : null}
    </div>
  );
};

const key = new PluginKey<DecorationSet>('attr_edit_decoration');

export class AttrEditDecorationPlugin extends Plugin<DecorationSet> {
  constructor(ui: EditorUI, pandocExtensions: PandocExtensions, editors: AttrEditOptions[]) {

    const decoratorForNode = (editor: AttrEditOptions, 
                              selection: Selection, 
                              node: ProsemirrorNode, 
                              pos: number) => {

      // if we prefer hidden and have no attributes then bail
      const range = { from: pos, to: pos + node.nodeSize };
      if (editor.preferHidden && 
          !pandocAttrAvailable(node.attrs) && 
          !selectionIsWithinRange(selection, range)) {
        return undefined;
      }

      // provide some editor defaults
      editor.tags = editor.tags ||
        (editorNode => {
          const attrTags = [];
          if (editorNode.attrs.id) {
            attrTags.push(`#${editorNode.attrs.id}`);
          }
          if (editorNode.attrs.classes && editorNode.attrs.classes.length) {
            attrTags.push(`${editorNode.attrs.classes.map((clz: string) => '.' + clz).join(' ')}`);
          }
          if (editorNode.attrs.keyvalue && editorNode.attrs.keyvalue.length) {
            attrTags.push(`${editorNode.attrs.keyvalue.map(
              (kv: [string,string]) => kv[0] + '="' + (kv[1] || '1') + '"').join(' ')}
            `);
          }
          return attrTags;
        });
      editor.offset = editor.offset || { top: 0, right: 0 };

      // get editFn
      const editFn = (editorUI: EditorUI) => attrEditNodeCommandFn(
        { node, pos }, 
        editorUI, 
        pandocExtensions, 
        editors
      );

      // get attrs/tags
      const attrs = node.attrs;
      const tags = editor.tags(node);

      // attr_edit controls
      return Decoration.widget(
        pos,
        (view: EditorView, getPos: () => number) => {
          // does the offsetParent have any right padding we need to offset for?
          // we normally use right: 5px for positioning but that is relative to
          // the edge of the offsetParent. However, some offset parents (e.g. a
          // td or a nested div) have their own internal padding to account for
          // so we look for it here
          let rightPaddingOffset = 0;
          const attrsNode = view.nodeDOM(getPos());
          if (attrsNode) {
            const attrsEl = attrsNode as HTMLElement;
            if (attrsEl.offsetParent) {
              const offsetParentStyle = window.getComputedStyle(attrsEl.offsetParent);
              rightPaddingOffset = -parseInt(offsetParentStyle.paddingRight!, 10) || 0;
            }
          }

          // cacculate position offsets
          const baseOffset = editor.offset || { top: 0, right: 0 };
          const xOffset = baseOffset.right + rightPaddingOffset;
          const yOffset = baseOffset.top + 6;
          const cssProps: React.CSSProperties = {
            transform: `translate(${xOffset}px,-${yOffset}px)`,
          };

          // create attr edit react component
          const attrEdit = (
            <AttrEditDecoration
              tags={tags}
              attrs={attrs}
              editFn={editFn(ui)}
              getPos={getPos}
              view={view}
              ui={ui}
              style={cssProps}
            />
          );

          // create decorator and render attr editor into it
          const decoration = window.document.createElement('div');
          reactRenderForEditorView(attrEdit, decoration, view);

          return decoration;
        },
        {
          // re-use existing instance for same tags
          key: `tags:${tags.join('/')}`,
          ignoreSelection: true,
          stopEvent: () => {
            return true;
          },
          preferHidden: editor.preferHidden
        },
      );
    };

    function decoratorsForDoc(state: EditorState)  {
      const decorations: Decoration[] = [];
      const nodeTypes = editors.map(ed => ed.type(state.schema));
      findChildren(state.doc, node => nodeTypes.includes(node.type), true).forEach(attrNode => {
        const editor = editors.find(ed => ed.type(state.schema) === attrNode.node.type)!;
        if (!editor.noDecorator) {
          const decorator = decoratorForNode(editor, state.selection, attrNode.node, attrNode.pos);
          if (decorator) {
            decorations.push(decorator);
          }
       
        }
      });
      return DecorationSet.create(state.doc, decorations);
    }
    
    super({
      key,
      state: {
        init: (_config: { [key: string]: any }, state: EditorState) => {
          return decoratorsForDoc(state);
        },
        apply: (tr: Transaction, set: DecorationSet, oldState: EditorState, newState: EditorState) => {

          // replacing the entire editor triggers decorations
          if (tr.getMeta(kSetMarkdownTransaction)) {
            return decoratorsForDoc(newState);
          }

          // get schema and nodetypes
          const schema = newState.schema;
          const nodeTypes = editors.map(ed => ed.type(schema));

          // map 
          set = set.map(tr.mapping, tr.doc);

          // typing change, return existing decorations
          if (transactionsAreTypingChange([tr])) {
            return set;
          }

          // selection change, might need to toggle some decorations on/off
          if (tr.selectionSet) {
            
            // look through each decorator, if it has preferHidden, it's node has no attributes,
            // and it's no longer in the selection then remove it
            const preferHiddenDecorators = set.find(undefined, undefined, spec => !!spec.preferHidden);
            for (const dec of preferHiddenDecorators) {
              const node = newState.doc.nodeAt(dec.from);
              if (node && !pandocAttrAvailable(node.attrs)) {
                if (!selectionIsWithinRange(tr.selection, 
                  { from: dec.from, to: dec.from + node.nodeSize })) {
                    set = set.remove([dec]);
                }
              }
            }
         
            // now look for nodes above us with preferHidden and add decorators for them
            const $head = tr.selection.$head;
            for (let i=1; i<=$head.depth; i++) {
              const parentWithAttrs = { node: $head.node(i), pos: $head.before(i) };
              if (!nodeTypes.includes(parentWithAttrs.node.type)) {
                continue;
              }
              const { pos, node } = parentWithAttrs;
              const editor = editors.find(ed => ed.type(schema) === parentWithAttrs.node.type)!;
              if (editor?.preferHidden && set.find(pos, pos).length === 0) {
                const decorator = decoratorForNode(editor, tr.selection, node, pos);
                if (decorator) {
                  set = set.add(tr.doc, [decorator]);
                }
              }
            }
          }

          // doc didn't change, return existing decorations
          if (!tr.docChanged && !tr.storedMarksSet) {
            return set;
          }
        
          // scan for added/modified nodes that have attr_edit decorations
          forChangedNodes(
            oldState,
            newState,
            node => nodeTypes.includes(node.type),
            (node, pos) => {
              // remove existing decorations for changed nodes
              const removeDecorations = set.find(pos, pos, (spec: WidgetDecorationSpec) => {
                return !!spec.key && spec.key.startsWith("tags:");
              });
              if (removeDecorations.length > 0) {
                set = set.remove(removeDecorations);
              }

              // get editor and screen on noDecorator
              const editor = editors.find(ed => ed.type(schema) === node.type)!;
              if (!editor.noDecorator) {
                const decorator = decoratorForNode(editor, newState.selection, node, pos);
                if (decorator) {
                  set = set.add(tr.doc, [decorator]);
                }
              }
            },
          );
          
          // return the updated set
          return set;
        }
      },
      props: {
        decorations: (state: EditorState) => {
          return key.getState(state);
        },
      },
    });
  }
}

