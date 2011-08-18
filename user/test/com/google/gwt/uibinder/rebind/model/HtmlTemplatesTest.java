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

package com.google.gwt.uibinder.rebind.model;

import com.google.gwt.uibinder.rebind.IndentedWriter;
import com.google.gwt.uibinder.rebind.MortalLogger;
import com.google.gwt.uibinder.rebind.Tokenator;

import junit.framework.TestCase;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * HtmlTemplates unit tests.
 * 
 */
public class HtmlTemplatesTest extends TestCase {
  private Tokenator tokenator;
  private HtmlTemplatesWriter h;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    tokenator = new Tokenator();
    h = new HtmlTemplatesWriter(null, MortalLogger.NULL);
  }

  public void testHappy() throws IllegalArgumentException {
    String call =
        h.addSafeHtmlTemplate("<DialogBox id=\'" + tokenator.nextToken("\" + domId0 + \"")
            + "\'>this is a dialog box</DialogBox>", tokenator).getIndirectTemplateCall();

    assertEquals("template_html1()", call);

    // Confirm that we write the correct SafeHtmlTemplates interface

    StringWriter s = new StringWriter();
    IndentedWriter n = new IndentedWriter(new PrintWriter(s));
    h.writeInterface(n);

    String[] expectedInterface = {"interface Template extends SafeHtmlTemplates {", //
        "  @Template(\"<DialogBox id='{0}'>this is a dialog box</DialogBox>\")", //
        "  SafeHtml html1(String arg0);", //
        "   ", //
        "}", //
        "", //
        "Template template = GWT.create(Template.class);", //
    };

    assertExpectedStrings(expectedInterface, s.toString());

    // Confirm that we write template caller methods

    s = new StringWriter();
    n = new IndentedWriter(new PrintWriter(s));
    h.writeTemplateCallers(n);

    String[] expectedCaller = {"SafeHtml template_html1() {", //
        "  return template.html1(domId0);", //
        "}"};
    assertExpectedStrings(expectedCaller, s.toString());
  }

  public void testNullHtml() {
    try {
      h.addSafeHtmlTemplate(null, tokenator);
      fail();
    } catch (IllegalArgumentException e) {
      assertTrue("Expected empty html to generate error", e.getMessage().equals(
          "Template html cannot be null"));
    }

    assertTrue(h.isEmpty());
  }

  public void testNullTokenator() throws IllegalArgumentException {
    try {
      h.addSafeHtmlTemplate("<p>this is a static string</p>", null);
      fail();
    } catch (IllegalArgumentException e) {
      assertTrue("Expected empty tokenator to generate error", e.getMessage().equals(
          "Template tokenator cannot be null"));
    }

    assertTrue(h.isEmpty());
  }

  private void assertExpectedStrings(String[] expectedInterface, String string) {
    List<String> actual = Arrays.asList(string.split("\n"));

    Iterator<String> i = actual.iterator();
    for (String e : expectedInterface) {
      assertEquals(e, i.next());
    }
    assertFalse(i.hasNext());
  }
}
