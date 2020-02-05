/*
 * MarkdownPane.tsx
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

import { connect } from 'react-redux';

import { TFunction } from 'i18next';
import { withTranslation } from 'react-i18next';

import { IconNames } from '@blueprintjs/icons';

import CodeMirror from 'codemirror';
import 'codemirror/mode/markdown/markdown';
import 'codemirror/lib/codemirror.css';

import { WorkbenchState } from 'workbench/store/store';
import { Pane } from 'workbench/widgets/Pane';
import { Toolbar, ToolbarText, ToolbarButton } from 'workbench/widgets/Toolbar';
import { setPrefsShowMarkdown } from 'workbench/store/prefs/prefs-actions';
import { CommandManager, withCommandManager } from 'workbench/commands/CommandManager';
import { WorkbenchCommandId } from 'workbench/commands/commands';

import styles from './MarkdownPane.module.scss';

interface MarkdownPaneProps {
  setShowMarkdown: (showMarkdown: boolean) => void;
  showMarkdown: boolean;
  markdown: string;
  commandManager: CommandManager;
  t: TFunction;
}

export class MarkdownPane extends React.Component<MarkdownPaneProps> {
  private parent: HTMLDivElement | null;
  private cm: CodeMirror.Editor | null;

  constructor(props: Readonly<MarkdownPaneProps>) {
    super(props);
    this.parent = null;
    this.cm = null;
    this.onCloseClicked = this.onCloseClicked.bind(this);
  }

  public render() {
    // build className dynamically
    const classes = ['markdown-pane', styles.pane];
    if (this.props.showMarkdown) {
      classes.push('markdown-visible');
    }

    return (
      <Pane className={classes.join(' ')}>
        <Toolbar className={styles.toolbar}>
          <ToolbarText>{this.props.t('markdown_pane_caption')}</ToolbarText>
          <ToolbarButton
            title={this.props.t('close_button_title')}
            className={styles.closeButton}
            icon={IconNames.SMALL_CROSS}
            enabled={true}
            active={false}
            onClick={this.onCloseClicked}
          />
        </Toolbar>
        <div className={styles.codemirrorParent} ref={el => (this.parent = el)} />
      </Pane>
    );
  }

  public componentDidMount() {
    // initialize codemirror
    this.cm = CodeMirror(this.parent!, {
      mode: 'markdown',
      readOnly: true,
      autofocus: false,
      lineWrapping: true,
    });
    this.cm.setSize(null, '100%');
    this.updateCodeMirror();

    // register command used to toggle pane
    this.props.commandManager.addCommands([
      {
        id: WorkbenchCommandId.ShowMarkdown,
        menuText: this.props.t('commands:show_markdown_menu_text'),
        group: this.props.t('commands:group_view'),
        keymap: ['Ctrl-Alt-M'],
        isEnabled: () => true,
        isActive: () => this.props.showMarkdown,
        execute: () => {
          this.props.setShowMarkdown(!this.props.showMarkdown);
        },
      },
    ]);
  }

  public componentDidUpdate() {
    this.updateCodeMirror();
  }

  private updateCodeMirror() {
    const doc = this.cm!.getDoc();
    doc.replaceRange(
      this.props.markdown,
      { line: doc.firstLine(), ch: 0 },
      { line: doc.lastLine(), ch: doc.getLine(doc.lastLine()).length },
    );
  }

  private onCloseClicked() {
    this.props.setShowMarkdown(false);
  }
}

const mapStateToProps = (state: WorkbenchState) => {
  return {
    markdown: state.editor.markdown,
    showMarkdown: state.prefs.showMarkdown,
  };
};

const mapDispatchToProps = (dispatch: any) => {
  return {
    setShowMarkdown: (showMarkdown: boolean) => dispatch(setPrefsShowMarkdown(showMarkdown)),
  };
};

// @ts-ignore
export default withCommandManager(withTranslation()(connect(mapStateToProps, mapDispatchToProps)(MarkdownPane)));
