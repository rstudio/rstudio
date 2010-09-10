/*
 * Copyright 2010 Google Inc.
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

import com.google.gwt.i18n.client.HasDirection.Direction;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;

/**
 * Tests {@link InlineHTML}.
 * Note: tests only the direction and alignment logic.
 */
public class InlineHTMLTest extends LabelTest {

  static final String html = "<b>hello</b><i>world</i>";

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  // test that the SafeHtml constructor creates the HTML element correctly.
  public void testSafeHtmlConstructor() {
    InlineHTML htmlElement = 
      new InlineHTML(SafeHtmlUtils.fromSafeConstant(html));
    
    assertEquals(html, htmlElement.getHTML().toLowerCase());
  }

  // test that the SafeHtml constructor creates the direction HTML.
  public void testSafeHtmlConstructorWithDirection() {
    InlineHTML htmlElementLTR = 
      new InlineHTML(SafeHtmlUtils.fromSafeConstant(html), Direction.LTR);
    InlineHTML htmlElementRTL = 
      new InlineHTML(SafeHtmlUtils.fromSafeConstant(html), Direction.RTL);
    
    assertEquals(html, htmlElementRTL.getHTML().toLowerCase());
    assertEquals(html, htmlElementLTR.getHTML().toLowerCase());
    assertEquals(Direction.LTR, htmlElementLTR.getTextDirection());
    assertEquals(Direction.RTL, htmlElementRTL.getTextDirection());
  }

  public void testSetSafeHtml() {
    InlineHTML htmlElement = new InlineHTML("<b>foo</b>");
    htmlElement.setHTML(SafeHtmlUtils.fromSafeConstant(html));
    
    assertEquals(html, htmlElement.getHTML().toLowerCase());
  }
}
