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
 * Test for {@link TabLayoutPanelParser}.
 */
public class TabLayoutPanelParserTest extends TestCase {

  private static final String PARSED_TYPE = "com.google.gwt.user.client.ui.TabLayoutPanel";

  private ElementParserTester tester;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    tester = new ElementParserTester(PARSED_TYPE, new TabLayoutPanelParser());
  }

  public void testBad_noBarHeight() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:TabLayoutPanel/>");

    parseAndFail(b, "Missing required attribute", "barHeight");
  }

  public void testBad_notTab() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:TabLayoutPanel barHeight='2'>");
    b.append("  <div/>");
    b.append("</g:TabLayoutPanel>");

    parseAndFail(b, "Only <g:tab> children are allowed");
  }

  public void testBad_noWidget() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:TabLayoutPanel barHeight='2'>");
    b.append("  <g:tab>");
    b.append("  </g:tab>");
    b.append("</g:TabLayoutPanel>");

    parseAndFail(b, "Must have a child widget");
  }

  public void testBad_notWidget() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:TabLayoutPanel barHeight='2'>");
    b.append("  <g:tab>");
    b.append("    <div/>");
    b.append("  </g:tab>");
    b.append("</g:TabLayoutPanel>");

    parseAndFail(b, "Must be a widget");
  }

  public void testBad_twoWidgets() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:TabLayoutPanel barHeight='2'>");
    b.append("  <g:tab>");
    b.append("    <g:Button/>");
    b.append("    <g:Button/>");
    b.append("  </g:tab>");
    b.append("</g:TabLayoutPanel>");

    parseAndFail(b, "May have only one body element");
  }

  public void testBad_noHeader() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:TabLayoutPanel barHeight='2'>");
    b.append("  <g:tab>");
    b.append("    <g:Button/>");
    b.append("  </g:tab>");
    b.append("</g:TabLayoutPanel>");

    parseAndFail(b, "Requires either a <g:header> or <g:customHeader>");
  }

  public void testBad_twoHeaders() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:TabLayoutPanel barHeight='2'>");
    b.append("  <g:tab>");
    b.append("    <g:header size='3'>foo</g:header>");
    b.append("    <g:header size='3'>bar</g:header>");
    b.append("    <g:Button/>");
    b.append("  </g:tab>");
    b.append("</g:TabLayoutPanel>");

    parseAndFail(b, "May have only one <g:header> or <g:customHeader>");
  }

  public void testBad_twoCustomHeaders() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:TabLayoutPanel barHeight='2'>");
    b.append("  <g:tab>");
    b.append("    <g:customHeader size='3'><g:Label>foo</g:Label></g:customHeader>");
    b.append("    <g:customHeader size='3'><g:Label>bar</g:Label></g:customHeader>");
    b.append("    <g:Button/>");
    b.append("  </g:tab>");
    b.append("</g:TabLayoutPanel>");

    parseAndFail(b, "May have only one <g:header> or <g:customHeader>");
  }

  public void testBad_customHeader_notWidget() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:TabLayoutPanel barHeight='2'>");
    b.append("  <g:tab>");
    b.append("    <g:customHeader size='3'><div/></g:customHeader>");
    b.append("    <g:Button/>");
    b.append("  </g:tab>");
    b.append("</g:TabLayoutPanel>");

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
    b.append("<g:TabLayoutPanel barUnit='PX' barHeight='30'>");
    b.append("  <g:tab>");
    b.append("    <g:header size='3'>Re<b>mark</b>able</g:header>");
    b.append("    <g:Label id='able'>able</g:Label>");
    b.append("  </g:tab>");
    b.append("  <g:tab>");
    b.append("    <g:customHeader size='3'>");
    b.append("      <g:Label id='custom'>Custom</g:Label>");
    b.append("    </g:customHeader>");
    b.append("    <g:Label id='baker'>baker</g:Label>");
    b.append("  </g:tab>");
    b.append("</g:TabLayoutPanel>");

    String[] expected = {
        "fieldName.add(<g:Label id='able'>, \"@mockToken-" + ElementParserTester.FIELD_NAME
            + "-Re<b>mark</b>able\", true);",
        "fieldName.add(<g:Label id='baker'>, " + "<g:Label id='custom'>);",};

    FieldWriter w = tester.parse(b.toString());
    assertEquals("new " + PARSED_TYPE
        + "(30, com.google.gwt.dom.client.Style.Unit.PX)", w.getInitializer());

    Iterator<String> i = tester.writer.statements.iterator();
    for (String e : expected) {
      assertEquals(e, i.next());
    }
    assertFalse(i.hasNext());
    assertNull(tester.logger.died);
  }

  public void testNoUnits() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:TabLayoutPanel barHeight='3'>");
    b.append("  </g:TabLayoutPanel>");

    FieldWriter w = tester.parse(b.toString());
    assertEquals("new " + PARSED_TYPE
        + "(3, com.google.gwt.dom.client.Style.Unit.PX)", w.getInitializer());

    Iterator<String> i = tester.writer.statements.iterator();
    assertFalse(i.hasNext());
  }
}
