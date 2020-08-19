// TODO: comment


import React from "react";

import { Popup } from "../../api/widgets/popup";
import { WidgetProps } from "../../api/widgets/react";
import { TextButton } from "../../api/widgets/button";

import './insert_citation-panel.css';

interface InsertCitationPanelProps extends WidgetProps {
  size: [number, number];
  cancel: VoidFunction;
}

export const InsertCitationPanel: React.FC<InsertCitationPanelProps> = props => {

  const kPopupChromeHeight = 25;
  const popupHeight = props.size[0] - kPopupChromeHeight;
  const popupWidth = props.size[1];
  const style: React.CSSProperties = {
    ...props.style,
    height: popupHeight + 'px',
    width: popupWidth + 'px',
  };

  const addCitation = () => {
    window.alert("Add!");
  };


  return (
    <Popup style={style}>
      <div className="pm-cite-panel-container">

        <div className="pm-cite-panel-cite-selection">
          <div className="pm-cite-panel-cite-selection-sources">
            Sources
          </div>


          <div className="pm-cite-panel-cite-selection-items">
            Items
          </div>
        </div>
        <div className="pm-cite-panel-selected-cites">
          Cites
        </div>

        <div className="pm-cite-panel-buttons">
          <TextButton
            title="Insert"
            onClick={addCitation} />

          <TextButton
            title="Cancel"
            onClick={props.cancel} />

        </div>
      </div>
    </Popup>
  );
};