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
 * HTML-based implementation of {@link AreaBuilder}.
 */
public class HtmlAreaBuilder extends HtmlElementBuilderBase<AreaBuilder> implements AreaBuilder {

  HtmlAreaBuilder(HtmlBuilderImpl delegate) {
    super(delegate, true);
  }

  @Override
  public AreaBuilder accessKey(String accessKey) {
    return attribute("accessKey", accessKey);
  }

  @Override
  public AreaBuilder alt(String alt) {
    return attribute("alt", alt);
  }

  @Override
  public AreaBuilder coords(String coords) {
    return attribute("coords", coords);
  }

  @Override
  public AreaBuilder href(String href) {
    return attribute("href", href);
  }

  @Override
  public AreaBuilder shape(String shape) {
    return attribute("shape", shape);
  }

  @Override
  public AreaBuilder target(String target) {
    return attribute("target", target);
  }
}
