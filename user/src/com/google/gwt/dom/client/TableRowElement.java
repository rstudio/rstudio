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
 * A row in a table.
 * 
 * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#edef-TR">W3C HTML Specification</a>
 */
@TagName(TableRowElement.TAG)
public class TableRowElement extends Element {

  public static final String TAG = "tr";

  /**
   * Assert that the given {@link Element} is compatible with this class and
   * automatically typecast it.
   */
  public static TableRowElement as(Element elem) {
    assert elem.getTagName().equalsIgnoreCase(TAG);
    return (TableRowElement) elem;
  }

  protected TableRowElement() {
  }

  /**
   * Delete a cell from the current row.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-align-TD">W3C HTML Specification</a>
   */
  public final native void deleteCell(int index) /*-{
    this.deleteCell(index);
  }-*/;

  /**
   * Horizontal alignment of data within cells of this row.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-align-TD">W3C HTML Specification</a>
   */
  public final native String getAlign() /*-{
    return this.align;
  }-*/;

  /**
   * The collection of cells in this row.
   */
  public final native NodeList<TableCellElement> getCells() /*-{
    return this.cells;
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
   * This is in logical order and not in document order. The rowIndex does take
   * into account sections (THEAD, TFOOT, or TBODY) within the table, placing
   * THEAD rows first in the index, followed by TBODY rows, followed by TFOOT
   * rows.
   */
  public final native int getRowIndex() /*-{
    return this.rowIndex;
  }-*/;

  /**
   * The index of this row, relative to the current section (THEAD, TFOOT, or
   * TBODY), starting from 0.
   */
  public final native int getSectionRowIndex() /*-{
    return this.sectionRowIndex;
  }-*/;

  /**
   * Vertical alignment of data within cells of this row.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-valign">W3C HTML Specification</a>
   */
  public final native String getVAlign() /*-{
    return this.vAlign;
  }-*/;

  /**
   * Insert an empty TD cell into this row. If index is -1 or equal to the
   * number of cells, the new cell is appended.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-align-TD">W3C HTML Specification</a>
   */
  public final native TableCellElement insertCell(int index) /*-{
    return this.insertCell(index);
  }-*/;

  /**
   * Horizontal alignment of data within cells of this row.
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
   * Vertical alignment of data within cells of this row.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-valign">W3C HTML Specification</a>
   */
  public final native void setVAlign(String vAlign) /*-{
    this.vAlign = vAlign;
  }-*/;
}
