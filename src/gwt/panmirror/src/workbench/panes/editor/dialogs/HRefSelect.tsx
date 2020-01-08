import React, { useState } from 'react';

import { useTranslation } from 'react-i18next';

import { ControlGroup, FormGroup, HTMLSelect, Classes, InputGroup } from '@blueprintjs/core';

import { LinkTargets, LinkCapabilities, LinkType } from 'editor/src/api/link';

export interface HRefSelectProps {
  type: LinkType;
  href: string;
  onChange: (type: LinkType, href: string) => any;
  targets: LinkTargets;
  capabilities: LinkCapabilities;
  typeRef: (ref: HTMLSelectElement | null) => any;
}

export const HRefSelect: React.RefForwardingComponent<HTMLInputElement, HRefSelectProps> = props => {
  const { t } = useTranslation();

  const [type, setType] = useState(props.type);
  const [href, setHRef] = useState(props.href);

  const suggestionsForType = (type: LinkType) => {
    switch (type) {
      case LinkType.URL:
        return [];
      case LinkType.Heading:
        return props.targets.headings.map(heading => ({
          label: heading.text,
          value: heading.text,
        }));
      case LinkType.ID:
        return props.targets.ids.map(id => ({ value: '#' + id }));
    }
  };

  const defaultHRefForType = (type: LinkType) => {
    const suggestions = suggestionsForType(type);
    return suggestions.length ? suggestions[0].value : '';
  };

  return (
    <FormGroup label={t('edit_link_dialog_href')}>
      <ControlGroup fill={true}>
        <HTMLSelect
          defaultValue={type.toString()}
          onChange={event => {
            const type = parseInt(event.currentTarget.value);
            const href = defaultHRefForType(type);
            setType(type);
            setHRef(href);
            props.onChange(type, href);
          }}
          elementRef={props.typeRef}
          options={[
            { label: t('edit_link_dialog_type_url'), value: LinkType.URL },
            ...(props.capabilities.headings
              ? [{ label: t('edit_link_dialog_type_heading'), value: LinkType.Heading }]
              : []),
            { label: t('edit_link_dialog_type_id'), value: LinkType.ID },
          ]}
          className={Classes.FIXED}
        />

        {type === LinkType.URL ? (
          <InputGroup
            defaultValue={href}
            onChange={(event: React.ChangeEvent<HTMLInputElement>) => {
              const href = event.currentTarget.value;
              setHRef(href);
              props.onChange(type, href);
            }}
            fill={true}
          />
        ) : (
          <HTMLSelect
            defaultValue={href}
            onChange={event => {
              const href = event.currentTarget.value;
              setHRef(href);
              props.onChange(type, href);
            }}
            options={suggestionsForType(type)}
            fill={true}
          />
        )}
      </ControlGroup>
    </FormGroup>
  );
};
