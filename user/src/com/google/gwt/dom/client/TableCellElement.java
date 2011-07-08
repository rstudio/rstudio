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
 * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#edef-TD">W3C HTML Specification</a>
 */
@TagName({TableCellElement.TAG_TD, TableCellElement.TAG_TH})
public class TableCellElement extends Element {

  public static final String TAG_TD = "td";
  public static final String TAG_TH = "th";

  /**
   * Assert that the given {@link Element} is compatible with this class and
   * automatically typecast it.
   */
  public static TableCellElement as(Element elem) {
    assert elem.getTagName().equalsIgnoreCase(TAG_TD)
      || elem.getTagName().equalsIgnoreCase(TAG_TH);  
    return (TableCellElement) elem;
  }

  protected TableCellElement() {
  }

  /**
   * Horizontal alignment of data in cell.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-align-TD">W3C HTML Specification</a>
   */
  public final native String getAlign() /*-{
     return this.align;
   }-*/;

  /**
   * The index of this cell in the row, starting from 0. This index is in
   * document tree order and not display order.
   * 
   * Note: This method always returns 0 on Safari 2 (bug 3295).
   */
  public final native int getCellIndex() /*-{
     return this.cellIndex;
   }-*/;

  /**
   * Alignment character for cells in a column.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-char">W3C HTML Specification</a>
   */
  public final native String getCh() /*-{
     return this.ch;
   }-*/;

  /**
   * Offset of alignment character.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-charoff">W3C HTML Specification</a>
   */
  public final native String getChOff() /*-{
     return this.chOff;
   }-*/;

  /**
   * Number of columns spanned by cell.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-colspan">W3C HTML Specification</a>
   */
  public final native int getColSpan() /*-{
     return this.colSpan;
   }-*/;

  /**
   * List of id attribute values for header cells.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-headers">W3C HTML Specification</a>
   */
  public final native String getHeaders() /*-{
     return this.headers;
   }-*/;

  /**
   * Number of rows spanned by cell.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-rowspan">W3C HTML Specification</a>
   */
  public final native int getRowSpan() /*-{
     return this.rowSpan;
   }-*/;

  /**
   * Vertical alignment of data in cell.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-valign">W3C HTML Specification</a>
   */
  public final native String getVAlign() /*-{
     return this.vAlign;
   }-*/;

  /**
   * Horizontal alignment of data in cell.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-align-TD">W3C HTML Specification</a>
   */
  public final native void setAlign(String align) /*-{
     this.align = align;
   }-*/;

  /**
   * Alignment character for cells in a column.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-char">W3C HTML Specification</a>
   */
  public final native void setCh(String ch) /*-{
     this.ch = ch;
   }-*/;

  /**
   * Offset of alignment character.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-charoff">W3C HTML Specification</a>
   */
  public final native void setChOff(String chOff) /*-{
     this.chOff = chOff;
   }-*/;

  /**
   * Number of columns spanned by cell.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-colspan">W3C HTML Specification</a>
   */
  public final native void setColSpan(int colSpan) /*-{
     this.colSpan = colSpan;
   }-*/;

  /**
   * List of id attribute values for header cells.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-headers">W3C HTML Specification</a>
   */
  public final native void setHeaders(String headers) /*-{
     this.headers = headers;
   }-*/;

  /**
   * Number of rows spanned by cell.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-rowspan">W3C HTML Specification</a>
   */
  public final native void setRowSpan(int rowSpan) /*-{
     this.rowSpan = rowSpan;
   }-*/;

  /**
   * Vertical alignment of data in cell.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-valign">W3C HTML Specification</a>
   */
  public final native void setVAlign(String vAlign) /*-{
     this.vAlign = vAlign;
   }-*/;
}
