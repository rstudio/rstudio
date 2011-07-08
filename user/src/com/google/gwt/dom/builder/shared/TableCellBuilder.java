/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.dom.builder.shared;

/**
 * Builds an tablecell element.
 */
public interface TableCellBuilder extends ElementBuilderBase<TableCellBuilder> {

  /**
   * Horizontal alignment of data in cell.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-align-TD">W3C
   *      HTML Specification</a>
   */
  TableCellBuilder align(String align);

  /**
   * Alignment character for cells in a column.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-char">W3C
   *      HTML Specification</a>
   */
  TableCellBuilder ch(String ch);

  /**
   * Offset of alignment character.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-charoff">W3C
   *      HTML Specification</a>
   */
  TableCellBuilder chOff(String chOff);

  /**
   * Number of columns spanned by cell.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-colspan">W3C
   *      HTML Specification</a>
   */
  TableCellBuilder colSpan(int colSpan);

  /**
   * List of id attribute values for header cells.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-headers">W3C
   *      HTML Specification</a>
   */
  TableCellBuilder headers(String headers);

  /**
   * Number of rows spanned by cell.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-rowspan">W3C
   *      HTML Specification</a>
   */
  TableCellBuilder rowSpan(int rowSpan);

  /**
   * Vertical alignment of data in cell.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-valign">W3C
   *      HTML Specification</a>
   */
  TableCellBuilder vAlign(String vAlign);
}
