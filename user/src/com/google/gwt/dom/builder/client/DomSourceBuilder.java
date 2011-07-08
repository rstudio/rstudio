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

import com.google.gwt.dom.builder.shared.SourceBuilder;
import com.google.gwt.dom.client.SourceElement;

/**
 * DOM-based implementation of {@link SourceBuilder}.
 */
public class DomSourceBuilder extends DomElementBuilderBase<SourceBuilder, SourceElement> implements
    SourceBuilder {

  DomSourceBuilder(DomBuilderImpl delegate) {
    super(delegate, true);
  }

  @Override
  public SourceBuilder src(String url) {
    assertCanAddAttribute().setSrc(url);
    return this;
  }

  @Override
  public SourceBuilder type(String type) {
    assertCanAddAttribute().setType(type);
    return this;
  }
}
