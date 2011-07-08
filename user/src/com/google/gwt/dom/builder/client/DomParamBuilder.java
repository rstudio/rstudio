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

import com.google.gwt.dom.builder.shared.ParamBuilder;
import com.google.gwt.dom.client.ParamElement;

/**
 * DOM-based implementation of {@link ParamBuilder}.
 */
public class DomParamBuilder extends DomElementBuilderBase<ParamBuilder, ParamElement> implements ParamBuilder {

  DomParamBuilder(DomBuilderImpl delegate) {
    super(delegate, true);
  }

  @Override
  public ParamBuilder name(String name) {
    assertCanAddAttribute().setName(name);
    return this;
  }

  @Override
  public ParamBuilder value(String value) {
    assertCanAddAttribute().setValue(value);
    return this;
  }
}
