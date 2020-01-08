import React from 'react';
import { useTranslation } from 'react-i18next';

import { Checkbox, FormGroup, HTMLSelect } from '@blueprintjs/core';

import { OrderedListProps, OrderedListEditResult } from 'editor/src/api/ui';
import { ListNumberStyle, ListNumberDelim } from 'editor/src/nodes/list/list';
import { ListCapabilities } from 'editor/src/api/list';

import { Dialog } from 'workbench/widgets/dialog/Dialog';
import { DialogNumericInput } from 'workbench/widgets/dialog/DialogInputs';
import { focusInput } from 'workbench/widgets/utils';

export interface EditorDialogEditOrderedListProps {
  list: OrderedListProps;
  capabilities: ListCapabilities;
  isOpen: boolean;
  onClosed: (result: OrderedListEditResult | null) => void;
}

export function defaultEditOrderedListProps(): EditorDialogEditOrderedListProps {
  return {
    list: {
      tight: true,
      order: 1,
      number_style: ListNumberStyle.Decimal.toString(),
      number_delim: ListNumberDelim.Period.toString(),
    },
    capabilities: {
      tasks: true,
      order: true,
      fancy: true,
      example: true,
    },
    isOpen: false,
    onClosed: () => {
      /* */
    },
  };
}

export const EditorDialogEditOrderedList: React.FC<EditorDialogEditOrderedListProps> = props => {
  const { t } = useTranslation();

  let inputOrder: HTMLInputElement | null = null;
  const setInputOrder = (input: HTMLInputElement | null) => (inputOrder = input);

  let inputNumberStyle: HTMLSelectElement | null = null;
  const setInputNumberStyle = (input: HTMLSelectElement | null) => (inputNumberStyle = input);

  let inputNumberDelimiter: HTMLSelectElement | null = null;
  const setInputNumberDelimiter = (input: HTMLSelectElement | null) => (inputNumberDelimiter = input);

  let inputTight: HTMLInputElement | null = null;
  const setInputTight = (input: HTMLInputElement | null) => (inputTight = input);

  const onOpened = () => {
    if (inputOrder) {
      focusInput(inputOrder);

      // for whatever reason the Enter key is reloading the page,
      // suppress that behavior here
      inputOrder.addEventListener('keypress', (e: KeyboardEvent) => {
        if (e.key === 'Enter') {
          e.preventDefault();
          e.stopPropagation();
          onOK();
        }
      });
    }
  };

  const onOK = () => {
    props.onClosed({
      ...props.list,
      tight: inputTight!.checked,
      order: props.capabilities.order ? parseInt(inputOrder!.value || '1', 10) : 1,
      number_style: props.capabilities.fancy ? inputNumberStyle!.value : ListNumberStyle.DefaultStyle,
      number_delim: props.capabilities.fancy ? inputNumberDelimiter!.value : ListNumberDelim.DefaultDelim,
    });
  };

  const onCancel = () => {
    props.onClosed(null);
  };

  return (
    <Dialog
      isOpen={props.isOpen}
      title={t('edit_ordered_list_dialog_caption')}
      onOpened={onOpened}
      onOK={onOK}
      onCancel={onCancel}
    >
      {props.capabilities.order ? (
        <DialogNumericInput
          defaultValue={props.list.order.toString()}
          label={t('edit_ordered_list_dialog_order')}
          min={1}
          max={100}
          ref={setInputOrder}
        />
      ) : null}
      {props.capabilities.fancy ? (
        <>
          <FormGroup label={t('edit_ordered_list_dialog_style')}>
            <HTMLSelect
              defaultValue={props.list.number_style}
              elementRef={setInputNumberStyle}
              options={Object.values(ListNumberStyle).filter(
                value => props.capabilities.example || value !== ListNumberStyle.Example,
              )}
              fill={true}
            />
          </FormGroup>
          <FormGroup
            label={t('edit_ordered_list_dialog_delimiter')}
            helperText={t('edit_ordered_list_dialog_delimiter_helper_text')}
          >
            <HTMLSelect
              defaultValue={props.list.number_delim}
              elementRef={setInputNumberDelimiter}
              options={Object.values(ListNumberDelim)}
              fill={true}
            />
          </FormGroup>
        </>
      ) : null}
      <FormGroup>
        <Checkbox defaultChecked={props.list.tight} inputRef={setInputTight}>
          {t('edit_ordered_list_dialog_tight')}
        </Checkbox>
      </FormGroup>
    </Dialog>
  );
};
