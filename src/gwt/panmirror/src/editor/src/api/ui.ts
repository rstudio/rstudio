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

export interface EditorUI {
  dialogs: EditorDialogs;
  context: EditorUIContext;
}

export interface EditorDialogs {
  alert: AlertFn;
  editLink: LinkEditorFn;
  editImage: ImageEditorFn;
  editOrderedList: OrderedListEditorFn;
  editAttr: AttrEditorFn;
  editSpan: AttrEditorFn;
  editDiv: AttrEditorFn;
  editRawInline: RawFormatEditorFn;
  editRawBlock: RawFormatEditorFn;
  insertTable: InsertTableFn;
  insertCitation: InsertCitationFn;
}

export interface EditorUIContext {
  translateResourcePath: (path: string) => string;
}

export enum AlertType {
  Info,
  Warning,
  Error,
}

export type AlertFn = (message: string, title?: string, type?: AlertType) => Promise<boolean>;

export type AttrEditorFn = (attr: AttrProps) => Promise<AttrEditResult | null>;

export type LinkEditorFn = (
  link: LinkProps,
  targets: LinkTargets,
  capabilities: LinkCapabilities,
) => Promise<LinkEditResult | null>;

export type ImageEditorFn = (image: ImageProps, editAttributes: boolean) => Promise<ImageEditResult | null>;

export type OrderedListEditorFn = (
  list: OrderedListProps,
  capabilities: ListCapabilities,
) => Promise<OrderedListEditResult | null>;

export type RawFormatEditorFn = (raw: RawFormatProps) => Promise<RawFormatResult | null>;

export type InsertTableFn = () => Promise<InsertTableResult | null>;

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
