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

import com.google.gwt.dom.builder.shared.FormBuilder;
import com.google.gwt.dom.client.FormElement;

/**
 * DOM-based implementation of {@link FormBuilder}.
 */
public class DomFormBuilder extends DomElementBuilderBase<FormBuilder, FormElement> implements
    FormBuilder {

  DomFormBuilder(DomBuilderImpl delegate) {
    super(delegate);
  }

  @Override
  public FormBuilder acceptCharset(String acceptCharset) {
    assertCanAddAttribute().setAcceptCharset(acceptCharset);
    return this;
  }

  @Override
  public FormBuilder action(String action) {
    assertCanAddAttribute().setAction(action);
    return this;
  }

  @Override
  public FormBuilder enctype(String enctype) {
    assertCanAddAttribute().setEnctype(enctype);
    return this;
  }

  @Override
  public FormBuilder method(String method) {
    assertCanAddAttribute().setMethod(method);
    return this;
  }

  @Override
  public FormBuilder name(String name) {
    assertCanAddAttribute().setName(name);
    return this;
  }

  @Override
  public FormBuilder target(String target) {
    assertCanAddAttribute().setTarget(target);
    return this;
  }
}
