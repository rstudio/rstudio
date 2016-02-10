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
import com.google.gwt.safehtml.shared.annotations.IsTrustedResourceUri;
import com.google.gwt.safehtml.shared.annotations.SuppressIsTrustedResourceUriCastCheck;

/**
 * HTML-based implementation of {@link FrameBuilder}.
 */
public class HtmlFrameBuilder extends HtmlElementBuilderBase<FrameBuilder> implements FrameBuilder {

  HtmlFrameBuilder(HtmlBuilderImpl delegate) {
    super(delegate, true);
  }

  @Override
  public FrameBuilder frameBorder(int frameBorder) {
    return trustedAttribute("frameBorder", frameBorder);
  }

  @Override
  public FrameBuilder longDesc(SafeUri longDesc) {
    return longDesc(longDesc.asString());
  }

  @Override
  public FrameBuilder longDesc(String longDesc) {
    return trustedAttribute("longDesc", longDesc);
  }

  @Override
  public FrameBuilder marginHeight(int marginHeight) {
    return trustedAttribute("marginHeight", marginHeight);
  }

  @Override
  public FrameBuilder marginWidth(int marginWidth) {
    return trustedAttribute("marginWidth", marginWidth);
  }

  @Override
  public FrameBuilder name(String name) {
    return trustedAttribute("name", name);
  }

  @Override
  public FrameBuilder noResize() {
    return trustedAttribute("noresize", "noresize");
  }

  @Override
  public FrameBuilder scrolling(String scrolling) {
    return trustedAttribute("scrolling", scrolling);
  }

  @Override
  @SuppressIsTrustedResourceUriCastCheck
  public FrameBuilder src(@IsTrustedResourceUri SafeUri src) {
    return src(src.asString());
  }

  @Override
  public FrameBuilder src(@IsTrustedResourceUri String src) {
    return trustedAttribute("src", src);
  }
}
