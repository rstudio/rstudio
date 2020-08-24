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

export function doiPanel(ui: EditorUI): CitationPanel {
  return {
    key: '76561E2A-8FB7-4D4B-B235-9DD8B8270EA1',
    panel: CitationDOIPanel,
    treeNode: {
      key: 'DOI',
      name: "Find DOI",
      image: ui.images.citations?.doi,
      type: kDOIType,
      children: [],
      expanded: true
    }
  };
}

export const CitationDOIPanel: React.FC<CitationPanelProps> = props => {


  const [csl, setCsl] = React.useState<CSL>();
  const [previewFields, setPreviewFields] = React.useState<CiteField[]>([]);
  React.useEffect(() => {
    if (csl) {
      const previewFields = formatForPreview(csl);
      setPreviewFields(previewFields);
      console.log(previewFields);
    } else {
      setPreviewFields([]);
    }
  }, [csl]);

  const doiChanged = (e: React.ChangeEvent<HTMLInputElement>) => {
    e.persist();
    const debounced = debounce(async () => {
      const search = e.target.value;
      const result = await props.server.doi.fetchCSL(search, 350);
      const csl = result.message;
      if (csl && !csl.id) {
        csl.id = suggestCiteId(props.bibliographyManager.allSources().map(source => source.id), csl);
      }
      setCsl(csl);
    }, 50);
    debounced();
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
      <div>
        <table>
          <tbody>
            {previewFields.map(previewField =>
              (<tr key={previewField.name}>
                <td>{previewField.name}</td>
                <td>{previewField.value}</td>
              </tr>)
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};
export const kDOIType = 'DOI Search';
