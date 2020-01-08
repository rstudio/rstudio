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
