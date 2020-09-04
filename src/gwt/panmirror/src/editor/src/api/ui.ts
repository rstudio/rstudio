/*
 * ui.ts
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

import { SkinTone } from './emoji';
import { EditorRmdChunk } from './rmd';
import { XRef } from './xref';

import { EditorUIImages } from './ui-images';
import { EditorDialogs } from './ui-dialogs';
import { EditorUISpelling } from './spelling';

export interface EditorUI {
  dialogs: EditorDialogs;
  display: EditorDisplay;
  execute: EditorUIExecute;
  math: EditorUIMath;
  context: EditorUIContext;
  prefs: EditorUIPrefs;
  images: EditorUIImages;
  chunks: EditorUIChunks;
  spelling: EditorUISpelling;
}

export interface EditorUIChunks {
  // create a code chunk editor
  createChunkEditor: (type: string, index: number, getPos: () => number) => ChunkEditor;
}

export interface ChunkEditor {
  editor: unknown;
  setMode(mode: string): void;
  executeSelection(): void;
  element: HTMLElement;
  destroy(): void;
}


export interface EditorUIContext {
  // check if we are the active tab
  isActiveTab: () => boolean;

  // get the path to the current document
  getDocumentPath: () => string | null;

  // ensure the edited document is saved on the server before proceeding
  // (note this just means that the server has a copy of it for e.g. 
  // indexing xrefs, from the user's standpoint the doc is still dirty)
  withSavedDocument: () => Promise<boolean>;

  // get the default directory for resources (e.g. where relative links point to)
  getDefaultResourceDir: () => string;

  // map from a filesystem path to a resource reference
  mapPathToResource: (path: string) => string;

  // map from a resource reference (e.g. images/foo.png) to a URL we can use in the document
  mapResourceToURL: (path: string) => string;

  // watch a resource for changes (returns an unsubscribe function)
  watchResource: (path: string, notify: VoidFunction) => VoidFunction;

  // translate a string
  translateText: (text: string) => string;

  // are we running in windows desktop mode?
  isWindowsDesktop: () => boolean;
}

export interface EditorMenuItem {
  text?: string;
  exec?: VoidFunction;
  command?: string;
  separator?: boolean;
  subMenu?: {
    items: EditorMenuItem[];
  };
}

export interface EditorUIMath {
  typeset?: (el: HTMLElement, text: string, priority: boolean) => Promise<boolean>;
}

export interface EditorDisplay {
  openURL: (url: string) => void;
  navigateToXRef: (file: string, xref: XRef) => void;
  showContextMenu?: (items: EditorMenuItem[], clientX: number, clientY: number) => Promise<boolean>;
}

export interface EditorUIPrefs {
  darkMode: () => boolean;
  equationPreview: () => boolean;
  tabKeyMoveFocus: () => boolean;
  emojiSkinTone: () => SkinTone;
  setEmojiSkinTone: (skinTone: SkinTone) => void;
  zoteroUseBetterBibtex: () => boolean;
  setBibliographyDefaultType: (type: string) => void;
  bibliographyDefaultType: () => string;
}

