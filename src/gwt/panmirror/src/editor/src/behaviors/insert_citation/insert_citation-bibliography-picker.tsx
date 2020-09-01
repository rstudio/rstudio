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

import { EditorUI } from "../../api/ui";
import { joinPaths, changeExtension } from "../../api/path";
import { WidgetProps } from "../../api/widgets/react";
import { TextInput } from "../../api/widgets/text";
import { SelectInput } from "../../api/widgets/select";
import { BibliographyFile } from "../../api/bibliography/bibliography";

import './insert_citation-bibliography-picker.css';

export interface CitationBiblographyPickerProps extends WidgetProps {
  bibliographyFiles: BibliographyFile[];
  biblographyFileChanged: (file: BibliographyFile) => void;
  ui: EditorUI;
}

interface BibliographyType {
  extension: string;
  displayName: string;
}

function newBibliographyFile(path: string, ui: EditorUI): BibliographyFile {
  return {
    displayPath: path,
    fullPath: joinPaths(ui.context.getDefaultResourceDir(), path),
    isProject: false,
    writable: true
  };
}

export const CitationBibliographyPicker: React.FC<CitationBiblographyPickerProps> = props => {

  const defaultBiblioType = props.ui.prefs.bibliographyDefaultType();

  // The types of bibliography files and the default value
  const bibliographyTypes: BibliographyType[] = [
    {
      displayName: props.ui.context.translateText('BibLaTeX'),
      extension: 'bib',
    },
    {
      displayName: props.ui.context.translateText('CSL-YAML'),
      extension: 'yaml',
    },
    {
      displayName: props.ui.context.translateText('CSL-JSON'),
      extension: 'json',
    },
  ];

  // Switch to the default file type
  const suggestedBibliographyFileName = changeExtension('references.bib', defaultBiblioType || bibliographyTypes[0].extension);

  // On first load, initialize the default bibliography file
  React.useEffect(() => {
    const initialBibFile = (props.bibliographyFiles.length > 0) ? props.bibliographyFiles[0] : newBibliographyFile(suggestedBibliographyFileName, props.ui);
    setBibliographyFile(initialBibFile);
    props.biblographyFileChanged(initialBibFile);
  }, []);

  // The file that the user selected (either for creation or selected from list)
  const [bibliographyFile, setBibliographyFile] = React.useState<BibliographyFile>();

  // The name of the file that the user would like to create
  const [createFileName, setCreateFileName] = React.useState<string>(suggestedBibliographyFileName);

  // Selection of file from list
  const onChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const index = e.target.selectedIndex;
    props.biblographyFileChanged(props.bibliographyFiles[index]);
  };

  // Whenever the create file changes, store the value and notify listeners
  const createFileChanged = (text: string) => {
    const newFile = newBibliographyFile(text, props.ui);
    setCreateFileName(text);
    setBibliographyFile(newFile);
    props.biblographyFileChanged(newFile);
  };

  // Change to the file we should create
  const onTextChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const text = e.target.value;
    createFileChanged(text);
  };

  // File type change
  const onTypeChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const index = e.target.selectedIndex;
    const type = bibliographyTypes[index];
    const newPath = changeExtension(createFileName, type.extension);
    createFileChanged(newPath);
    props.ui.prefs.setBibliographyDefaultType(type.extension);
  };

  return (
    <div className='pm-citation-bibliography-picker-container' style={props.style}>
      <div className='pm-citation-bibliography-picker-label pm-text-color'>{props.bibliographyFiles.length > 0 ?
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
                className='pm-citation-bibliography-picker-textbox pm-block-border-color'
                placeholder={props.ui.context.translateText('Bibligraphy File Name')}
                value={createFileName}
                onChange={onTextChange}
              />

              <SelectInput
                onChange={onTypeChange}
                defaultValue={defaultBiblioType}>
                {bibliographyTypes.map(bibType => (<option key={bibType.extension} value={bibType.extension}>{bibType.displayName}</option>))}
              </SelectInput>
            </div>
          )
      }
    </div>
  );
};