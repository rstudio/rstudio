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

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;

/**
 * Tests {@link InlineHyperlinkTest}.
 */
public class InlineHyperlinkTest extends GWTTestCase {

  static final String html = "<b>hello</b><i>world</i>";

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.DebugTest";
  }

  public void testSafeHtmlConstructor() {
    String token = "myToken";
    InlineHyperlink link = 
      new InlineHyperlink(SafeHtmlUtils.fromSafeConstant(html), token);
    
    assertEquals(html, link.getHTML().toLowerCase());
  }

  public void testSetSafeHtml() {
    String token = "myToken";
    InlineHyperlink link = new InlineHyperlink("foobar", token);
    link.setHTML(SafeHtmlUtils.fromSafeConstant(html));
    
    assertEquals(html, link.getHTML().toLowerCase());
  }
}
