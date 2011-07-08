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
 * Builds an table element.
 */
public interface TableBuilder extends ElementBuilderBase<TableBuilder> {

  String UNSUPPORTED_HTML = "Table elements do not support setting inner html. Use "
      + "startTBody/startTFoot/startTHead() instead to append a table section to the table.";

  /**
   * The width of the border around the table.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-border-TABLE">W3C
   *      HTML Specification</a>
   */
  TableBuilder border(int border);

  /**
   * Specifies the horizontal and vertical space between cell content and cell
   * borders.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-cellpadding">W3C
   *      HTML Specification</a>
   */
  TableBuilder cellPadding(int cellPadding);

  /**
   * Specifies the horizontal and vertical separation between cells.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-cellspacing">W3C
   *      HTML Specification</a>
   */
  TableBuilder cellSpacing(int cellSpacing);

  /**
   * Specifies which external table borders to render.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-frame">W3C
   *      HTML Specification</a>
   */
  TableBuilder frame(String frame);

  /**
   * Specifies which internal table borders to render.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-rules">W3C
   *      HTML Specification</a>
   */
  TableBuilder rules(String rules);

  /**
   * Specifies the desired table width.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-width-TABLE">W3C
   *      HTML Specification</a>
   */
  TableBuilder width(String width);
}
