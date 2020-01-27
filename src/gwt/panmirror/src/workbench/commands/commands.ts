/*
 * commands.ts
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

import { IconName, MaybeElement } from '@blueprintjs/core';

import { EditorCommandId } from 'editor/src/api/command';

import { toKeyCode, keyCodeString } from './keycodes';

export enum WorkbenchCommandId {
  Cut = '2A311A8B-0302-4CC7-A635-2778CA5006B8',
  Copy = 'A2F6CCE0-CF01-4711-BD97-17110694A41A',
  Paste = '7E084FF6-83C6-47E3-B98F-928D17571B8A',
  ActivateEditor = '7C9ED183-CEF0-43E5-95FC-079863290EEC',
  ShowOutline = 'FCDADCF6-4EDE-41B2-A34D-0A3F23ECFDFD',
  ShowMarkdown = '7F5FB6B9-7359-4B33-AD7E-4ED81CCE1DCB',
  Rename = '75C0E53F-19B5-4F63-AFDE-D259F4E634EE',
  Print = '885D0AFD-D2D1-4878-B85A-E0617A7A4239',
  KeyboardShortcuts = '44AF49DC-E403-46B4-AC66-7FEED4E15C0E',
  EnableDevTools = '87902997-C95B-4DF1-85C5-303DC0FA33B8',
}

export type CommandId = EditorCommandId | WorkbenchCommandId;

export interface Command {
  // unique  id
  readonly id: CommandId;

  // text for menu
  readonly menuText: string;

  // group (for display in keyboard shortcuts dialog)
  readonly group: string;

  // optional blueprint icon for toolbar/menu
  readonly icon?: IconName | MaybeElement;

  // keys to bind to
  readonly keymap: readonly string[];

  // don't bind the keys (they are handled by another component e.g. prosemirror)
  readonly keysUnbound?: boolean;

  // don't show the keys in the keyboard shortcuts dialogs
  readonly keysHidden?: boolean;

  // is the command available?
  isEnabled: () => boolean;

  // is it active/latched
  isActive: () => boolean;

  // execute the command
  execute: () => void;
}

export function commandKeymapText(command: Command, pretty: boolean) {
  if (command.keymap.length) {
    const keyCode = toKeyCode(command.keymap[0]);
    return keyCodeString(keyCode, pretty);
  } else {
    return '';
  }
}

export function commandTooltipText(command: Command) {
  let text = command.menuText;
  const keymapText = commandKeymapText(command, true);
  if (keymapText) {
    text = `${text} (${keymapText})`;
  }
  return text;
}
