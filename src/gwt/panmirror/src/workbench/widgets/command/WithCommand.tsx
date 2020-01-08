import React, { useContext } from 'react';

import { IProps } from '@blueprintjs/core';

import { CommandId } from 'workbench/commands/commands';
import { CommandManagerContext } from 'workbench/commands/CommandManager';

export interface WithCommandProps extends IProps {
  id: CommandId;
}

export const WithCommand: React.FC<WithCommandProps> = props => {
  const commmandManager = useContext(CommandManagerContext);
  if (commmandManager.commands[props.id]) {
    return <>{props.children}</>;
  } else {
    return null;
  }
};
