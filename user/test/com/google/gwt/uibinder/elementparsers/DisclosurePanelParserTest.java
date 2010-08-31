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
package com.google.gwt.uibinder.elementparsers;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.uibinder.rebind.FieldWriter;

import junit.framework.TestCase;

import java.util.Iterator;

/**
 * Test for {@link DisclosurePanelParser}.
 */
public class DisclosurePanelParserTest extends TestCase {

  private static final String PARSED_TYPE = "com.google.gwt.user.client.ui.DisclosurePanel";

  private ElementParserTester tester;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    tester = new ElementParserTester(PARSED_TYPE, new DisclosurePanelParser());
  }

  public void testBad_notWidget() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:DisclosurePanel>");
    b.append("  <div/>");
    b.append("</g:DisclosurePanel>");

    parseAndFail(b, "Must be a widget");
  }

  public void testBad_twoWidgets() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:DisclosurePanel>");
    b.append("  <g:Button id='1'/>");
    b.append("  <g:Button id='2'/>");
    b.append("</g:DisclosurePanel>");

    parseAndFail(b, "May have only one body element");
  }

  public void testBad_twoHeaders() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:DisclosurePanel>");
    b.append("  <g:header>111</g:header>");
    b.append("  <g:header>222</g:header>");
    b.append("  <g:Button/>");
    b.append("</g:DisclosurePanel>");

    parseAndFail(b, "May have only one <g:header> or <g:customHeader>");
  }

  public void testBad_twoCustomHeaders() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:DisclosurePanel>");
    b.append("  <g:customHeader><g:Label>111</g:Label></g:customHeader>");
    b.append("  <g:customHeader><g:Label>222</g:Label></g:customHeader>");
    b.append("  <g:Button/>");
    b.append("</g:DisclosurePanel>");

    parseAndFail(b, "May have only one <g:header> or <g:customHeader>");
  }

  public void testBad_withHeader_onlyOpenImage() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:DisclosurePanel>");
    b.append("  <g:header openImage='{open}'>foo</g:header>");
    b.append("  <g:Button/>");
    b.append("</g:DisclosurePanel>");

    parseAndFail(b,
        "Both openImage and closedImage must be specified, or neither");
  }

  public void testBad_withHeader_onlyClosedImage() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:DisclosurePanel>");
    b.append("  <g:header closedImage='{closed}'>foo</g:header>");
    b.append("  <g:Button/>");
    b.append("</g:DisclosurePanel>");

    parseAndFail(b,
        "Both openImage and closedImage must be specified, or neither");
  }

  public void testBad_withCustomHeader_notWidget() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:DisclosurePanel>");
    b.append("  <g:customHeader><div/></g:customHeader>");
    b.append("  <g:Button/>");
    b.append("</g:DisclosurePanel>");

    parseAndFail(b, "Must be a widget", "<div>");
  }

  /**
   * Parses bad code in given {@link StringBuffer} and asserts that failure
   * message has expected strings.
   */
  private void parseAndFail(StringBuffer b, String... expectedFailures)
      throws Exception {
    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException e) {
      String died = tester.logger.died;
      for (String expectedFailure : expectedFailures) {
        assertTrue(died, died.contains(expectedFailure));
      }
    }
  }

  public void testGood_onlyWidget() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:DisclosurePanel>");
    b.append("  <g:Button/>");
    b.append("</g:DisclosurePanel>");

    tester.parse(b.toString());

    assertStatements("fieldName.add(<g:Button>);");
  }

  public void testGood_withHeader() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:DisclosurePanel>");
    b.append("  <g:header>foo</g:header>");
    b.append("  <g:Button/>");
    b.append("</g:DisclosurePanel>");

    FieldWriter w = tester.parse(b.toString());
    assertEquals("new com.google.gwt.user.client.ui.DisclosurePanel(\"foo\")",
        w.getInitializer());

    assertStatements("fieldName.add(<g:Button>);");
  }

  public void testGood_withHeader_withImages() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:DisclosurePanel>");
    b.append("  <g:header openImage='{open}' closedImage='{closed}'>foo</g:header>");
    b.append("  <g:Button/>");
    b.append("</g:DisclosurePanel>");

    FieldWriter w = tester.parse(b.toString());
    assertEquals(
        "new com.google.gwt.user.client.ui.DisclosurePanel(open, closed, \"foo\")",
        w.getInitializer());

    assertStatements("fieldName.add(<g:Button>);");
  }

  public void testGood_withCustomHeader() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:DisclosurePanel>");
    b.append("  <g:customHeader><g:Label>foo</g:Label></g:customHeader>");
    b.append("  <g:Button/>");
    b.append("</g:DisclosurePanel>");

    FieldWriter w = tester.parse(b.toString());
    assertEquals(null, w.getInitializer());

    assertStatements("fieldName.add(<g:Button>);",
        "fieldName.setHeader(<g:Label>);");
  }

  private void assertStatements(String... expected) {
    Iterator<String> i = tester.writer.statements.iterator();
    for (String e : expected) {
      assertEquals(e, i.next());
    }
    assertFalse(i.hasNext());
    assertNull(tester.logger.died);
  }
}
