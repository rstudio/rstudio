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

import org.xml.sax.SAXParseException;

import java.util.Iterator;

/**
 * Test for {@link StackPanelParser}.
 */
public class StackPanelParserTest extends TestCase {

  private static final String PARSED_TYPE = "com.google.gwt.user.client.ui.StackPanel";

  private ElementParserTester tester;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    tester = new ElementParserTester(PARSED_TYPE, new StackPanelParser());
  }

  public void testHappy_noStackText() throws SAXParseException, UnableToCompleteException {
    StringBuffer b = new StringBuffer();
    b.append("<g:StackPanel>");
    b.append("  <g:Button/>");
    b.append("</g:StackPanel>");

    tester.parse(b.toString());

    assertStatements("fieldName.add(<g:Button>);");
  }

  public void testHappy_hasStackText() throws SAXParseException, UnableToCompleteException {
    StringBuffer b = new StringBuffer();
    b.append("<g:StackPanel>");
    b.append("  <g:Button g:StackPanel-text='Foo'/>");
    b.append("</g:StackPanel>");

    tester.parse(b.toString());

    assertStatements("fieldName.add(<g:Button>, \"Foo\");");
  }
  
  public void testBadChild() throws SAXParseException {
    StringBuffer b = new StringBuffer();
    b.append("<g:StackPanel>");
    b.append("  <g:UIObject/>");
    b.append("</g:StackPanel>");
    try {
      tester.parse(b.toString());
      fail("Expected UnableToCompleteException");
    } catch (UnableToCompleteException e) {
      /* pass */
    }
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
