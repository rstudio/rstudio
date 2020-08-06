/*
 * BibDB.ts
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
import { BibDB, EntryObject, BibFieldTypes, NameDictObject, NodeArray, RangeArray, BibField, BibLatexExporter, BibTypes, BibType } from "biblatex-csl-converter";

import { CSL, CSLDate, cslDateToEDTFDate, CSLName } from "../csl";

// Generates bibLaTeX for a given CSL object / id
export function toBibLaTeX(id: string, csl: CSL) {

  // A BibDB is basically a map of key / EntryObject[] that is
  // used by the exporter to generate BibLaTeX
  const bibDB = cslToBibDB(id, csl);
  if (bibDB) {
    // Use the exported to parse the bibDB and generate bibLaTeX
    const exporter: BibLatexExporter = new BibLatexExporter(bibDB);
    const sourceAsBibLaTeX = exporter.parse();

    // Indent any , new lines
    return sourceAsBibLaTeX.replace(/,\n/g, ',\n\t');
  }
}

// Converts a single CSL item to a bibDB containing
// a single EntryObject representing that CSL item
function cslToBibDB(id: string, csl: CSL): BibDB | undefined {

  const bibType = bibTypeForCSL(csl.type);
  if (bibType) {

    const bibObject: EntryObject = {
      bib_type: bibType[0],
      entry_key: id,
      'fields': {}
    };

    const enumerableCSL = csl as any;
    Object.keys(enumerableCSL).forEach(key => {
      const value: any = enumerableCSL[key];

      const bibFieldDatas = bibFieldForValue(key, csl.type);

      bibFieldDatas?.forEach(bibFieldData => {
        if (bibFieldData) {
          const bibFieldKey = bibFieldData[0];
          const bibField = bibFieldData[1];
          const type = bibField.type;
          let nodeValue: any;
          switch (type) {
            case ('f_date'):
              // f_date = // EDTF 1.0 level 0/1 compliant string. (2000-12-31)
              const cslDate = value as CSLDate;
              if (cslDate) {
                const edtfDate = cslDateToEDTFDate(cslDate);
                if (edtfDate) {
                  nodeValue = edtfDate;
                }
              }
              break;
            case ('f_integer'):
            case ('f_literal'):
            case ('f_long_literal'):
            case ('f_title'):
              // f_integer, f_literal, f_long_literal, f_title = [nodeValue]
              // l_literal = [nodeValue]
              if (value) {
                nodeValue = textNodes(value);
              }
              break;
            case ('l_literal'):
              // l_literal = [NodeArray]
              if (value) {
                nodeValue = [textNodes(value)];
              }
              break;
            case ('f_key'):
              // f_key: string | NodeArray (string points to another key 
              // name in BibObject whose value is used for this key)
              if (bibField.options) {
                const options = bibField.options as any;
                Object.keys(options).find(optionKey => {
                  const optionValue: any = options[optionKey];
                  if (optionValue.csl === value) {
                    nodeValue = optionKey;
                    return true;
                  }
                });

                if (!nodeValue) {
                  nodeValue = textNodes(value);
                }
              }

              break;
            case ('l_key'):
              // l_key, list of [string | NodeArray]
              if (bibField.options) {
                const options = bibField.options as any;
                Object.keys(options).find(optionKey => {
                  const optionValue: any = options[optionKey];
                  if (optionValue.csl === value) {
                    nodeValue = [optionKey];
                    return true;
                  }
                });

                if (!nodeValue) {
                  nodeValue = textNodes(value);
                }
              }
              break;
            case ('l_range'):
              // l_range Array<RangeArray>
              const valueStr = value as string;
              const parts = valueStr.split('-');
              const range = rangeArray(parts);
              if (range) {
                nodeValue = [range];
              }
              break;
            case ('f_uri'):
            case ('f_verbatim'):
              // f_uri, f_verbatim: string
              nodeValue = value;
              break;
            case ('l_name'):
              // l_name Array<NameDictObject>
              const names = value as CSLName[];
              nodeValue = names.map(name => {
                const nameDict: NameDictObject = {
                  family: textNodes(name.family),
                  given: textNodes(name.given),
                  literal: name.literal ? textNodes(name.literal) : undefined,
                };
                return nameDict;
              });

              break;
            case ('l_tag'):
              // l_tag: string[]
              nodeValue = [value];
              break;
          }

          if (nodeValue) {
            if (shouldIncludeField(bibFieldKey, bibType[1])) {
              bibObject.fields[bibFieldKey] = nodeValue;
            }
          }
        }
      });
    });

    const bibDB: BibDB = {
      'item': bibObject
    };
    return bibDB;

  }
}

function shouldIncludeField(bibDBFieldName: string, bibType: BibType) {
  if (bibType.required.includes(bibDBFieldName)) {
    return true;
  }

  if (bibType.optional.includes(bibDBFieldName)) {
    return true;
  }

  if (bibType.eitheror.includes(bibDBFieldName)) {
    return true;
  }

  return false;
}


function textNodes(str: string): NodeArray {
  // TODO: Need to parse text and add marks
  return [{
    type: 'text',
    text: str,
    marks: [],
    attrs: {}
  }];
}

function rangeArray(parts: string[]): RangeArray | undefined {
  if (parts.length === 1) {
    return [textNodes(parts[0])];
  } else if (parts.length === 2) {
    return [textNodes(parts[0]), textNodes(parts[1])];
  }
}

function bibTypeForCSL(cslType: string): [string, BibType] | undefined {
  const key = Object.keys(BibTypes).find(bibTypeKey => {
    const bibType = BibTypes[bibTypeKey];
    if (bibType.csl === cslType) {
      return bibTypeKey;
    }
  });

  if (key) {
    const bibType = BibTypes[key];
    return [key, bibType];
  }
}

function bibFieldForValue(cslKey: string, cslType: string): Array<[string, BibField]> | undefined {
  // Special case the following fields:
  // article-journal issue
  // patent number
  // * collection-number
  // See https://discourse.citationstyles.org/t/issue-number-and-bibtex/1072
  // https://github.com/fiduswriter/biblatex-csl-converter/blob/35d152935eba253ebadd00e285fb13c5828f167f/src/const.js#L561
  if (cslType === 'article-journal' && cslKey === 'issue' ||
    cslType === 'patent' && cslKey === 'number' ||
    cslKey === 'collection-number') {
    const bibField = {
      type: 'f_literal',
      biblatex: 'number',
      csl: cslKey
    };
    return [['number', bibField]];
  }

  // Find the key that corresponds to this CSL key
  const keys = Object.keys(BibFieldTypes).filter(bibFieldKey => {
    const bibField = BibFieldTypes[bibFieldKey];
    const cslFieldName = bibField.csl;
    if (cslFieldName && cslFieldName === cslKey) {
      return bibField;
    }
  });

  // Get the field and return
  if (keys) {
    return keys.map(key => {
      const bibField = BibFieldTypes[key];
      return [key, bibField];
    });
  }
}
