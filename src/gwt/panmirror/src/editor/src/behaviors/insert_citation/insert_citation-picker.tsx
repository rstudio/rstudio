/*
 * insert_citation_picker.ts
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

import { Node as ProsemirrorNode } from 'prosemirror-model';

import { WidgetProps } from "../../api/widgets/react";

import { BibliographyManager, BibliographySource, BibliographyFile } from "../../api/bibliography/bibliography";
import { EditorUI } from "../../api/ui";
import { NavigationTreeNode, containsChild, NavigationTree } from "../../api/widgets/navigation_tree";
import { TagInput, TagItem } from "../../api/widgets/tag_input";

import { bibliographyPanel } from "./panels/insert_citation-panel-bibliography";
import { doiPanel } from "./panels/insert_citation-panel-doi";

import './insert_citation-picker.css';
import { EditorServer } from "../../api/server";
import { kLocalBiliographyProviderKey } from "../../api/bibliography/bibliography-provider_local";
import { CitationBibliographyPicker } from "./insert_citation-bibliography-picker";


// Citation Panels are the coreUI element of ths dialog. Each panel provides
// the main panel UI as well as the tree to display when the panel is displayed.
export interface CitationPanel {
  key: string;
  panel: React.FC<CitationPanelProps>;
  treeNode: NavigationTreeNode;
}

// Panels get a variety of information as properties to permit them to search
// citations and add them
export interface CitationPanelProps extends WidgetProps {
  ui: EditorUI;
  bibliographyManager: BibliographyManager;
  server: EditorServer;
  height: number;
  selectedNode?: NavigationTreeNode;
  sourcesToAdd: BibliographySource[];
  addSource: (source: BibliographySource) => void;
  removeSource: (source: BibliographySource) => void;
}

// The picker is a full featured UI for finding and selecting citation data
// to be added to a document.
interface InsertCitationPickerProps extends WidgetProps {
  ui: EditorUI;
  doc: ProsemirrorNode;
  height: number;
  width: number;
  bibliographyManager: BibliographyManager;
  server: EditorServer;
  onSourceChanged: (sources: BibliographySource[]) => void;
  onBibliographyChanged: (bibliographyFile: BibliographyFile) => void;
}

export const InsertCitationPicker: React.FC<InsertCitationPickerProps> = props => {

  // The panels that are being displayed and which one is selected
  const [panels, setPanels] = React.useState<CitationPanel[]>([]);
  const [selectedPanel, setSelectedPanel] = React.useState<CitationPanel>();

  // The node of the SelectTree that is selected
  const [selectedNode, setSelectedNode] = React.useState<NavigationTreeNode>();

  // Data for the SelectTree
  const [treeSourceData, setTreeSourceData] = React.useState<NavigationTreeNode[]>([]);

  // The accumulated bibliography sources to be inserted
  const [sourcesToAdd, setSourcesToAdd] = React.useState<BibliographySource[]>([]);

  // The initial loading of data for the panel. 
  React.useEffect(() => {
    async function loadData() {
      await props.bibliographyManager.load(props.ui, props.doc);

      // Load the panels
      const allPanels = [
        bibliographyPanel(props.doc, props.ui, props.bibliographyManager),
        doiPanel(props.ui)
      ];
      setPanels(allPanels);

      // Load the tree and select the root node
      const treeNodes = allPanels.map(panel => panel.treeNode);
      setTreeSourceData(treeNodes);
      setSelectedNode(treeNodes[0]);
    }
    loadData();
  }, []);

  // Whenever the user selects a new node, lookup the correct panel for that node and 
  // select that panel.
  React.useEffect(() => {
    const panelForNode = (treeNode: NavigationTreeNode, panelItems: CitationPanel[]) => {
      const panelItem = panelItems.find(panel => {
        return containsChild(treeNode.key, panel.treeNode);
      });
      return panelItem;
    };
    if (selectedNode) {
      const rootPanel = panelForNode(selectedNode, panels);
      if (rootPanel?.key !== selectedPanel?.key) {
        setSelectedPanel(rootPanel);
      }
    }
  }, [selectedNode]);

  // Notify the handler whenever this list changes
  React.useEffect(() => {
    props.onSourceChanged(sourcesToAdd);
  }, [sourcesToAdd]);

  // Style properties
  const style: React.CSSProperties = {
    height: props.height + 'px',
    width: props.width + 'px',
    ...props.style,
  };

  // Size the tag element, bibliography picker, and main panel
  const tagHeight = 50;
  const tagStyle: React.CSSProperties = {
    minHeight: `${tagHeight}px`
  };
  const bibliographyHeight = 40;
  const biblioStyle: React.CSSProperties = {
    minHeight: `${bibliographyHeight}px`,
  };

  const buttonHeight = 30;

  const panelHeight = props.height - tagHeight - bibliographyHeight - buttonHeight;


  // Load the panel that is displayed for the selected node
  const citationProps: CitationPanelProps = {
    ui: props.ui,
    bibliographyManager: props.bibliographyManager,
    server: props.server,
    height: panelHeight,
    selectedNode,
    sourcesToAdd,
    addSource: (source: BibliographySource) => {
      setSourcesToAdd([...sourcesToAdd, source]);
    },
    removeSource: (src: BibliographySource) => {
      deleteSource(src.id);
    }
  };
  const panelToDisplay = selectedPanel ? React.createElement(selectedPanel.panel, citationProps) : undefined;

  const nodeSelected = (node: NavigationTreeNode) => {
    setSelectedNode(node);
  };

  const deleteSource = (id: string) => {
    const filteredSources = sourcesToAdd.filter(source => source.id !== id);
    setSourcesToAdd(filteredSources);
  };

  const deleteTag = (tag: TagItem) => {
    const filteredSources = sourcesToAdd.filter(source => source.id !== tag.key);
    setSourcesToAdd(filteredSources);
  };

  const tagEdited = (key: string, text: string) => {
    const targetSource = sourcesToAdd.find(source => source.id === key);
    if (targetSource) {
      targetSource.id = text;
    }
  };

  const bibliographyFileChanged = (biblographyFile: BibliographyFile) => {
    props.onBibliographyChanged(biblographyFile);
  };

  return (
    <div className='pm-cite-panel-container' style={style}>

      <div className='pm-cite-panel-cite-selection'>
        <div className='pm-cite-panel-cite-selection-sources pm-block-border-color pm-background-color'>
          <NavigationTree
            height={panelHeight}
            nodes={treeSourceData}
            selectedNode={selectedNode}
            nodeSelected={nodeSelected}
          />
        </div>

        <div className='pm-cite-panel-cite-selection-items pm-block-border-color pm-background-color'>
          {panelToDisplay}
        </div>
      </div>
      <div
        className='pm-cite-panel-selected-cites pm-block-border-color pm-background-color'
        style={tagStyle}
      >
        <TagInput
          tags={sourcesToAdd.map(source => ({
            key: source.id,
            displayText: source.id,
            displayPrefix: '@',
            isEditable: source.providerKey !== kLocalBiliographyProviderKey,
          }))}
          deleteTag={deleteTag}
          tagEdited={tagEdited}
          ui={props.ui}
          style={tagStyle} />
      </div>
      <div className='pm-cite-panel-select-bibliography'>
        <CitationBibliographyPicker
          bibliographyFiles={props.bibliographyManager.writableBibliographyFiles(props.doc, props.ui)}
          biblographyFileChanged={bibliographyFileChanged}
          style={biblioStyle}
          ui={props.ui} />
      </div>
    </div>
  );
};

