/*
 * basekeys.ts
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

import {
  splitBlock,
  liftEmptyBlock,
  createParagraphNear,
  selectNodeBackward,
  joinBackward,
  deleteSelection,
  selectNodeForward,
  joinForward,
  chainCommands,
} from 'prosemirror-commands';
import { undoInputRule } from 'prosemirror-inputrules';
import { keymap } from 'prosemirror-keymap';
import { EditorState, Transaction, Plugin, Selection } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';

import { setTextSelection } from 'prosemirror-utils';

import { CommandFn } from './command';
import { editingRootNodeClosestToPos, editingRootNode } from './node';
import { selectionIsBodyTopLevel } from './selection';

export enum BaseKey {
  Enter = 'Enter',
  ModEnter = 'Mod-Enter',
  ShiftEnter = 'Shift-Enter',
  Backspace = 'Backspace',
  Delete = 'Delete|Mod-Delete', // Use pipes to register multiple commands
  Tab = 'Tab',
  ShiftTab = 'Shift-Tab',
  ArrowUp = 'Up|ArrowUp',
  ArrowDown = 'Down|ArrowDown',
  ArrowLeft = 'Left|ArrowLeft',
  ArrowRight = 'Right|ArrowRight',
  ModArrowUp = 'Mod-Up|Mod-ArrowUp',
  ModArrowDown = 'Mod-Down|Mod-ArrowDown',
  CtrlHome = 'Ctrl-Home',
  CtrlEnd = 'Ctrl-End',
  ShiftArrowLeft = "Shift-Left|Shift-ArrowLeft",
  ShiftArrowRight = "Shift-Right|Shift-ArrowRight",
  AltArrowLeft = "Alt-Left|Alt-ArrowLeft",
  AltArrowRight = "Alt-Right|Alt-ArrowRight",
  CtrlArrowLeft = "Ctrl-Left|Ctrl-ArrowLeft",
  CtrlArrowRight = "Ctrl-Right|Ctrl-ArrowRight",
  CtrlShiftArrowLeft = "Ctrl-Shift-Left|Ctrl-Shift-ArrowLeft",
  CtrlShiftArrowRight = "Ctrl-Shift-Right|Ctrl-Shift-ArrowRight",
}

export interface BaseKeyBinding {
  key: BaseKey;
  command: CommandFn;
}

export function baseKeysPlugin(keys: readonly BaseKeyBinding[]): Plugin {
  // collect all keys
  const pluginKeys = [
    // base enter key behaviors
    { key: BaseKey.Enter, command: splitBlock },
    { key: BaseKey.Enter, command: liftEmptyBlock },
    { key: BaseKey.Enter, command: createParagraphNear },

    // base backspace key behaviors
    { key: BaseKey.Backspace, command: selectNodeBackward },
    { key: BaseKey.Backspace, command: joinBackward },
    { key: BaseKey.Backspace, command: deleteSelection },

    // base delete key behaviors
    { key: BaseKey.Delete, command: selectNodeForward },
    { key: BaseKey.Delete, command: joinForward },
    { key: BaseKey.Delete, command: deleteSelection },

    // base tab key behavior (ignore)
    { key: BaseKey.Tab, command: ignoreKey },
    { key: BaseKey.ShiftTab, command: ignoreKey },

    // base arrow key behavior (prevent traversing top-level body notes)
    { key: BaseKey.ArrowLeft, command: arrowBodyNodeBoundary('left') },
    { key: BaseKey.ArrowUp, command: arrowBodyNodeBoundary('up') },
    { key: BaseKey.ArrowRight, command: arrowBodyNodeBoundary('right') },
    { key: BaseKey.ArrowDown, command: arrowBodyNodeBoundary('down') },
    { key: BaseKey.ModArrowDown, command: endTopLevelBodyNodeBoundary() },
    { key: BaseKey.CtrlEnd, command: endTopLevelBodyNodeBoundary() },

    // merge keys provided by extensions
    ...keys,

    // undoInputRule is always the highest priority backspace key
    { key: BaseKey.Backspace, command: undoInputRule },
  ];

  // build arrays for each BaseKey type
  const commandMap: { [key: string]: CommandFn[] } = {};
  for (const baseKey of Object.values(BaseKey)) {
    commandMap[baseKey] = [];
  }
  pluginKeys.forEach(key => {
    commandMap[key.key].unshift(key.command);
  });

  const bindings: { [key: string]: CommandFn } = {};
  for (const baseKey of Object.values(BaseKey)) {
    const commands = commandMap[baseKey];
    // baseKey may contain multiple keys, separated by |
    for (const subkey of baseKey.split(/\|/)) {
      bindings[subkey] = chainCommands(...commands);
    }
  }

  // return keymap
  return keymap(bindings);
}

function ignoreKey(state: EditorState, dispatch?: (tr: Transaction) => void) {
  return true;
}

function arrowBodyNodeBoundary(dir: 'up' | 'down' | 'left' | 'right') {
  return (state: EditorState, dispatch?: (tr: Transaction<any>) => void, view?: EditorView) => {
    if (view && view.endOfTextblock(dir) && selectionIsBodyTopLevel(state.selection)) {
      const side = dir === 'left' || dir === 'up' ? -1 : 1;
      const $head = state.selection.$head;
      const nextPos = Selection.near(state.doc.resolve(side > 0 ? $head.after() : $head.before()), side);
      const currentRootNode = editingRootNodeClosestToPos($head);
      const nextRootNode = editingRootNodeClosestToPos(nextPos.$head);
      return currentRootNode?.node?.type !== nextRootNode?.node?.type;
    } else {
      return false;
    }
  };
}

function endTopLevelBodyNodeBoundary() {
  return (state: EditorState, dispatch?: (tr: Transaction<any>) => void, view?: EditorView) => {
    const editingNode = editingRootNode(state.selection);
    if (editingNode && selectionIsBodyTopLevel(state.selection)) {
      if (dispatch) {
        const tr = state.tr;
        setTextSelection(editingNode.pos + editingNode.node.nodeSize - 2)(tr).scrollIntoView();
        dispatch(tr);
      }
      return true;
    } else {
      return false;
    }
  };
}
