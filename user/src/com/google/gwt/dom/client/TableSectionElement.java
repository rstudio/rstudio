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
 * The THEAD, TFOOT, and TBODY elements.
 */
@TagName({TableSectionElement.TAG_TBODY, TableSectionElement.TAG_TFOOT, 
  TableSectionElement.TAG_THEAD})
public class TableSectionElement extends Element {
  
  static final String[] TAGS = {TableSectionElement.TAG_TBODY, TableSectionElement.TAG_TFOOT, 
    TableSectionElement.TAG_THEAD};

  public static final String TAG_TBODY = "tbody";
  public static final String TAG_TFOOT = "tfoot";
  public static final String TAG_THEAD = "thead";

  /**
   * Assert that the given {@link Element} is compatible with this class and
   * automatically typecast it.
   */
  public static TableSectionElement as(Element elem) {
    assert elem.getTagName().equalsIgnoreCase(TAG_THEAD)
        || elem.getTagName().equalsIgnoreCase(TAG_TFOOT)
        || elem.getTagName().equalsIgnoreCase(TAG_TBODY);
    return (TableSectionElement) elem;
  }

  protected TableSectionElement() {
  }

  /**
   * Delete a row from this section.
   * 
   * @param index The index of the row to be deleted, or -1 to delete the last
   *          row. This index starts from 0 and is relative only to the rows
   *          contained inside this section, not all the rows in the table.
   */
  public final native void deleteRow(int index) /*-{
    this.deleteRow(index);
  }-*/;

  /**
   * Horizontal alignment of data in cells. See the align attribute for
   * HTMLTheadElement for details.
   */
  public final native String getAlign() /*-{
    return this.align;
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
   * The collection of rows in this table section.
   */
  public final native NodeList<TableRowElement> getRows() /*-{
    return this.rows;
  }-*/;

  /**
   * Vertical alignment of data in cells. See the valign attribute for
   * HTMLTheadElement for details.
   */
  public final native String getVAlign() /*-{
    return this.vAlign;
  }-*/;

  /**
   * Insert a row into this section. The new row is inserted immediately before
   * the current indexth row in this section. If index is -1 or equal to the
   * number of rows in this section, the new row is appended.
   * 
   * @param index The row number where to insert a new row. This index starts
   *          from 0 and is relative only to the rows contained inside this
   *          section, not all the rows in the table.
   * @return The newly created row.
   */
  public final native TableRowElement insertRow(int index) /*-{
    return this.insertRow(index);
  }-*/;

  /**
   * Horizontal alignment of data in cells. See the align attribute for
   * HTMLTheadElement for details.
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
   * Vertical alignment of data in cells. See the valign attribute for
   * HTMLTheadElement for details.
   */
  public final native void setVAlign(String vAlign) /*-{
    this.vAlign = vAlign;
  }-*/;
}
