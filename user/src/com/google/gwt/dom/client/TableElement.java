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
 * The create* and delete* methods on the table allow authors to construct and
 * modify tables. [HTML 4.01] specifies that only one of each of the CAPTION,
 * THEAD, and TFOOT elements may exist in a table. Therefore, if one exists, and
 * the createTHead() or createTFoot() method is called, the method returns the
 * existing THead or TFoot element.
 * 
 * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#edef-TABLE">W3C HTML Specification</a>
 */
@TagName(TableElement.TAG)
public class TableElement extends Element {

  public static final String TAG = "table";

  /**
   * Assert that the given {@link Element} is compatible with this class and
   * automatically typecast it.
   */
  public static TableElement as(Element elem) {
    assert elem.getTagName().equalsIgnoreCase(TAG);
    return (TableElement) elem;
  }

  protected TableElement() {
  }

  /**
   * Create a new table caption object or return an existing one.
   * 
   * @return A CAPTION element.
   */
  public final native TableCaptionElement createCaption() /*-{
    return this.createCaption();
  }-*/;

  /**
   * Create a table footer row or return an existing one.
   * 
   * @return A footer element (TFOOT)
   */
  public final native TableSectionElement createTFoot() /*-{
    return this.createTFoot();
  }-*/;

  /**
   * Create a table header row or return an existing one.
   * 
   * @return A new table header element (THEAD)
   */
  public final native TableSectionElement createTHead() /*-{
    return this.createTHead();
  }-*/;

  /**
   * Delete the table caption, if one exists.
   */
  public final native void deleteCaption() /*-{
    this.deleteCaption();
  }-*/;

  /**
   * Delete a table row.
   * 
   * @param index The index of the row to be deleted. This index starts from 0
   *          and is relative to the logical order (not document order) of all
   *          the rows contained inside the table. If the index is -1 the last
   *          row in the table is deleted
   */
  public final native void deleteRow(int index) /*-{
    this.deleteRow(index);
  }-*/;

  /**
   * Delete the header from the table, if one exists.
   */
  public final native void deleteTFoot() /*-{
    this.deleteTFoot();
  }-*/;

  /**
   * Delete the header from the table, if one exists.
   */
  public final native void deleteTHead() /*-{
    this.deleteTHead();
  }-*/;

  /**
   * The width of the border around the table.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-border-TABLE">W3C HTML Specification</a>
   */
  public final native int getBorder() /*-{
    return this.border;
  }-*/;

  /**
   * The table's CAPTION, or null if none exists.
   */
  public final native TableCaptionElement getCaption() /*-{
     return this.caption;
   }-*/;

  /**
   * Specifies the horizontal and vertical space between cell content and cell
   * borders.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-cellpadding">W3C HTML Specification</a>
   */
  public final native int getCellPadding() /*-{
    return this.cellPadding;
  }-*/;

  /**
   * Specifies the horizontal and vertical separation between cells.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-cellspacing">W3C HTML Specification</a>
   */
  public final native int getCellSpacing() /*-{
    return this.cellSpacing;
  }-*/;

  /**
   * Specifies which external table borders to render.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-frame">W3C HTML Specification</a>
   */
  public final native String getFrame() /*-{
    return this.frame;
  }-*/;

  /**
   * Returns a collection of all the rows in the table, including all in THEAD,
   * TFOOT, all TBODY elements.
   */
  public final native NodeList<TableRowElement> getRows() /*-{
    return this.rows;
  }-*/;

  /**
   * Specifies which internal table borders to render.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-rules">W3C HTML Specification</a>
   */
  public final native String getRules() /*-{
    return this.rules;
  }-*/;

  /**
   * Returns a collection of the table bodies (including implicit ones).
   */
  public final native NodeList<TableSectionElement> getTBodies() /*-{
    return this.tBodies;
  }-*/;

  /**
   * The table's TFOOT, or null if none exists.
   */
  public final native TableSectionElement getTFoot() /*-{
     return this.tFoot;
   }-*/;

  /**
   * The table's THEAD, or null if none exists.
   */
  public final native TableSectionElement getTHead() /*-{
     return this.tHead;
   }-*/;

  /**
   * Specifies the desired table width.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-width-TABLE">W3C HTML Specification</a>
   */
  public final native String getWidth() /*-{
     return this.width;
   }-*/;

  /**
   * Insert a new empty row in the table. The new row is inserted immediately
   * before and in the same section as the current indexth row in the table. If
   * index is -1 or equal to the number of rows, the new row is appended. In
   * addition, when the table is empty the row is inserted into a TBODY which is
   * created and inserted into the table.
   * 
   * Note: A table row cannot be empty according to [HTML 4.01].
   * 
   * @param index The row number where to insert a new row. This index starts
   *          from 0 and is relative to the logical order (not document order)
   *          of all the rows contained inside the table
   * @return The newly created row
   */
  public final native TableRowElement insertRow(int index) /*-{
    return this.insertRow(index);
  }-*/;

  /**
   * The width of the border around the table.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-border-TABLE">W3C HTML Specification</a>
   */
  public final native void setBorder(int border) /*-{
    this.border = border;
  }-*/;

  /**
   * The table's CAPTION, or null if none exists.
   */
  public final native void setCaption(TableCaptionElement caption) /*-{
     this.caption = caption;
   }-*/;

  /**
   * Specifies the horizontal and vertical space between cell content and cell
   * borders.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-cellpadding">W3C HTML Specification</a>
   */
  public final native void setCellPadding(int cellPadding) /*-{
    this.cellPadding = cellPadding;
  }-*/;

  /**
   * Specifies the horizontal and vertical separation between cells.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-cellspacing">W3C HTML Specification</a>
   */
  public final native void setCellSpacing(int cellSpacing) /*-{
    this.cellSpacing = cellSpacing;
  }-*/;

  /**
   * Specifies which external table borders to render.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-frame">W3C HTML Specification</a>
   */
  public final native void setFrame(String frame) /*-{
    this.frame = frame;
  }-*/;

  /**
   * Specifies which internal table borders to render.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-rules">W3C HTML Specification</a>
   */
  public final native void setRules(String rules) /*-{
    this.rules = rules;
  }-*/;

  /**
   * The table's TFOOT, or null if none exists.
   */
  public final native void setTFoot(TableSectionElement tFoot) /*-{
     this.tFoot = tFoot;
   }-*/;

  /**
   * The table's THEAD, or null if none exists.
   */
  public final native void setTHead(TableSectionElement tHead) /*-{
     this.tHead = tHead;
   }-*/;

  /**
   * Specifies the desired table width.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-width-TABLE">W3C HTML Specification</a>
   */
  public final native void setWidth(String width) /*-{
     this.width = width;
   }-*/;
}
