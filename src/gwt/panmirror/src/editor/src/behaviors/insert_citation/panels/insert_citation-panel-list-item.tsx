import { ListChildComponentProps } from "react-window";
import { BibliographySource } from "../../../api/bibliography/bibliography";
import { EditorUI } from "../../../api/ui";
import { entryForSource } from "../../../marks/cite/cite-bibliography_entry";
import React from "react";
import { OutlineButton } from "../../../api/widgets/button";




interface CitationListData {
  data: BibliographySource[];
  sourcesToAdd: BibliographySource[];
  addSource: (source: BibliographySource) => void;
  removeSource: (source: BibliographySource) => void;
  ui: EditorUI;
}

//image
//title
//deail
//adornmentImage?
//TODO: Add table of preview fields below
//TODO: Don't require bibliography source, use CSL?

export const CitationListItem = (props: ListChildComponentProps) => {

  const citationListData: CitationListData = props.data;
  const source = citationListData.data[props.index];
  const entry = entryForSource(source, props.data.ui);

  const maxIdLength = 30;
  const id = entry.source.id.length > maxIdLength ? `@${entry.source.id.substr(0, maxIdLength - 1)}…` : `@${entry.source.id}`;
  const authorWidth = Math.max(10, 50 - id.length);

  const alreadyAdded = citationListData.sourcesToAdd.map(src => src.id).includes(source.id);

  const onClick = () => {
    if (alreadyAdded) {
      citationListData.removeSource(source);
    } else {
      citationListData.addSource(source);
    }
  };

  return (
    <div>
      <div className='pm-insert-citation-panel-item' style={props.style}>
        <div className='pm-insert-citation-panel-item-container'>
          <div className='pm-insert-citation-panel-item-type'>
            {entry.adornmentImage ? <img className='pm-insert-citation-panel-item-adorn pm-block-border-color pm-background-color' src={entry.adornmentImage} /> : undefined}
            <img className='pm-insert-citation-panel-item-icon pm-block-border-color' src={entry.image} />
          </div>
          <div className='pm-insert-citation-panel-item-summary'>
            <div className='pm-insert-citation-panel-item-id'>
              <div className='pm-insert-citation-panel-item-title pm-fixedwidth-font'>{id}</div>
              <div className='pm-insert-citation-panel-item-detail'>{entry.authorsFormatter(source.author, authorWidth)} {entry.issuedDateFormatter(source.issued)}</div>
            </div>
            <div className='pm-insert-citation-panel-item-subtitle-text'>{source.title}</div>
          </div>
          <div className='pm-insert-citation-panel-item-button'>
            <OutlineButton
              style={{ width: '70px' }}
              title={alreadyAdded ? 'Remove' : 'Add'}
              onClick={onClick}
            />
          </div>
        </div>
        <div className='pm-insert-citation-panel-item-separator pm-block-border-color' />
      </div>
    </div>
  );
};