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
import React, { CSSProperties } from "react";
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

interface CitationWithPreview {
  citation: CitationListEntry;
  preview: CiteField[];
}

export const DOISourcePanel = React.forwardRef<HTMLDivElement, CitationSourcePanelProps>((props, ref) => {

  // The citation entry returned for this DOI (if any)
  const [citationWithPreview, setCitationWithPreview] = React.useState<CitationWithPreview>();

  // Perform first load tasks
  const searchBoxRef = React.useRef<HTMLInputElement>(null);

  // Debounced search function
  const searchForDOI = React.useCallback(debounce(async (doi: string) => {
    if (doi.length === 0) {
      // No text, just clear the results
      setCitationWithPreview(undefined);
    } else {
      const result = await props.server.doi.fetchCSL(doi, 350);
      if (result.status === 'ok') {
        // Form the entry
        const csl = result.message;
        const citation = toCitationEntry(csl, props.bibliographyManager, props.ui);
        if (citation) {
          const preview = formatForPreview(csl);
          setCitationWithPreview({ citation, preview });
        } else {
          setCitationWithPreview(undefined);
        }

        // Notify the call of this citation
        props.selectedCitation(citation);
      } else {
        // Error / failure, just clear the results
        setCitationWithPreview(undefined);
      }
    }
  }, 100), []);

  const doiChanged = (e: React.ChangeEvent<HTMLInputElement>) => {
    searchForDOI(e.target.value);
  };

  const focusSearch = () => {
    searchBoxRef.current?.focus();
  };

  const selectedIndexChanged = () => {
    // No op - there is always only a single result
  };

  const style: CSSProperties = {
    height: props.height + 'px',
    ...props.style
  };

  return (
    <div style={style} className='pm-insert-doi-source-panel' ref={ref} tabIndex={-1} onFocus={focusSearch}>
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
      <div className='pm-insert-doi-source-panel-results pm-block-border-color pm-background-color'>
        <div className='pm-insert-doi-source-panel-heading'>
          {citationWithPreview ?
            <CitationSourcePanelListItem
              index={0}
              data={{
                citations: [citationWithPreview.citation],
                citationsToAdd: props.citationsToAdd,
                addCitation: props.addCitation,
                removeCitation: props.removeCitation,
                setSelectedIndex: selectedIndexChanged,
                ui: props.ui
              }}
              style={{}}
              isScrolling={false}
            /> :
            <div className='pm-insert-doi-source-panel-no-result'>
              <div className='pm-insert-doi-source-panel-no-result-text pm-text-color'>
                {searchBoxRef.current?.value.length || 0 === 0 ?
                  props.ui.context.translateText('Paste a DOI to load data from Crossref, DataCite, or mEDRA.') :
                  props.ui.context.translateText('No item matching this identifier could be located.')}
              </div>
            </div>}
        </div>
        <div className='pm-insert-doi-source-panel-fields'>
          <table>
            <tbody>
              {citationWithPreview?.preview.map(previewField =>
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

function toCitationEntry(csl: CSL | undefined, bibliographyManager: BibliographyManager, ui: EditorUI): CitationListEntry | undefined {
  if (csl) {
    const id = suggestCiteId(bibliographyManager.localSources().map(source => source.id), csl);
    const providerKey = 'doi';
    return {
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
    };
  }
  return undefined;
}