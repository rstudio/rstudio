import React, { useState } from 'react';

import { IProps } from '@blueprintjs/core';

import { CommandId, Command } from './commands';

type Commands = { [id in CommandId]?: Command };

export interface CommandManager {
  commands: Commands;
  addCommands: (commands: Command[]) => void;
  execCommand: (id: CommandId) => void;
}

export const CommandManagerContext = React.createContext<CommandManager>({
  commands: {},
  addCommands: () => {
    /* */
  },
  execCommand: (id: CommandId) => {
    /* */
  },
});

export const CommandManagerProvider: React.FC<IProps> = props => {
  // establish commands state
  const initialCommands: Commands = {};
  const [commands, setCommands] = useState(initialCommands);

  // command manager that enables reading commands and adding new ones
  const commandManager = {
    commands,
    addCommands: (newCommands: Command[]) => {
      setCommands((prevCommands: Commands) => {
        // index commands by name
        const newCommandsById: Commands = {};
        newCommands.forEach(command => {
          newCommandsById[command.id] = command;
        });

        return {
          ...prevCommands,
          ...newCommandsById,
        };
      });
    },
    execCommand: (id: CommandId) => {
      const command = commands[id];
      if (command) {
        command.execute();
      }
    },
  };

  return <CommandManagerContext.Provider value={commandManager}>{props.children}</CommandManagerContext.Provider>;
};

// https://stackoverflow.com/questions/50612299/react-typescript-consuming-context-via-hoc
// https://medium.com/@jrwebdev/react-higher-order-component-patterns-in-typescript-42278f7590fb
export function withCommandManager<P extends WithCommandManagerProps>(Component: React.ComponentType<P>) {
  return function CommandsComponent(props: Pick<P, Exclude<keyof P, keyof WithCommandManagerProps>>) {
    return (
      <CommandManagerContext.Consumer>
        {(commandManager: CommandManager) => <Component {...(props as P)} commandManager={commandManager} />}
      </CommandManagerContext.Consumer>
    );
  };
}

interface WithCommandManagerProps {
  commandManager: CommandManager;
}
