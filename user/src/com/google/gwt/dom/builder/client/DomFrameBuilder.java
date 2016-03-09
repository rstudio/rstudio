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

import com.google.gwt.dom.builder.shared.FrameBuilder;
import com.google.gwt.dom.client.FrameElement;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.annotations.IsSafeUri;
import com.google.gwt.safehtml.shared.annotations.IsTrustedResourceUri;

/**
 * DOM-based implementation of {@link FrameBuilder}.
 */
public class DomFrameBuilder extends DomElementBuilderBase<FrameBuilder, FrameElement> implements
    FrameBuilder {

  DomFrameBuilder(DomBuilderImpl delegate) {
    super(delegate, true);
  }

  @Override
  public FrameBuilder frameBorder(int frameBorder) {
    assertCanAddAttribute().setFrameBorder(frameBorder);
    return this;
  }

  @Override
  public FrameBuilder longDesc(SafeUri longDesc) {
    assertCanAddAttribute().setLongDesc(longDesc);
    return this;
  }

  @Override
  public FrameBuilder longDesc(@IsSafeUri String longDesc) {
    assertCanAddAttribute().setLongDesc(longDesc);
    return this;
  }

  @Override
  public FrameBuilder marginHeight(int marginHeight) {
    assertCanAddAttribute().setMarginHeight(marginHeight);
    return this;
  }

  @Override
  public FrameBuilder marginWidth(int marginWidth) {
    assertCanAddAttribute().setMarginWidth(marginWidth);
    return this;
  }

  @Override
  public FrameBuilder name(String name) {
    assertCanAddAttribute().setName(name);
    return this;
  }

  @Override
  public FrameBuilder noResize() {
    assertCanAddAttribute().setNoResize(true);
    return this;
  }

  @Override
  public FrameBuilder scrolling(String scrolling) {
    assertCanAddAttribute().setScrolling(scrolling);
    return this;
  }

  @Override
  public FrameBuilder src(@IsTrustedResourceUri SafeUri src) {
    assertCanAddAttribute().setSrc(src);
    return this;
  }

  @Override
  public FrameBuilder src(@IsTrustedResourceUri String src) {
    assertCanAddAttribute().setSrc(src);
    return this;
  }
}
