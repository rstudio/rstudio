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

import com.google.gwt.safehtml.shared.SafeUri;

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
    return trustedAttribute("accessKey", accessKey);
  }

  @Override
  public AnchorBuilder href(SafeUri href) {
    return href(href.asString());
  }

  @Override
  public AnchorBuilder href(String href) {
    return trustedAttribute("href", href);
  }

  @Override
  public AnchorBuilder hreflang(String hreflang) {
    return trustedAttribute("hreflang", hreflang);
  }

  @Override
  public AnchorBuilder name(String name) {
    return trustedAttribute("name", name);
  }

  @Override
  public AnchorBuilder rel(String rel) {
    return trustedAttribute("rel", rel);
  }

  @Override
  public AnchorBuilder target(String target) {
    return trustedAttribute("target", target);
  }

  @Override
  public AnchorBuilder type(String type) {
    return trustedAttribute("type", type);
  }
}
