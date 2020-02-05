/*
 * DialogInputs.tsx
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

import { Classes, FormGroup } from '@blueprintjs/core';

export interface DialogInputProps {
  label: string;
  labelInfo?: string;
  placeholder?: string;
  readOnly?: boolean;

  // controlled api
  value?: string;
  onChange?: React.FormEventHandler<HTMLInputElement>;

  // uncontrolled api (use w/ ref)
  defaultValue?: string;
}

export type DialogTextInputProps = DialogInputProps;

export const DialogTextInput = dialogInput<DialogTextInputProps>('text');

export interface DialogNumericInputProps extends DialogInputProps {
  min: number;
  max: number;
}

export const DialogNumericInput = dialogInput<DialogNumericInputProps>('number');

function dialogInput<P extends DialogInputProps>(type: string) {
  return React.forwardRef<HTMLInputElement, P>((props: P, ref) => {
    const { label, labelInfo, defaultValue, value, placeholder, readOnly, onChange, ...passThroughProps } = props;

    return (
      <FormGroup label={props.label} labelInfo={labelInfo}>
        <input
          type={type}
          className={[Classes.INPUT, Classes.FILL].join(' ')}
          value={value}
          onChange={onChange}
          placeholder={placeholder}
          defaultValue={defaultValue}
          ref={ref}
          readOnly={readOnly}
          {...passThroughProps}
        />
      </FormGroup>
    );
  });
}
