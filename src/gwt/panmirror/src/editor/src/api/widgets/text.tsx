/*
 * text.tsx
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

import React, { ChangeEventHandler, KeyboardEventHandler } from 'react';

import { WidgetProps } from './react';

import './text.css';

export interface TextInputProps extends WidgetProps {
  widthChars: number;
  tabIndex?: number;
  className?: string;
  placeholder?: string;
  iconAdornment?: string;
  onChange?: ChangeEventHandler;
  onKeyDown?: KeyboardEventHandler<HTMLInputElement>;
  onKeyUp?: KeyboardEventHandler<HTMLInputElement>;
}

export const TextInput = React.forwardRef<HTMLInputElement, TextInputProps>((props, ref) => {
  const style: React.CSSProperties = {
    ...props.style,
    width: props.widthChars + 'ch',
  };

  return (
    <div className="pm-textinput-container">
      <img src={props.iconAdornment} className="pm-textinput-icon" alt="" />
      <input
        type="text"
        placeholder={props.placeholder}
        className={`pm-input-text pm-textinput-input pm-text-color pm-background-color ${props.className}`}
        style={style}
        onChange={props.onChange}
        onKeyDown={props.onKeyDown}
        onKeyUp={props.onKeyUp}
        tabIndex={props.tabIndex}
        ref={ref}
      />
    </div>
  );
});
