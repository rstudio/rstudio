import React from 'react';

import { useTranslation } from 'react-i18next';

import { HTMLSelect, IHTMLSelectProps, FormGroup } from '@blueprintjs/core';

export const RawFormatSelect: React.FC<IHTMLSelectProps> = props => {
  const { t } = useTranslation();

  const options = [
    { value: '', label: t('edit_raw_dialog_choose_format') },
    { value: 'asciidoc' },
    { value: 'asciidoctor' },
    { value: 'beamer' },
    { value: 'commonmark' },
    { value: 'context' },
    { value: 'docbook' },
    { value: 'docbook4' },
    { value: 'docbook5' },
    { value: 'docx' },
    { value: 'dokuwiki' },
    { value: 'dzslides' },
    { value: 'epub' },
    { value: 'epub2' },
    { value: 'epub3' },
    { value: 'fb2' },
    { value: 'gfm' },
    { value: 'haddock' },
    { value: 'html' },
    { value: 'html4' },
    { value: 'html5' },
    { value: 'icml' },
    { value: 'ipynb' },
    { value: 'jats' },
    { value: 'jira' },
    { value: 'json' },
    { value: 'latex' },
    { value: 'man' },
    { value: 'markdown' },
    { value: 'markdown_github' },
    { value: 'markdown_mmd' },
    { value: 'markdown_phpextra' },
    { value: 'markdown_strict' },
    { value: 'mediawiki' },
    { value: 'ms' },
    { value: 'muse' },
    { value: 'native' },
    { value: 'odt' },
    { value: 'openxml' },
    { value: 'opendocument' },
    { value: 'opml' },
    { value: 'org' },
    { value: 'plain' },
    { value: 'pptx' },
    { value: 'revealjs' },
    { value: 'rst' },
    { value: 'rtf' },
    { value: 's5' },
    { value: 'slideous' },
    { value: 'slidy' },
    { value: 'tei' },
    { value: 'texinfo' },
    { value: 'textile' },
    { value: 'xwiki' },
    { value: 'zimwiki' },
  ];

  return (
    <FormGroup label={t('edit_raw_dialog_format')}>
      <HTMLSelect {...props} options={options} />
    </FormGroup>
  );
};
