// Panels get a variety of information as properties to permit them to search

import { WidgetProps } from "../../../api/widgets/react";

import { EditorUI } from "../../../api/ui";

import { NavigationTreeNode } from "../../../api/widgets/navigation-tree";
import { BibliographySource } from "../../../api/bibliography/bibliography";

// citations and add them
export interface CitationSourcePanelProps extends WidgetProps {
  ui: EditorUI;
  height: number;

  searchTerm: string;
  onSearchTermChanged: (term: string) => void;
  onExecuteSearch: (term: string) => void;

  citations: CitationListEntry[];
  citationsToAdd: CitationListEntry[];

  // TODO: could be indexes
  onAddCitation: (citation: CitationListEntry) => void;
  onRemoveCitation: (citation: CitationListEntry) => void;
  onConfirm: VoidFunction;

  selectedIndex: number;
  onSelectedIndexChanged: (index: number) => void;
  status: CitationSourceListStatus;

  ref: React.Ref<any>;
}

// Citation Panels Providers are the core element of ths dialog. Each provider provides
// the main panel UI as well as the tree to display when the panel is displayed.
export interface CitationSourcePanelProvider {
  key: string;
  panel: React.FC<CitationSourcePanelProps>;
  treeNode(): NavigationTreeNode;
  typeAheadSearch: (term: string, selectedNode: NavigationTreeNode) => CitationListEntry[] | null;
  search: (term: string, selectedNode: NavigationTreeNode) => Promise<CitationListEntry[] | null>;
}

export interface CitationListEntry extends BibliographySourceProvider {
  id: string;
  image?: string;
  imageAdornment?: string;
  type: string;
  title: string;
  authors: (width: number) => string;
  date: string;
  journal: string | undefined;
  doi?: string;
  providerKey: string;
}

export interface BibliographySourceProvider {
  id: string;
  showProgress: boolean;
  toBibliographySource: (id: string) => Promise<BibliographySource>;
}

export interface CitationSourceListStatusText {
  placeholder: string;
  error: string;
  progress: string;
  noResults: string;
}

export enum CitationSourceListStatus {
  default,
  inProgress,
  noResults,
  error
}

