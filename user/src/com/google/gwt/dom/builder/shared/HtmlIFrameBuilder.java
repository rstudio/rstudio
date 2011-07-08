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
    return attribute("frameBorder", frameBorder);
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
    return attribute("marginHeight", marginHeight);
  }

  @Override
  public IFrameBuilder marginWidth(int marginWidth) {
    return attribute("marginWidth", marginWidth);
  }

  @Override
  public IFrameBuilder name(String name) {
    return attribute("name", name);
  }

  @Override
  public IFrameBuilder noResize() {
    return attribute("noresize", "noresize");
  }

  @Override
  public IFrameBuilder scrolling(String scrolling) {
    return attribute("scrolling", scrolling);
  }

  @Override
  public IFrameBuilder src(String src) {
    return attribute("src", src);
  }

  @Override
  public HtmlIFrameBuilder text(String text) {
    throw new UnsupportedOperationException();
  }
}
