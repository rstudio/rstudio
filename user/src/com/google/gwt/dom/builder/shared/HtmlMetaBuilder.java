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
 * HTML-based implementation of {@link MetaBuilder}.
 */
public class HtmlMetaBuilder extends HtmlElementBuilderBase<MetaBuilder> implements MetaBuilder {

  HtmlMetaBuilder(HtmlBuilderImpl delegate) {
    super(delegate, true);
  }

  @Override
  public MetaBuilder content(String content) {
    return trustedAttribute("content", content);
  }

  @Override
  public MetaBuilder httpEquiv(String httpEquiv) {
    return trustedAttribute("httpEquiv", httpEquiv);
  }

  @Override
  public MetaBuilder name(String name) {
    return trustedAttribute("name", name);
  }
}
