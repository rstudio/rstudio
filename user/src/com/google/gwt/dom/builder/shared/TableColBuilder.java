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
 * Builds an tablecol element.
 */
public interface TableColBuilder extends ElementBuilderBase<TableColBuilder> {

  /**
   * Horizontal alignment of cell data in column.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-align-TD">W3C
   *      HTML Specification</a>
   */
  TableColBuilder align(String align);

  /**
   * Alignment character for cells in a column.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-char">W3C
   *      HTML Specification</a>
   */
  TableColBuilder ch(String ch);

  /**
   * Offset of alignment character.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-charoff">W3C
   *      HTML Specification</a>
   */
  TableColBuilder chOff(String chOff);

  /**
   * Indicates the number of columns in a group or affected by a grouping.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-span-COL">W3C
   *      HTML Specification</a>
   */
  TableColBuilder span(int span);

  /**
   * Vertical alignment of cell data in column.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-valign">W3C
   *      HTML Specification</a>
   */
  TableColBuilder vAlign(String vAlign);

  /**
   * Default column width.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-width-COL">W3C
   *      HTML Specification</a>
   */
  TableColBuilder width(String width);
}
