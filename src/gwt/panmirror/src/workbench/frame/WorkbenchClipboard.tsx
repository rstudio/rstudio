/*
 * WorkbenchClipboard.tsx
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

import { TFunction } from 'i18next';
import { withTranslation } from 'react-i18next';

import { AlertType } from 'editor/src/api/ui';

import { WorkbenchCommandId, CommandId } from 'workbench/commands/commands';
import { CommandManager, withCommandManager } from 'workbench/commands/CommandManager';
import { keyCodeString } from 'workbench/commands/keycodes';

import { AlertDialog } from 'workbench/widgets/dialog/AlertDialog';

interface WorkbenchClipboardProps {
  commandManager: CommandManager;
  t: TFunction;
}

interface WorkbenchClipboardState {
  dialogIsOpen: boolean;
  commandMenuText?: string;
  commandKeycode?: string;
}

class WorkbenchClipboard extends React.Component<WorkbenchClipboardProps, WorkbenchClipboardState> {
  constructor(props: WorkbenchClipboardProps) {
    super(props);
    this.state = {
      dialogIsOpen: false,
    };
    this.onDialogClosed = () => {
      this.setState({
        dialogIsOpen: false,
      });
      this.focusEditor();
    };
  }

  public render() {
    return (
      <AlertDialog
        title={'Use Keyboard Shortcut'}
        message={'the message'}
        type={AlertType.Warning}
        isOpen={this.state.dialogIsOpen}
        onClosed={this.onDialogClosed}
      >
        <p>{this.props.t('clipboard_dialog_title')}</p>
        <p>
          {this.props.t('clipboard_dialog_message', {
            keycode: this.state.commandKeycode,
            command: this.state.commandMenuText,
          })}
        </p>
      </AlertDialog>
    );
  }

  public componentDidMount() {
    this.props.commandManager.addCommands([
      this.clipboardCommand(WorkbenchCommandId.Copy, 'copy', this.props.t('commands:copy_menu_text'), 'Mod-c'),
      this.clipboardCommand(WorkbenchCommandId.Cut, 'cut', this.props.t('commands:cut_menu_text'), 'Mod-x'),
      this.clipboardCommand(WorkbenchCommandId.Paste, 'paste', this.props.t('commands:paste_menu_text'), 'Mod-v'),
    ]);
  }

  private clipboardCommand(id: CommandId, domId: string, menuText: string, keymap: string) {
    return {
      id,
      menuText,
      group: this.props.t('commands:group_text_editing'),
      keymap: [keymap],
      keysUnbound: true,
      focusEditor: true,
      isEnabled: () => !document.queryCommandSupported(domId) || document.queryCommandEnabled(domId),
      isActive: () => false,
      execute: () => {
        if (document.queryCommandSupported(domId)) {
          document.execCommand(domId);
          this.focusEditor();
        } else {
          // open dialog
          this.setState({
            dialogIsOpen: true,
            commandMenuText: menuText,
            commandKeycode: keyCodeString(keymap),
          });
        }
      },
    };
  }

  private onDialogClosed() {
    this.setState({
      dialogIsOpen: false,
    });
    this.focusEditor();
  }

  private focusEditor() {
    this.props.commandManager.execCommand(WorkbenchCommandId.ActivateEditor);
  }
}

export default withTranslation()(withCommandManager(WorkbenchClipboard));
