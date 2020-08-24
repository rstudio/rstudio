/*
 * insert_citation-panel-bibliography.ts
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

import { FixedSizeList, ListChildComponentProps } from "react-window";

import { BibliographySource, BibliographyManager, BibliographyContainer } from "../../../api/bibliography/bibliography";
import { kZoteroProviderKey } from "../../../api/bibliography/bibliography-provider_zotero";
import { kLocalBiliographyProviderKey } from "../../../api/bibliography/bibliography-provider_local";
import { entryForSource } from "../../../marks/cite/cite-bibliography_entry";
import { EditorUI } from "../../../api/ui";
import { TextInput } from "../../../api/widgets/text";
import { OutlineButton } from "../../../api/widgets/button";
import { SelectTreeNode } from "../../../api/widgets/select_tree";

import { CitationPanelProps, CitationPanel } from "../insert_citation-picker";

import './insert_citation-panel-bibliography.css';


export const kAllLocalType = 'All Local Sources';

export const CitationListPanel: React.FC<CitationPanelProps> = props => {

  const bibMgr = props.bibliographyManager;
  const [itemData, setItemData] = React.useState<BibliographySource[]>([]);
  const [searchTerm, setSearchTerm] = React.useState<string>();

  React.useEffect(() => {
    async function loadData() {
      if (props.selectedNode) {
        const selectedNode = props.selectedNode;

        // The node could be the root node
        const providerKey = selectedNode.type === kAllLocalType ? undefined : selectedNode.type;

        // The node could be a provider or a collection
        const collectionKey = (
          selectedNode.type !== kAllLocalType &&
          selectedNode.key !== kZoteroProviderKey &&
          selectedNode.key !== kLocalBiliographyProviderKey) ? selectedNode.key : undefined;

        setItemData(bibMgr.search(searchTerm, providerKey, collectionKey));
      }
    }
    loadData();

    // load the right panel
  }, [props.selectedNode, searchTerm]);

  const searchChanged = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchTerm(e?.target.value);
  };

  // Dynamically size the ListBox
  const searchBoxRef = React.useRef<HTMLInputElement>(null);
  const [listHeight, setListHeight] = React.useState<number>(props.height);
  React.useEffect(() => {
    const searchBoxHeight = searchBoxRef.current?.clientHeight;
    if (searchBoxHeight) {
      setListHeight(props.height - searchBoxHeight);
    }
  }, []);

  return (
    <div style={props.style} className='pm-insert-citation-panel'>
      <div className='pm-insert-citation-panel-textbox-container'>
        <TextInput
          width='100%'
          iconAdornment={props.ui.images.search}
          tabIndex={0}
          className='pm-insert-citation-panel-textbox pm-block-border-color'
          placeholder={props.ui.context.translateText('Search for citation')}
          onChange={searchChanged}
          ref={searchBoxRef}
        />

      </div>
      <FixedSizeList
        height={listHeight}
        width='100%'
        itemCount={itemData.length}
        itemSize={64}
        itemData={{ data: itemData, sourcesToAdd: props.sourcesToAdd, addSource: props.addSource, removeSource: props.removeSource, ui: props.ui }}
      >
        {CitationListItem}
      </FixedSizeList>
    </div>);
};

export function bibliographyPanel(doc: ProsemirrorNode, ui: EditorUI, bibliographyManager: BibliographyManager): CitationPanel {
  const providers = bibliographyManager.localProviders();
  const localProviderNodes = providers.map(provider => {
    const node: any = {};
    node.key = provider.key;
    node.name = provider.name;
    node.type = provider.key;
    node.image = libraryImageForProvider(provider.key, ui);
    node.children = toTree(provider.key, provider.containers(doc, ui), folderImageForProvider(provider.key, ui));
    return node;
  });

  return {
    key: '17373086-77FE-410F-A319-33E314482125',
    panel: CitationListPanel,
    treeNode: {
      key: 'My Sources',
      name: 'My Sources',
      image: ui.images.citations?.local_sources,
      type: kAllLocalType,
      children: localProviderNodes,
      expanded: true
    }
  };
}

interface CitationListData {
  data: BibliographySource[];
  sourcesToAdd: BibliographySource[];
  addSource: (source: BibliographySource) => void;
  removeSource: (source: BibliographySource) => void;
  ui: EditorUI;
}

const CitationListItem = (props: ListChildComponentProps) => {

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

function libraryImageForProvider(providerKey: string, ui: EditorUI) {
  switch (providerKey) {
    case kZoteroProviderKey:
      return ui.images.citations?.zotero_library;
    case kLocalBiliographyProviderKey:
      return ui.images.citations?.bibligraphy;
  }
}

function folderImageForProvider(providerKey: string, ui: EditorUI) {
  switch (providerKey) {
    case kZoteroProviderKey:
      return ui.images.citations?.zotero_folder;
    case kLocalBiliographyProviderKey:
      return ui.images.citations?.bibligraphy_folder;
  }
}

// Takes a flat data structure of containers and turns it into a hierarchical
// tree structure for display as TreeNodes.
function toTree(type: string, containers: BibliographyContainer[], folderImage?: string): SelectTreeNode[] {

  const treeMap: { [id: string]: SelectTreeNode } = {};
  const rootNodes: SelectTreeNode[] = [];


  containers.sort((a, b) => a.name.localeCompare(b.name)).forEach(container => {

    // First see if we have an existing node for this item
    // A node could already be there if we had to insert a 'placeholder' 
    // node to contain the node's children before we encountered the node.
    const currentNode = treeMap[container.key] || { key: container.key, name: container.name, image: folderImage, children: [], type };

    // Always set its name to be sure we fill this in when we encounter it
    currentNode.name = container.name;

    if (container.parentKey) {
      let parentNode = treeMap[container.parentKey];
      if (!parentNode) {
        // This is a placeholder node - we haven't yet encountered this child's parent
        // so we insert this to hold the child. Once we encounter the true parent node, 
        // we will fix up the values in this placeholder node.
        parentNode = { key: container.parentKey, name: '', image: folderImage, children: [], type };
        treeMap[container.parentKey] = parentNode;
      }
      parentNode.children?.push(currentNode);
    } else {
      rootNodes.push(currentNode);
    }
    treeMap[container.key] = currentNode;
  });
  return rootNodes;
}

