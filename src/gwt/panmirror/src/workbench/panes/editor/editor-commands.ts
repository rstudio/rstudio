/*
 * editor-commands.ts
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

import { t } from 'i18n';

import printJS from 'print-js';

import applyDevTools from 'prosemirror-dev-tools';

import { MaybeElement } from '@blueprintjs/core';

import { IconName, IconNames } from '@blueprintjs/icons';

import { Editor } from 'editor/src/editor';
import { EditorCommand, EditorCommandId } from 'editor/src/api/command';

import { Command, WorkbenchCommandId } from 'workbench/commands/commands';

interface CommandDefs {
  [group: string]: {
    [name in EditorCommandId]?: { icon?: IconName | MaybeElement; menuText: string; keysHidden?: boolean };
  };
}

function editorCommandDefs(): CommandDefs {
  return {
    [t('commands:group_text_editing')]: {
      [EditorCommandId.Undo]: {
        icon: IconNames.UNDO,
        menuText: t('commands:undo_menu_text'),
      },
      [EditorCommandId.Redo]: {
        icon: IconNames.REDO,
        menuText: t('commands:redo_menu_text'),
      },
      [EditorCommandId.SelectAll]: {
        menuText: t('commands:select_all_menu_text'),
      },
    },
    [t('commands:group_formatting')]: {
      [EditorCommandId.Strong]: {
        icon: IconNames.BOLD,
        menuText: t('commands:strong_menu_text'),
      },
      [EditorCommandId.Em]: {
        icon: IconNames.ITALIC,
        menuText: t('commands:em_menu_text'),
      },
      [EditorCommandId.Code]: {
        icon: IconNames.CODE,
        menuText: t('commands:code_menu_text'),
      },
      [EditorCommandId.Strikeout]: {
        menuText: t('commands:strikeout_menu_text'),
      },
      [EditorCommandId.Superscript]: {
        menuText: t('commands:superscript_menu_text'),
      },
      [EditorCommandId.Subscript]: {
        menuText: t('commands:subscript_menu_text'),
      },
      [EditorCommandId.Smallcaps]: {
        menuText: t('commands:smallcaps_menu_text'),
      },
      [EditorCommandId.Span]: {
        menuText: t('commands:span_menu_text'),
      },
      [EditorCommandId.Paragraph]: {
        icon: IconNames.PARAGRAPH,
        menuText: t('commands:paragraph_menu_text'),
      },
      [EditorCommandId.Heading1]: {
        icon: IconNames.HEADER_ONE,
        menuText: t('commands:heading1_menu_text'),
      },
      [EditorCommandId.Heading2]: {
        icon: IconNames.HEADER_TWO,
        menuText: t('commands:heading2_menu_text'),
      },
      [EditorCommandId.Heading3]: {
        menuText: t('commands:heading3_menu_text'),
      },
      [EditorCommandId.Heading4]: {
        menuText: t('commands:heading4_menu_text'),
        keysHidden: true,
      },
      [EditorCommandId.Heading5]: {
        menuText: t('commands:heading5_menu_text'),
        keysHidden: true,
      },
      [EditorCommandId.Heading6]: {
        menuText: t('commands:heading6_menu_text'),
        keysHidden: true,
      },
      [EditorCommandId.CodeBlock]: {
        icon: IconNames.CODE,
        menuText: t('commands:code_block_menu_text'),
      },
      [EditorCommandId.Blockquote]: {
        icon: IconNames.CITATION,
        menuText: t('commands:blockquote_menu_text'),
      },
      [EditorCommandId.LineBlock]: {
        menuText: t('commands:line_block_menu_text'),
      },
      [EditorCommandId.Div]: {
        menuText: t('commands:div_menu_text'),
      },
      [EditorCommandId.AttrEdit]: {
        menuText: t('commands:attr_edit_menu_text'),
      },
    },

    [t('commands:group_lists')]: {
      [EditorCommandId.BulletList]: {
        icon: IconNames.PROPERTIES,
        menuText: t('commands:bullet_list_menu_text'),
      },
      [EditorCommandId.OrderedList]: {
        icon: IconNames.NUMBERED_LIST,
        menuText: t('commands:ordered_list_menu_text'),
      },
      [EditorCommandId.TightList]: {
        menuText: t('commands:tight_list_menu_text'),
      },
      [EditorCommandId.ListItemSink]: {
        menuText: t('commands:list_item_sink_menu_text'),
      },
      [EditorCommandId.ListItemLift]: {
        menuText: t('commands:list_item_lift_menu_text'),
      },
      [EditorCommandId.ListItemCheck]: {
        menuText: t('commands:list_item_check_menu_text'),
      },
      [EditorCommandId.ListItemCheckToggle]: {
        menuText: t('commands:list_item_check_toggle_menu_text'),
      },
      [EditorCommandId.OrderedListEdit]: {
        menuText: t('commands:ordered_list_edit_menu_text'),
      },
    },

    [t('commands:group_tables')]: {
      [EditorCommandId.TableInsertTable]: {
        icon: IconNames.TH,
        menuText: t('commands:table_insert_table'),
      },
      [EditorCommandId.TableToggleHeader]: {
        menuText: t('commands:table_toggle_header'),
      },
      [EditorCommandId.TableToggleCaption]: {
        menuText: t('commands:table_toggle_caption'),
      },
      [EditorCommandId.TableAddColumnAfter]: {
        icon: IconNames.ADD_COLUMN_RIGHT,
        menuText: t('commands:table_add_column_after'),
      },
      [EditorCommandId.TableAddColumnBefore]: {
        icon: IconNames.ADD_COLUMN_LEFT,
        menuText: t('commands:table_add_column_before'),
      },
      [EditorCommandId.TableDeleteColumn]: {
        icon: IconNames.REMOVE_COLUMN,
        menuText: t('commands:table_delete_column'),
      },
      [EditorCommandId.TableAddRowAfter]: {
        icon: IconNames.ADD_ROW_BOTTOM,
        menuText: t('commands:table_add_row_after'),
      },
      [EditorCommandId.TableAddRowBefore]: {
        icon: IconNames.ADD_ROW_TOP,
        menuText: t('commands:table_add_row_before'),
      },
      [EditorCommandId.TableDeleteRow]: {
        icon: IconNames.EXCLUDE_ROW,
        menuText: t('commands:table_delete_row'),
      },
      [EditorCommandId.TableDeleteTable]: {
        icon: IconNames.TH_DISCONNECT,
        menuText: t('commands:table_delete_table'),
      },
      [EditorCommandId.TableNextCell]: {
        menuText: t('commands:table_next_cell'),
      },
      [EditorCommandId.TablePreviousCell]: {
        menuText: t('commands:table_previous_cell'),
      },
      [EditorCommandId.TableAlignColumnLeft]: {
        menuText: t('commands:table_align_column_left'),
      },
      [EditorCommandId.TableAlignColumnRight]: {
        menuText: t('commands:table_align_column_right'),
      },
      [EditorCommandId.TableAlignColumnCenter]: {
        menuText: t('commands:table_align_column_center'),
      },
      [EditorCommandId.TableAlignColumnDefault]: {
        menuText: t('commands:table_align_column_default'),
      },
    },

    [t('commands:group_insert')]: {
      [EditorCommandId.Link]: {
        icon: IconNames.LINK,
        menuText: t('commands:link_menu_text'),
      },
      [EditorCommandId.Image]: {
        icon: IconNames.MEDIA,
        menuText: t('commands:image_menu_text'),
      },
      [EditorCommandId.Footnote]: {
        menuText: t('commands:footnote_menu_text'),
      },
      [EditorCommandId.HorizontalRule]: {
        menuText: t('commands:horizontal_rule_menu_text'),
      },
      [EditorCommandId.ParagraphInsert]: {
        icon: IconNames.PARAGRAPH,
        menuText: t('commands:paragraph_insert_menu_text'),
      },
      [EditorCommandId.InlineMath]: {
        menuText: t('commands:inline_math_menu_text'),
      },
      [EditorCommandId.DisplayMath]: {
        menuText: t('commands:display_math_menu_text'),
      },
      [EditorCommandId.InlineLatex]: {
        menuText: t('commands:inline_latex_menu_text'),
      },
      [EditorCommandId.RawInline]: {
        menuText: t('commands:raw_inline_menu_text'),
      },
      [EditorCommandId.RawBlock]: {
        menuText: t('commands:raw_block_menu_text'),
      },
      [EditorCommandId.YamlMetadata]: {
        menuText: t('commands:yaml_metadata_menu_text'),
      },
      [EditorCommandId.RmdChunk]: {
        icon: IconNames.CODE_BLOCK,
        menuText: t('commands:rmd_chunk_menu_text'),
      },
      [EditorCommandId.DefinitionList]: {
        menuText: t('commands:definition_list_menu_text'),
      },
      [EditorCommandId.DefinitionTerm]: {
        menuText: t('commands:definition_term_menu_text'),
      },
      [EditorCommandId.DefinitionDescription]: {
        menuText: t('commands:definition_description_menu_text'),
      },
      [EditorCommandId.Citation]: {
        menuText: t('commands:citation_menu_text'),
      },
    },
  };
}

export function editorProsemirrorCommands(editorCommands: EditorCommand[]): Command[] {
  const commandDefs = editorCommandDefs();
  const commands: Command[] = [];
  Object.keys(commandDefs).forEach(group => {
    const groupCommands = commandDefs[group];
    for (const [id, groupedCommand] of Object.entries(groupCommands)) {
      const editorCommand = editorCommands.find(cmd => cmd.id === id)!;
      const command: Command = {
        ...editorCommand,
        ...groupedCommand!,
        group,
        keysUnbound: true,
      };
      commands.push(command);
    }
  });

  return commands;
}

export function editorExternalCommands(editor: Editor): Command[] {
  return [
    {
      id: WorkbenchCommandId.ActivateEditor,
      menuText: t('commands:activate_editor_menu_text'),
      group: t('commands:group_view'),
      keymap: [],
      isEnabled: () => true,
      isActive: () => false,
      execute: () => {
        editor.focus();
      },
    },
    {
      id: WorkbenchCommandId.Print,
      menuText: t('commands:print_menu_text'),
      icon: IconNames.PRINT,
      group: t('commands:group_utilities'),
      keymap: ['Ctrl+Alt+P'],
      isEnabled: () => true,
      isActive: () => false,
      execute: () => {
        printJS({
          printable: editor.getHTML(),
          type: 'raw-html',
          css: '/styles/print.css',
        });
      },
    },
  ];
}

export function editorDebugCommands(editor: Editor): Command[] {
  return [
    {
      id: WorkbenchCommandId.EnableDevTools,
      menuText: t('commands:enable_dev_tools_menu_text'),
      group: t('commands:group_utilities'),
      keymap: [],
      isEnabled: () => true,
      isActive: () => false,
      execute: () => {
        editor.enableDevTools(applyDevTools);
      },
    },
  ];
}
