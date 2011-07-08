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

import com.google.gwt.dom.builder.shared.AnchorBuilder;
import com.google.gwt.dom.client.AnchorElement;

/**
 * DOM-based implementation of {@link AnchorBuilder}.
 */
public class DomAnchorBuilder extends DomElementBuilderBase<AnchorBuilder, AnchorElement> implements
    AnchorBuilder {

  DomAnchorBuilder(DomBuilderImpl delegate) {
    super(delegate);
  }

  @Override
  public AnchorBuilder accessKey(String accessKey) {
    assertCanAddAttribute().setAccessKey(accessKey);
    return this;
  }

  @Override
  public AnchorBuilder href(String href) {
    assertCanAddAttribute().setHref(href);
    return this;
  }

  @Override
  public AnchorBuilder hreflang(String hreflang) {
    assertCanAddAttribute().setHreflang(hreflang);
    return this;
  }

  @Override
  public AnchorBuilder name(String name) {
    assertCanAddAttribute().setName(name);
    return this;
  }

  @Override
  public AnchorBuilder rel(String rel) {
    assertCanAddAttribute().setRel(rel);
    return this;
  }

  @Override
  public AnchorBuilder target(String target) {
    assertCanAddAttribute().setTarget(target);
    return this;
  }

  @Override
  public AnchorBuilder type(String type) {
    assertCanAddAttribute().setType(type);
    return this;
  }
}
