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

import org.xml.sax.SAXException;

import java.util.Iterator;

/**
 * GridParser unit tests.
 */
public class GridParserTest extends TestCase {
  private static final String PARSED_TYPE = "com.google.gwt.user.client.ui.Grid";

  private ElementParserTester tester;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    tester = new ElementParserTester(PARSED_TYPE, new GridParser());
  }
  
  public void testCellWithWhitespaces() throws SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:Grid>");
    b.append("  <g:row>");
    b.append("    <g:customCell>    </g:customCell>");
    b.append("  </g:row>");
    b.append("</g:Grid>");
    
    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException e) {
      assertNotNull(tester.logger.died);
    }
  }
  
  public void testEmtpyChild() throws SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:Grid>");
    b.append("  <g:row>");
    b.append("    <g:customCell></g:customCell>");
    b.append("  </g:row>");
    b.append("</g:Grid>");
    
    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException e) {
      assertNotNull(tester.logger.died);
    }
  }
  
  public void testInvalidChild() throws SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:Grid>");
    b.append("  <foo/>");
    b.append("</g:Grid>");
    
    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException e) {
      assertNotNull(tester.logger.died);
    }
  }
  
  public void testValidChild() throws UnableToCompleteException, SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:Grid>");
    b.append("  <g:row>");
    b.append("    <g:cell>");
    b.append("      <div>foo HTML element</div>");
    b.append("    </g:cell>");
    b.append("    <g:cell>");
    b.append("      <div>bar HTML element</div>");
    b.append("    </g:cell>");
    b.append("  </g:row>");
    b.append("  <g:row>");
    b.append("    <g:customCell>");
    b.append("      <g:Label/>");
    b.append("    </g:customCell>");
    b.append("    <g:customCell>");
    b.append("      <g:Label/>");
    b.append("    </g:customCell>");
    b.append("  </g:row>");
    b.append("</g:Grid>");
    
    String[] expected = {
        "fieldName.setHTML(0, 0, \"<div>foo HTML element</div>\");",
        "fieldName.setHTML(0, 1, \"<div>bar HTML element</div>\");",
        "fieldName.setWidget(1, 0, <g:Label>);",
        "fieldName.setWidget(1, 1, <g:Label>);" };
    
    FieldWriter w = tester.parse(b.toString());
    assertEquals("new " + PARSED_TYPE + "(2, 2)", w.getInitializer());
    
    Iterator<String> i = tester.writer.statements.iterator();
    for (String e : expected) {
      assertEquals(e, i.next());
    }
    assertFalse(i.hasNext());
    assertNull(tester.logger.died);
  }
  
  public void testValidChildWithDifferentNumberOfElementsInRows() throws UnableToCompleteException, SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:Grid>");
    b.append("  <g:row>");
    b.append("    <g:cell>");
    b.append("      <div>foo HTML element</div>");
    b.append("    </g:cell>");
    b.append("  </g:row>");
    b.append("  <g:row>");
    b.append("    <g:customCell>");
    b.append("      <g:Label/>");
    b.append("    </g:customCell>");
    b.append("    <g:customCell>");
    b.append("      <g:Label/>");
    b.append("    </g:customCell>");
    b.append("  </g:row>");
    b.append("</g:Grid>");
    
    String[] expected = {
        "fieldName.setHTML(0, 0, \"<div>foo HTML element</div>\");",
        "fieldName.setWidget(1, 0, <g:Label>);",
        "fieldName.setWidget(1, 1, <g:Label>);" };
    
    FieldWriter w = tester.parse(b.toString());
    assertEquals("new " + PARSED_TYPE + "(2, 2)", w.getInitializer());
    
    Iterator<String> i = tester.writer.statements.iterator();
    for (String e : expected) {
      assertEquals(e, i.next());
    }
    assertFalse(i.hasNext());
    assertNull(tester.logger.died);
  }

}
