/*
 * editor-menus.ts
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

import { EditorMenuItem, EditorUI } from '../api/ui';
import { tableMenu } from '../api/table';
import { EditorCommandId, EditorCommand } from '../api/command';
import { Editor } from './editor';

export interface EditorMenus {
  format: EditorMenuItem[];
  insert: EditorMenuItem[];
  table: EditorMenuItem[];
}

export function editorMenus(ui: EditorUI, commands: EditorCommand[]) {
  return {
    format: formatMenu(ui, commands),
    insert: insertMenu(ui, commands),
    table: tableMenu(true, ui),
  };
}

function formatMenu(ui: EditorUI, commands: EditorCommand[]) {
  return [
    { command: EditorCommandId.Strong },
    { command: EditorCommandId.Em },
    { command: EditorCommandId.Code },
    {
      text: ui.context.translateText('Text'),
      subMenu: {
        items: [
          { command: EditorCommandId.Strikeout },
          { command: EditorCommandId.Superscript },
          { command: EditorCommandId.Subscript },
          { command: EditorCommandId.Smallcaps },
        ],
      },
    },
    { separator: true },
    {
      text: ui.context.translateText('Bullets & Numbering'),
      subMenu: {
        items: [
          { command: EditorCommandId.BulletList },
          { command: EditorCommandId.OrderedList },
          { separator: true },
          { command: EditorCommandId.TightList },
          { separator: true },
          { command: EditorCommandId.ListItemCheck },
          { command: EditorCommandId.ListItemCheckToggle },
          { separator: true },
          { command: EditorCommandId.ListItemSink },
          { command: EditorCommandId.ListItemLift },
          { separator: true },
          { command: EditorCommandId.EditListProperties },
        ],
      },
    },
    { separator: true },
    { command: codeBlockCommand(commands) },
    { command: EditorCommandId.Blockquote },
    { command: EditorCommandId.LineBlock },
    { separator: haveAnyOf(commands, EditorCommandId.Div, EditorCommandId.Span) },
    { command: EditorCommandId.Div },
    { command: EditorCommandId.Span },
    { separator: true },
    {
      text: ui.context.translateText('Raw'),
      subMenu: {
        items: [
          { command: EditorCommandId.HTMLInline },
          { command: EditorCommandId.HTMLBlock },
          { separator: true },
          { command: EditorCommandId.TexInline },
          { command: EditorCommandId.TexBlock },
          { separator: true },
          { command: EditorCommandId.RawInline },
          { command: EditorCommandId.RawBlock },
        ],
      },
    },
    { separator: true },
    { command: EditorCommandId.ClearFormatting },
    { separator: true },
    { command: EditorCommandId.AttrEdit },
  ];
}

function insertMenu(ui: EditorUI, commands: EditorCommand[]) {
  return [
    { command: EditorCommandId.OmniInsert },
    ...(haveAnyOf(commands, EditorCommandId.RCodeChunk, EditorCommandId.PythonCodeChunk)
      ? [
        { separator: true },
        {
          text: ui.context.translateText('Code Chunk'),
          subMenu: {
            items: [
              { command: EditorCommandId.RCodeChunk },
              { separator: true },
              { command: EditorCommandId.PythonCodeChunk },
              { command: EditorCommandId.BashCodeChunk },
              { command: EditorCommandId.RcppCodeChunk },
              { command: EditorCommandId.SQLCodeChunk },
              { command: EditorCommandId.D3CodeChunk },
              { command: EditorCommandId.StanCodeChunk },
            ],
          },
        },
      ]
      : []),
    { separator: true },
    { command: EditorCommandId.Citation },
    { command: EditorCommandId.CrossReference },
    { command: EditorCommandId.Footnote },
    { separator: true },
    { command: EditorCommandId.Image },
    { command: EditorCommandId.Link },
    { command: EditorCommandId.HorizontalRule },
    ...(haveAnyOf(commands, EditorCommandId.DefinitionList)
      ? [
        { separator: true },
        {
          text: ui.context.translateText('Definition'),
          subMenu: {
            items: [
              { command: EditorCommandId.DefinitionList },
              { separator: true },
              { command: EditorCommandId.DefinitionTerm },
              { command: EditorCommandId.DefinitionDescription },
            ],
          },
        },
      ]
      : []),
    { separator: true },
    { command: EditorCommandId.InlineMath },
    { command: EditorCommandId.DisplayMath },
    { separator: true },
    {
      text: ui.context.translateText('Special Characters'),
      subMenu: {
        items: [
          { command: EditorCommandId.Emoji },
          { command: EditorCommandId.Symbol },
          { separator: true },
          { command: EditorCommandId.EnDash },
          { command: EditorCommandId.EmDash },
          { separator: true },
          { command: EditorCommandId.NonBreakingSpace },
          { separator: true },
          { command: EditorCommandId.HardLineBreak }
        ],
      },
    },
    { separator: true },
    { command: EditorCommandId.ParagraphInsert },
    { command: EditorCommandId.CodeBlockFormat },
    { command: EditorCommandId.InsertDiv },
    { command: EditorCommandId.YamlMetadata },
    { separator: true },
    { command: EditorCommandId.Shortcode },
    { separator: true },
    { command: EditorCommandId.HTMLComment },
  ];
}

function haveAnyOf(commands: EditorCommand[], ...ids: EditorCommandId[]) {
  for (const command of commands) {
    if (ids.includes(command.id)) {
      return true;
    }
  }
  return false;
}

function codeBlockCommand(commands: EditorCommand[]) {
  return haveAnyOf(commands, EditorCommandId.CodeBlockFormat)
    ? EditorCommandId.CodeBlockFormat
    : EditorCommandId.CodeBlock;
}
