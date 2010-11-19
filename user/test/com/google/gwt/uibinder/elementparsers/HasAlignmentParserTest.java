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

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.util.Iterator;

/**
 * A unit test. Guess what of.
 */
public class HasAlignmentParserTest extends TestCase {
  private static final String PARSED_TYPE = "com.google.gwt.user.client.ui.DockLayoutPanel";

  private ElementParserTester tester;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    tester = new ElementParserTester(PARSED_TYPE, new HasAlignmentParser());
  }

  public void testInvalidVerticalAlignment() throws SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:DockLayoutPanel verticalAlignment='FOO'>");
    b.append("</g:DockLayoutPanel>");

    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException e) {
      assertTrue("Expect vertical alignment parse error",  
          tester.logger.died.contains("Cannot parse value: \"FOO\""));
    }
  }

  public void testInvalidHorizontalAlignment() throws SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:DockLayoutPanel horizontalAlignment='BAR'>");
    b.append("</g:DockLayoutPanel>");

    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException e) {
      assertTrue("Expect horizontal alignment parse error",  
          tester.logger.died.contains("Cannot parse value: \"BAR\""));
    }
  }
  
  public void testNoAlignmentArgs() throws UnableToCompleteException, 
     SAXParseException {
    StringBuffer b = new StringBuffer();
    b.append("<g:DockLayoutPanel>");
    b.append("</g:DockLayoutPanel>");
    
    tester.parse(b.toString());
    assertTrue(tester.writer.statements.isEmpty());
  }
  
  public void testValidArgs() throws UnableToCompleteException, 
    SAXParseException {
    StringBuffer b = new StringBuffer();
    b.append("<g:DockLayoutPanel verticalAlignment='ALIGN_MIDDLE' horizontalAlignment='ALIGN_LEFT'>");
    b.append("</g:DockLayoutPanel>");
    
    tester.parse(b.toString());
    assertStatements("fieldName.setHorizontalAlignment(com.google.gwt.user.client.ui.HasHorizontalAlignment.ALIGN_LEFT);",
        "fieldName.setVerticalAlignment(com.google.gwt.user.client.ui.HasVerticalAlignment.ALIGN_MIDDLE);");
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
