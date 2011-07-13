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
 * HTML-based implementation of {@link ButtonBuilder}.
 */
public class HtmlButtonBuilder extends HtmlElementBuilderBase<ButtonBuilder> implements
    ButtonBuilder {

  HtmlButtonBuilder(HtmlBuilderImpl delegate) {
    super(delegate);
  }

  @Override
  public ButtonBuilder accessKey(String accessKey) {
    return trustedAttribute("accessKey", accessKey);
  }

  @Override
  public ButtonBuilder disabled() {
    return trustedAttribute("disabled", "disabled");
  }

  @Override
  public ButtonBuilder name(String name) {
    return trustedAttribute("name", name);
  }

  @Override
  public ButtonBuilder value(String value) {
    return trustedAttribute("value", value);
  }
}
