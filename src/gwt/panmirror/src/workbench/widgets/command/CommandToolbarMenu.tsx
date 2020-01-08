import React, { useContext } from 'react';

import { useSelector } from 'react-redux';

import { IProps, Menu } from '@blueprintjs/core';

import { CommandManagerContext } from 'workbench/commands/CommandManager';
import { editorSelection } from 'workbench/store/editor/editor-selectors';
import { Command, CommandId } from 'workbench/commands/commands';
import { ToolbarMenu } from 'workbench/widgets/Toolbar';

import { CommandMenuItem, CommandMenuItemActive } from './CommandMenuItem';
import { uuidv4 } from 'editor/src/api/util';

const kSeparator = '---';

export interface CommandToolbarMenuProps extends IProps {
  commands: Array<CommandId | '---'>;
}

export const CommandToolbarMenu: React.FC<CommandToolbarMenuProps> = (props: CommandToolbarMenuProps) => {
  // force re-render when the selection changes
  useSelector(editorSelection);

  // read command instances
  type CommandItem = Command | '---';
  const commandManager = useContext(CommandManagerContext);
  const commands: CommandItem[] = props.commands.reduce((allCmds, command) => {
    if (command === kSeparator) {
      allCmds.push(kSeparator);
    } else {
      const cmd = commandManager.commands[command];
      if (cmd) {
        allCmds.push(cmd);
      }
    }
    return allCmds;
  }, Array<CommandItem>());

  // if we have any then build the menu
  if (commands.length) {
    // turn into JSX (get selected item while we iterate)
    let selected = '';
    const menuItems = commands.map(command => {
      if (command === kSeparator) {
        return <Menu.Divider key={uuidv4()} />;
      } else {
        if (command.isActive()) {
          selected = command.menuText;
        }
        return <CommandMenuItem key={command.id} id={command.id} active={CommandMenuItemActive.Check} />;
      }
    });

    // if nothing is selected then display the first/default
    const disabled = !selected;
    if (disabled) {
      selected = (commands[0] as Command).menuText;
    }

    // return JSX popover + menu items
    return (
      <ToolbarMenu className={props.className} text={selected}>
        {menuItems}
      </ToolbarMenu>
    );
  } else {
    return null;
  }
};
