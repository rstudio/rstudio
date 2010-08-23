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
package com.google.gwt.safehtml.shared;

import junit.framework.TestCase;

/**
 * Unit tests for SafeHtmlBuilder
 */
public class SafeHtmlBuilderTest extends TestCase {

  private static final String FOOBARBAZ_HTML = "foo<em>bar</em>baz";

  public void testEmpty() {
    SafeHtmlBuilder b = new SafeHtmlBuilder();
    assertEquals("", b.toSafeHtml().asString());
  }

  public void testFromSafeHtml() {
    SafeHtml html = new SafeHtmlString(FOOBARBAZ_HTML);
    SafeHtmlBuilder b = new SafeHtmlBuilder().append(html);
    assertEquals(FOOBARBAZ_HTML, b.toSafeHtml().asString());
  }

  public void testAppend() {
    SafeHtml html = new SafeHtmlString(FOOBARBAZ_HTML);
    SafeHtmlBuilder b = new SafeHtmlBuilder().appendHtmlConstant(
        "Yabba dabba &amp; doo\n").appendEscaped("What's up so&so\n").append(
        html);

    String expected = "Yabba dabba &amp; doo\n" + "What&#39;s up so&amp;so\n"
        + FOOBARBAZ_HTML;
    assertEquals(expected, b.toSafeHtml().asString());
  }
}
