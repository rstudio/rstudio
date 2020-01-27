/*
 * EditorTableMenuItems.tsx
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

import { Menu } from '@blueprintjs/core';
import { IconNames } from '@blueprintjs/icons';

import { EditorCommandId } from 'editor/src/api/command';

import { CommandMenuItem, CommandMenuItemActive } from 'workbench/widgets/command/CommandMenuItem';
import { CommandSubMenu } from 'workbench/widgets/command/CommandSubMenu';

const EditorTableMenuItems: React.FC = () => {
  const { t } = useTranslation();

  return (
    <>
      <CommandMenuItem id={EditorCommandId.TableInsertTable} />
      <Menu.Divider />
      <CommandMenuItem id={EditorCommandId.TableToggleHeader} active={CommandMenuItemActive.Check} />
      <CommandMenuItem id={EditorCommandId.TableToggleCaption} active={CommandMenuItemActive.Check} />
      <Menu.Divider />
      <CommandSubMenu text={t('table_column_alignment_menu')} icon={IconNames.SPLIT_COLUMNS}>
        <CommandMenuItem id={EditorCommandId.TableAlignColumnLeft} />
        <CommandMenuItem id={EditorCommandId.TableAlignColumnRight} />
        <CommandMenuItem id={EditorCommandId.TableAlignColumnCenter} />
        <Menu.Divider />
        <CommandMenuItem id={EditorCommandId.TableAlignColumnDefault} />
      </CommandSubMenu>
      <Menu.Divider />
      <CommandMenuItem id={EditorCommandId.TableAddRowBefore} />
      <CommandMenuItem id={EditorCommandId.TableAddRowAfter} />
      <CommandMenuItem id={EditorCommandId.TableAddColumnBefore} />
      <CommandMenuItem id={EditorCommandId.TableAddColumnAfter} />
      <Menu.Divider />
      <CommandMenuItem id={EditorCommandId.TableDeleteRow} />
      <CommandMenuItem id={EditorCommandId.TableDeleteColumn} />
      <CommandMenuItem id={EditorCommandId.TableDeleteTable} />
    </>
  );
};

export default EditorTableMenuItems;
