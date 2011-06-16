/*
 * Copyright 2009 Google Inc.
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
 * Test for {@link StackLayoutPanelParser}.
 */
public class StackLayoutPanelParserTest extends TestCase {

  private static final String PARSED_TYPE = "com.google.gwt.user.client.ui.StackLayoutPanel";

  private ElementParserTester tester;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    tester = new ElementParserTester(PARSED_TYPE, new StackLayoutPanelParser());
  }

  public void testBad_notStack() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:StackLayoutPanel unit='EM'>");
    b.append("  <div/>");
    b.append("</g:StackLayoutPanel>");

    parseAndFail(b, "Only <g:stack> children are allowed");
  }

  public void testBad_noWidget() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:StackLayoutPanel unit='EM'>");
    b.append("  <g:stack>");
    b.append("  </g:stack>");
    b.append("</g:StackLayoutPanel>");

    parseAndFail(b, "Must have a child widget");
  }

  public void testBad_notWidget() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:StackLayoutPanel unit='EM'>");
    b.append("  <g:stack>");
    b.append("    <div/>");
    b.append("  </g:stack>");
    b.append("</g:StackLayoutPanel>");

    parseAndFail(b, "Must be a widget");
  }

  public void testBad_twoWidgets() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:StackLayoutPanel unit='EM'>");
    b.append("  <g:stack>");
    b.append("    <g:Button/>");
    b.append("    <g:Button/>");
    b.append("  </g:stack>");
    b.append("</g:StackLayoutPanel>");

    parseAndFail(b, "May have only one body element");
  }

  public void testBad_noHeader() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:StackLayoutPanel unit='EM'>");
    b.append("  <g:stack>");
    b.append("    <g:Button/>");
    b.append("  </g:stack>");
    b.append("</g:StackLayoutPanel>");

    parseAndFail(b, "Requires either a <g:header> or <g:customHeader>");
  }

  public void testBad_twoHeaders() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:StackLayoutPanel unit='EM'>");
    b.append("  <g:stack>");
    b.append("    <g:header size='3'>foo</g:header>");
    b.append("    <g:header size='3'>bar</g:header>");
    b.append("    <g:Button/>");
    b.append("  </g:stack>");
    b.append("</g:StackLayoutPanel>");

    parseAndFail(b, "May have only one <g:header> or <g:customHeader>");
  }

  public void testBad_twoCustomHeaders() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:StackLayoutPanel unit='EM'>");
    b.append("  <g:stack>");
    b.append("    <g:customHeader size='3'><g:Label>111</g:Label></g:customHeader>");
    b.append("    <g:customHeader size='3'><g:Label>222</g:Label></g:customHeader>");
    b.append("    <g:Button/>");
    b.append("  </g:stack>");
    b.append("</g:StackLayoutPanel>");

    parseAndFail(b, "May have only one <g:header> or <g:customHeader>");
  }

  public void testBad_withCustomHeader_notWidget() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:StackLayoutPanel unit='EM'>");
    b.append("  <g:stack>");
    b.append("    <g:customHeader size='3'><div/></g:customHeader>");
    b.append("    <g:Button/>");
    b.append("  </g:stack>");
    b.append("</g:StackLayoutPanel>");

    parseAndFail(b, "Is not a widget", "<div>");
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
    b.append("<g:StackLayoutPanel unit='PX'>");
    b.append("  <g:stack>");
    b.append("    <g:header size='3'>Re<b>mark</b>able</g:header>");
    b.append("    <g:Label id='able'>able</g:Label>");
    b.append("  </g:stack>");
    b.append("  <g:stack>");
    b.append("    <g:customHeader size='3'>");
    b.append("      <g:Label id='custom'>Custom</g:Label>");
    b.append("    </g:customHeader>");
    b.append("    <g:Label id='baker'>baker</g:Label>");
    b.append("  </g:stack>");
    b.append("</g:StackLayoutPanel>");

    FieldWriter w = tester.parse(b.toString());
    assertEquals("new " + PARSED_TYPE
        + "(com.google.gwt.dom.client.Style.Unit.PX)", w.getInitializer());

    assertStatements(
        "fieldName.add(<g:Label id='able'>, \"@mockToken-" + ElementParserTester.FIELD_NAME
            + "-Re<b>mark</b>able\", true, 3);",
        "fieldName.add(<g:Label id='baker'>, " + "<g:Label id='custom'>, 3);");
  }

  public void testNoUnits() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("  <g:StackLayoutPanel>");
    b.append("  </g:StackLayoutPanel>");

    FieldWriter w = tester.parse(b.toString());
    assertEquals("new " + PARSED_TYPE
        + "(com.google.gwt.dom.client.Style.Unit.PX)", w.getInitializer());

    assertStatements();
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
