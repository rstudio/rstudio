/*
 * dialog-buttons.tsx
 *
 * Copyright (C) 2020 by RStudio, PBC
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

import { WidgetProps } from "./react";

import { EditorUI } from "../ui";

import { TextButton } from "./button"; import React from "react";

import './dialog-buttons.css';


export interface DialogButtonsProps extends WidgetProps {
  okLabel: string;
  cancelLabel: string;
  onOk: () => void;
  onCancel: () => void;
}

export const DialogButtons: React.FC<DialogButtonsProps> = props => {
  return (
    <div className='pm-dialog-buttons-panel' style={props.style}>
      <TextButton
        title={props.okLabel}
        classes={['pm-default-theme', 'pm-dialog-buttons-button', 'pm-rstudio-button']}
        onClick={props.onOk}
        style={{
          fontWeight: 600
        }} />

      <TextButton
        title={props.cancelLabel}
        classes={['pm-default-theme', 'pm-dialog-buttons-button', 'pm-rstudio-button']}
        onClick={props.onCancel}
      />
    </div>
  );
};
