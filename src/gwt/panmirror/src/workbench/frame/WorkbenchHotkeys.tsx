/*
 * WorkbenchHotkeys.tsx
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

import React from 'react';

import { TFunction } from 'i18next';
import { withTranslation } from 'react-i18next';

import { Hotkeys, Hotkey, IHotkeyProps } from '@blueprintjs/core';
import { HotkeysEvents, HotkeyScope } from '@blueprintjs/core/lib/esm/components/hotkeys/hotkeysEvents';
import { showHotkeysDialog } from '@blueprintjs/core/lib/esm/components/hotkeys/hotkeysDialog';

import { CommandManager, withCommandManager } from 'workbench/commands/CommandManager';
import { Command, WorkbenchCommandId } from 'workbench/commands/commands';
import { toBlueprintHotkeyCombo } from 'workbench/commands/keycodes';

interface WorkbenchHotkeysProps {
  commandManager: CommandManager;
  t: TFunction;
}

class WorkbenchHotkeys extends React.Component<WorkbenchHotkeysProps> {
  private hotkeysEvents: HotkeysEvents;

  constructor(props: WorkbenchHotkeysProps) {
    super(props);
    this.hotkeysEvents = new HotkeysEvents(HotkeyScope.GLOBAL);
  }

  public componentDidMount() {
    if (super.componentDidMount != null) {
      super.componentDidMount();
    }

    // attach global key event listeners
    document.addEventListener('keydown', this.hotkeysEvents!.handleKeyDown);
    document.addEventListener('keyup', this.hotkeysEvents!.handleKeyUp);

    // register keyboard shortcuts command
    this.props.commandManager.addCommands([
      {
        id: WorkbenchCommandId.KeyboardShortcuts,
        menuText: this.props.t('commands:keyboard_shortcuts_menu_text'),
        group: this.props.t('commands:group_utilities'),
        keymap: ['Ctrl+Alt+K'],
        isEnabled: () => true,
        isActive: () => false,
        execute: () => {
          showHotkeysDialog(this.renderHotkeys().props.children.map((child: { props: IHotkeyProps }) => child.props));
        },
      },
    ]);
  }

  public componentWillUnmount() {
    if (super.componentWillUnmount != null) {
      super.componentWillUnmount();
    }
    document.removeEventListener('keydown', this.hotkeysEvents!.handleKeyDown);
    document.removeEventListener('keyup', this.hotkeysEvents!.handleKeyUp);
    this.hotkeysEvents!.clear();
  }

  public render() {
    const hotkeys = this.renderHotkeys();
    this.hotkeysEvents!.setHotkeys(hotkeys.props);
    return <></>;
  }

  public renderHotkeys() {
    const hotkeys: { [key: string]: Command } = {};
    Object.values(this.props.commandManager.commands).forEach(command => {
      if (command) {
        if (!command.keysHidden && command.keymap) {
          command.keymap.forEach(keyCombo => {
            hotkeys[keyCombo] = command;
          });
        }
      }
    });

    return (
      <Hotkeys>
        {Object.keys(hotkeys).map(key => {
          const command = hotkeys[key];
          const handler = command.keysUnbound
            ? undefined
            : () => {
                if (command.isEnabled()) {
                  command.execute();
                }
              };
          const combo = toBlueprintHotkeyCombo(key);
          return (
            <Hotkey
              global={true}
              group={command.group}
              key={key}
              allowInInput={true}
              combo={combo}
              label={command.menuText}
              onKeyUp={handler}
              preventDefault={!command.keysUnbound}
              stopPropagation={!command.keysUnbound}
            />
          );
        })}
      </Hotkeys>
    );
  }
}

export default withTranslation()(withCommandManager(WorkbenchHotkeys));
