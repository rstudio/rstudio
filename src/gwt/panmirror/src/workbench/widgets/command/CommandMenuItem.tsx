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
