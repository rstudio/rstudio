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

import com.google.gwt.dom.builder.shared.StyleBuilder;
import com.google.gwt.dom.client.StyleElement;
import com.google.gwt.safehtml.shared.SafeHtml;

/**
 * DOM-based implementation of {@link StyleBuilder}.
 */
public class DomStyleBuilder extends DomElementBuilderBase<StyleBuilder, StyleElement> implements
    StyleBuilder {

  DomStyleBuilder(DomBuilderImpl delegate) {
    super(delegate);
  }

  @Override
  public StyleBuilder cssText(String cssText) {
    assertCanAddAttribute().setCssText(cssText);
    /*
     * The HTML version appends text inline, so we prevent additional attributes
     * after setting the text.
     */
    getDelegate().lockCurrentElement();
    return this;
  }

  @Override
  public StyleBuilder disabled() {
    assertCanAddAttribute().setDisabled(true);
    return this;
  }

  @Override
  public StyleBuilder html(SafeHtml html) {
    throw new UnsupportedOperationException(UNSUPPORTED_HTML);
  }

  @Override
  public boolean isChildElementSupported() {
    return false;
  }

  @Override
  public StyleBuilder media(String media) {
    assertCanAddAttribute().setMedia(media);
    return this;
  }

  @Override
  public StyleBuilder text(String text) {
    throw new UnsupportedOperationException(UNSUPPORTED_HTML);
  }

  @Override
  public StyleBuilder type(String type) {
    assertCanAddAttribute().setType(type);
    return this;
  }
}
