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

import org.xml.sax.SAXException;

import java.util.Iterator;

/**
 * A unit test. Guess what of.
 */
public class DockLayoutPanelParserTest extends TestCase {

  private static final String PARSED_TYPE = "com.google.gwt.user.client.ui.DockLayoutPanel";

  private ElementParserTester tester;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    tester = new ElementParserTester(PARSED_TYPE, new DockLayoutPanelParser());
  }

  public void testBadChild() throws SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:DockLayoutPanel unit='EM'>");
    b.append("  <g:west><foo/></g:west>");
    b.append("</g:DockLayoutPanel>");

    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException e) {
      assertTrue("expect \"must contain a widget\" error",
          tester.logger.died.contains("must contain a widget"));
    }
  }

  public void testHappy() throws UnableToCompleteException, SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:DockLayoutPanel unit='EM'>");
    b.append("  <g:north size='5'>");
    b.append("    <g:Label id='north'>north</g:Label>");
    b.append("  </g:north>");
    b.append("  <g:center>");
    b.append("    <g:Label id='center'>center</g:Label>");
    b.append("  </g:center>");
    b.append("</g:DockLayoutPanel>");

    String[] expected = {
        "fieldName.addNorth(<g:Label id='north'>, 5);",
        "fieldName.add(<g:Label id='center'>);"};

    FieldWriter w = tester.parse(b.toString());
    assertEquals("new " + PARSED_TYPE
        + "(com.google.gwt.dom.client.Style.Unit.EM)", w.getInitializer());

    Iterator<String> i = tester.writer.statements.iterator();
    for (String e : expected) {
      assertEquals(e, i.next());
    }
    assertFalse(i.hasNext());
    assertNull(tester.logger.died);
  }

  public void testNiceCenter() throws UnableToCompleteException, SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:DockLayoutPanel unit='EM'>");
    b.append("  <g:center>");
    b.append("    <g:Label id='center'>center</g:Label>");
    b.append("  </g:center>");
    b.append("  <g:north size='5'>");
    b.append("    <g:Label id='north'>north</g:Label>");
    b.append("  </g:north>");
    b.append("</g:DockLayoutPanel>");

    String[] expected = {
        "fieldName.addNorth(<g:Label id='north'>, 5);",
        "fieldName.add(<g:Label id='center'>);",};

    FieldWriter w = tester.parse(b.toString());
    assertEquals("new " + PARSED_TYPE
        + "(com.google.gwt.dom.client.Style.Unit.EM)", w.getInitializer());

    Iterator<String> i = tester.writer.statements.iterator();
    for (String e : expected) {
      assertEquals(e, i.next());
    }
    assertFalse(i.hasNext());
  }

  public void testTooManyCenters() throws SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:DockLayoutPanel unit='EM'>");
    b.append("  <g:center>");
    b.append("    <g:Label id='center'>center</g:Label>");
    b.append("  </g:center>");
    b.append("  <g:center>");
    b.append("    <g:Label id='centerAlso'>centaur</g:Label>");
    b.append("  </g:center>");
    b.append("</g:DockLayoutPanel>");

    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException e) {
      assertNotNull(tester.logger.died);
    }
  }

  public void testNoUnits() throws SAXException, UnableToCompleteException {
    StringBuffer b = new StringBuffer();
    b.append("<g:DockLayoutPanel>");
    b.append("</g:DockLayoutPanel>");

    FieldWriter w = tester.parse(b.toString());
    assertEquals("new " + PARSED_TYPE
        + "(com.google.gwt.dom.client.Style.Unit.PX)", w.getInitializer());

    Iterator<String> i = tester.writer.statements.iterator();
    assertFalse(i.hasNext());
  }

  public void testNoSize() throws SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:DockLayoutPanel>");
    b.append("  <g:north>");
    b.append("    <g:Label id='north'>north</g:Label>");
    b.append("  </g:north>");
    b.append("</g:DockLayoutPanel>");

    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException e) {
      assertTrue("Expect to hear about required attribute size", 
          tester.logger.died.contains("required attribute \"size\""));
    }
  }
}
