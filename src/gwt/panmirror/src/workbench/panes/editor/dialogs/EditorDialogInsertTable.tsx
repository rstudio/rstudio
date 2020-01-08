import React from 'react';
import { useTranslation } from 'react-i18next';

import { ControlGroup, FormGroup, Checkbox } from '@blueprintjs/core';

import { InsertTableResult } from 'editor/src/api/ui';

import { Dialog } from 'workbench/widgets/dialog/Dialog';
import { focusInput } from 'workbench/widgets/utils';
import { DialogNumericInput, DialogTextInput } from 'workbench/widgets/dialog/DialogInputs';

export interface EditorDialogInsertTableProps {
  isOpen: boolean;
  onClosed: (result: InsertTableResult | null) => void;
}

export function defaultInsertTableProps(): EditorDialogInsertTableProps {
  return {
    isOpen: false,
    onClosed: () => {
      /* */
    },
  };
}

export const EditorDialogInsertTable: React.FC<EditorDialogInsertTableProps> = props => {
  const { t } = useTranslation();

  const defaultRows = '3';
  let inputRows: HTMLInputElement | null = null;
  const setInputRows = (input: HTMLInputElement | null) => (inputRows = input);

  const defaultCols = '3';
  let inputCols: HTMLInputElement | null = null;
  const setInputCols = (input: HTMLInputElement | null) => (inputCols = input);

  let inputHeader: HTMLInputElement | null = null;
  const setInputHeader = (input: HTMLInputElement | null) => (inputHeader = input);

  let inputCaption: HTMLInputElement | null = null;
  const setInputCaption = (input: HTMLInputElement | null) => (inputCaption = input);

  const onOpened = () => {
    focusInput(inputRows);
  };

  const onOK = () => {
    props.onClosed({
      rows: parseInt(inputRows!.value || defaultRows, 10),
      cols: parseInt(inputCols!.value || defaultCols, 10),
      header: inputHeader!.checked,
      caption: inputCaption!.value,
    });
  };

  const onCancel = () => {
    props.onClosed(null);
  };

  return (
    <Dialog
      isOpen={props.isOpen}
      title={t('insert_table_dialog_caption')}
      onOpened={onOpened}
      onOK={onOK}
      onCancel={onCancel}
    >
      <ControlGroup fill={true} style={{ marginRight: '10px' }}>
        <DialogNumericInput
          defaultValue={defaultRows}
          label={t('insert_table_dialog_rows')}
          min={1}
          max={1000}
          ref={setInputRows}
        />
        <DialogNumericInput
          defaultValue={defaultCols}
          label={t('insert_table_dialog_cols')}
          min={1}
          max={1000}
          ref={setInputCols}
        />
      </ControlGroup>
      <DialogTextInput
        defaultValue={''}
        label={t('insert_table_dialog_table_caption')}
        labelInfo={t('label_optional')}
        ref={setInputCaption}
      />
      <FormGroup>
        <Checkbox defaultChecked={true} inputRef={setInputHeader}>
          {t('insert_table_dialog_header')}
        </Checkbox>
      </FormGroup>
    </Dialog>
  );
};
