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
 * HTML-based implementation of {@link OptionBuilder}.
 */
public class HtmlOptionBuilder extends HtmlElementBuilderBase<OptionBuilder>
    implements OptionBuilder {

  HtmlOptionBuilder(HtmlBuilderImpl delegate) {
    super(delegate);
  }

  @Override
  public OptionBuilder defaultSelected() {
    return trustedAttribute("defaultSelected", "defaultSelected");
  }

  @Override
  public OptionBuilder disabled() {
    return trustedAttribute("disabled", "disabled");
  }

  @Override
  public OptionBuilder label(String label) {
    return trustedAttribute("label", label);
  }

  @Override
  public OptionBuilder selected() {
    return trustedAttribute("selected", "selected");
  }

  @Override
  public OptionBuilder value(String value) {
    return trustedAttribute("value", value);
  }
}
