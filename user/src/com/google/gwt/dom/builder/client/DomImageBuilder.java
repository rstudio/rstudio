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

import com.google.gwt.dom.builder.shared.ImageBuilder;
import com.google.gwt.dom.client.ImageElement;

/**
 * DOM-based implementation of {@link ImageBuilder}.
 */
public class DomImageBuilder extends DomElementBuilderBase<ImageBuilder, ImageElement> implements
    ImageBuilder {

  DomImageBuilder(DomBuilderImpl delegate) {
    super(delegate, true);
  }

  @Override
  public ImageBuilder alt(String alt) {
    assertCanAddAttribute().setAlt(alt);
    return this;
  }

  @Override
  public ImageBuilder height(int height) {
    assertCanAddAttribute().setHeight(height);
    return this;
  }

  @Override
  public ImageBuilder isMap() {
    assertCanAddAttribute().setIsMap(true);
    return this;
  }

  @Override
  public ImageBuilder src(String src) {
    assertCanAddAttribute().setSrc(src);
    return this;
  }

  @Override
  public ImageBuilder width(int width) {
    assertCanAddAttribute().setWidth(width);
    return this;
  }
}
