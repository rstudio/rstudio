/*
 * bibliography.ts
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

import Fuse from 'fuse.js';
import { PandocServer } from '../pandoc';
import uniqby from 'lodash.uniqby';

import { EditorUI } from '../ui';
import { ParsedYaml, parseYamlNodes } from '../yaml';
import { CSL } from '../csl';
import { ZoteroServer } from '../zotero';
import { BibliographyDataProviderLocal, kLocalItemType } from './bibliography-provider_local';
import { BibliographyDataProviderZotero } from './bibliography-provider_zotero';


export interface BibliographyDataProvider {
  load(docPath: string | null, resourcePath: string, yamlBlocks: ParsedYaml[]): Promise<boolean>;
  items(): BibliographySource[];
  projectBibios(): string[];
}

export interface Bibliography {
  sources: CSL[];
  project_biblios: string[];
}

// The individual bibliographic source
export interface BibliographySource extends CSL {
  id: string;
  provider: string;
}

// The fields and weights that will indexed and searched
// when searching bibliographic sources
const kFields: Fuse.FuseOptionKeyObject[] = [
  { name: 'id', weight: .35 },
  { name: 'author.family', weight: .25 },
  { name: 'author.literal', weight: .25 },
  { name: 'title', weight: .1 },
  { name: 'author.given', weight: .025 },
  { name: 'issued', weight: .025 },
];

export class BibliographyManager {

  private fuse: Fuse<BibliographySource, Fuse.IFuseOptions<any>> | undefined;
  private providers: BibliographyDataProvider[];
  private sources?: BibliographySource[];
  private projectBibs?: string[];

  public constructor(server: PandocServer, zoteroServer: ZoteroServer) {
    this.providers = [new BibliographyDataProviderLocal(server), new BibliographyDataProviderZotero(zoteroServer)];
  }

  public async load(ui: EditorUI, doc: ProsemirrorNode): Promise<void> {

    // read the Yaml blocks from the document
    const parsedYamlNodes = parseYamlNodes(doc);

    // Currently edited doc
    const docPath = ui.context.getDocumentPath();

    // Load each provider
    const providersNeedUpdate = await Promise.all(this.providers.map(provider => provider.load(docPath, ui.context.getDefaultResourceDir(), parsedYamlNodes)));

    // Once loaded, see if any of the providers required an index update
    const needsIndexUpdate = providersNeedUpdate.reduce((prev, curr) => prev || curr);

    // Update the index if anything requires that we do so
    if (needsIndexUpdate) {
      // Get the entries
      const providersEntries = this.providers.map(provider => provider.items());
      this.sources = ([] as BibliographySource[]).concat(...providersEntries);

      // Get the project biblios
      const providersProjectBibs = this.providers.map(provider => provider.projectBibios());
      this.projectBibs = ([] as string[]).concat(...providersProjectBibs);

      this.updateIndex(this.sources);
    }
  }

  public hasSources() {
    return this.allSources().length > 0;
  }

  public allSources(): BibliographySource[] {
    if (this.sources) {
      return this.sources;
    }
    return [];
  }

  public localSources(): BibliographySource[] {
    return this.allSources().filter(source => source.provider === kLocalItemType);
  }

  public projectBiblios(): string[] {
    if (this.projectBibs) {
      return this.projectBibs;
    }
    return [];
  }

  public findDoiInLocalBibliography(doi: string): BibliographySource | undefined {
    // NOTE: This will only search sources that have already been loaded.
    // Please be sure to use load() before calling this or
    // accept the risk that this will not properly search for a DOI if the
    // bibliography hasn't already been loaded.
    return this.localSources().find(source => source.DOI === doi);
  }

  public findIdInLocalBibliography(id: string): BibliographySource | undefined {
    // NOTE: This will only search sources that have already been loaded.
    // Please be sure to use load() before calling this or
    // accept the risk that this will not properly search for a DOI if the
    // bibliography hasn't already been loaded.
    return this.localSources().find(source => source.id === id);
  }

  public searchAllSources(query: string, limit: number): BibliographySource[] {
    // NOTE: This will only search sources that have already been loaded.
    // Please be sure to use load() before calling this or
    // accept the risk that this will not properly search for a source if the
    // bibliography hasn't already been loaded.
    if (this.fuse) {
      const options = {
        isCaseSensitive: false,
        shouldSort: true,
        includeMatches: false,
        includeScore: false,
        limit,
        keys: kFields,
      };
      const results: Array<Fuse.FuseResult<BibliographySource>> = this.fuse.search(query, options);
      const items = results.map((result: { item: any }) => result.item);

      return uniqby(items, (source: BibliographySource) => source.id);

    } else {
      return [];
    }
  }

  private updateIndex(bibSources: BibliographySource[]) {
    // build search index
    const options = {
      keys: kFields.map(field => field.name),
    };
    const index = Fuse.createIndex(options.keys, bibSources);
    this.fuse = new Fuse(bibSources, options, index);
  }

}

