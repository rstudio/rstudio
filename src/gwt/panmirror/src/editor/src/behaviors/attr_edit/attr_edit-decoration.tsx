/*
 * attr_edit-decoration.tsx
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

import { Plugin, PluginKey, Transaction, EditorState } from 'prosemirror-state';
import { DecorationSet, EditorView, Decoration } from 'prosemirror-view';

import { findParentNodeOfType } from 'prosemirror-utils';

import * as React from 'react';

import { EditorUI } from '../../api/ui';
import { AttrEditOptions } from '../../api/attr_edit';
import { CommandFn } from '../../api/command';
import { AttrProps } from '../../api/ui-dialogs';
import { WidgetProps, reactRenderForEditorView } from '../../api/widgets/react';
import { PandocExtensions } from '../../api/pandoc';

import { kEditAttrShortcut } from './attr_edit';
import { attrEditCommandFn } from './attr_edit-command';

import './attr_edit-decoration.css';
import { ImageButton } from '../../api/widgets/button';

interface AttrEditDecorationProps extends WidgetProps {
  tags: string[];
  attrs: AttrProps;
  editFn: CommandFn;
  view: EditorView;
  ui: EditorUI;
}

const AttrEditDecoration: React.FC<AttrEditDecorationProps> = props => {
  const buttonTitle = `${props.ui.context.translateText('Edit Attributes')} (${kEditAttrShortcut})`;

  const onClick = () => {
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
    super({
      key,
      state: {
        init: (_config: { [key: string]: any }) => {
          return DecorationSet.empty;
        },
        apply: (tr: Transaction, old: DecorationSet, _oldState: EditorState, newState: EditorState) => {
          // node types
          const schema = newState.schema;
          const nodeTypes = editors.map(ed => ed.type(schema));

          // provide decoration if selection is contained within one of the node types
          const parentWithAttrs = findParentNodeOfType(nodeTypes)(tr.selection);
          if (!parentWithAttrs) {
            return DecorationSet.empty;
          }

          // get editor
          const editor = editors.find(ed => ed.type(schema) === parentWithAttrs.node.type)!;

          // screen for noDecorator
          if (editor.noDecorator) {
            return DecorationSet.empty;
          }

          // provide some editor defaults
          editor.tags =
            editor.tags ||
            (editorNode => {
              const attrTags = [];
              if (editorNode.attrs.id) {
                attrTags.push(`#${editorNode.attrs.id}`);
              }
              if (editorNode.attrs.classes && editorNode.attrs.classes.length) {
                attrTags.push(`${editorNode.attrs.classes.map((clz: string) => '.' + clz).join(' ')}`);
              }
              return attrTags;
            });
          editor.offset = editor.offset || { top: 0, right: 0 };

          // get editFn
          const editFn = (editorUI: EditorUI) => attrEditCommandFn(editorUI, pandocExtensions, editors);

          // get attrs/tags
          const node = parentWithAttrs.node;
          const attrs = node.attrs;
          const tags = editor.tags(node);

          // attr_edit controls
          const attrEditDecoration = Decoration.widget(
            parentWithAttrs.pos,
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
            },
          );

          // return decorations
          return DecorationSet.create(tr.doc, [attrEditDecoration]);
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
