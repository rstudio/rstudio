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

import { EditorUI } from "../../api/ui";
import { EditorServer } from "../../api/server";
import { WidgetProps } from "../../api/widgets/react";
import { TagInput, TagItem } from "../../api/widgets/tag_input";
import { NavigationTreeNode, containsChild, NavigationTree } from "../../api/widgets/navigation_tree";

import { BibliographyManager, BibliographySource, BibliographyFile } from "../../api/bibliography/bibliography";

import { bibliographySourcePanel } from "./source_panels/insert_citation-source-panel-bibliography";
import { doiSourcePanel } from "./source_panels/insert_citation-source-panel-doi";

import { kLocalBiliographyProviderKey } from "../../api/bibliography/bibliography-provider_local";
import { CitationBibliographyPicker } from "./insert_citation-bibliography-picker";

import './insert_citation-panel.css';
import { DialogButtons } from "../../api/widgets/dialog-buttons";

// Citation Panels are the coreUI element of ths dialog. Each panel provides
// the main panel UI as well as the tree to display when the panel is displayed.
export interface CitationSourcePanel {
  key: string;
  panel: React.FC<CitationSourcePanelProps>;
  treeNode: NavigationTreeNode;
}

// Panels get a variety of information as properties to permit them to search
// citations and add them
export interface CitationSourcePanelProps extends WidgetProps {
  ui: EditorUI;
  bibliographyManager: BibliographyManager;
  server: EditorServer;
  height: number;
  selectedNode?: NavigationTreeNode;
  sourcesToAdd: BibliographySource[];
  addSource: (source: BibliographySource) => void;
  removeSource: (source: BibliographySource) => void;
  confirm: VoidFunction;
}

// The picker is a full featured UI for finding and selecting citation data
// to be added to a document.
interface InsertCitationPanelProps extends WidgetProps {
  ui: EditorUI;
  doc: ProsemirrorNode;
  height: number;
  width: number;
  bibliographyManager: BibliographyManager;
  server: EditorServer;
  onSourceChanged: (sources: BibliographySource[]) => void;
  onBibliographyChanged: (bibliographyFile: BibliographyFile) => void;
  onOk: () => void;
  onCancel: () => void;
}

export const InsertCitationPanel: React.FC<InsertCitationPanelProps> = props => {

  // The panels that are being displayed and which one is selected
  const [providerPanels, setProviderPanels] = React.useState<CitationSourcePanel[]>([]);
  const [selectedProviderPanel, setSelectedProviderPanel] = React.useState<CitationSourcePanel>();

  // The node of the SelectTree that is selected
  const [selectedNode, setSelectedNode] = React.useState<NavigationTreeNode>();

  // Data for the Navigation Tree
  const [treeSourceData, setTreeSourceData] = React.useState<NavigationTreeNode[]>([]);

  // The accumulated bibliography sources to be inserted
  const [sourcesToAdd, setSourcesToAdd] = React.useState<BibliographySource[]>([]);

  // The initial loading of data for the panel. 
  React.useEffect(() => {
    async function loadData() {
      await props.bibliographyManager.load(props.ui, props.doc);

      // Load the panels
      const allPanels = [
        bibliographySourcePanel(props.doc, props.ui, props.bibliographyManager),
        doiSourcePanel(props.ui)
      ];
      setProviderPanels(allPanels);

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
    const panelForNode = (treeNode: NavigationTreeNode, panelItems: CitationSourcePanel[]) => {
      const panelItem = panelItems.find(panel => {
        return containsChild(treeNode.key, panel.treeNode);
      });
      return panelItem;
    };
    if (selectedNode) {
      const rootPanel = panelForNode(selectedNode, providerPanels);
      if (rootPanel?.key !== selectedProviderPanel?.key) {
        setSelectedProviderPanel(rootPanel);
      }
    }
  }, [selectedNode]);

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
  const bibliographyHeight = 28;
  const biblioStyle: React.CSSProperties = {
    minHeight: `${bibliographyHeight}px`,
    paddingLeft: '2px'
  };

  // Figure out the panel height (the height of the main panel less padding and other elements)
  const padding = 30;
  const panelHeight = props.height - tagHeight - bibliographyHeight - padding;

  // Load the panel that is displayed for the selected node
  const citationProps: CitationSourcePanelProps = {
    ui: props.ui,
    bibliographyManager: props.bibliographyManager,
    server: props.server,
    height: panelHeight,
    selectedNode,
    sourcesToAdd,
    addSource: (source: BibliographySource) => {
      const newSources = [...sourcesToAdd, source];
      setSourcesToAdd(newSources);
      props.onSourceChanged(newSources);
    },
    removeSource: (src: BibliographySource) => {
      deleteSource(src.id);
    },
    confirm: props.onOk
  };

  // Create the panel that should be displayed for the selected node of the tree
  const panelToDisplay = selectedProviderPanel ? React.createElement(selectedProviderPanel.panel, citationProps) : undefined;

  const nodeSelected = (node: NavigationTreeNode) => {
    setSelectedNode(node);
  };

  const deleteSource = (id: string) => {
    const filteredSources = sourcesToAdd.filter(source => source.id !== id);
    setSourcesToAdd(filteredSources);
    props.onSourceChanged(sourcesToAdd);
  };

  const deleteTag = (tag: TagItem) => {
    deleteSource(tag.key);
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

  // Support keyboard shortcuts for dismissing dialog
  const onKeyPress = (event: React.KeyboardEvent) => {
    if (event.key === 'Enter') {
      event.stopPropagation();
      props.onOk();
    }
  };

  // Esc can cause loss of focus so catch it early
  const onKeyDown = (event: React.KeyboardEvent) => {
    if (event.key === 'Escape') {
      event.stopPropagation();
      props.onCancel();
    }
  };

  return (
    <div className='pm-cite-panel-container' style={style} onKeyPress={onKeyPress} onKeyDown={onKeyDown}>

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
          tagDeleted={deleteTag}
          tagChanged={tagEdited}
          ui={props.ui}
          style={tagStyle} />
      </div>
      <div className='pm-cite-panel-select-bibliography'>
        <CitationBibliographyPicker
          bibliographyFiles={props.bibliographyManager.writableBibliographyFiles(props.doc, props.ui)}
          biblographyFileChanged={bibliographyFileChanged}
          style={biblioStyle}
          ui={props.ui} />

        <DialogButtons
          onOk={props.onOk}
          onCancel={props.onCancel}
          ui={props.ui} />
      </div>
    </div>
  );
};

