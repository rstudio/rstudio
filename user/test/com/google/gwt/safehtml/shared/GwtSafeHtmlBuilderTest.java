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

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Unit tests for {@link SafeHtmlBuilder}.
 */
public class GwtSafeHtmlBuilderTest extends GWTTestCase {

  private static final String FOOBARBAZ_HTML = "foo<em>bar</em>baz";

  public void testEmpty() {
    SafeHtmlBuilder b = new SafeHtmlBuilder();
    assertEquals("", b.toSafeHtml().asString());
  }

  public void testFromSafeHtml() {
    SafeHtml html = SafeHtmlUtils.fromSafeConstant(FOOBARBAZ_HTML);
    SafeHtmlBuilder b = new SafeHtmlBuilder().append(html);
    assertEquals(FOOBARBAZ_HTML, b.toSafeHtml().asString());
  }

  public void testAppend() {
    SafeHtml html = SafeHtmlUtils.fromSafeConstant(FOOBARBAZ_HTML);
    SafeHtmlBuilder b = new SafeHtmlBuilder().appendHtmlConstant(
        "Yabba dabba &amp; doo\n").appendEscaped("What's up so&so\n").append(
        html);

    String expected = "Yabba dabba &amp; doo\n" + "What&#39;s up so&amp;so\n"
        + FOOBARBAZ_HTML;
    assertEquals(expected, b.toSafeHtml().asString());
  }

  public void testAppendHtmlConstant_innerHtml() {
    SafeHtml html = new SafeHtmlBuilder()
        .appendHtmlConstant("<div id=\"div_0\">")
        .appendEscaped("0 < 1")
        .appendHtmlConstant("</div>").toSafeHtml();
    assertEquals("<div id=\"div_0\">0 &lt; 1</div>", html.asString());
  }

  public void testAppendHtmlConstant_withIncompleteHtml() {
    if (GWT.isProdMode()) {
      // appendHtmlConstant does not parse/validate its argument in prod mode.
      // Hence we short-circuit this test in prod mode.
      return;
    }
    SafeHtmlBuilder b = new SafeHtmlBuilder();
    try {
      b.appendHtmlConstant("<a href=\"");
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  public void testAppendChars() {
    SafeHtmlBuilder b = new SafeHtmlBuilder();
    b.append('a');
    b.append('&');
    b.append('b');
    b.append('<');
    b.append('c');
    b.append('>');
    b.append('d');
    b.append('"');
    b.append('e');
    b.append('\'');
    b.append('f');

    SafeHtml html = b.toSafeHtml();
    assertEquals("a&amp;b&lt;c&gt;d&quot;e&#39;f", html.asString());
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.safehtml.SafeHtmlTestsModule";
  }
}
