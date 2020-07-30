/*
 * insert_symbol-plugin.tsx
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

import { InsertCitationPanel } from './insert_citation-panel';
import { EditorEvents } from '../../api/events';
import { ResizeEvent } from '../../api/event-types';
import { WidgetProps } from '../../api/widgets/react';
import ReactDOM from 'react-dom';
import debounce from 'lodash.debounce';

export function showInsertCitationPopup(events: EditorEvents) {
  // Render the element into the window



  const container = window.document.createElement('div');
  container.tabIndex = 0;
  container.style.position = 'absolute';
  container.style.zIndex = '1000';
  container.style.height = window.innerHeight + 'px';
  container.style.width = window.innerWidth + 'px';

  const cancelHandler = () => {
    container.remove();
  };

  ReactDOM.render(
    <InsertCitePopup
      events={events}
      cancelHandler={cancelHandler} />, container);
  window.document.body.appendChild(container);
}

interface InsertCitePopupProps extends WidgetProps {
  events: EditorEvents;
  cancelHandler: VoidFunction;
}

const InsertCitePopup: React.FC<InsertCitePopupProps> = props => {

  const computeSize = () => {
    const kMaxHeight = 800;
    const kMaxHeightProportion = .9;
    const kWidthProportion = .75;

    const windowHeight = window.innerHeight;
    const windowWidth = window.innerWidth;

    const height = Math.min(kMaxHeight, windowHeight * kMaxHeightProportion);
    const width = Math.min(kWidthProportion * height, windowWidth * .9);
    const size: [number, number] = [height, width];
    return size;
  };

  // Listen for resize events and update the size and position of the dialog 
  React.useEffect(() => {
    return props.events.subscribe(ResizeEvent, debounce(() => {
      const newSize = computeSize();
      setPanelSize(newSize);
    }, 15));
  });

  const initialSize = computeSize();
  const [panelSize, setPanelSize] = React.useState<[number, number]>(initialSize);

  const top = (window.innerHeight - panelSize[0]) / 2;
  const left = (window.innerWidth - panelSize[1]) / 2;

  const popupStyles: React.CSSProperties = {
    top: top + 'px',
    left: left + 'px',
    position: 'absolute',
    zIndex: 1000
  };
  return (
    <div style={popupStyles} >
      <InsertCitationPanel
        size={panelSize}
        cancel={props.cancelHandler} />
    </div >);
};



