/*
 * bibliography-provider_zotero.ts
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
import { Node as ProsemirrorNode } from 'prosemirror-model';

import { ZoteroCollection, ZoteroServer, kZoteroBibLaTeXTranslator, kZoteroMyLibrary } from "../zotero";

import { ParsedYaml } from "../yaml";
import { suggestCiteId } from "../cite";

import { BibliographyDataProvider, BibliographySource, BibliographyFile } from "./bibliography";
import { EditorUI } from '../ui';
import { CSL } from '../csl';
import { toBibLaTeX } from './bibDB';

export const kZoteroItemProvider = 'Zotero';

export class BibliographyDataProviderZotero implements BibliographyDataProvider {

  private collections: ZoteroCollection[] = [];
  private server: ZoteroServer;
  private warning: string | undefined;

  public constructor(server: ZoteroServer) {
    this.server = server;
  }

  public name: string = "Zotero";

  public async load(docPath: string, _resourcePath: string, yamlBlocks: ParsedYaml[]): Promise<boolean> {

    let hasUpdates = false;
    if (zoteroEnabled(docPath, yamlBlocks)) {

      try {

        // Don't send the items back through to the server
        const collectionSpecs = this.collections.map(({ items, ...rest }) => rest);

        // If there is a warning, stop using the cache and force a fresh trip
        // through the whole pipeline to be sure we're trying to clear that warning
        const useCache = this.warning === undefined;

        // TODO: remove collection names from server call
        const result = await this.server.getCollections(docPath, null, collectionSpecs || [], useCache);
        this.warning = result.warning;
        if (result.status === "ok") {

          if (result.message) {

            const newCollections = (result.message as ZoteroCollection[]).map(collection => {
              const existingCollection = this.collections.find(col => col.name === collection.name);
              if (useCache && existingCollection && existingCollection.version === collection.version) {
                collection.items = existingCollection.items;
              } else {
                hasUpdates = true;
              }
              return collection;

            });
            hasUpdates = hasUpdates || newCollections.length !== this.collections.length;
            this.collections = newCollections;
          }
        } else {
          // console.log(result.status);
        }
      }
      catch (err) {
        // console.log(err);
      }
    } else {
      // Zotero is disabled, clear any already loaded bibliography
      if (this.collections.length > 0) {
        hasUpdates = true;
      }
      this.collections = [];
    }
    return hasUpdates;
  }

  public containers(doc: ProsemirrorNode, ui: EditorUI): string[] {
    return this.collections.map(collection => collection.name);
  }

  public items(): BibliographySource[] {
    const entryArrays = this.collections?.map(collection => this.bibliographySources(collection)) || [];
    const zoteroEntries = ([] as BibliographySource[]).concat(...entryArrays);
    return zoteroEntries;
  }

  public bibliographyPaths(doc: ProsemirrorNode, ui: EditorUI): BibliographyFile[] {
    return [];
  }

  public async generateBibLaTeX(ui: EditorUI, id: string, csl: CSL): Promise<string | undefined> {
    if (csl.key && ui.prefs.zoteroUseBetterBibtex()) {
      const bibLaTeX = await this.server.betterBibtexExport([csl.key], kZoteroBibLaTeXTranslator, kZoteroMyLibrary);
      if (bibLaTeX) {
        return Promise.resolve(bibLaTeX.message);
      }
    }
    return Promise.resolve(toBibLaTeX(id, csl));
  }

  public warningMessage(): string | undefined {
    return this.warning;
  }

  private bibliographySources(collection: ZoteroCollection): BibliographySource[] {

    const items = collection.items?.map(item => {
      return {
        ...item,
        id: item.id || suggestCiteId([], item),
        provider: kZoteroItemProvider,
      };
    });
    return items || [];
  }
}


// The Zotero header allows the following:
// zotero: true | false                           Globally enables or disables the zotero integration
//                                                If true, uses all collections. If false uses none.
//
// By default, zotero integration is enabled. Add this header to disable integration
//
function zoteroEnabled(docPath: string | null, parsedYamls: ParsedYaml[]): boolean | undefined {
  const zoteroYaml = parsedYamls.filter(
    parsedYaml => parsedYaml.yaml !== null && typeof parsedYaml.yaml === 'object'
  );

  if (zoteroYaml.length > 0) {
    // Pandoc will use the last biblography node when generating a bibliography.
    // So replicate this and use the last biblography node that we find
    const zoteroParsedYaml = zoteroYaml[zoteroYaml.length - 1];
    const zoteroConfig = zoteroParsedYaml.yaml.zotero;

    if (typeof zoteroConfig === 'boolean') {
      return zoteroConfig;
    }

    // There is a zotero header that isn't boolean, 
    // It is enabled
    return true;

    // any doc with a path could have project level zotero
  } else if (docPath !== null) {
    return true;
  } else {
    return false;
  }
}

