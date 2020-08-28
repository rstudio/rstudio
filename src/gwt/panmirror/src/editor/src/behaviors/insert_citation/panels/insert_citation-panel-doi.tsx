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

import { EditorUI } from "../../../api/ui";

import { CitationPanelProps, CitationPanel } from "../insert_citation-picker";

import './insert_citation-panel-doi.css';
import { TextInput } from "../../../api/widgets/text";
import debounce from "lodash.debounce";
import { CSL } from "../../../api/csl";
import { formatForPreview, CiteField, suggestCiteId } from "../../../api/cite";
import { CitationListItem } from "./insert_citation-panel-list-item";
import { BibliographyManager, BibliographySource } from "../../../api/bibliography/bibliography";

export function doiPanel(ui: EditorUI): CitationPanel {
  return {
    key: '76561E2A-8FB7-4D4B-B235-9DD8B8270EA1',
    panel: CitationDOIPanel,
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

export const CitationDOIPanel: React.FC<CitationPanelProps> = props => {


  const defaultNoResultsMessage = 'Paste a DOI to load data from Crossref, DataCite, or mEDRA.';
  const noMatchingResultsMessage = 'No item matching this identifier could be located.'

  const [csl, setCsl] = React.useState<CSL>();
  const [noResultsText, setNoResultsText] = React.useState<string>(defaultNoResultsMessage);
  const [searchText, setSearchText] = React.useState<string>('');
  const [previewFields, setPreviewFields] = React.useState<CiteField[]>([]);
  React.useEffect(() => {
    if (csl) {
      const previewFields = formatForPreview(csl);
      setPreviewFields(previewFields);
    } else {
      setPreviewFields([]);
      if (searchText.length > 0) {
        setNoResultsText(noMatchingResultsMessage);
      } else {
        setNoResultsText(defaultNoResultsMessage);
      }
    }
  }, [csl]);

  React.useEffect(() => {
    if (searchText) {
      const debounced = debounce(async () => {
        const result = await props.server.doi.fetchCSL(searchText, 350);
        const csl = result.message;
        setCsl(csl);
      }, 50);
      debounced();
    } else {
      setCsl(undefined);
    }
  }, [searchText])

  const doiChanged = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchText(e.target.value);
  };

  return (
    <div style={props.style} className='pm-insert-doi-panel'>
      <div className='pm-insert-doi-panel-textbox-container'>
        <TextInput
          width='100%'
          iconAdornment={props.ui.images.search}
          tabIndex={0}
          className='pm-insert-doi-panel-textbox pm-block-border-color'
          placeholder={props.ui.context.translateText('Search for a DOI')}
          onChange={doiChanged}
        />
      </div>
      <div className='pm-insert-doi-panel-heading'>
        {csl ?
          <CitationListItem
            index={0}
            data={{
              data: toBibliographyEntry(csl, props.bibliographyManager, props.ui),
              sourcesToAdd: props.sourcesToAdd,
              addSource: props.addSource,
              removeSource: props.removeSource,
              ui: props.ui
            }}
            style={{}}
            isScrolling={false}
          /> :
          <div className='pm-insert-doi-panel-no-result'>
            <div className='pm-insert-doi-panel-no-result-text'>
              {props.ui.context.translateText(noResultsText)}
            </div>
          </div>}
      </div>
      <div className='pm-insert-doi-panel-fields'>
        <table>
          <tbody>
            {previewFields.map(previewField =>
              (<tr key={previewField.name}>
                <td className='pm-insert-doi-panel-fields-name'>{previewField.name}:</td>
                <td className='pm-insert-doi-panel-fields-value'>{previewField.value}</td>
              </tr>)
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};
export const kDOIType = 'DOI Search';


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