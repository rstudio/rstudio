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

import java.io.IOException;
import java.util.Iterator;

/**
 * A unit test. Guess what of.
 */
public class TabLayoutPanelParserTest extends TestCase {

  private static final String PARSED_TYPE = "com.google.gwt.user.client.ui.TabLayoutPanel";

  private ElementParserTester tester;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    tester = new ElementParserTester(PARSED_TYPE, new TabLayoutPanelParser());
  }

  public void testBadChild() throws SAXException, IOException {
    StringBuffer b = new StringBuffer();
    b.append("<g:TabLayoutPanel unit='EM'>");
    b.append("  <g:west><foo/></g:west>");
    b.append("</g:TabLayoutPanel>");

    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException e) {
      assertNotNull(tester.logger.died);
    }
  }

  public void testHappy() throws UnableToCompleteException, SAXException,
      IOException {
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
        "fieldName.add(<g:Label id='able'>, \"Re<b>mark</b>able\", true);",
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

  public void testNoUnits() throws SAXException, IOException,
      UnableToCompleteException {
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
