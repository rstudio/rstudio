import React, { useContext } from 'react';

import { SubMenuProps, SubMenu } from 'workbench/widgets/Menu';

import { CommandMenuItem } from './CommandMenuItem';
import { CommandManagerContext } from 'workbench/commands/CommandManager';
import { CommandId } from 'workbench/commands/commands';

export const CommandSubMenu: React.FC<SubMenuProps> = props => {
  // get command manager for command lookup
  const commandManager = useContext(CommandManagerContext);

  let haveCommands = false;
  const children = React.Children.toArray(props.children);
  for (let i = 0; i < children.length; i++) {
    const child = children[i];
    if (
      React.isValidElement(child) &&
      child.type === CommandMenuItem &&
      commandManager.commands[child.props.id as CommandId]
    ) {
      haveCommands = true;
      break;
    }
  }

  if (haveCommands) {
    return <SubMenu {...props} />;
  } else {
    return null;
  }
};
