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

import { EditorUI } from "../../../api/ui";
import { NavigationTreeNode } from "../../../api/widgets/navigation-tree";
import { BibliographyManager, BibliographyCollection, BibliographySource } from "../../../api/bibliography/bibliography";
import { kZoteroProviderKey } from "../../../api/bibliography/bibliography-provider_zotero";
import { kLocalBiliographyProviderKey } from "../../../api/bibliography/bibliography-provider_local";
import { formatAuthors, formatIssuedDate, imageForType } from "../../../api/cite";

import { CitationSourcePanelProps, CitationSourcePanelProvider, CitationListEntry } from "../insert_citation-panel";
import { CitationSourceTypeheadSearchPanel } from "./insert_citation-source-panel-typeahead-search";
import uniqby from "lodash.uniqby";

const kAllLocalSourcesRootNodeType = 'All Local Sources';

export function bibliographySourcePanel(doc: ProsemirrorNode, ui: EditorUI, bibliographyManager: BibliographyManager): CitationSourcePanelProvider {

  const providers = bibliographyManager.localProviders();
  const providerNodes: { [key: string]: NavigationTreeNode } = {};

  // For each of the providers, discover their collections
  providers.filter(provider => provider.isEnabled()).forEach(provider => {

    const getFolder = (prov: string, hasParent: boolean) => {
      return folderImageForProvider(prov, hasParent, ui);
    };

    // Get the response which could be items or could be a stream
    const collectionSpecs = provider.collections();
    // Note the temporary collections
    providerNodes[provider.key] = {
      key: provider.key,
      name: ui.context.translateText(provider.name),
      type: provider.key,
      image: rootImageForProvider(provider.key, ui),
      children: toTree(provider.key, collectionSpecs, getFolder),
      expanded: true
    };
  });

  return {
    key: '17373086-77FE-410F-A319-33E314482125',
    panel: BibligraphySourcePanel,
    treeNode: () => {
      return {
        key: 'My Sources',
        name: ui.context.translateText('My Sources'),
        image: ui.images.citations?.local_sources,
        type: kAllLocalSourcesRootNodeType,
        children: Object.values(providerNodes),
        expanded: true
      };
    },
    typeAheadSearch: (searchTerm: string, selectedNode: NavigationTreeNode) => {

      const providerForNode = (node: NavigationTreeNode): string | undefined => {
        // The node could be the root node, no provider
        return node.type === kAllLocalSourcesRootNodeType ? undefined : node.type;
      };

      const collectionKeyForNode = (node: NavigationTreeNode): string | undefined => {
        // The node could be a provider root or a collection
        return (node.type !== kAllLocalSourcesRootNodeType &&
          node.key !== kZoteroProviderKey &&
          node.key !== kLocalBiliographyProviderKey) ? node.key : undefined;
      };
      const sources = bibliographyManager.search(searchTerm, providerForNode(selectedNode), collectionKeyForNode(selectedNode));
      const uniqueSources = uniqby(sources, source => source.id);
      return toCitationEntries(uniqueSources, ui);

    },
    search: (searchTerm: string, selectedNode: NavigationTreeNode) => {
      return Promise.resolve(null);
    }
  };
}

export const BibligraphySourcePanel = React.forwardRef<HTMLDivElement, CitationSourcePanelProps>((props: CitationSourcePanelProps, ref) => {
  return (
    <CitationSourceTypeheadSearchPanel
      height={props.height}
      citations={props.citations}
      citationsToAdd={props.citationsToAdd}
      searchTerm={props.searchTerm}
      onSearchTermChanged={props.onSearchTermChanged}
      selectedIndex={props.selectedIndex}
      onSelectedIndexChanged={props.onSelectedIndexChanged}
      onAddCitation={props.onAddCitation}
      onRemoveCitation={props.onRemoveCitation}
      onConfirm={props.onConfirm}
      status={props.status}
      statusText={
        {
          placeholder: props.ui.context.translateText(''),
          progress: props.ui.context.translateText(''),
          noResults: props.ui.context.translateText('No matching items'),
          error: props.ui.context.translateText('An error occurred'),
        }
      }
      ui={props.ui}
      ref={ref}
    />
  );
});


function rootImageForProvider(providerKey: string, ui: EditorUI) {
  switch (providerKey) {
    case kZoteroProviderKey:
      return ui.images.citations?.zotero_root;
    case kLocalBiliographyProviderKey:
      return ui.images.citations?.bibligraphy;
  }
}

function folderImageForProvider(providerKey: string, hasParent: boolean, ui: EditorUI) {

  switch (providerKey) {
    case kZoteroProviderKey:
      if (hasParent) {
        return ui.images.citations?.zotero_collection;
      } else {
        return ui.images.citations?.zotero_library;
      }
    case kLocalBiliographyProviderKey:
      return ui.images.citations?.bibligraphy_folder;
  }
}

// Takes a flat data structure of containers and turns it into a hierarchical
// tree structure for display as TreeNodes.
function toTree(type: string, containers: BibliographyCollection[], folderImage?: (providerKey: string, hasParent: boolean) => string | undefined): NavigationTreeNode[] {

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
    const currentNode = treeMap[container.key] || { key: container.key, name: container.name, children: [], type };

    // Always set its name to be sure we fill this in when we encounter it
    const hasParent = container.parentKey !== undefined && container.parentKey.length > 0;
    currentNode.name = container.name;
    currentNode.image = folderImage ? folderImage(container.provider, hasParent) : undefined;

    if (container.parentKey) {
      let parentNode = treeMap[container.parentKey];
      if (!parentNode) {
        // This is a placeholder node - we haven't yet encountered this child's parent
        // so we insert this to hold the child. Once we encounter the true parent node, 
        // we will fix up the values in this placeholder node.
        parentNode = { key: container.parentKey, name: '', children: [], type };
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
