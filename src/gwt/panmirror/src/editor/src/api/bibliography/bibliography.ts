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
import { NodeWithPos } from 'prosemirror-utils';

import Fuse from 'fuse.js';
import { PandocServer } from '../pandoc';
import uniqby from 'lodash.uniqby';

import { EditorUI } from '../ui';
import { yamlMetadataNodes, stripYamlDelimeters, parseYaml } from '../yaml';
import { CSL } from '../csl';
import { urlForDOI } from '../doi';
import { ZoteroServer } from '../zotero';
import { BibliographyDataProviderLocal } from './bibliography-provider_local';
import { BibliographyDataProviderZotero } from './bibliography-provider_zotero';


// TODO: Add a type distinction so the UI can differentiate (e.g. an overlay would be nice)
export interface BibliographyDataProvider {
  load(docPath: string, resourcePath: string, yamlBlocks: ParsedYaml[]): Promise<boolean>;
  bibliography(): Bibliography;
}

export interface Bibliography {
  sources: BibliographySource[];
  project_biblios: string[];
}

// The individual bibliographic source
export interface BibliographySource extends CSL {
  id: string; // TODO: Should we just figure out how to make id required and get rid of this interface
}

// The fields and weights that will indexed and searched
// when searching bibliographic sources
const kFields: Fuse.FuseOptionKeyObject[] = [
  { name: 'id', weight: 20 },
  { name: 'author.family', weight: 10 },
  { name: 'author.literal', weight: 10 },
  { name: 'author.given', weight: 1 },
  { name: 'title', weight: 1 },
  { name: 'issued', weight: 1 },
];

export class BibliographyManager {

  private fuse: Fuse<BibliographySource, Fuse.IFuseOptions<any>> | undefined;
  private providers: BibliographyDataProvider[];

  public constructor(server: PandocServer, zoteroServer: ZoteroServer) {
    this.providers = [new BibliographyDataProviderLocal(server), new BibliographyDataProviderZotero(zoteroServer)];
  }

  public async loadBibliography(ui: EditorUI, doc: ProsemirrorNode): Promise<Bibliography> {

    // read the Yaml blocks from the document
    const parsedYamlNodes = parseYamlNodes(doc);

    // Currently edited doc
    const docPath = ui.context.getDocumentPath() || '';

    // Load each provider
    const needsIndexUpdates = await Promise.all(this.providers.map(provider => provider.load(docPath, ui.context.getDefaultResourceDir(), parsedYamlNodes)));

    // Once loaded, see if any of the providers required an index update
    const needsIndexUpdate = needsIndexUpdates.reduce((prev, curr) => prev || curr);

    // Get the entries
    const providersEntries = this.providers.map(provider => provider.bibliography().sources);
    const providerEntries = ([] as BibliographySource[]).concat(...providersEntries);

    // Get the project biblios
    const providersProjectBibs = this.providers.map(provider => provider.bibliography().project_biblios);
    const providerProjectBibs = ([] as string[]).concat(...providersProjectBibs);

    // Update the index if anything requires that we do so
    if (needsIndexUpdate) {

      this.updateIndex(providerEntries);
    }

    // return sources
    return { sources: providerEntries, project_biblios: providerProjectBibs };
  }


  public findDoiInLoadedBibliography(doi: string): BibliographySource | undefined {

    // NOTE: This will only search sources that have already been loaded.
    // Please be sure to use loadBibliography before calling this or
    // accept the risk that this will not properly search for a DOI if the
    // bibliography hasn't already been loaded.
    return this.findPerfectMatch('doi', doi);
  }

  public findIdInLoadedBibliography(id: string): BibliographySource | undefined {
    // NOTE: This will only search sources that have already been loaded.
    // Please be sure to use loadBibliography before calling this or
    // accept the risk that this will not properly search for a DOI if the
    // bibliography hasn't already been loaded.
    return this.findPerfectMatch('id', id);
  }

  private findPerfectMatch(name: string, value: string): BibliographySource | undefined {
    if (this.fuse) {
      const options = {
        isCaseSensitive: false,
        shouldSort: true,
        includeMatches: false,
        includeScore: false,
        findAllMatches: false,
        limit: 1,
        threshold: 0.0, // Perfect match
        keys: { name },
      };
      const results: Array<Fuse.FuseResult<BibliographySource>> = this.fuse.search(value, options);
      const items = results.map((result: { item: any }) => result.item);
      if (items.length > 0) {
        return items[0];
      }
    }
  }

  public searchInLoadedBibliography(query: string, limit: number): BibliographySource[] {
    // NOTE: This will only search sources that have already been loaded.
    // Please be sure to use loadBibliography before calling this or
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

  public urlForSource(source: BibliographySource): string | undefined {
    if (source.URL) {
      return source.URL;
    } else if (source.DOI) {
      return urlForDOI(source.DOI);
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

// TODO: This should probably be moved to yaml.ts
export interface ParsedYaml {
  yamlCode: string;
  yaml: any;
  node: NodeWithPos;
}

export function parseYamlNodes(doc: ProsemirrorNode): ParsedYaml[] {
  const yamlNodes = yamlMetadataNodes(doc);

  const parsedYamlNodes = yamlNodes.map<ParsedYaml>(node => {
    const yamlText = node.node.textContent;
    const yamlCode = stripYamlDelimeters(yamlText);
    return { yamlCode, yaml: parseYaml(yamlCode), node };
  });
  return parsedYamlNodes;
}

export function cslFromDoc(doc: ProsemirrorNode): string | undefined {

  // read the Yaml blocks from the document
  const parsedYamlNodes = parseYamlNodes(doc);

  const cslParsedYamls = parsedYamlNodes.filter(
    parsedYaml => parsedYaml.yaml !== null && typeof parsedYaml.yaml === 'object' && parsedYaml.yaml.csl,
  );

  // Look through any yaml nodes to see whether any contain csl information
  if (cslParsedYamls.length > 0) {

    // Pandoc uses the last csl block (whether or not it shares a yaml block with the
    // bibliographies element that pandoc will ultimately use) so just pick the last csl
    // block.
    const cslParsedYaml = cslParsedYamls[cslParsedYamls.length - 1];
    const cslFile = cslParsedYaml.yaml.csl;
    return cslFile;
  }
  return undefined;
}


