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
 * HTML-based implementation of {@link TableBuilder}.
 */
public class HtmlTableBuilder extends HtmlElementBuilderBase<TableBuilder> implements TableBuilder {

  HtmlTableBuilder(HtmlBuilderImpl delegate) {
    super(delegate, false);
  }

  @Override
  public TableBuilder border(int border) {
    return trustedAttribute("border", border);
  }

  @Override
  public TableBuilder cellPadding(int cellPadding) {
    return trustedAttribute("cellPadding", cellPadding);
  }

  @Override
  public TableBuilder cellSpacing(int cellSpacing) {
    return trustedAttribute("cellSpacing", cellSpacing);
  }

  @Override
  public TableBuilder frame(String frame) {
    return trustedAttribute("frame", frame);
  }

  @Override
  public TableBuilder html(SafeHtml html) {
    throw new UnsupportedOperationException(UNSUPPORTED_HTML);
  }

  @Override
  public TableBuilder rules(String rules) {
    return trustedAttribute("rules", rules);
  }

  @Override
  public TableBuilder text(String text) {
    throw new UnsupportedOperationException(UNSUPPORTED_HTML);
  }

  @Override
  public TableBuilder width(String width) {
    return trustedAttribute("width", width);
  }
}
