/*
 * RawFormatSelect.tsx
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
