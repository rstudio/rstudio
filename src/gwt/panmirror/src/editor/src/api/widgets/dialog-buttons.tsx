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
  onOk: () => void;
  onCancel: () => void;
  ui: EditorUI;
}

export const DialogButtons: React.FC<DialogButtonsProps> = props => {
  const className = ['pm-link', 'pm-link-text-color'].concat(props.classes || []).join(' ');

  const style: React.CSSProperties = {
    ...props.style,
    height: '28px',
    width: '80px',
    borderRadius: '6px',
    fontSize: '12px',
    fontFamily: 'Lucida Sans, DejaVu Sans, Lucida Grande, Segoe UI, Verdana, Helvetica, sans-serif, serif',
  };

  return (
    <div className='pm-dialog-buttons-panel' style={props.style}>
      <TextButton
        title={props.ui.context.translateText('OK')}
        classes={['pm-default-theme', 'pm-dialog-buttons-button']}
        onClick={props.onOk}
        style={{
          fontWeight: 600,
          opacity: 0.8,
          marginRight: '6px',
        }} />

      <TextButton
        title={props.ui.context.translateText('Cancel')}
        classes={['pm-default-theme', 'pm-dialog-buttons-button']}
        onClick={props.onCancel}
      />
    </div>
  );
};
