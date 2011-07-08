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
 * HTML-based implementation of {@link TextAreaBuilder}.
 */
public class HtmlTextAreaBuilder extends HtmlElementBuilderBase<TextAreaBuilder> implements
    TextAreaBuilder {

  HtmlTextAreaBuilder(HtmlBuilderImpl delegate) {
    super(delegate);
  }

  @Override
  public TextAreaBuilder accessKey(String accessKey) {
    return attribute("accessKey", accessKey);
  }

  @Override
  public TextAreaBuilder cols(int cols) {
    return attribute("cols", cols);
  }

  @Override
  public TextAreaBuilder defaultValue(String defaultValue) {
    return attribute("defaultValue", defaultValue);
  }

  @Override
  public TextAreaBuilder disabled() {
    return attribute("disabled", "disabled");
  }

  @Override
  public TextAreaBuilder html(SafeHtml html) {
    throw new UnsupportedOperationException(UNSUPPORTED_HTML);
  }

  @Override
  public boolean isChildElementSupported() {
    return false;
  }

  @Override
  public TextAreaBuilder name(String name) {
    return attribute("name", name);
  }

  @Override
  public TextAreaBuilder readOnly() {
    return attribute("readonly", "readonly");
  }

  @Override
  public TextAreaBuilder rows(int rows) {
    return attribute("rows", rows);
  }

  @Override
  public TextAreaBuilder value(String value) {
    return attribute("value", value);
  }
}
