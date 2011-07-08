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
package com.google.gwt.dom.builder.client;

import com.google.gwt.dom.builder.shared.InputBuilder;
import com.google.gwt.dom.client.InputElement;

/**
 * DOM-based implementation of {@link InputBuilder}.
 */
public class DomInputBuilder extends DomElementBuilderBase<InputBuilder, InputElement> implements
    InputBuilder {

  DomInputBuilder(DomBuilderImpl delegate) {
    super(delegate, true);
  }

  @Override
  public InputBuilder accept(String accept) {
    assertCanAddAttribute().setAccept(accept);
    return this;
  }

  @Override
  public InputBuilder accessKey(String accessKey) {
    assertCanAddAttribute().setAccessKey(accessKey);
    return this;
  }

  @Override
  public InputBuilder alt(String alt) {
    assertCanAddAttribute().setAlt(alt);
    return this;
  }

  @Override
  public InputBuilder checked() {
    assertCanAddAttribute().setChecked(true);
    return this;
  }

  @Override
  public InputBuilder defaultChecked() {
    assertCanAddAttribute().setDefaultChecked(true);
    return this;
  }

  @Override
  public InputBuilder defaultValue(String defaultValue) {
    assertCanAddAttribute().setDefaultValue(defaultValue);
    return this;
  }

  @Override
  public InputBuilder disabled() {
    assertCanAddAttribute().setDisabled(true);
    return this;
  }

  @Override
  public InputBuilder maxLength(int maxLength) {
    assertCanAddAttribute().setMaxLength(maxLength);
    return this;
  }

  @Override
  public InputBuilder name(String name) {
    assertCanAddAttribute().setName(name);
    return this;
  }

  @Override
  public InputBuilder readOnly() {
    assertCanAddAttribute().setReadOnly(true);
    return this;
  }

  @Override
  public InputBuilder size(int size) {
    assertCanAddAttribute().setSize(size);
    return this;
  }

  @Override
  public InputBuilder src(String src) {
    assertCanAddAttribute().setSrc(src);
    return this;
  }

  @Override
  public InputBuilder value(String value) {
    assertCanAddAttribute().setValue(value);
    return this;
  }
}
