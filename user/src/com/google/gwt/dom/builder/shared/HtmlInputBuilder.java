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
 * HTML-based implementation of {@link InputBuilder}.
 */
public class HtmlInputBuilder extends HtmlElementBuilderBase<InputBuilder> implements InputBuilder {

  HtmlInputBuilder(HtmlBuilderImpl delegate) {
    super(delegate, true);
  }

  @Override
  public InputBuilder accept(String accept) {
    return trustedAttribute("accept", accept);
  }

  @Override
  public InputBuilder accessKey(String accessKey) {
    return trustedAttribute("accessKey", accessKey);
  }

  @Override
  public InputBuilder alt(String alt) {
    return trustedAttribute("alt", alt);
  }

  @Override
  public InputBuilder checked() {
    return trustedAttribute("checked", "checked");
  }

  @Override
  public InputBuilder defaultChecked() {
    return trustedAttribute("defaultChecked", "defaultChecked");
  }

  @Override
  public InputBuilder defaultValue(String defaultValue) {
    return trustedAttribute("defaultValue", defaultValue);
  }

  @Override
  public InputBuilder disabled() {
    return trustedAttribute("disabled", "disabled");
  }

  @Override
  public InputBuilder maxLength(int maxLength) {
    return trustedAttribute("maxlength", maxLength);
  }

  @Override
  public InputBuilder name(String name) {
    return trustedAttribute("name", name);
  }

  @Override
  public InputBuilder readOnly() {
    return trustedAttribute("readonly", "readonly");
  }

  @Override
  public InputBuilder size(int size) {
    return trustedAttribute("size", size);
  }

  @Override
  public InputBuilder src(String src) {
    return trustedAttribute("src", src);
  }

  @Override
  public InputBuilder value(String value) {
    return trustedAttribute("value", value);
  }
}
