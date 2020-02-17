/*
 * AtrrEditor.tsx
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

import { TFunction } from 'i18next';
import { Translation } from 'react-i18next';

import { TextArea, FormGroup } from '@blueprintjs/core';

import { AttrProps, attrPropsToInput, attrInputToProps } from 'editor/src/api/ui';

import { DialogTextInput } from 'workbench/widgets/dialog/DialogInputs';
import { focusInput } from 'workbench/widgets/utils';

export interface AttrEditorProps {
  defaultValue: AttrProps;
}

export class AttrEditor extends React.Component<AttrEditorProps> {
  private idInput: React.RefObject<HTMLInputElement>;
  private classesInput: React.RefObject<HTMLInputElement>;
  private keyvalueInput: HTMLTextAreaElement | null;

  constructor(props: AttrEditorProps) {
    super(props);
    this.state = {};
    this.idInput = React.createRef<HTMLInputElement>();
    this.classesInput = React.createRef<HTMLInputElement>();
    this.keyvalueInput = null;
  }

  public focus() {
    focusInput(this.idInput.current);
  }

  public get value(): AttrProps {
    return attrInputToProps({
      id: this.idInput.current!.value,
      classes: this.classesInput.current!.value,
      keyvalue: this.keyvalueInput!.value,
    });
  }

  public render() {
    const value = attrPropsToInput(this.props.defaultValue);

    const setKeyvalueInput = (ref: HTMLTextAreaElement | null) => {
      this.keyvalueInput = ref;
    };

    return (
      <Translation>
        {(t: TFunction) => (
          <>
            <DialogTextInput label={t('attr_editor_id')} defaultValue={value.id} ref={this.idInput} />
            <DialogTextInput label={t('attr_editor_classes')} defaultValue={value.classes} ref={this.classesInput} />
            <FormGroup label={t('attr_editor_keyvalue')}>
              <TextArea defaultValue={value.keyvalue} inputRef={setKeyvalueInput} fill={true} />
            </FormGroup>
          </>
        )}
      </Translation>
    );
  }
}

interface AttrEditorInput {
  id?: string;
  classes?: string;
  keyvalue?: string;
}
