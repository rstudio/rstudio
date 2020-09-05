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

import { ZoteroCollection, ZoteroServer, kZoteroBibLaTeXTranslator, kZoteroMyLibrary, ZoteroCollectionSpec } from "../zotero";

import { ParsedYaml } from "../yaml";
import { suggestCiteId } from "../cite";

import { BibliographyDataProvider, BibliographySource, BibliographyFile, BibliographyCollection, BibliographySourceWithCollections } from "./bibliography";
import { EditorUI } from '../ui';
import { CSL } from '../csl';
import { toBibLaTeX } from './bibDB';

export const kZoteroProviderKey = '2509FBBE-5BB0-44C4-B119-6083A81ED673';

export class BibliographyDataProviderZotero implements BibliographyDataProvider {

  private allCollections: ZoteroCollection[] = [];
  private allCollectionSpecs: ZoteroCollectionSpec[] = [];
  private server: ZoteroServer;
  private warning: string | undefined;
  private enabled = true;

  public constructor(server: ZoteroServer) {
    this.server = server;
  }

  public name: string = "Zotero";
  public key: string = kZoteroProviderKey;

  public async load(docPath: string, _resourcePath: string, yamlBlocks: ParsedYaml[]): Promise<boolean> {

    let hasUpdates = false;
    const config = zoteroConfig(yamlBlocks);
    if (config) {

      // Enabled
      this.enabled = true;
      try {

        // Don't send the items back through to the server
        const collectionSpecs = this.allCollections.map(({ items, ...rest }) => rest);

        // If there is a warning, stop using the cache and force a fresh trip
        // through the whole pipeline to be sure we're trying to clear that warning
        const useCache = this.warning === undefined || this.warning.length === 0;

        // The collection specified in the document header
        const collections = Array.isArray(config) ? config : [];

        // Read collection specs (this is required to discover the entire tree structure, not just the root collections)
        const allCollectionSpecsResult = await this.server.getConfiguredCollectionSpecs(docPath, collections);
        if (allCollectionSpecsResult && allCollectionSpecsResult.status === 'ok') {
          this.allCollectionSpecs = allCollectionSpecsResult.message;
        }

        const result = await this.server.getCollections(docPath, collections, collectionSpecs || [], useCache);
        this.warning = result.warning;
        if (result.status === 'ok') {

          if (result.message) {

            const newCollections = (result.message as ZoteroCollection[]).map(collection => {
              const existingCollection = this.allCollections.find(col => col.name === collection.name);
              if (useCache && existingCollection && existingCollection.version === collection.version) {
                collection.items = existingCollection.items;
              } else {
                hasUpdates = true;
              }
              return collection;

            });
            hasUpdates = hasUpdates || newCollections.length !== this.collections.length;
            this.allCollections = newCollections;
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
      // Disabled
      this.enabled = false;
      this.allCollections = [];
      this.allCollectionSpecs = [];
    }
    return hasUpdates;
  }

  // Respect enabled;
  public isEnabled(): boolean {
    return this.enabled;
  }

  public collections(doc: ProsemirrorNode, ui: EditorUI): BibliographyCollection[] {
    return this.allCollectionSpecs.map(spec => ({ name: spec.name, key: spec.key, parentKey: spec.parentKey, provider: kZoteroProviderKey }));
  }

  public items(): BibliographySourceWithCollections[] {
    const entryArrays = this.allCollections?.map(collection => this.bibliographySources(collection)) || [];
    const zoteroEntries = ([] as BibliographySourceWithCollections[]).concat(...entryArrays);
    return zoteroEntries;
  }

  public itemsForCollection(collectionKey?: string): BibliographySourceWithCollections[] {
    if (!collectionKey) {
      return this.items();
    }

    return this.items().filter((item: any) => {
      if (item.collectionKeys) {
        return item.collectionKeys.includes(collectionKey);
      }
      return false;
    });
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

  private bibliographySources(collection: ZoteroCollection): BibliographySourceWithCollections[] {

    const items = collection.items?.map(item => {
      return {
        ...item,
        id: item.id || suggestCiteId([], item),
        providerKey: this.key,
        collectionKeys: item.collectionKeys || []
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
function zoteroConfig(parsedYamls: ParsedYaml[]): boolean | string[] {
  const zoteroYaml = parsedYamls.filter(
    parsedYaml => parsedYaml.yaml !== null && typeof parsedYaml.yaml === 'object'
  );

  if (zoteroYaml.length > 0) {
    // Pandoc will use the last biblography node when generating a bibliography.
    // So replicate this and use the last biblography node that we find
    const zoteroParsedYaml = zoteroYaml[zoteroYaml.length - 1];
    const zotero = zoteroParsedYaml.yaml.zotero;

    if (typeof zotero === 'boolean') {
      return zotero;
    } else if (typeof zotero === 'string') {
      return [zotero];
    } else if (Array.isArray(zotero)) {
      return zotero.map(String);
    } else {
      return true;
    }

  } else {
    return true;
  }
}

