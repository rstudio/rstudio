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
   * Failed in all modes due to HtmlUnit bug:
   * https://sourceforge.net/tracker/?func=detail&aid=2897532&group_id=47038&atid=448266
   */
  @DoNotRunWith({Platform.HtmlUnitBug})
  public void testGetAbsolutePositionWhenScrolled() {
    // Force the document body into RTL mode.
    RootPanel.get();

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
    int absLeftStart = inner.getAbsoluteLeft();

    // Check the position when scrolled. In RTL mode, the absolute position of
    // the inner element depends on the position of the scrollbar of the outer
    // element. Some browsers render the scrollbar on the right even in RTL
    // mode, which pushes the inner element about 15 pixels to the left. In
    // order to work around this ambiguity, we compare the old scroll
    // position to the new scroll position, but do not assume the absolute
    // position.
    outer.setScrollLeft(-50);
    assertEquals(outer.getScrollLeft(), -50);
    int absLeftScrolled = inner.getAbsoluteLeft();
    assertEquals(50, absLeftScrolled - absLeftStart);

    // Cleanup test
    RootPanel.getBodyElement().removeChild(outer);
  }

}
