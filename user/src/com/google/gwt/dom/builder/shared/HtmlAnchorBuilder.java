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
 * HTML-based implementation of {@link AnchorBuilder}.
 */
public class HtmlAnchorBuilder extends HtmlElementBuilderBase<AnchorBuilder> implements
    AnchorBuilder {

  HtmlAnchorBuilder(HtmlBuilderImpl delegate) {
    super(delegate);
  }

  @Override
  public AnchorBuilder accessKey(String accessKey) {
    return attribute("accessKey", accessKey);
  }

  @Override
  public AnchorBuilder href(String href) {
    return attribute("href", href);
  }

  @Override
  public AnchorBuilder hreflang(String hreflang) {
    return attribute("hreflang", hreflang);
  }

  @Override
  public AnchorBuilder name(String name) {
    return attribute("name", name);
  }

  @Override
  public AnchorBuilder rel(String rel) {
    return attribute("rel", rel);
  }

  @Override
  public AnchorBuilder target(String target) {
    return attribute("target", target);
  }

  @Override
  public AnchorBuilder type(String type) {
    return attribute("type", type);
  }
}
