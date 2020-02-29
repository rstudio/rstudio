/*
 * WorkbenchTitle.tsx
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
import ReactDOM from 'react-dom';

import { connect } from 'react-redux';

import { TFunction } from 'i18next';
import { withTranslation } from 'react-i18next';

import { IProps, EditableText } from '@blueprintjs/core';

import { WorkbenchState } from 'workbench/store/store';
import { setEditorTitle } from 'workbench/store/editor/editor-actions';

import { CommandManager, withCommandManager } from 'workbench/commands/CommandManager';
import { WorkbenchCommandId } from 'workbench/commands/commands';

import { focusInput } from 'workbench/widgets/utils';

import styles from './WorkbenchNavbar.module.scss';

export interface WorkbenchTitleProps extends IProps {
  title: string;
  setTitle: (title: string) => void;
  commandManager: CommandManager;
  t: TFunction;
}

class WorkbenchTitle extends React.Component<WorkbenchTitleProps> {
  constructor(props: WorkbenchTitleProps) {
    super(props);
    this.focusInput = this.focusInput.bind(this);
    this.focusEditor = this.focusEditor.bind(this);
  }

  public componentDidMount() {
    // register keyboard shortcuts command
    this.props.commandManager.addCommands([
      {
        id: WorkbenchCommandId.Rename,
        menuText: this.props.t('commands:rename_menu_text'),
        group: this.props.t('commands:group_utilities'),
        keymap: [],
        isEnabled: () => true,
        isActive: () => false,
        execute: this.focusInput,
      },
    ]);
  }

  public render() {
    return (
      <EditableText
        className={styles.title}
        placeholder={this.props.t('untitled_document')}
        value={this.props.title}
        onChange={this.props.setTitle}
        onCancel={this.focusEditor}
        onConfirm={this.focusEditor}
        onEdit={this.focusInput}
      />
    );
  }

  private focusInput() {
    // no ref property available on EditableText, so we need this hack:
    //  https://github.com/palantir/blueprint/issues/2492
    const editableText = ReactDOM.findDOMNode(this) as Element;
    const editableTextContent = editableText!.querySelector('.bp3-editable-text-content');
    editableTextContent!.dispatchEvent(new Event('focus'));
    setTimeout(() => {
      const editableTextInput = editableText!.querySelector('.bp3-editable-text-input');
      focusInput(editableTextInput as HTMLInputElement);
    }, 50);
  }

  private focusEditor() {
    // delay so the enter key doesn't go to the editor
    setTimeout(() => {
      this.props.commandManager.execCommand(WorkbenchCommandId.ActivateEditor);
    }, 0);
  }
}

const mapStateToProps = (state: WorkbenchState) => {
  return {
    title: state.editor.title,
  };
};

const mapDispatchToProps = (dispatch: any) => {
  return {
    setTitle: (title: string) => dispatch(setEditorTitle(title)),
  };
};

// @ts-ignore
export default withCommandManager(withTranslation()(connect(mapStateToProps, mapDispatchToProps)(WorkbenchTitle)));
