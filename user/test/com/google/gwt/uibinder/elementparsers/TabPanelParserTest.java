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

import junit.framework.TestCase;

import java.util.Iterator;

/**
 * Test for {@link TabPanelParser}.
 */
public class TabPanelParserTest extends TestCase {

  private static final String PARSED_TYPE = "com.google.gwt.user.client.ui.TabPanel";

  private ElementParserTester tester;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    tester = new ElementParserTester(PARSED_TYPE, new TabPanelParser());
  }

  public void testBad_notTab() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:TabPanel>");
    b.append("  <div/>");
    b.append("</g:TabPanel>");

    parseAndFail(b, "Only <g:Tab> children are allowed");
  }

  public void testBad_noWidget() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:TabPanel>");
    b.append("  <g:Tab>");
    b.append("  </g:Tab>");
    b.append("</g:TabPanel>");

    parseAndFail(b, "Must have a child widget");
  }

  public void testBad_notWidget() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:TabPanel>");
    b.append("  <g:Tab>");
    b.append("    <div/>");
    b.append("  </g:Tab>");
    b.append("</g:TabPanel>");

    parseAndFail(b, "Must be a widget");
  }

  public void testBad_twoWidgets() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:TabPanel>");
    b.append("  <g:Tab>");
    b.append("    <g:Button/>");
    b.append("    <g:Button/>");
    b.append("  </g:Tab>");
    b.append("</g:TabPanel>");

    parseAndFail(b, "May only have a single child widget");
  }

  public void testBad_noHeader() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:TabPanel>");
    b.append("  <g:Tab>");
    b.append("    <g:Button/>");
    b.append("  </g:Tab>");
    b.append("</g:TabPanel>");

    parseAndFail(b, "Requires either a \"text\" attribute or <g:TabHTML>");
  }

  public void testBad_twoHeaders() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:TabPanel>");
    b.append("  <g:Tab text='Foo'>");
    b.append("    <g:TabHTML>Bar</g:TabHTML>");
    b.append("    <g:Button/>");
    b.append("  </g:Tab>");
    b.append("</g:TabPanel>");

    parseAndFail(b, "May have only one \"text\" attribute or <g:TabHTML>");
  }

  public void testBad_twoCustomHeaders() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:TabPanel>");
    b.append("  <g:Tab>");
    b.append("    <g:TabHTML>Foo</g:TabHTML>");
    b.append("    <g:TabHTML>Bar</g:TabHTML>");
    b.append("    <g:Button/>");
    b.append("  </g:Tab>");
    b.append("</g:TabPanel>");

    parseAndFail(b, "May have only one \"text\" attribute or <g:TabHTML>");
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

  public void testHappy() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:TabPanel>");
    b.append("  <g:Tab text='Foo'>");
    b.append("    <g:Label id='0'/>");
    b.append("  </g:Tab>");
    b.append("  <g:Tab>");
    b.append("    <g:TabHTML>B<b>a</b>r</g:TabHTML>");
    b.append("    <g:Label id='1'/>");
    b.append("  </g:Tab>");
    b.append("</g:TabPanel>");

    tester.parse(b.toString());

    assertStatements("fieldName.add(<g:Label id='0'>, \"Foo\");",
        "fieldName.add(<g:Label id='1'>, \"@mockToken-" + ElementParserTester.FIELD_NAME
            + "-B<b>a</b>r\", true);");
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
