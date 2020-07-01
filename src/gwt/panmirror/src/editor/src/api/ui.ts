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

import { ListCapabilities } from './list';
import { LinkTargets, LinkCapabilities, LinkType } from './link';
import { TableCapabilities } from './table';
import { ImageDimensions } from './image';
import { EditorUIImages } from './ui-images';

import { kStyleAttrib } from './pandoc_attr';
import { EditorRmdChunk } from './rmd';
import { SkinTone } from './emoji';

export interface EditorUI {
  dialogs: EditorDialogs;
  display: EditorDisplay;
  execute: EditorUIExecute;
  math: EditorUIMath;
  context: EditorUIContext;
  prefs: EditorUIPrefs;
  images: EditorUIImages;
}

export interface EditorDialogs {
  alert: AlertFn;
  editLink: LinkEditorFn;
  editImage: ImageEditorFn;
  editCodeBlock: CodeBlockEditorFn;
  editList: ListEditorFn;
  editAttr: AttrEditorFn;
  editSpan: AttrEditorFn;
  editDiv: DivAttrEditorFn;
  editRawInline: RawFormatEditorFn;
  editRawBlock: RawFormatEditorFn;
  insertTable: InsertTableFn;
}

export interface EditorUIContext {
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
}

export interface EditorMenuItem {
  command?: string;
  separator?: boolean;
  subMenu?: {
    text: string;
    items: EditorMenuItem[];
  };
}

export interface EditorUIExecute {
  executeRmdChunk?: (chunk: EditorRmdChunk) => void;
}

export interface EditorUIMath {
  typeset?: (el: HTMLElement, text: string) => Promise<boolean>;
}

export interface EditorDisplay {
  openURL: (url: string) => void;
  navigateToXRef: (file: string, xref: string) => void;
  showContextMenu?: (items: EditorMenuItem[], clientX: number, clientY: number) => Promise<boolean>;
}

export interface EditorUIPrefs {
  darkMode: () => boolean;
  equationPreview: () => boolean;
  tabKeyMoveFocus: () => boolean;
  emojiSkinTone: () => SkinTone;
  setEmojiSkinTone: (skinTone: SkinTone) => void;
}

export enum AlertType {
  Info,
  Warning,
  Error,
}

export type AlertFn = (message: string, title?: string, type?: AlertType) => Promise<boolean>;

export type AttrEditorFn = (attr: AttrProps, idHint?: string) => Promise<AttrEditResult | null>;

export type DivAttrEditorFn = (attr: AttrProps, removeEnabled: boolean) => Promise<AttrEditResult | null>;

export type LinkEditorFn = (
  link: LinkProps,
  targets: LinkTargets,
  capabilities: LinkCapabilities,
) => Promise<LinkEditResult | null>;

export type ImageEditorFn = (
  image: ImageProps,
  dims: ImageDimensions | null,
  editAttributes: boolean,
) => Promise<ImageEditResult | null>;

export type CodeBlockEditorFn = (
  codeBlock: CodeBlockProps,
  attributes: boolean,
  languages: string[],
) => Promise<CodeBlockEditResult | null>;

export type ListEditorFn = (list: ListProps, capabilities: ListCapabilities) => Promise<ListEditResult | null>;

export type RawFormatEditorFn = (raw: RawFormatProps, outputFormats: string[]) => Promise<RawFormatResult | null>;

export type InsertTableFn = (capabilities: TableCapabilities) => Promise<InsertTableResult | null>;

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
  linkTo?: string;
  width?: number;
  height?: number;
  units?: string;
  lockRatio?: boolean;
}

export type ImageEditResult = ImageProps;

export interface CodeBlockProps extends AttrProps {
  lang: string;
}

export type CodeBlockEditResult = CodeBlockProps;

export enum ListType {
  Ordered = 'OrderedList',
  Bullet = 'BulletList',
}

export interface ListProps {
  type: ListType;
  tight: boolean;
  order: number;
  number_style: string;
  number_delim: string;
}

export type ListEditResult = ListProps;

export interface InsertTableResult {
  rows: number;
  cols: number;
  header: boolean;
  caption?: string;
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
  let style: string | undefined;
  let keyvalue: string | undefined;
  if (attr.keyvalue) {
    const partitionedKeyvalue = attrPartitionKeyvalue([kStyleAttrib], attr.keyvalue);
    if (partitionedKeyvalue.partitioned.length > 0) {
      style = partitionedKeyvalue.partitioned[0][1];
    }
    keyvalue = attrTextFromKeyvalue(partitionedKeyvalue.base);
  }

  return {
    id: asHtmlId(attr.id) || undefined,
    classes: attr.classes ? attr.classes.map(asHtmlClass).join(' ') : undefined,
    style,
    keyvalue,
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

export function attrPartitionKeyvalue(partition: string[], keyvalue: Array<[string, string]>): AttrKeyvaluePartitioned {
  const base = new Array<[string, string]>();
  const partitioned = new Array<[string, string]>();

  keyvalue.forEach(kv => {
    if (partition.includes(kv[0])) {
      partitioned.push(kv);
    } else {
      base.push(kv);
    }
  });

  return {
    base,
    partitioned,
  };
}

function attrTextFromKeyvalue(keyvalue: Array<[string, string]>) {
  return keyvalue.map(kv => `${kv[0]}=${kv[1]}`).join('\n');
}

function attrKeyvalueFromText(text: string): Array<[string, string]> {
  const lines = text.trim().split('\n');
  return lines.map(line => {
    const parts = line.trim().split('=');
    return [parts[0], (parts[1] || '').replace(/^"/, '').replace(/"$/, '')];
  });
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
