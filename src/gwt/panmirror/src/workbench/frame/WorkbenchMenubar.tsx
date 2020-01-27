/*
 * WorkbenchMenubar.tsx
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

import React from 'react';
import { useTranslation } from 'react-i18next';

import { ButtonGroup, Menu } from '@blueprintjs/core';

import { EditorCommandId } from 'editor/src/api/command';

import { MainMenu, MenubarMenu } from 'workbench/widgets/Menu';
import { WithCommand } from 'workbench/widgets/command/WithCommand';
import { CommandMenuItem, CommandMenuItemActive } from 'workbench/widgets/command/CommandMenuItem';
import { CommandSubMenu } from 'workbench/widgets/command/CommandSubMenu';
import { WorkbenchCommandId } from 'workbench/commands/commands';
import EditorTableMenuItems from 'workbench/panes/editor/EditorTableMenuItems';
import EditorParagraphStyleMenuItems from 'workbench/panes/editor/EditorParagraphStyleMenuItems';

import styles from './WorkbenchMenubar.module.scss';

const CommandId = { ...EditorCommandId, ...WorkbenchCommandId };

const FileMenu: React.FC = () => {
  return (
    <MenubarMenu>
      <CommandMenuItem id={WorkbenchCommandId.Rename} />
      <Menu.Divider />
      <CommandMenuItem id={WorkbenchCommandId.Print} />
    </MenubarMenu>
  );
};

const EditMenu: React.FC = () => {
  return (
    <MenubarMenu>
      <CommandMenuItem id={CommandId.Undo} />
      <CommandMenuItem id={CommandId.Redo} />
      <Menu.Divider />
      <CommandMenuItem id={CommandId.Cut} />
      <CommandMenuItem id={CommandId.Copy} />
      <CommandMenuItem id={CommandId.Paste} />
      <Menu.Divider />
      <CommandMenuItem id={CommandId.SelectAll} />
    </MenubarMenu>
  );
};

const ViewMenu: React.FC = () => {
  return (
    <MenubarMenu>
      <CommandMenuItem id={WorkbenchCommandId.ShowOutline} active={CommandMenuItemActive.Check} />
      <CommandMenuItem id={WorkbenchCommandId.ShowMarkdown} active={CommandMenuItemActive.Check} />
      <Menu.Divider />
      <CommandMenuItem id={WorkbenchCommandId.ActivateEditor} />
    </MenubarMenu>
  );
};

const InsertMenu: React.FC = () => {
  const { t } = useTranslation();

  return (
    <MenubarMenu>
      <CommandMenuItem id={CommandId.Image} />
      <CommandMenuItem id={CommandId.Link} />
      <Menu.Divider />
      <CommandMenuItem id={CommandId.RmdChunk} />
      <Menu.Divider />
      <CommandMenuItem id={CommandId.ParagraphInsert} />
      <CommandMenuItem id={CommandId.HorizontalRule} />
      <Menu.Divider />
      <CommandMenuItem id={CommandId.Footnote} />
      <CommandMenuItem id={CommandId.Citation} />
      <Menu.Divider />
      <CommandSubMenu text={t('insert_definitions_menu')}>
        <CommandMenuItem id={CommandId.DefinitionList} />
        <Menu.Divider />
        <CommandMenuItem id={CommandId.DefinitionTerm} />
        <CommandMenuItem id={CommandId.DefinitionDescription} />
      </CommandSubMenu>
      <Menu.Divider />
      <CommandMenuItem id={CommandId.InlineMath} />
      <CommandMenuItem id={CommandId.DisplayMath} />
      <CommandMenuItem id={CommandId.InlineLatex} />
      <Menu.Divider />
      <CommandMenuItem id={CommandId.RawBlock} />
      <CommandMenuItem id={CommandId.RawInline} />
      <Menu.Divider />
      <CommandMenuItem id={CommandId.YamlMetadata} />
    </MenubarMenu>
  );
};

const FormatMenu: React.FC = () => {
  const { t } = useTranslation();

  return (
    <MenubarMenu>
      <CommandSubMenu text={t('format_text_menu')}>
        <CommandMenuItem id={CommandId.Strong} />
        <CommandMenuItem id={CommandId.Em} />
        <CommandMenuItem id={CommandId.Code} />
        <CommandMenuItem id={CommandId.Strikeout} />
        <CommandMenuItem id={CommandId.Superscript} />
        <CommandMenuItem id={CommandId.Subscript} />
        <CommandMenuItem id={CommandId.Smallcaps} />
        <Menu.Divider />
        <CommandMenuItem id={CommandId.RawInline} text={t('commands:raw_inline_menu_text_short')} />
        <WithCommand id={CommandId.Span}>
          <Menu.Divider />
          <CommandMenuItem id={CommandId.Span} />
        </WithCommand>
      </CommandSubMenu>
      <CommandSubMenu text={t('format_paragraph_style_menu')}>
        <EditorParagraphStyleMenuItems />
      </CommandSubMenu>
      <CommandSubMenu text={t('format_bullets_and_numbering_menu')}>
        <CommandMenuItem id={CommandId.BulletList} />
        <CommandMenuItem id={CommandId.OrderedList} />
        <CommandMenuItem id={CommandId.TightList} active={CommandMenuItemActive.Check} />
        <Menu.Divider />
        <CommandMenuItem id={CommandId.ListItemCheck} />
        <CommandMenuItem id={CommandId.ListItemCheckToggle} active={CommandMenuItemActive.Check} />
        <Menu.Divider />
        <CommandMenuItem id={CommandId.ListItemSink} />
        <CommandMenuItem id={CommandId.ListItemLift} />
        <Menu.Divider />
        <CommandMenuItem id={CommandId.OrderedListEdit} />
      </CommandSubMenu>
      <Menu.Divider />
      <CommandMenuItem id={CommandId.Blockquote} active={CommandMenuItemActive.Check} />
      <CommandMenuItem id={CommandId.LineBlock} active={CommandMenuItemActive.Check} />
      <Menu.Divider />
      <CommandMenuItem id={CommandId.Div} />
      <CommandMenuItem id={CommandId.RawBlock} />
      <WithCommand id={CommandId.AttrEdit}>
        <Menu.Divider />
        <CommandMenuItem id={CommandId.AttrEdit} />
      </WithCommand>
    </MenubarMenu>
  );
};

const TableMenu: React.FC = () => {
  return (
    <MenubarMenu>
      <EditorTableMenuItems />
    </MenubarMenu>
  );
};

const HelpMenu: React.FC = () => {
  return (
    <MenubarMenu>
      <CommandMenuItem id={CommandId.EnableDevTools} />
      <Menu.Divider />
      <CommandMenuItem id={CommandId.KeyboardShortcuts} />
    </MenubarMenu>
  );
};

const WorkbenchMenubar: React.FC = () => {
  const { t } = useTranslation();

  return (
    <ButtonGroup className={styles.menubarButtons} minimal={true}>
      <MainMenu text={t('file_menu')} menu={<FileMenu />} />
      <MainMenu text={t('edit_menu')} menu={<EditMenu />} />
      <MainMenu text={t('view_menu')} menu={<ViewMenu />} />
      <MainMenu text={t('insert_menu')} menu={<InsertMenu />} />
      <MainMenu text={t('format_menu')} menu={<FormatMenu />} />
      <WithCommand id={CommandId.TableInsertTable}>
        <MainMenu text={t('table_menu')} menu={<TableMenu />} />
      </WithCommand>
      <MainMenu text={t('help_menu')} menu={<HelpMenu />} />
    </ButtonGroup>
  );
};

export default WorkbenchMenubar;
