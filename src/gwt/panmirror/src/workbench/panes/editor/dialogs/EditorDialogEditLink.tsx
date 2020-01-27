/*
 * EditorDialogEditLink.tsx
 *
 * Copyright (C) 2019-20 by RStudio, Inc.
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

import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';

import { Button } from '@blueprintjs/core';

import { LinkEditResult, LinkProps } from 'editor/src/api/ui';
import { LinkTargets, LinkCapabilities, LinkType } from 'editor/src/api/link';

import { Dialog } from 'workbench/widgets/dialog/Dialog';
import { DialogTextInput } from 'workbench/widgets/dialog/DialogInputs';
import { focusInput } from 'workbench/widgets/utils';

import { AttrEditor } from './AttrEditor';
import { HRefSelect } from './HRefSelect';

export interface EditorDialogEditLinkProps {
  link: LinkProps;
  targets: LinkTargets;
  capabilities: LinkCapabilities;
  isOpen: boolean;
  onClosed: (result: LinkEditResult | null) => void;
}

export function defaultEditLinkProps(): EditorDialogEditLinkProps {
  return {
    link: {
      type: LinkType.ID,
      text: '',
      href: '',
    },
    targets: {
      ids: [],
      headings: [],
    },
    isOpen: false,
    capabilities: {
      headings: true,
      attributes: true,
    },
    onClosed: () => {
      /* */
    },
  };
}

export const EditorDialogEditLink: React.FC<EditorDialogEditLinkProps> = props => {
  const { t } = useTranslation();

  const [target, setTarget] = useState({ type: props.link.type, href: props.link.href });

  const textInput = React.createRef<HTMLInputElement>();
  const titleInput = React.createRef<HTMLInputElement>();
  const attrInput = React.createRef<AttrEditor>();

  let typeInput: HTMLSelectElement | null;
  const setTypeInput = (ref: HTMLSelectElement | null) => {
    typeInput = ref;
  };

  const onTargetChanged = (type: LinkType, href: string) => {
    setTarget({ type, href });
  };

  const onOpening = () => {
    setTarget({ type: props.link.type, href: props.link.href });
  };

  const onOpened = () => {
    if (textInput.current && !textInput.current!.value) {
      focusInput(textInput.current);
    } else if (typeInput) {
      focusInput(typeInput);
    }
  };

  const onActionClicked = (action: 'edit' | 'remove') => {
    return () => {
      if (target.type === LinkType.Heading) {
        props.onClosed({
          action,
          link: {
            type: LinkType.Heading,
            text: target.href,
            href: target.href,
          },
        });
      } else {
        if (action === 'edit' && (!textInput.current!.value || !target.href)) {
          onCancel();
        } else {
          props.onClosed({
            action,
            link: {
              type: target.type,
              text: textInput.current!.value,
              href: target.href,
              title: titleInput.current!.value,
              ...(props.capabilities.attributes ? attrInput.current!.value : {}),
            },
          });
        }
      }
    };
  };

  const onOK = () => {
    onActionClicked('edit')();
  };

  const onCancel = () => {
    props.onClosed(null);
  };

  const removeButton = props.link.href ? (
    <Button onClick={onActionClicked('remove')}>{t('edit_link_dialog_remove')}</Button>
  ) : (
    undefined
  );

  return (
    <Dialog
      isOpen={props.isOpen}
      title={t('edit_link_dialog_caption')}
      onOpening={onOpening}
      onOpened={onOpened}
      onOK={onOK}
      onCancel={onCancel}
      leftButtons={removeButton}
    >
      <HRefSelect
        href={props.link.href}
        type={props.link.type}
        capabilities={props.capabilities}
        targets={props.targets}
        typeRef={setTypeInput}
        onChange={onTargetChanged}
      />
      {target.type !== LinkType.Heading ? (
        <>
          <DialogTextInput label={t('edit_link_dialog_text')} defaultValue={props.link.text} ref={textInput} />
          <DialogTextInput label={t('edit_link_dialog_title')} defaultValue={props.link.title} ref={titleInput} />
          {props.capabilities.attributes ? <AttrEditor defaultValue={props.link} ref={attrInput} /> : null}
        </>
      ) : null}
    </Dialog>
  );
};
