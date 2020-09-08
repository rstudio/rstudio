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
import { formatForPreview, CiteField, suggestCiteId, formatAuthors, formatIssuedDate, imageForType } from "../../../api/cite";
import { BibliographyManager } from "../../../api/bibliography/bibliography";

import { CitationSourcePanelProps, CitationSourcePanel, CitationListEntry } from "../insert_citation-panel";
import { CitationSourcePanelListItem } from "./insert_citation-source-panel-list-item";

import './insert_citation-source-panel-doi.css';

const kDOIType = 'DOI Search';

export function doiSourcePanel(ui: EditorUI): CitationSourcePanel {
  return {
    key: '76561E2A-8FB7-4D4B-B235-9DD8B8270EA1',
    panel: DOISourcePanel,
    treeNode: () => {
      return {
        key: 'DOI',
        name: ui.context.translateText('From DOI'),
        image: ui.images.citations?.doi,
        type: kDOIType,
        children: [],
        expanded: true
      };
    }
  };
}

export const DOISourcePanel = React.forwardRef<HTMLDivElement, CitationSourcePanelProps>((props, ref) => {

  const defaultMessage = props.ui.context.translateText('Paste a DOI to load data from Crossref, DataCite, or mEDRA.');
  const noMatchingResultsMessage = props.ui.context.translateText('No item matching this identifier could be located.');

  const [csl, setCsl] = React.useState<CSL>();
  const [noResultsText, setNoResultsText] = React.useState<string>(defaultMessage);
  const [searchText, setSearchText] = React.useState<string>('');
  const [previewFields, setPreviewFields] = React.useState<CiteField[]>([]);

  const clearResults = (message: string) => {
    setCsl(undefined);
    setPreviewFields([]);
    setNoResultsText(message);
  };

  // Perform first load tasks
  const searchBoxRef = React.useRef<HTMLInputElement>(null);
  const [listHeight, setListHeight] = React.useState<number>(props.height);
  React.useLayoutEffect(() => {

    // Size the list Box
    const searchBoxHeight = searchBoxRef.current?.clientHeight;
    const padding = 8;
    if (searchBoxHeight) {
      setListHeight(props.height - padding - searchBoxHeight);
    }
  }, []);

  const debouncedCSL = React.useCallback(debounce(async (doi: string) => {
    const result = await props.server.doi.fetchCSL(doi, 350);
    if (result.status === 'ok') {
      setCsl(result.message);
      const preview = formatForPreview(result.message);
      setPreviewFields(preview);
    } else {
      clearResults(noMatchingResultsMessage);
    }

  }, 100), []);

  React.useEffect(() => {
    if (searchText) {
      debouncedCSL(searchText);
    } else {
      clearResults(defaultMessage);
    }
  }, [searchText]);

  const doiChanged = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchText(e.target.value);
  };

  return (
    <div style={props.style} className='pm-insert-doi-source-panel' ref={ref}>
      <div className='pm-insert-doi-source-panel-textbox-container'>
        <TextInput
          width='100%'
          iconAdornment={props.ui.images.search}
          tabIndex={0}
          className='pm-insert-doi-source-panel-textbox pm-block-border-color'
          placeholder={props.ui.context.translateText('Paste a DOI to search')}
          onChange={doiChanged}
          ref={searchBoxRef}
        />
      </div>
      <div className='pm-insert-doi-source-panel-results pm-block-border-color pm-background-color' style={{ height: listHeight }}>
        <div className='pm-insert-doi-source-panel-heading'>
          {csl ?
            <CitationSourcePanelListItem
              index={0}
              data={{
                citations: toCitationEntry(csl, props.bibliographyManager, props.ui),
                citationsToAdd: props.citationsToAdd,
                addCitation: props.addCitation,
                removeCitation: props.removeCitation,
                ui: props.ui
              }}
              style={{}}
              isScrolling={false}
            /> :
            <div className='pm-insert-doi-source-panel-no-result'>
              <div className='pm-insert-doi-source-panel-no-result-text pm-text-color'>
                {props.ui.context.translateText(noResultsText)}
              </div>
            </div>}
        </div>
        <div className='pm-insert-doi-source-panel-fields'>
          <table>
            <tbody>
              {previewFields.map(previewField =>
                (<tr key={previewField.name}>
                  <td className='pm-insert-doi-source-panel-fields-name pm-text-color'>{previewField.name}:</td>
                  <td className='pm-insert-doi-source-panel-fields-value pm-text-color'>{previewField.value}</td>
                </tr>)
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
});

function toCitationEntry(csl: CSL | undefined, bibliographyManager: BibliographyManager, ui: EditorUI): CitationListEntry[] {
  if (csl) {
    const id = suggestCiteId(bibliographyManager.allSources().map(source => source.id), csl);
    const providerKey = 'doi';
    return [
      {
        id,
        title: csl.title || '',
        providerKey,
        authors: (length: number) => {
          return formatAuthors(csl.author, length);
        },
        date: formatIssuedDate(csl.issued),
        journal: '',
        image: imageForType(ui, csl.type)[0],
        toBibliographySource: () => {
          return Promise.resolve({ ...csl, id, providerKey });
        }
      }];
  }
  return [];
}