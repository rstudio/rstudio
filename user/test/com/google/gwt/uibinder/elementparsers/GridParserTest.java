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
    } catch (UnableToCompleteException exception) {
      assertNotNull(tester.logger.died);
    }
  }
  
  public void testEmptyChild() throws SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:Grid>");
    b.append("  <g:row>");
    b.append("    <g:customCell></g:customCell>");
    b.append("  </g:row>");
    b.append("</g:Grid>");

    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException exception) {
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
    } catch (UnableToCompleteException exception) {
      assertNotNull(tester.logger.died);
    }
  }

  public void testEmpty() throws UnableToCompleteException, SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:Grid></g:Grid>");
    FieldWriter w = tester.parse(b.toString());
    assertNull(w.getInitializer());
    assertTrue(tester.writer.statements.isEmpty());
    /*
     * Note in particular that we should not write out a call to resize() if
     * there is no content. The less we do in the empty case, the less chance
     * we'll interfere with someone's custom subclass of Grid.
     */
  }

  public void testEmptyRows() throws UnableToCompleteException, SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:Grid>");
    b.append("  <g:row></g:row>");
    b.append("  <g:row></g:row>");
    b.append("</g:Grid>");

    String[] expected = {"fieldName.resize(2, 0);",};

    FieldWriter w = tester.parse(b.toString());
    assertNull(w.getInitializer());

    Iterator<String> i = tester.writer.statements.iterator();
    for (String e : expected) {
      assertEquals(e, i.next());
    }
    assertFalse(i.hasNext());
    assertNull(tester.logger.died);
  }

  public void testStyleName() throws UnableToCompleteException, SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:Grid>");
    b.append("  <g:row styleName=\"rowHeaderStyle\">");
    b.append("    <g:cell styleName=\"headerStyle\">foo</g:cell>");
    b.append("    <g:cell styleName=\"headerStyle\">bar</g:cell>");
    b.append("  </g:row>");
    b.append("  <g:row>");
    b.append("    <g:cell>foo</g:cell>");
    b.append("    <g:cell>bar</g:cell>");
    b.append("  </g:row>");
    b.append("</g:Grid>");

    String[] expected = {"fieldName.resize(2, 2);",
        "fieldName.getRowFormatter().setStyleName(0, \"rowHeaderStyle\");",
        "fieldName.setHTML(0, 0, \"@mockToken-" + ElementParserTester.FIELD_NAME
            + "-foo\");",
        "fieldName.getCellFormatter().setStyleName(0, 0, \"headerStyle\");",
        "fieldName.setHTML(0, 1, \"@mockToken-" + ElementParserTester.FIELD_NAME
            + "-bar\");",
        "fieldName.getCellFormatter().setStyleName(0, 1, \"headerStyle\");",
        "fieldName.setHTML(1, 0, \"@mockToken-" + ElementParserTester.FIELD_NAME
            + "-foo\");",
        "fieldName.setHTML(1, 1, \"@mockToken-" + ElementParserTester.FIELD_NAME
            + "-bar\");"};

    FieldWriter w = tester.parse(b.toString());

    Iterator<String> i = tester.writer.statements.iterator();
    for (String e : expected) {
      assertEquals(e, i.next());
    }
    assertFalse(i.hasNext());
    assertNull(tester.logger.died);
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
        "fieldName.resize(2, 2);",
        "fieldName.setHTML(0, 0, \"@mockToken-" + ElementParserTester.FIELD_NAME
            + "-<div>foo HTML element</div>\");",
        "fieldName.setHTML(0, 1, \"@mockToken-" + ElementParserTester.FIELD_NAME
            + "-<div>bar HTML element</div>\");",
        "fieldName.setWidget(1, 0, <g:Label>);",
        "fieldName.setWidget(1, 1, <g:Label>);"};

    FieldWriter w = tester.parse(b.toString());
    assertNull(w.getInitializer());

    Iterator<String> i = tester.writer.statements.iterator();
    for (String e : expected) {
      assertEquals(e, i.next());
    }
    assertFalse(i.hasNext());
    assertNull(tester.logger.died);
  }

  public void testValidChildWithDifferentNumberOfElementsInRows()
      throws UnableToCompleteException, SAXException {
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
        "fieldName.resize(2, 2);",
        "fieldName.setHTML(0, 0, \"@mockToken-" + ElementParserTester.FIELD_NAME
            + "-<div>foo HTML element</div>\");",
        "fieldName.setWidget(1, 0, <g:Label>);",
        "fieldName.setWidget(1, 1, <g:Label>);"};

    FieldWriter w = tester.parse(b.toString());
    assertNull(w.getInitializer());

    Iterator<String> i = tester.writer.statements.iterator();
    for (String e : expected) {
      assertEquals(e, i.next());
    }
    assertFalse(i.hasNext());
    assertNull(tester.logger.died);
  }

}
