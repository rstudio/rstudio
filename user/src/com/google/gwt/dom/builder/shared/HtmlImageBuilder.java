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
 * HTML-based implementation of {@link ImageBuilder}.
 */
public class HtmlImageBuilder extends HtmlElementBuilderBase<ImageBuilder> implements ImageBuilder {

  HtmlImageBuilder(HtmlBuilderImpl delegate) {
    super(delegate, true);
  }

  @Override
  public ImageBuilder alt(String alt) {
    return trustedAttribute("alt", alt);
  }

  @Override
  public ImageBuilder height(int height) {
    return trustedAttribute("height", height);
  }

  @Override
  public ImageBuilder isMap() {
    return trustedAttribute("ismap", "ismap");
  }

  @Override
  public ImageBuilder src(String src) {
    return trustedAttribute("src", src);
  }

  @Override
  public ImageBuilder width(int width) {
    return trustedAttribute("width", width);
  }
}
