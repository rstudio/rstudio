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

  onAddCitation: (citation: CitationListEntry) => void;
  onRemoveCitation: (citation: CitationListEntry) => void;
  onConfirm: VoidFunction;

  selectedIndex: number;
  onSelectedIndexChanged: (index: number) => void;

  status: CitationSourceListStatus;
  statusMessage: string;

  warningMessage: string;

  ref: React.Ref<any>;
}

// Citation Panels Providers are the core element of ths dialog. Each provider provides
// the main panel UI as well as the tree to display when the panel is displayed.
export interface CitationSourcePanelProvider {
  key: string;
  panel: React.FC<CitationSourcePanelProps>;
  treeNode(): NavigationTreeNode;
  placeHolderMessage?: string;
  progressMessage?: string;
  warningMessage?: string;
  typeAheadSearch: (term: string, selectedNode: NavigationTreeNode, existingCitationIds: string[]) => CitationSourcePanelSearchResult | null;
  search: (term: string, selectedNode: NavigationTreeNode, existingCitationIds: string[]) => Promise<CitationSourcePanelSearchResult>;
}

export interface CitationSourcePanelSearchResult {
  citations: CitationListEntry[];
  status: CitationSourceListStatus;
  statusMessage: string;
}

export interface CitationListEntry extends BibliographySourceProvider {
  id: string;
  isIdEditable: boolean;
  image?: string;
  imageAdornment?: string;
  type: string;
  title: string;
  authors: (width: number) => string;
  date: string;
  journal: string | undefined;
  doi?: string;
}

export interface BibliographySourceProvider {
  id: string;
  isSlowGeneratingBibliographySource: boolean;
  toBibliographySource: (id: string) => Promise<BibliographySource>;
}

export enum CitationSourceListStatus {
  default,
  inProgress,
  noResults,
  error
}

export function errorForStatus(ui: EditorUI, status: string, providerName: string) {
  return status === 'nohost' ?
    ui.context.translateText(`Unable to search ${providerName}. Please check your network connection and try again.`) :
    ui.context.translateText(`An error occurred while searching ${providerName}.`);
}
