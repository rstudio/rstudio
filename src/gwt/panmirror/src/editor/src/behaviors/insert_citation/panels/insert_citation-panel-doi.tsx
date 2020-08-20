import React from "react";

import { CitationPanelProps, CitationPanel } from "../insert_citation-picker";
import { EditorUI } from "../../../api/ui";

export const CitationDOIPanel: React.FC<CitationPanelProps> = props => {
  return (<div>DOI TOWN!</div>);
};
export const kDOIType = 'DOI Search';

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