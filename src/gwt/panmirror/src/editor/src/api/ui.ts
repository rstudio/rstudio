/*
 * ui.ts
 *
 * Copyright (C) 2019-20 by RStudio, PBC
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

import { ListCapabilities } from './list';
import { LinkTargets, LinkCapabilities, LinkType } from './link';
import { TableCapabilities } from './table';

export interface EditorUI {
  dialogs: EditorDialogs;
  display: EditorDisplay;
  context: EditorUIContext;
}

export interface EditorDialogs {
  alert: AlertFn;
  editLink: LinkEditorFn;
  editImage: ImageEditorFn;
  editOrderedList: OrderedListEditorFn;
  editAttr: AttrEditorFn;
  editSpan: AttrEditorFn;
  editDiv: DivAttrEditorFn;
  editRawInline: RawFormatEditorFn;
  editRawBlock: RawFormatEditorFn;
  insertTable: InsertTableFn;
  insertCitation: InsertCitationFn;
}

export interface EditorUIContext {
  // get the current resource directory (e.g. where relative links point to)
  getResourceDir: () => string;

  // provide a URL that can be used to fetch the given resource path
  mapResourcePath: (path: string) => string;

  // translate a string
  translateText: (text: string) => string;
}

export interface EditorDisplay {
  openURL: (url: string) => void;
}

export enum AlertType {
  Info,
  Warning,
  Error,
}

export type AlertFn = (message: string, title?: string, type?: AlertType) => Promise<boolean>;

export type AttrEditorFn = (attr: AttrProps) => Promise<AttrEditResult | null>;

export type DivAttrEditorFn = (attr: AttrProps, removeEnabled: boolean) => Promise<AttrEditResult | null>;

export type LinkEditorFn = (
  link: LinkProps,
  targets: LinkTargets,
  capabilities: LinkCapabilities,
) => Promise<LinkEditResult | null>;

export type ImageEditorFn = (
  image: ImageProps,
  resourceDir: string,
  editAttributes: boolean,
) => Promise<ImageEditResult | null>;

export type OrderedListEditorFn = (
  list: OrderedListProps,
  capabilities: ListCapabilities,
) => Promise<OrderedListEditResult | null>;

export type RawFormatEditorFn = (raw: RawFormatProps) => Promise<RawFormatResult | null>;

export type InsertTableFn = (capabilities: TableCapabilities) => Promise<InsertTableResult | null>;

export type InsertCitationFn = () => Promise<InsertCitationResult | null>;

export interface AttrProps {
  readonly id?: string;
  readonly classes?: string[];
  readonly keyvalue?: Array<[string, string]>;
}

export interface AttrEditResult {
  readonly action: 'edit' | 'remove';
  readonly attr: AttrProps;
}

export interface LinkProps extends AttrProps {
  readonly type: LinkType;
  readonly text: string;
  readonly href: string;
  readonly heading?: string;
  readonly title?: string;
}

export interface LinkEditResult {
  readonly action: 'edit' | 'remove';
  readonly link: LinkProps;
}

export enum ImageType {
  Image,
  Figure,
}

export interface ImageProps extends AttrProps {
  src: string | null;
  title?: string;
  alt?: string;
}

export type ImageEditResult = ImageProps;

export interface OrderedListProps {
  tight: boolean;
  order: number;
  number_style: string;
  number_delim: string;
}

export type OrderedListEditResult = OrderedListProps;

export interface InsertTableResult {
  rows: number;
  cols: number;
  header: boolean;
  caption?: string;
}

export interface InsertCitationResult {
  id: string;
  locator: string;
}

export interface RawFormatProps {
  content: string;
  format: string;
}

export interface RawFormatResult {
  readonly action: 'edit' | 'remove';
  readonly raw: RawFormatProps;
}

export interface AttrEditInput {
  id?: string;
  classes?: string;
  style?: string;
  keyvalue?: string;
}

export interface AttrKeyvaluePartitioned {
  base: Array<[string, string]>;
  partitioned: Array<[string, string]>;
}

export function attrPropsToInput(attr: AttrProps): AttrEditInput {

  let style : string | undefined;
  let keyvalue: string | undefined;
  if (attr.keyvalue) {
    const partitionedKeyvalue = partitionKeyvalue(["style"], attr.keyvalue);
    if (partitionedKeyvalue.partitioned.length > 0) {
      style = partitionedKeyvalue.partitioned[0][1];
    }
    keyvalue = attrTextFromKeyvalue(partitionedKeyvalue.base);
  }

  return {
    id: asHtmlId(attr.id) || undefined,
    classes: attr.classes ? attr.classes.map(asHtmlClass).join(' ') : undefined,
    style,
    keyvalue
  };
}

export function attrInputToProps(attr: AttrEditInput): AttrProps {
  const classes = attr.classes ? attr.classes.split(/\s+/) : [];
  let keyvalue: Array<[string, string]> | undefined;
  if (attr.keyvalue || attr.style) {
    let text = attr.keyvalue || '';
    if (attr.style) {
      text += `\nstyle=${attr.style}\n`;
    }
    keyvalue = attrKeyvalueFromText(text);
  }
  return {
    id: asPandocId(attr.id || ''),
    classes: classes.map(asPandocClass),
    keyvalue,
  };
}

function attrTextFromKeyvalue(keyvalue: Array<[string,string]>) {
  return keyvalue.map(kv => `${kv[0]}=${kv[1]}`).join('\n');
}


function attrKeyvalueFromText(text: string) : Array<[string,string]> {
  const lines = text.trim().split('\n');
  return lines.map(line => {
    const parts = line.trim().split('=');
    return [parts[0], (parts[1] || '').replace(/^"/, '').replace(/"$/, '')];
  });
}

function partitionKeyvalue(partition: string[], keyvalue: Array<[string, string]>) : AttrKeyvaluePartitioned {
  
  const base = new Array<[string,string]>();
  const partitioned = new Array<[string,string]>();

  keyvalue.forEach(kv => {
    if (partition.includes(kv[0])) {
      partitioned.push(kv);
    } else {
      base.push(kv);
    }
  });
  
  return {
    base,
    partitioned
  };
}

function asHtmlId(id: string | undefined) {
  if (id) {
    if (id.startsWith('#')) {
      return id;
    } else {
      return '#' + id;
    }
  } else {
    return id;
  }
}

function asHtmlClass(clz: string | undefined) {
  if (clz) {
    if (clz.startsWith('.')) {
      return clz;
    } else {
      return '.' + clz;
    }
  } else {
    return clz;
  }
}

function asPandocId(id: string) {
  return id.replace(/^#/, '');
}

function asPandocClass(clz: string) {
  return clz.replace(/^\./, '');
}
