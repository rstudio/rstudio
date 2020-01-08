import React from 'react';

import { TFunction } from 'i18next';
import { Translation } from 'react-i18next';

import { TextArea, FormGroup } from '@blueprintjs/core';

import { AttrProps } from 'editor/src/api/ui';

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
    return pandocAttrFromInput({
      id: asPandocId(this.idInput.current!.value),
      classes: this.classesInput
        .current!.value.split(/\s/)
        .map(asPandocClass)
        .join(' '),
      keyvalue: this.keyvalueInput!.value,
    });
  }

  public render() {
    const value = pandocAttrToInput(this.props.defaultValue);

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

function pandocAttrToInput(attr: AttrProps): AttrEditorInput {
  return {
    id: asHtmlId(attr.id) || undefined,
    classes: attr.classes ? attr.classes.map(asHtmlClass).join(' ') : undefined,
    keyvalue: attr.keyvalue ? attr.keyvalue.map(keyvalue => `${keyvalue[0]}=${keyvalue[1]}`).join('\n') : undefined,
  };
}

function asHtmlId(id: string | undefined) {
  if (id) {
    if (id.startsWith('#')) {
      return id;
    } else {
      return '#' + id;
    }
  } else {
    return id;
  }
}

function asHtmlClass(clz: string | undefined) {
  if (clz) {
    if (clz.startsWith('.')) {
      return clz;
    } else {
      return '.' + clz;
    }
  } else {
    return clz;
  }
}

function asPandocId(id: string) {
  return id.replace(/^#/, '');
}

function asPandocClass(clz: string) {
  return clz.replace(/^\./, '');
}

function pandocAttrFromInput(attr: AttrEditorInput): AttrProps {
  const classes = attr.classes ? attr.classes.split(/\s+/) : [];
  let keyvalue: Array<[string, string]> | undefined;
  if (attr.keyvalue) {
    const lines = attr.keyvalue.trim().split('\n');
    keyvalue = lines.map(line => line.trim().split('=') as [string, string]);
  }
  return {
    id: attr.id,
    classes,
    keyvalue,
  };
}
