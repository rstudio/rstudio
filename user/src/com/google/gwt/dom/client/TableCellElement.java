/*
 * Copyright 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dom.client;

/**
 * The object used to represent the TH and TD elements.
 * 
 * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#edef-TD
 */
public class TableCellElement extends Element {

  /**
   * Assert that the given {@link Element} is compatible with this class and
   * automatically typecast it.
   */
  public static TableCellElement as(Element elem) {
    assert elem.getTagName().equalsIgnoreCase("td");
    return (TableCellElement) elem;
  }

  protected TableCellElement() {
  }

  /**
   * The index of this cell in the row, starting from 0. This index is in
   * document tree order and not display order.
   */
  public final native int getCellIndex() /*-{
     return this.cellIndex;
   }-*/;

  /**
   * Horizontal alignment of data in cell.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-align-TD
   */
  public final native String getAlign() /*-{
     return this.align;
   }-*/;

  /**
   * Horizontal alignment of data in cell.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-align-TD
   */
  public final native void setAlign(String align) /*-{
     this.align = align;
   }-*/;

  /**
   * Alignment character for cells in a column.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-char
   */
  public final native String getCh() /*-{
     return this.ch;
   }-*/;

  /**
   * Alignment character for cells in a column.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-char
   */
  public final native void setCh(String ch) /*-{
     this.ch = ch;
   }-*/;

  /**
   * Offset of alignment character.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-charoff
   */
  public final native String getChOff() /*-{
     return this.chOff;
   }-*/;

  /**
   * Offset of alignment character.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-charoff
   */
  public final native void setChOff(String chOff) /*-{
     this.chOff = chOff;
   }-*/;

  /**
   * Number of columns spanned by cell.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-colspan
   */
  public final native int getColSpan() /*-{
     return this.colSpan;
   }-*/;

  /**
   * Number of columns spanned by cell.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-colspan
   */
  public final native void setColSpan(int colSpan) /*-{
     this.colSpan = colSpan;
   }-*/;

  /**
   * List of id attribute values for header cells.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-headers
   */
  public final native String getHeaders() /*-{
     return this.headers;
   }-*/;

  /**
   * List of id attribute values for header cells.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-headers
   */
  public final native void setHeaders(String headers) /*-{
     this.headers = headers;
   }-*/;

  /**
   * Number of rows spanned by cell.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-rowspan
   */
  public final native int getRowSpan() /*-{
     return this.rowSpan;
   }-*/;

  /**
   * Number of rows spanned by cell.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-rowspan
   */
  public final native void setRowSpan(int rowSpan) /*-{
     this.rowSpan = rowSpan;
   }-*/;

  /**
   * Vertical alignment of data in cell.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-valign
   */
  public final native String getVAlign() /*-{
     return this.vAlign;
   }-*/;

  /**
   * Vertical alignment of data in cell.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-valign
   */
  public final native void setVAlign(String vAlign) /*-{
     this.vAlign = vAlign;
   }-*/;
}
