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
 * Test for {@link AbsolutePanelParser}.
 */
public class AbsolutePanelParserTest extends TestCase {

  private static final String PARSED_TYPE = "com.google.gwt.user.client.ui.AbsolutePanel";

  private ElementParserTester tester;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    tester = new ElementParserTester(PARSED_TYPE, new AbsolutePanelParser());
  }

  public void testBadChild() throws Exception {
    checkBadLine("<span/>", "Expecting only <g:at> or widget children");
  }

  public void testBadPosition_leftNo() throws Exception {
    checkBadPosition("top='0'", "Missing required attribute \"left\"");
  }

  public void testBadPosition_left() throws Exception {
    checkBadPosition("left='bad' top='0'", "Cannot parse");
  }

  public void testBadPosition_topNo() throws Exception {
    checkBadPosition("left='0'", "Missing required attribute \"top\"");
  }

  public void testBadPosition_top() throws Exception {
    checkBadPosition("left='0' top='bad'", "Cannot parse");
  }

  public void testBad_noWidget() throws Exception {
    checkBadLine("<g:at left='1' top='2'/>",
        "Element must have a single child element");
  }

  public void testBad_moreThanOneWidget() throws Exception {
    checkBadLine("<g:at left='1' top='2'>"
        + "<g:Button id='1'/><g:Button id='2'/></g:at>",
        "Element may only contain a single child element");
  }

  private void checkBadLine(String badLine, String expectedDied)
      throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:AbsolutePanel>");
    b.append("  " + badLine);
    b.append("</g:AbsolutePanel>");

    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException e) {
      String died = tester.logger.died;
      assertTrue(died, died.contains(expectedDied));
    }
  }

  private void checkBadPosition(String positionAttributes, String expectedDied)
      throws Exception {
    String badLine = "<g:at " + positionAttributes + "><g:Button/></g:at>";
    checkBadLine(badLine, expectedDied);
  }

  public void testGood() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:AbsolutePanel>");
    b.append("  <g:at left='1' top='2'>");
    b.append("    <g:Button/>");
    b.append("  </g:at>");
    b.append("  <g:Label/>");
    b.append("  <g:at left='10' top='20'>");
    b.append("    <g:Label/>");
    b.append("  </g:at>");
    b.append("</g:AbsolutePanel>");

    tester.parse(b.toString());

    assertStatements("fieldName.add(<g:Button>, 1, 2);",
        "fieldName.add(<g:Label>);", "fieldName.add(<g:Label>, 10, 20);");
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
