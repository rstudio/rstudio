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
 * Tests {@link HTML}.
 * Note: tests only the direction and alignment logic.
 */
public class HTMLTest extends LabelTest {

  private static final String html = "<b>hello</b><i>world</i>";

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  public void testDirectionEstimator() {
    HTML html = new HTML();
    html.setText("<b>bar</b>", Direction.RTL);
    html.setDirectionEstimator(true);
    assertEquals("<b>bar</b>", html.getText());
  }

  // test that the SafeHtml constructor creates the HTML element correctly.
  public void testSafeHtmlConstructor() {
    HTML htmlElement = new HTML(SafeHtmlUtils.fromSafeConstant(html));
    
    assertEquals(html, htmlElement.getHTML().toLowerCase());
  }

  // test that the SafeHtml constructor creates the wordwrapped'ed HTML.
  public void testSafeHtmlConstructorWithDirection() {
    HTML htmlElementLTR = new HTML(
        SafeHtmlUtils.fromSafeConstant(html), Direction.LTR);
    HTML htmlElementRTL = new HTML(
        SafeHtmlUtils.fromSafeConstant(html), Direction.RTL);
    
    assertEquals(html, htmlElementRTL.getHTML().toLowerCase());
    assertEquals(html, htmlElementLTR.getHTML().toLowerCase());
    
    assertEquals(Direction.LTR, htmlElementLTR.getTextDirection());
    assertEquals(Direction.RTL, htmlElementRTL.getTextDirection());
  }

  public void testSetSafeHtml() {
    HTML htmlElement = new HTML("<b>foo</b>");
    htmlElement.setHTML(SafeHtmlUtils.fromSafeConstant(html));
    
    assertEquals(html, htmlElement.getHTML().toLowerCase());
  }

  @SuppressWarnings("deprecation")
  public void testSetSafeHtmlWithDirection() {
    HTML htmlElement = new HTML("<b>foo</b>");
    htmlElement.setHTML(SafeHtmlUtils.fromSafeConstant(html), Direction.LTR);
    
    assertEquals(html, htmlElement.getHTML().toLowerCase());
    assertEquals(Direction.LTR, htmlElement.getDirection());
  }

  public void testSetText() {
    // test that setting plain text works
    HTML html1 = new HTML();
    html1.setText("<b>test</b>");
    assertEquals("<b>test</b>", html1.getText());
    
    // test that setting plain text with direction works
    HTML html2 = new HTML();
    html2.setText("<b>foo</b>", Direction.RTL);
    assertEquals("<b>foo</b>", html2.getText());
  }
}
