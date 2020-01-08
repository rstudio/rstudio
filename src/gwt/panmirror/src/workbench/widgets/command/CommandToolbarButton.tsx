import React, { useContext } from 'react';

import { useSelector } from 'react-redux';

import { IProps } from '@blueprintjs/core';

import { editorSelection } from 'workbench/store/editor/editor-selectors';
import { ToolbarButton } from 'workbench/widgets/Toolbar';
import { CommandId, commandTooltipText } from 'workbench/commands/commands';
import { CommandManagerContext } from 'workbench/commands/CommandManager';

export interface CommandToolbarButtonProps extends IProps {
  command: CommandId;
}

export const CommandToolbarButton: React.FC<CommandToolbarButtonProps> = (props: CommandToolbarButtonProps) => {
  // force re-render when the selection changes
  useSelector(editorSelection);

  // get command
  const commandManager = useContext(CommandManagerContext);
  const command = commandManager.commands[props.command];
  if (command) {
    return (
      <ToolbarButton
        className={props.className}
        icon={command.icon}
        title={commandTooltipText(command)}
        enabled={command.isEnabled()}
        active={command.isActive()}
        onClick={command.execute}
      />
    );
  } else {
    return null;
  }
};
