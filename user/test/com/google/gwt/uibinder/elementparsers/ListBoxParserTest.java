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

import junit.framework.TestCase;

import org.xml.sax.SAXException;

import java.util.Iterator;

/**
 * A unit test. Guess what of.
 */
public class ListBoxParserTest extends TestCase {
  private static final String PARSED_TYPE = "com.google.gwt.user.client.ui.ListBox";

  private ElementParserTester tester;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    tester = new ElementParserTester(PARSED_TYPE, new ListBoxParser());
  }
  
  public void testChildWithWhitespaces() throws UnableToCompleteException, SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:ListBox>");
    b.append("  <g:item>    </g:item>");
    b.append("</g:ListBox>");
    
    String[] expected = {
        "fieldName.addItem(\"\");" };
    
    tester.parse(b.toString());
    
    Iterator<String> i = tester.writer.statements.iterator();
    for (String e : expected) {
      assertEquals(e, i.next());
    }
    assertFalse(i.hasNext());
    assertNull(tester.logger.died);
  }
  
  public void testEmptyChild() throws UnableToCompleteException, SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:ListBox>");
    b.append("  <g:item></g:item>");
    b.append("</g:ListBox>");
    
    String[] expected = {
        "fieldName.addItem(\"\");" };
    
    tester.parse(b.toString());
    
    Iterator<String> i = tester.writer.statements.iterator();
    for (String e : expected) {
      assertEquals(e, i.next());
    }
    assertFalse(i.hasNext());
    assertNull(tester.logger.died);
  }
  
  public void testInvalidChild() throws SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:ListBox>");
    b.append("  <foo/>");
    b.append("</g:ListBox>");
    
    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException e) {
      assertNotNull(tester.logger.died);
    }
  }
  
  public void testValidChild() throws UnableToCompleteException, SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:ListBox>");
    b.append("  <g:item>");
    b.append("    test item");
    b.append("  </g:item>");
    
    b.append("  <g:item value='testValue'>");
    b.append("    item with test value");
    b.append("  </g:item>");
    b.append("</g:ListBox>");
    
    String[] expected = {
        "fieldName.addItem(\"test item\");",
        "fieldName.addItem(\"item with test value\", \"testValue\");" };
    
    tester.parse(b.toString());
    
    Iterator<String> i = tester.writer.statements.iterator();
    for (String e : expected) {
      assertEquals(e, i.next());
    }
    assertFalse(i.hasNext());
    assertNull(tester.logger.died);
  }
  
  public void testWidgetAsChild() throws SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:ListBox>");
    b.append("  <g:item>");
    b.append("    <g:Label> Test Label </g:Label>");
    b.append("  </g:item>");
    b.append("</g:ListBox>");
    
    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException e) {
      assertNotNull(tester.logger.died);
    }
  }
}
