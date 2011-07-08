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

import com.google.gwt.dom.builder.shared.CanvasBuilder;
import com.google.gwt.dom.client.CanvasElement;

/**
 * DOM-based implementation of {@link CanvasBuilder}.
 */
public class DomCanvasBuilder extends DomElementBuilderBase<CanvasBuilder, CanvasElement> implements
    CanvasBuilder {

  DomCanvasBuilder(DomBuilderImpl delegate) {
    super(delegate);
  }

  @Override
  public CanvasBuilder height(int height) {
    assertCanAddAttribute().setHeight(height);
    return this;
  }

  @Override
  public CanvasBuilder width(int width) {
    assertCanAddAttribute().setWidth(width);
    return this;
  }
}
