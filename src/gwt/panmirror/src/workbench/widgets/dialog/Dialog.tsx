import React from 'react';

import { Classes, Button, Intent, Dialog as BlueprintDialog, IProps } from '@blueprintjs/core';

import dialogStyles from './Dialog.module.scss';

export interface DialogProps extends IProps {
  title: string;
  isOpen: boolean;
  leftButtons?: JSX.Element;
  onOpening?: () => void;
  onOpened?: () => void;
  onOK?: () => void;
  onCancel?: () => void;
}

export const Dialog: React.FC<DialogProps> = props => {
  const onKeyUp = (event: React.KeyboardEvent<HTMLFormElement>) => {
    const isTextArea = () => {
      return document.activeElement && document.activeElement.tagName === 'TEXTAREA';
    };
    if (event.keyCode === 13 && props.onOK && !isTextArea()) {
      props.onOK();
    }
  };

  return (
    <BlueprintDialog
      isOpen={props.isOpen}
      title={props.title}
      onOpening={props.onOpening}
      onOpened={props.onOpened}
      autoFocus={true}
      canEscapeKeyClose={true}
      onClose={props.onCancel}
    >
      <form onKeyUp={onKeyUp}>
        <div className={Classes.DIALOG_BODY}>{props.children}</div>
        <div className={[Classes.DIALOG_FOOTER, dialogStyles.dialogFooter].join(' ')}>
          <div className={[Classes.DIALOG_FOOTER_ACTIONS, dialogStyles.dialogFooterActions].join(' ')}>
            <div className={dialogStyles.dialogFooterActionsLeft}>{props.leftButtons}</div>
            <div className={dialogStyles.dialogFooterActionsRight}>
              <Button onClick={props.onCancel}>Cancel</Button>
              <Button intent={Intent.PRIMARY} onClick={props.onOK}>
                OK
              </Button>
            </div>
          </div>
        </div>
      </form>
    </BlueprintDialog>
  );
};
