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
 * HTML-based implementation of {@link TableCellBuilder}.
 */
public class HtmlTableCellBuilder extends HtmlElementBuilderBase<TableCellBuilder> implements
    TableCellBuilder {

  HtmlTableCellBuilder(HtmlBuilderImpl delegate) {
    super(delegate);
  }

  @Override
  public TableCellBuilder align(String align) {
    return trustedAttribute("align", align);
  }

  @Override
  public TableCellBuilder ch(String ch) {
    return trustedAttribute("ch", ch);
  }

  @Override
  public TableCellBuilder chOff(String chOff) {
    return trustedAttribute("chOff", chOff);
  }

  @Override
  public TableCellBuilder colSpan(int colSpan) {
    return trustedAttribute("colSpan", colSpan);
  }

  @Override
  public TableCellBuilder headers(String headers) {
    return trustedAttribute("headers", headers);
  }

  @Override
  public TableCellBuilder rowSpan(int rowSpan) {
    return trustedAttribute("rowSpan", rowSpan);
  }

  @Override
  public TableCellBuilder vAlign(String vAlign) {
    return trustedAttribute("vAlign", vAlign);
  }
}
