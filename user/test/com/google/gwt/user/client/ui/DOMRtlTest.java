/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.user.client.ui;

import com.google.gwt.dom.client.Document;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

/**
 * Tests standard DOM operations in the {@link DOM} class in RTL mode.
 */
public class DOMRtlTest extends GWTTestCase {
  @Override
  public String getModuleName() {
    return "com.google.gwt.user.UserTestRtl";
  }

  /**
   * Tests {@link DOM#getAbsoluteLeft(Element)} for consistency when the element
   * contains children and has scrollbars.
   */
  @DoNotRunWith({Platform.HtmlUnit})
  public void testGetAbsolutePositionWhenScrolled() {
    assertTrue(LocaleInfo.getCurrentLocale().isRTL());
    final Element outer = DOM.createDiv();
    final Element inner = DOM.createDiv();

    // Create a scrollable element
    outer.getStyle().setProperty("position", "absolute");
    outer.getStyle().setProperty("overflow", "auto");
    outer.getStyle().setPropertyPx("top", 0);
    outer.getStyle().setPropertyPx("left", 100);
    outer.getStyle().setPropertyPx("width", 200);
    outer.getStyle().setPropertyPx("height", 200);
    RootPanel.getBodyElement().appendChild(outer);

    // Create a static positioned inner element
    inner.getStyle().setPropertyPx("width", 300);
    inner.getStyle().setPropertyPx("height", 300);
    outer.appendChild(inner);
    inner.setInnerText(":-)");

    // Check the position when scrolled
    outer.setScrollLeft(50);
    assertEquals(outer.getScrollLeft(), 50);
    int absLeft = inner.getAbsoluteLeft() - Document.get().getBodyOffsetLeft();
    // TODO (jlabanca): FF2 incorrectly reports the absolute left as 49.  When
    // we drop FF2 support, the only valid return value is 50.
    assertTrue(50 == absLeft || 49 == absLeft);

    // Cleanup test
    RootPanel.getBodyElement().removeChild(outer);
  }

}