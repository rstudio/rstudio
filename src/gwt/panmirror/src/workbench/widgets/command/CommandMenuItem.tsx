/*
 * CommandMenuItem.tsx
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

import { useSelector } from 'react-redux';
import React, { useContext } from 'react';

import { Menu } from '@blueprintjs/core';
import { IconNames } from '@blueprintjs/icons';

import { editorSelection } from 'workbench/store/editor/editor-selectors';
import { keyCodeString } from 'workbench/commands/keycodes';
import { commandKeymapText, CommandId } from 'workbench/commands/commands';
import { CommandManagerContext } from 'workbench/commands/CommandManager';

export enum CommandMenuItemActive {
  Check = 'check',
  Latch = 'latch',
  None = 'none',
}

export interface CommandMenuItemProps {
  id: CommandId;
  text?: string;
  keyCode?: string;
  active?: CommandMenuItemActive;
}

export const CommandMenuItem: React.FC<CommandMenuItemProps> = props => {
  const { id, keyCode, active = CommandMenuItemActive.None } = props;

  // force re-render when the selection changes
  useSelector(editorSelection);

  // get command
  const commandManager = useContext(CommandManagerContext);
  const command = commandManager.commands[id];

  if (command) {
    // resolve label
    const label = keyCode ? keyCodeString(keyCode, true) : commandKeymapText(command, true);

    // resolve icon
    const icon =
      active === 'check'
        ? command.isActive()
          ? IconNames.SMALL_TICK
          : IconNames.BLANK
        : command.icon || IconNames.BLANK;

    const isActive = active === 'latch' ? command.isActive() : false;

    return (
      <Menu.Item
        icon={icon}
        text={props.text || command.menuText}
        onClick={command.execute}
        active={isActive}
        disabled={!command.isEnabled()}
        label={label}
      />
    );
  } else {
    return null;
  }
};
