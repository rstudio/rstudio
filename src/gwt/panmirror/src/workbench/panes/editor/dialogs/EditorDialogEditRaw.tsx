/*
 * EditorDialogEditRaw.tsx
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
import { useTranslation } from 'react-i18next';

import { Button, FormGroup, TextArea } from '@blueprintjs/core';

import { RawFormatResult, RawFormatProps } from 'editor/src/api/ui';

import { Dialog } from 'workbench/widgets/dialog/Dialog';
import { focusInput } from 'workbench/widgets/utils';

import { RawFormatSelect } from './RawFormatSelect';

import styles from './EditorDialogEditRaw.module.scss';

export interface EditorDialogEditRawProps {
  raw: RawFormatProps;
  minRows?: number;
  isOpen: boolean;
  onClosed: (result: RawFormatResult | null) => void;
}

export function defaultEditRawProps(): EditorDialogEditRawProps {
  return {
    raw: { content: '', format: '' },
    isOpen: false,
    onClosed: () => {
      /* */
    },
  };
}

export const EditorDialogEditRaw: React.FC<EditorDialogEditRawProps> = props => {
  const { t } = useTranslation();

  const removeEnabled = !!props.raw.format;

  let formatInput: HTMLSelectElement | null = null;
  const setFormatInput = (input: HTMLSelectElement | null) => {
    if (input) {
      formatInput = input;
      if (props.raw.format) {
        formatInput.value = props.raw.format;
      }
      focusInput(formatInput);
    }
  };

  let contentInput: HTMLTextAreaElement | null;
  const setContentInput = (ref: HTMLTextAreaElement | null) => {
    contentInput = ref;
  };

  const onActionClicked = (action: 'edit' | 'remove') => {
    return () => {
      // get input
      const raw = {
        format: formatInput!.value,
        content: contentInput!.value,
      };
      // edit w/o input means cancel
      if (action === 'edit' && !raw.format) {
        onCancel();
      } else {
        props.onClosed({ action, raw });
      }
    };
  };

  const onCancel = () => {
    props.onClosed(null);
  };

  const removeButton = removeEnabled ? (
    <Button onClick={onActionClicked('remove')}>{t('edit_raw_dialog_remove')}</Button>
  ) : (
    undefined
  );

  const kMaxRows = 10;
  const { minRows = 1 } = props;
  const rows = Math.min(kMaxRows, Math.max(minRows, props.raw.content.split('\n').length));

  return (
    <Dialog
      isOpen={props.isOpen}
      title={t('edit_raw_dialog_caption')}
      onOK={onActionClicked('edit')}
      onCancel={onCancel}
      leftButtons={removeButton}
    >
      <RawFormatSelect fill={true} elementRef={setFormatInput} />
      <FormGroup label={t('edit_raw_dialog_content')}>
        <TextArea
          defaultValue={props.raw.content}
          inputRef={setContentInput}
          fill={true}
          rows={rows}
          className={styles.rawContent}
        />
      </FormGroup>
    </Dialog>
  );
};
