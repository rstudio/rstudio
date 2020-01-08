import React from 'react';

import { connect } from 'react-redux';

import { CSSTransition } from 'react-transition-group';

import { TFunction } from 'i18next';
import { withTranslation } from 'react-i18next';

import { IProps } from '@blueprintjs/core';

import { EditorOutline } from 'editor/src/api/outline';

import { WorkbenchState } from 'workbench/store/store';
import { setPrefsShowOutline } from 'workbench/store/prefs/prefs-actions';

import { CommandManager, withCommandManager } from 'workbench/commands/CommandManager';
import { WorkbenchCommandId } from 'workbench/commands/commands';

import { EditorOutlineButton } from './EditorOutlineButton';
import { EditorOutlineHeader } from './EditorOutlineHeader';
import { EditorOutlineTree } from './EditorOutlineTree';
import { EditorOutlineEmpty } from './EditorOutlineEmpty';

import styles from './EditorOutlineSidebar.module.scss';
import transition from './EditorOutlineTransition.module.scss';

export interface EditorOutlineSidebarProps extends IProps {
  setShowOutline: (showOutline: boolean) => void;
  showOutline: boolean;
  outline: EditorOutline;
  commandManager: CommandManager;
  t: TFunction;
}

class EditorOutlineSidebar extends React.Component<EditorOutlineSidebarProps> {
  constructor(props: EditorOutlineSidebarProps) {
    super(props);
    this.onOpenClicked = this.onOpenClicked.bind(this);
    this.onCloseClicked = this.onCloseClicked.bind(this);
  }

  public componentDidMount() {
    // register command used to toggle pane
    this.props.commandManager.addCommands([
      {
        id: WorkbenchCommandId.ShowOutline,
        menuText: this.props.t('commands:show_outline_menu_text'),
        group: this.props.t('commands:group_view'),
        keymap: ['Ctrl-Alt-O'],
        isEnabled: () => true,
        isActive: () => this.props.showOutline,
        execute: () => {
          this.props.setShowOutline(!this.props.showOutline);
        },
      },
    ]);
  }

  public render() {
    const outlineClassName = [styles.outline];
    if (this.props.showOutline) {
      outlineClassName.push(styles.outlineVisible);
    }

    return (
      <>
        <EditorOutlineButton visible={!this.props.showOutline} onClick={this.onOpenClicked} />
        <CSSTransition in={this.props.showOutline} timeout={200} classNames={{ ...transition }}>
          <div className={outlineClassName.join(' ')}>
            <EditorOutlineHeader onCloseClicked={this.onCloseClicked} />
            {this.props.outline.length ? <EditorOutlineTree outline={this.props.outline} /> : <EditorOutlineEmpty />}
          </div>
        </CSSTransition>
      </>
    );
  }

  private onOpenClicked() {
    this.props.setShowOutline(true);
  }

  private onCloseClicked() {
    this.props.setShowOutline(false);
  }
}

const mapStateToProps = (state: WorkbenchState) => {
  return {
    showOutline: state.prefs.showOutline,
    outline: state.editor.outline,
  };
};

const mapDispatchToProps = (dispatch: any) => {
  return {
    setShowOutline: (showOutline: boolean) => dispatch(setPrefsShowOutline(showOutline)),
  };
};

export default withCommandManager(
  // @ts-ignore
  withTranslation()(connect(mapStateToProps, mapDispatchToProps)(EditorOutlineSidebar)),
);
