/*
 * insert_citation-source-panel-list.tsx
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

import { FixedSizeList } from "react-window";

import { EditorUI } from "../../../api/ui";
import { WidgetProps } from "../../../api/widgets/react";

import { CitationSourcePanelListItem } from "./insert_citation-source-panel-list-item";
import { CitationListEntry } from "../insert_citation-panel";

import './insert_citation-source-panel-list.css';


export enum CitationSourceListStatus {
  default,
  loading,
  noResults
}

export interface CitationSourceListProps extends WidgetProps {
  height: number;
  citations: CitationListEntry[];
  citationsToAdd: CitationListEntry[];
  addCitation: (citation: CitationListEntry) => void;
  removeCitation: (citation: CitationListEntry) => void;
  selectedCitation: (citation?: CitationListEntry) => void;
  confirm: VoidFunction;
  status: CitationSourceListStatus;
  placeholderText?: string;
  focusPrevious?: () => void;
  ui: EditorUI;
}


export const CitationSourceList = React.forwardRef<HTMLDivElement, CitationSourceListProps>((props: CitationSourceListProps, ref) => {
  const [selectedIndex, setSelectedIndex] = React.useState<number>();
  const [focused, setFocused] = React.useState<boolean>(false);
  const fixedList = React.useRef<FixedSizeList>(null);

  // Whenever selection changed, ensure that we are scrolled to that item
  React.useLayoutEffect(() => {
    if (selectedIndex) {
      fixedList.current?.scrollToItem(selectedIndex);
    }
  }, [selectedIndex]);

  // Item height and consequently page height
  const itemHeight = 64;
  const itemsPerPage = Math.floor(props.height / itemHeight);

  // Reset the index whenever the data changes
  React.useEffect(() => {
    setSelectedIndex(undefined);
    props.selectedCitation(undefined);
  }, [props.citations, props.citationsToAdd]);

  // Upddate selected item index (this will manage bounds)
  const incrementIndex = (event: React.KeyboardEvent, index: number) => {
    event.stopPropagation();
    event.preventDefault();
    if (props.citations) {
      const maxIndex = props.citations.length - 1;
      const newIndex = Math.min(Math.max(0, index), maxIndex);
      onSetSelectedIndex(newIndex);
    }
  };

  // Toggle the currently selected item as added or removed
  const toggleSelectedSource = (event: React.KeyboardEvent) => {
    event.stopPropagation();
    event.preventDefault();

    if (props.citations && selectedIndex !== undefined) {
      const source = props.citations[selectedIndex];
      if (source) {
        if (props.citationsToAdd.includes(source)) {
          props.removeCitation(source);
        } else {
          props.addCitation(source);
        }
      }
    }
  };

  const handleListKeyDown = (event: React.KeyboardEvent) => {
    const currentIndex = selectedIndex || 0;
    switch (event.key) {
      case 'ArrowUp':
        if (currentIndex === 0 && props.focusPrevious) {
          props.focusPrevious();
        } else {
          incrementIndex(event, currentIndex - 1);
        }
        break;

      case 'ArrowDown':
        incrementIndex(event, currentIndex + 1);
        break;

      case 'PageDown':
        incrementIndex(event, currentIndex + itemsPerPage);
        break;

      case 'PageUp':
        incrementIndex(event, currentIndex - itemsPerPage);
        break;

      case 'Enter':
        toggleSelectedSource(event);
        props.confirm();
        break;
      case ' ':
        toggleSelectedSource(event);
        break;
    }
  };

  // Focus / Blur are used to track whether to show selection highlighting
  const onFocus = (event: React.FocusEvent<HTMLDivElement>) => {
    setFocused(true);
    if (selectedIndex === undefined) {
      onSetSelectedIndex(0);
    }
  };

  const onBlur = (event: React.FocusEvent<HTMLDivElement>) => {
    setFocused(false);
    setSelectedIndex(undefined);
    props.selectedCitation(undefined);
  };

  const onSetSelectedIndex = (index: number) => {
    setSelectedIndex(index);
    props.selectedCitation(props.citations[index]);
  };

  const classes = ['pm-insert-citation-source-panel-list-container'].concat(props.classes || []).join(' ');
  const filteredCitations = props.citations.filter(citation => !props.citationsToAdd.map(citationToAdd => citationToAdd.id).includes(citation.id));

  switch (props.status) {
    case CitationSourceListStatus.default:
      if (props.citations.length > 0) {
        return (
          <div tabIndex={0} onKeyDown={handleListKeyDown} onFocus={onFocus} onBlur={onBlur} ref={ref} className={classes}>
            <FixedSizeList
              className='pm-insert-citation-source-panel-list'
              height={props.height}
              width='100%'
              itemCount={filteredCitations.length}
              itemSize={itemHeight}
              itemData={{
                selectedIndex,
                setSelectedIndex: onSetSelectedIndex,
                citations: filteredCitations,
                citationsToAdd: props.citationsToAdd,
                addCitation: props.addCitation,
                removeCitation: props.removeCitation,
                confirm: props.confirm,
                showSeparator: true,
                showSelection: focused,
                preventFocus: true,
                ui: props.ui,
              }}
              ref={fixedList}
            >
              {CitationSourcePanelListItem}
            </FixedSizeList>
          </div >
        );
      } else {
        return (
          <div className={classes} style={{ height: props.height + 'px' }} ref={ref} >
            <div className='pm-insert-citation-source-panel-list-noresults-text'>{props.placeholderText}</div>
          </div>
        );
      }

    case CitationSourceListStatus.loading:
      return (
        <div className={classes} style={{ height: props.height + 'px' }} ref={ref} >
          <div className='pm-insert-citation-source-panel-list-noresults-text'>
            <img src={props.ui.images.search_progress} className='pm-insert-citation-source-panel-list-progress' />
            {props.ui.context.translateText('Searching…')}
          </div>
        </div>
      );

    case CitationSourceListStatus.noResults:
      return (
        <div className={classes} style={{ height: props.height + 'px' }} ref={ref} >
          <div className='pm-insert-citation-source-panel-list-noresults-text'>{props.ui.context.translateText('No matching items')}</div>
        </div >
      );
  }
});



