/*
 * insert_citation-picker-bibliography.tsx
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

import { WidgetProps } from "../../api/widgets/react";
import { EditorUI } from "../../api/ui";
import { BibliographyFile } from "../../api/bibliography/bibliography";
import { joinPaths, changeExtension } from "../../api/path";

import './insert_citation-bibliography-picker.css';
import { TextInput } from "../../api/widgets/text";
import { SelectInput } from "../../api/widgets/select";

export interface CitationBiblographyPickerProps extends WidgetProps {
  bibliographyFiles: BibliographyFile[];
  biblographyFileChanged: (file: BibliographyFile) => void;
  ui: EditorUI;
}

interface BibliographyType {
  extension: string;
  displayName: string;
}

export const CitationBibliographyPicker: React.FC<CitationBiblographyPickerProps> = props => {


  const [createBibText, setCreateBibText] = React.useState<string>('references.yaml');

  React.useEffect(() => {
    props.biblographyFileChanged({
      displayPath: createBibText,
      fullPath: joinPaths(props.ui.context.getDefaultResourceDir(), createBibText),
      isProject: false,
      writable: true
    });
  }, [createBibText]);


  const bibliographyTypes: BibliographyType[] = [
    {
      displayName: props.ui.context.translateText('CSL-YAML'),
      extension: 'yaml',
    },
    {
      displayName: props.ui.context.translateText('CSL-JSON'),
      extension: 'json',
    },
    {
      displayName: props.ui.context.translateText('BibLaTeX'),
      extension: 'bib',
    }
  ];

  const onChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const index = e.target.selectedIndex;
    props.biblographyFileChanged(props.bibliographyFiles[index]);
  };

  const onTextChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const text = e.target.value;
    setCreateBibText(text);
  };

  const onTypeChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const index = e.target.selectedIndex;
    const type = bibliographyTypes[index];
    setCreateBibText(changeExtension(createBibText, type.extension));
  };

  return (
    <div className='pm-citation-bibliography-picker-container' style={props.style}>
      <div className='pm-citation-bibliography-picker-label'>{props.bibliographyFiles.length > 0 ?
        props.ui.context.translateText('Add to bibliography:') :
        props.ui.context.translateText('Create bibliography file:')
      }</div>
      {
        props.bibliographyFiles.length > 0 ?
          (<SelectInput onChange={onChange}>
            {props.bibliographyFiles.map(file => (<option key={file.fullPath} value={file.fullPath}>{file.displayPath}</option>))}
          </SelectInput>) :
          (
            <div>
              <TextInput
                width='100'
                tabIndex={0}
                value={createBibText}
                className='pm-citation-bibliography-picker-textbox pm-block-border-color'
                placeholder={props.ui.context.translateText('Bibligraphy File Name')}
                onChange={onTextChange}
              />

              <SelectInput
                onChange={onTypeChange}>
                {bibliographyTypes.map(bibType => (<option key={bibType.extension} value={bibType.extension}>{bibType.displayName}</option>))}
              </SelectInput>
            </div>
          )
      }
    </div>
  );
};