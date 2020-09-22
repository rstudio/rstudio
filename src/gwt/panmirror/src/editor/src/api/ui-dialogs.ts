/*
 * ui-dialogs.ts
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

import { LinkTargets, LinkCapabilities, LinkType } from "./link";
import { ImageDimensions } from "./image";
import { ListCapabilities, ListType } from "./list";
import { TableCapabilities } from "./table";
import { CSL } from "./csl";
import { CiteField } from "./cite";
import { kStyleAttrib, attrPartitionKeyvalue, pandocAttrKeyvalueFromText } from "./pandoc_attr";

export interface EditorDialogs {
  alert: AlertFn;
  yesNoMessage: YesNoMessageFn;
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
  insertCite: InsertCiteFn;
  htmlDialog: EditorHTMLDialogFn;
}

export type EditorHTMLDialogFn = (
  title: string,
  okText: string | null,
  create: EditorHTMLDialogCreateFn,
  focus: VoidFunction,
  validate: EditorHTMLDialogValidateFn
) => Promise<boolean>;

export type EditorHTMLDialogCreateFn = (
  containerWidth: number,
  containerHeight: number,
  confirm: VoidFunction,
  cancel: VoidFunction,
  showProgress: (message: string) => void,
  hideProgress: VoidFunction
) => HTMLElement;

export type EditorHTMLDialogValidateFn = () => string | null;

export const kAlertTypeInfo = 1;
export const kAlertTypeWarning = 2;
export const kAlertTypeError = 3;

export type AlertFn = (message: string, title: string, type: number) => Promise<boolean>;

export type YesNoMessageFn = (message: string, title: string, type: number, yesLabel: string, noLabel: string) => Promise<boolean>;

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

export type InsertCiteFn = (props: InsertCiteProps) => Promise<InsertCiteResult | null>;

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

export interface InsertCiteProps {
  doi: string;
  existingIds: string[];
  bibliographyFiles: string[];
  provider?: string;
  csl?: CSL;
  citeUI?: InsertCiteUI;
}

export interface InsertCiteUI {
  suggestedId: string;
  previewFields: CiteField[];
}

export interface InsertCiteResult {
  id: string;
  bibliographyFile: string;
  csl: CSL;
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

export function attrInputToProps(attr: AttrEditInput): AttrProps {
  const classes = attr.classes ? attr.classes.split(/\s+/) : [];
  let keyvalue: Array<[string, string]> | undefined;
  if (attr.keyvalue || attr.style) {
    let text = attr.keyvalue || '';
    if (attr.style) {
      text += `\nstyle=${attr.style}\n`;
    }
    keyvalue = pandocAttrKeyvalueFromText(text, '\n');
  }
  return {
    id: asPandocId(attr.id || ''),
    classes: classes.map(asPandocClass),
    keyvalue,
  };
}



function asPandocId(id: string) {
  return id.replace(/^#/, '');
}

function asPandocClass(clz: string) {
  return clz.replace(/^\./, '');
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

function attrTextFromKeyvalue(keyvalue: Array<[string, string]>) {
  return keyvalue.map(kv => `${kv[0]}=${kv[1]}`).join('\n');
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


