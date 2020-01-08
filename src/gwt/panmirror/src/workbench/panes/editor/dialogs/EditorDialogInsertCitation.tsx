import React from 'react';
import { useTranslation } from 'react-i18next';

import { InsertCitationResult } from 'editor/src/api/ui';

import { Dialog } from 'workbench/widgets/dialog/Dialog';
import { DialogTextInput } from 'workbench/widgets/dialog/DialogInputs';
import { focusInput } from 'workbench/widgets/utils';

export interface EditorDialogInsertCitationProps {
  isOpen: boolean;
  onClosed: (result: InsertCitationResult | null) => void;
}

export function defaultInsertCitationProps(): EditorDialogInsertCitationProps {
  return {
    isOpen: false,
    onClosed: () => {
      /* */
    },
  };
}

export const EditorDialogInsertCitation: React.FC<EditorDialogInsertCitationProps> = props => {
  const { t } = useTranslation();

  const idInput = React.createRef<HTMLInputElement>();
  const locatorInput = React.createRef<HTMLInputElement>();

  const onOpened = () => {
    focusInput(idInput.current);
  };

  const onOK = () => {
    if (idInput.current!.value) {
      props.onClosed({
        id: idInput.current!.value,
        locator: locatorInput.current!.value,
      });
    } else {
      onCancel();
    }
  };

  const onCancel = () => {
    props.onClosed(null);
  };

  return (
    <Dialog
      isOpen={props.isOpen}
      title={t('insert_citation_dialog_caption')}
      onOpened={onOpened}
      onOK={onOK}
      onCancel={onCancel}
    >
      <DialogTextInput
        label={t('insert_citation_dialog_id')}
        labelInfo={t('insert_citation_dialog_id_info')}
        ref={idInput}
      />
      <DialogTextInput
        label={t('insert_citation_dialog_locator')}
        labelInfo={t('insert_citation_dialog_locator_info')}
        placeholder={t('label_optional')}
        ref={locatorInput}
      />
    </Dialog>
  );
};
