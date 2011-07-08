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
    return attribute("accept", accept);
  }

  @Override
  public InputBuilder accessKey(String accessKey) {
    return attribute("accessKey", accessKey);
  }

  @Override
  public InputBuilder alt(String alt) {
    return attribute("alt", alt);
  }

  @Override
  public InputBuilder checked() {
    return attribute("checked", "checked");
  }

  @Override
  public InputBuilder defaultChecked() {
    return attribute("defaultChecked", "defaultChecked");
  }

  @Override
  public InputBuilder defaultValue(String defaultValue) {
    return attribute("defaultValue", defaultValue);
  }

  @Override
  public InputBuilder disabled() {
    return attribute("disabled", "disabled");
  }

  @Override
  public InputBuilder maxLength(int maxLength) {
    return attribute("maxlength", maxLength);
  }

  @Override
  public InputBuilder name(String name) {
    return attribute("name", name);
  }

  @Override
  public InputBuilder readOnly() {
    return attribute("readonly", "readonly");
  }

  @Override
  public InputBuilder size(int size) {
    return attribute("size", size);
  }

  @Override
  public InputBuilder src(String src) {
    return attribute("src", src);
  }

  @Override
  public InputBuilder value(String value) {
    return attribute("value", value);
  }
}
