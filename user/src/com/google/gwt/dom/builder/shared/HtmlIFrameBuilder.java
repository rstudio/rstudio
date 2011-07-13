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

import com.google.gwt.safehtml.shared.SafeHtml;

/**
 * HTML-based implementation of {@link IFrameBuilder}.
 */
public class HtmlIFrameBuilder extends HtmlElementBuilderBase<IFrameBuilder> implements
    IFrameBuilder {

  HtmlIFrameBuilder(HtmlBuilderImpl delegate) {
    super(delegate);
  }

  @Override
  public IFrameBuilder frameBorder(int frameBorder) {
    return trustedAttribute("frameBorder", frameBorder);
  }

  @Override
  public HtmlIFrameBuilder html(SafeHtml html) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isChildElementSupported() {
    return false;
  }

  @Override
  public IFrameBuilder marginHeight(int marginHeight) {
    return trustedAttribute("marginHeight", marginHeight);
  }

  @Override
  public IFrameBuilder marginWidth(int marginWidth) {
    return trustedAttribute("marginWidth", marginWidth);
  }

  @Override
  public IFrameBuilder name(String name) {
    return trustedAttribute("name", name);
  }

  @Override
  public IFrameBuilder noResize() {
    return trustedAttribute("noresize", "noresize");
  }

  @Override
  public IFrameBuilder scrolling(String scrolling) {
    return trustedAttribute("scrolling", scrolling);
  }

  @Override
  public IFrameBuilder src(String src) {
    return trustedAttribute("src", src);
  }

  @Override
  public HtmlIFrameBuilder text(String text) {
    throw new UnsupportedOperationException();
  }
}
