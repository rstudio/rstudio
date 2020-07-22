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
import { ZoteroCollection, ZoteroServer, ZoteroCollectionSpec } from "../zotero";

import { ParsedYaml } from "../yaml";
import { suggestCiteId } from "../cite";

import { BibliographyDataProvider, BibliographySource } from "./bibliography";

export const kZoteroItemProvider = 'Zotero';

export class BibliographyDataProviderZotero implements BibliographyDataProvider {

  private collections: ZoteroCollection[] = [];
  private server: ZoteroServer;

  public constructor(server: ZoteroServer) {
    this.server = server;
  }

  public async load(docPath: string, _resourcePath: string, yamlBlocks: ParsedYaml[]): Promise<boolean> {

    let hasUpdates = false;
    if (zoteroEnabled(docPath, yamlBlocks)) {
      const collectionNames = zoteroCollectionsForDoc(yamlBlocks);

      try {

        // Don't send the items back through to the server
        const collectionSpecs = this.collections.map(collection => ({ name: collection.name, version: collection.version }));

        const useCache = true;
        const result = await this.server.getCollections(docPath, collectionNames, collectionSpecs || [], useCache);
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

  public items(): BibliographySource[] {
    const entryArrays = this.collections?.map(collection => this.bibliographySources(collection)) || [];
    const zoteroEntries = ([] as BibliographySource[]).concat(...entryArrays);
    return zoteroEntries;
  }

  public projectBibios(): string[] {
    return [];
  }

  private bibliographySources(collection: ZoteroCollection): BibliographySource[] {

    const items = collection.items?.map(item => {
      return {
        ...item,
        id: suggestCiteId([], item.author, item.issued),
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
// zotero: <collectionname> | [<collectionname>]  A single collection name or an array of collection
//                                                names that will be used when searching for citation
// 
// Be default, zotero integration is disabled. Add this header to enable integration
//
function zoteroEnabled(docPath: string | null, parsedYamls: ParsedYaml[]): boolean | undefined {
  const zoteroYaml = parsedYamls.filter(
    parsedYaml => parsedYaml.yaml !== null && typeof parsedYaml.yaml === 'object' && parsedYaml.yaml.zotero,
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

function zoteroCollectionsForDoc(parsedYamls: ParsedYaml[]): string[] | null {
  const zoteroYaml = parsedYamls.filter(
    parsedYaml => parsedYaml.yaml !== null && typeof parsedYaml.yaml === 'object' && parsedYaml.yaml.zotero,
  );

  // Look through any yaml nodes to see whether any contain bibliography information
  if (zoteroYaml.length > 0) {
    // Pandoc will use the last biblography node when generating a bibliography.
    // So replicate this and use the last biblography node that we find
    const zoteroParsedYaml = zoteroYaml[zoteroYaml.length - 1];
    const zoteroCollections = zoteroParsedYaml.yaml.zotero;

    if (
      Array.isArray(zoteroCollections) &&
      zoteroCollections.every(collection => typeof collection === 'string')) {
      return zoteroCollections;
    } else if (typeof zoteroCollections === 'string') {
      return [zoteroCollections];
      // zotero: true means request all collections (signified by null)
    } else if (typeof zoteroCollections === 'boolean' && zoteroCollections === true) {
      return null;
    }
  }
  return [];

}
