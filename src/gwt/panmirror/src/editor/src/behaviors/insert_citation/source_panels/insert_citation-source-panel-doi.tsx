/*
 * insert_citation-panel-doi.ts
 *
 * Copyright (C) 2020 by RStudio, PBC
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
import React from "react";
import debounce from "lodash.debounce";

import { EditorUI } from "../../../api/ui";

import { CSL } from "../../../api/csl";
import { TextInput } from "../../../api/widgets/text";
import { formatForPreview, CiteField, suggestCiteId } from "../../../api/cite";
import { BibliographyManager, BibliographySource } from "../../../api/bibliography/bibliography";

import { CitationSourcePanelProps, CitationSourcePanel } from "../insert_citation-panel";
import { CitationSourcePanelListItem } from "./insert_citation-source-panel-list-item";

import './insert_citation-source-panel-doi.css';

const kDOIType = 'DOI Search';

export function doiSourcePanel(ui: EditorUI): CitationSourcePanel {
  return {
    key: '76561E2A-8FB7-4D4B-B235-9DD8B8270EA1',
    panel: DOISourcePanel,
    treeNode: {
      key: 'DOI',
      name: ui.context.translateText('Lookup DOI'),
      image: ui.images.citations?.doi,
      type: kDOIType,
      children: [],
      expanded: true
    }
  };
}

export const DOISourcePanel: React.FC<CitationSourcePanelProps> = props => {

  const defaultMessage = props.ui.context.translateText('Paste a DOI to load data from Crossref, DataCite, or mEDRA.');
  const noMatchingResultsMessage = props.ui.context.translateText('No item matching this identifier could be located.');

  const [csl, setCsl] = React.useState<CSL>();
  const [noResultsText, setNoResultsText] = React.useState<string>(defaultMessage);
  const [searchText, setSearchText] = React.useState<string>('');
  const [previewFields, setPreviewFields] = React.useState<CiteField[]>([]);

  React.useEffect(() => {
    if (csl) {
      const preview = formatForPreview(csl);
      setPreviewFields(preview);
    } else {
      setPreviewFields([]);
      if (searchText.length > 0) {
        setNoResultsText(noMatchingResultsMessage);
      } else {
        setNoResultsText(defaultMessage);
      }
    }
  }, [csl]);

  React.useEffect(() => {
    if (searchText) {
      const debounced = debounce(async () => {
        const result = await props.server.doi.fetchCSL(searchText, 350);
        if (result.status === 'ok') {
          setCsl(result.message);
        } else {
          setCsl(undefined);
        }
      }, 50);
      debounced();
    } else {
      setCsl(undefined);
    }
  }, [searchText]);

  const doiChanged = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchText(e.target.value);
  };

  return (
    <div style={props.style} className='pm-insert-doi-source-panel'>
      <div className='pm-insert-doi-source-panel-textbox-container'>
        <TextInput
          width='100%'
          iconAdornment={props.ui.images.search}
          tabIndex={0}
          className='pm-insert-doi-source-panel-textbox pm-block-border-color'
          placeholder={props.ui.context.translateText('Search for a DOI')}
          onChange={doiChanged}
        />
      </div>
      <div className='pm-insert-doi-source-panel-heading'>
        {csl ?
          <CitationSourcePanelListItem
            index={0}
            data={{
              allSources: toBibliographyEntry(csl, props.bibliographyManager, props.ui),
              sourcesToAdd: props.sourcesToAdd,
              addSource: props.addSource,
              removeSource: props.removeSource,
              ui: props.ui
            }}
            style={{}}
            isScrolling={false}
          /> :
          <div className='pm-insert-doi-source-panel-no-result'>
            <div className='pm-insert-doi-source-panel-no-result-text'>
              {props.ui.context.translateText(noResultsText)}
            </div>
          </div>}
      </div>
      <div className='pm-insert-doi-source-panel-fields'>
        <table>
          <tbody>
            {previewFields.map(previewField =>
              (<tr key={previewField.name}>
                <td className='pm-insert-doi-source-panel-fields-name'>{previewField.name}:</td>
                <td className='pm-insert-doi-source-panel-fields-value'>{previewField.value}</td>
              </tr>)
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};

function toBibliographyEntry(csl: CSL | undefined, bibliographyManager: BibliographyManager, ui: EditorUI): BibliographySource[] {
  if (csl) {
    return [
      {
        ...csl,
        id: suggestCiteId(bibliographyManager.allSources().map(source => source.id), csl),
        providerKey: 'doi',
        collectionKeys: [],
      }
    ];
  }
  return [];
}