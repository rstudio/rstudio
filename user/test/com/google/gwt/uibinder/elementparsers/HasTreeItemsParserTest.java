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
 * Test for {@link HasTreeItemsParser}.
 */
public class HasTreeItemsParserTest extends TestCase {

  private static final String PARSED_TYPE = "com.google.gwt.user.client.ui.Tree";

  private HasTreeItemsParser parser;
  private ElementParserTester tester;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    parser = new HasTreeItemsParser();
    tester = new ElementParserTester(PARSED_TYPE, parser);
  }

  public void testBadChild_namespace() throws Exception {
    checkBadLine("<ui:blah/>", HasTreeItemsParser.BAD_CHILD);
  }

  public void testBadChild_name() throws Exception {
    checkBadLine("<g:MenuItem/>", HasTreeItemsParser.BAD_CHILD);
  }

  private void checkBadLine(String badLine, String expectedDied)
      throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:Tree>");
    b.append("  " + badLine);
    b.append("</g:Tree>");
    // parse failed
    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException e) {
      String died = tester.logger.died;
      assertTrue(died, died.contains(expectedDied));
    }
  }

  public void test_TreeItem() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:Tree>");
    b.append("  <g:TreeItem text='1'/>");
    b.append("  <g:TreeItem text='2'/>");
    b.append("</g:Tree>");
    // parse
    tester.parse(b.toString());
    assertStatements("fieldName.addItem(<g:TreeItem text='1'>);",
        "fieldName.addItem(<g:TreeItem text='2'>);");
  }

  public void test_Widget() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:Tree>");
    b.append("  <g:Button text='1'/>");
    b.append("  <g:Button text='2'/>");
    b.append("</g:Tree>");
    // parse
    tester.parse(b.toString());
    assertStatements("fieldName.addItem(<g:Button text='1'>);",
        "fieldName.addItem(<g:Button text='2'>);");
  }
  
  public void test_IsWidget() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:Tree>");
    b.append("  <g:IsWidget />");
    b.append("  <g:IsWidget />");
    b.append("</g:Tree>"); 
    // parse
    tester.parse(b.toString());
    assertStatements("fieldName.addItem(<g:IsWidget>);",
        "fieldName.addItem(<g:IsWidget>);");
  }

  public void test_WidgetItemMix() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:Tree>");
    b.append("  <g:Button text='1'/>");
    b.append("  <g:TreeItem text='2'/>");
    b.append("  <g:Button text='3'/>");
    b.append("  <g:TreeItem text='4'/>");
    b.append("</g:Tree>");
    // parse
    tester.parse(b.toString());
    assertStatements("fieldName.addItem(<g:Button text='1'>);",
        "fieldName.addItem(<g:TreeItem text='2'>);",
        "fieldName.addItem(<g:Button text='3'>);",
        "fieldName.addItem(<g:TreeItem text='4'>);");
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
