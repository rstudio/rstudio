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

import com.google.gwt.dom.builder.shared.MetaBuilder;
import com.google.gwt.dom.client.MetaElement;

/**
 * DOM-based implementation of {@link MetaBuilder}.
 */
public class DomMetaBuilder extends DomElementBuilderBase<MetaBuilder, MetaElement> implements
    MetaBuilder {

  DomMetaBuilder(DomBuilderImpl delegate) {
    super(delegate, true);
  }

  @Override
  public MetaBuilder content(String content) {
    assertCanAddAttribute().setContent(content);
    return this;
  }

  @Override
  public MetaBuilder httpEquiv(String httpEquiv) {
    assertCanAddAttribute().setHttpEquiv(httpEquiv);
    return this;
  }

  @Override
  public MetaBuilder name(String name) {
    assertCanAddAttribute().setName(name);
    return this;
  }
}
