import React from "react";

import { Node as ProsemirrorNode } from 'prosemirror-model';

import { FixedSizeList, ListChildComponentProps } from "react-window";

import { BibliographySource, BibliographyManager, BibliographyContainer } from "../../../api/bibliography/bibliography";
import { CitationPanelProps, CitationPanel } from "../insert_citation-picker";
import { EditorUI } from "../../../api/ui";
import { SelectTreeNode } from "../select_tree";
import { kZoteroProviderKey } from "../../../api/bibliography/bibliography-provider_zotero";
import { kLocalBiliographyProviderKey } from "../../../api/bibliography/bibliography-provider_local";


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

  const filteredItemData = itemData.filter(data => !props.sourcesToAdd.map(source => source.id).includes(data.id));

  const searchChanged = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchTerm(e?.target.value);
  };

  return (
    <div>
      <input type="text" value={searchTerm} onChange={searchChanged} />
      <FixedSizeList
        height={500}
        width='100%'
        itemCount={filteredItemData.length}
        itemSize={50}
        itemData={{ data: filteredItemData, addSource: props.addSource }}
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
  addSource: (source: BibliographySource) => void;
}

const CitationListItem = (props: ListChildComponentProps) => {

  const citationListData: CitationListData = props.data;
  const source = citationListData.data[props.index];

  const onClick = (event: React.MouseEvent) => {
    citationListData.addSource(source);
  };

  return (<div style={props.style} onClick={onClick}>
    {source.title}
  </div>);
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

