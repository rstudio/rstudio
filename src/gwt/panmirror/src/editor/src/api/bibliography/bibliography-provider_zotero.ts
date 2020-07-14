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
import { ZoteroCollection, ZoteroServer, ZoteroResult } from "../zotero";

import { BibliographyDataProvider, ParsedYaml, BibliographySource, Bibliography } from "./bibliography";

export class BibliographyDataProviderZotero implements BibliographyDataProvider {

  private collections?: ZoteroCollection[];
  private server: ZoteroServer;
  private biblio?: Bibliography;

  public constructor(server: ZoteroServer) {
    this.server = server;
  }


  public async load(docPath: string, _resourcePath: string, yamlBlocks: ParsedYaml[]): Promise<boolean> {
    if (zoteroEnabled(yamlBlocks)) {
      const collections = zoteroCollectionsForDoc(yamlBlocks);

      // TODO: Need to deal with version and cache and so on
      try {
        const result = await this.server.getCollections(docPath, collections, []);
        if (result.status === "ok") {
          if (!this.collections && result.message) {
            this.collections = result.message as ZoteroCollection[];
            const entryArrays = this.collections?.map(collection => this.bibliographySources(collection)) || [];
            const zoteroEntries = ([] as BibliographySource[]).concat(...entryArrays);
            this.biblio = { sources: zoteroEntries, project_biblios: [] };
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
      this.biblio = undefined;
    }
    return true;
  }

  public bibliography(): Bibliography {
    return this.biblio || { sources: [], project_biblios: [] };
  }

  private bibliographySources(collection: ZoteroCollection): BibliographySource[] {

    const items = collection.items?.map(item => {
      return {
        id: item.id || '',
        type: item.type || '',
        ...item
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

function zoteroEnabled(parsedYamls: ParsedYaml[]): boolean | undefined {
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
  }

  // TODO: Should we default this on or off
  return true;
}

function zoteroCollectionsForDoc(parsedYamls: ParsedYaml[]): string[] {
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
    } else {
      return [zoteroCollections];
    }
  }
  return [];

}
