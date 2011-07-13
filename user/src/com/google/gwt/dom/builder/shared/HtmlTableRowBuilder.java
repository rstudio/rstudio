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

import com.google.gwt.safehtml.shared.SafeHtml;

/**
 * HTML-based implementation of {@link TableRowBuilder}.
 */
public class HtmlTableRowBuilder extends HtmlElementBuilderBase<TableRowBuilder> implements
    TableRowBuilder {

  HtmlTableRowBuilder(HtmlBuilderImpl delegate) {
    super(delegate);
  }

  @Override
  public TableRowBuilder align(String align) {
    return trustedAttribute("align", align);
  }

  @Override
  public TableRowBuilder ch(String ch) {
    return trustedAttribute("ch", ch);
  }

  @Override
  public TableRowBuilder chOff(String chOff) {
    return trustedAttribute("chOff", chOff);
  }

  @Override
  public TableRowBuilder html(SafeHtml html) {
    throw new UnsupportedOperationException(UNSUPPORTED_HTML);
  }

  @Override
  public TableRowBuilder text(String text) {
    throw new UnsupportedOperationException(UNSUPPORTED_HTML);
  }

  @Override
  public TableRowBuilder vAlign(String vAlign) {
    return trustedAttribute("vAlign", vAlign);
  }
}
