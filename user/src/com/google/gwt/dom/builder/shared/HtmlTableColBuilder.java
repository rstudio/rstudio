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
 * HTML-based implementation of {@link TableColBuilder}.
 */
public class HtmlTableColBuilder extends HtmlElementBuilderBase<TableColBuilder> implements
    TableColBuilder {

  HtmlTableColBuilder(HtmlBuilderImpl delegate, boolean group) {
    super(delegate, !group);
  }

  @Override
  public TableColBuilder align(String align) {
    return trustedAttribute("align", align);
  }

  @Override
  public TableColBuilder ch(String ch) {
    return trustedAttribute("ch", ch);
  }

  @Override
  public TableColBuilder chOff(String chOff) {
    return trustedAttribute("chOff", chOff);
  }

  @Override
  public TableColBuilder span(int span) {
    return trustedAttribute("span", span);
  }

  @Override
  public TableColBuilder vAlign(String vAlign) {
    return trustedAttribute("vAlign", vAlign);
  }

  @Override
  public TableColBuilder width(String width) {
    return trustedAttribute("width", width);
  }
}
