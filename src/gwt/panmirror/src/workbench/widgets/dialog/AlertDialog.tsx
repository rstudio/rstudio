import React from 'react';

import { Alert, IconName, IProps } from '@blueprintjs/core';
import { IconNames } from '@blueprintjs/icons';

import { AlertType } from 'editor/src/api/ui';

import styles from './AlertDialog.module.scss';

export interface AlertDialogProps extends IProps {
  title?: string;
  message?: string;
  type: AlertType;
  isOpen: boolean;
  onClosed: () => void;
}

export const AlertDialog: React.FC<AlertDialogProps> = props => {
  let icon: IconName;
  switch (props.type) {
    case AlertType.Error:
      icon = IconNames.ERROR;
      break;
    case AlertType.Warning:
      icon = IconNames.WARNING_SIGN;
      break;
    case AlertType.Info:
    default:
      icon = IconNames.INFO_SIGN;
      break;
  }

  const children = props.children ? props.children : <p>{props.message}</p>;

  return (
    <Alert
      isOpen={props.isOpen}
      onClose={props.onClosed}
      onCancel={props.onClosed}
      canOutsideClickCancel={true}
      canEscapeKeyCancel={true}
      icon={icon}
      className={styles.alertDialog}
    >
      <p>
        <strong>{props.title}</strong>
      </p>
      {children}
    </Alert>
  );
};
