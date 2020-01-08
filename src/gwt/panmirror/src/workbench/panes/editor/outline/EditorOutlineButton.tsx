import React, { useContext } from 'react';

import { Button } from '@blueprintjs/core';
import { IconNames } from '@blueprintjs/icons';

import { CommandManagerContext } from 'workbench/commands/CommandManager';
import { WorkbenchCommandId, commandTooltipText } from 'workbench/commands/commands';

import styles from './EditorOutlineSidebar.module.scss';

export interface EditorOutlineButtonProps {
  visible: boolean;
  onClick: () => void;
}

export const EditorOutlineButton: React.FC<EditorOutlineButtonProps> = props => {
  const commandManager = useContext(CommandManagerContext);
  const command = commandManager.commands[WorkbenchCommandId.ShowOutline];
  const title = command ? commandTooltipText(command) : '';

  if (props.visible) {
    return (
      <Button
        icon={IconNames.ALIGN_JUSTIFY}
        title={title}
        large={true}
        minimal={true}
        className={styles.showOutlineButton}
        onClick={props.onClick}
      />
    );
  } else {
    return null;
  }
};
