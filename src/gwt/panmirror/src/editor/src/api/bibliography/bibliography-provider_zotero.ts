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

import { BibliographyDataProvider, BibliographySource, Bibliography } from "./bibliography";
import { ParsedYaml } from "../yaml";
import { suggestCiteId } from "../cite";

const kZoteroItemProvider = 'Zotero';

export class BibliographyDataProviderZotero implements BibliographyDataProvider {

  private collections?: ZoteroCollection[];
  private server: ZoteroServer;

  public constructor(server: ZoteroServer) {
    this.server = server;
  }

  // TODO: with JJA investigate streaming as way to improve responsiveness of initial load
  public async load(docPath: string | null, _resourcePath: string, yamlBlocks: ParsedYaml[]): Promise<boolean> {
    if (zoteroEnabled(docPath, yamlBlocks)) {
      const collections = zoteroCollectionsForDoc(yamlBlocks);

      // TODO: Need to deal with version and cache and so on
      try {
        const result = await this.server.getCollections(docPath, collections, this.collections as ZoteroCollectionSpec[] || []);
        if (result.status === "ok") {
          if (!this.collections && result.message) {
            this.collections = result.message as ZoteroCollection[];
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
      this.collections = undefined;
    }
    return true;
  }

  public items(): BibliographySource[] {
    const entryArrays = this.collections?.map(collection => this.bibliographySources(collection)) || [];
    const zoteroEntries = ([] as BibliographySource[]).concat(...entryArrays);
    return zoteroEntries;
  }

  public projectBibios(): string[] {
    return [];
  }

  private hasCollections(collectionsNamed: string[]) {
    if (!this.collections) {
      return false;
    }
    return collectionsNamed.every(collectionName => (this.collections as ZoteroCollectionSpec[]).find(collection => collection.name === collectionName));
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

  private collectionVersion(collectionName: string): number {
    const match = this.collections?.find(collection => collection.name === collectionName);
    if (match) {
      return match.version;
    }
    return 0;
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
