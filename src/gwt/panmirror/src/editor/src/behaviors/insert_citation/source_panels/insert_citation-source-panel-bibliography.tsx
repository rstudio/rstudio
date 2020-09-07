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

import debounce from "lodash.debounce";

import { EditorUI } from "../../../api/ui";
import { NavigationTreeNode } from "../../../api/widgets/navigation-tree";
import { BibliographyManager, BibliographyCollection, BibliographySource } from "../../../api/bibliography/bibliography";
import { kZoteroProviderKey } from "../../../api/bibliography/bibliography-provider_zotero";
import { kLocalBiliographyProviderKey } from "../../../api/bibliography/bibliography-provider_local";
import { formatAuthors, formatIssuedDate, imageForType } from "../../../api/cite";

import { CitationSourcePanelProps, CitationSourcePanel, CitationListEntry } from "../insert_citation-panel";
import { CitationSourceTypeheadSearchPanel } from "./insert_citation-source-panel-typeahead-search";
import uniqby from "lodash.uniqby";

const kAllLocalSourcesRootNodeType = 'All Local Sources';

export function bibliographySourcePanel(doc: ProsemirrorNode, ui: EditorUI, bibliographyManager: BibliographyManager): CitationSourcePanel {
  const providers = bibliographyManager.localProviders();
  const localProviderNodes = providers.filter(provider => provider.isEnabled()).map(provider => {
    const node: NavigationTreeNode = {
      key: provider.key,
      name: ui.context.translateText(provider.name),
      type: provider.key,
      image: libraryImageForProvider(provider.key, ui),
      children: toTree(provider.key, provider.collections(), folderImageForProvider(provider.key, ui)),
      expanded: true
    };
    return node;
  });

  return {
    key: '17373086-77FE-410F-A319-33E314482125',
    panel: BibligraphySourcePanel,
    treeNode: {
      key: 'My Sources',
      name: ui.context.translateText('My Sources'),
      image: ui.images.citations?.local_sources,
      type: kAllLocalSourcesRootNodeType,
      children: localProviderNodes,
      expanded: true
    }
  };
}

export const BibligraphySourcePanel: React.FC<CitationSourcePanelProps> = props => {

  const bibMgr = props.bibliographyManager;
  const [citations, setCitations] = React.useState<CitationListEntry[]>([]);
  const [searchTerm, setSearchTerm] = React.useState<string>();

  React.useEffect(debounce(() => {
    async function loadData() {
      if (props.selectedNode) {

        // Ignore other nodes
        if (props.selectedNode.type !== kLocalBiliographyProviderKey &&
          props.selectedNode.type !== kZoteroProviderKey &&
          props.selectedNode.type !== kAllLocalSourcesRootNodeType) {
          return;
        }

        const selectedNode = props.selectedNode;

        // The node could be the root node, no provider
        const providerKey = selectedNode.type === kAllLocalSourcesRootNodeType ? undefined : selectedNode.type;

        // The node could be a provider root or a collection
        const collectionKey = (
          selectedNode.type !== kAllLocalSourcesRootNodeType &&
          selectedNode.key !== kZoteroProviderKey &&
          selectedNode.key !== kLocalBiliographyProviderKey) ? selectedNode.key : undefined;

        const sources = bibMgr.search(searchTerm, providerKey, collectionKey);
        const uniqueSources = uniqby(sources, source => source.id);
        setCitations(toCitationEntries(uniqueSources, props.ui));
      }
    }
    loadData();
    // load the right panel
  }, 50), [props.selectedNode, searchTerm]);

  // If the nodes change, clear the search box value
  React.useLayoutEffect(() => {
    // TODO: Clear search term when node changes
  }, [props.selectedNode]);

  // Search the user search terms
  const searchChanged = (term: string) => {
    setSearchTerm(term);
  };

  return (
    <CitationSourceTypeheadSearchPanel
      height={props.height}
      citations={citations}
      selectedNode={props.selectedNode}
      citationsToAdd={props.citationsToAdd}
      searchTermChanged={searchChanged}
      addCitation={props.addCitation}
      removeCitation={props.removeCitation}
      selectedCitation={props.selectedCitation}
      confirm={props.confirm}
      ui={props.ui}
    />
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
function toTree(type: string, containers: BibliographyCollection[], folderImage?: string): NavigationTreeNode[] {

  const treeMap: { [id: string]: NavigationTreeNode } = {};
  const rootNodes: NavigationTreeNode[] = [];

  // Sort the folder in alphabetical order at each level of the tree
  containers.sort((a, b) => {
    // For Zotero collection, sort the 'My Library to the top always'
    if (a.provider === kZoteroProviderKey && a.key === '1') {
      return -1;
    }
    return a.name.localeCompare(b.name);
  }).forEach(container => {
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

function toCitationEntries(sources: BibliographySource[], ui: EditorUI): CitationListEntry[] {
  return sources.map(source => {
    return {
      id: source.id,
      title: source.title || '',
      providerKey: source.providerKey,
      authors: (length: number) => {
        return formatAuthors(source.author, length);
      },
      date: formatIssuedDate(source.issued),
      journal: '',
      image: imageForType(ui, source.type)[0],
      imageAdornment: source.providerKey === kZoteroProviderKey ? ui.images.citations?.zoteroOverlay : undefined,
      toBibliographySource: () => {
        return Promise.resolve(source);
      }
    };
  });
}
