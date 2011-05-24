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

/**
 * Test for {@link UiTextInterpreter}.
 */
public class UiTextInterpreterTest extends TestCase {

  private static final String PARSED_TYPE = "com.google.gwt.user.client.ui.HTMLPanel";

  private ElementParserTester tester;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    tester = new ElementParserTester(PARSED_TYPE, new HTMLPanelParser());
  }

  public void testNoFromAttribute() throws SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:HTMLPanel><ui:text/>");
    b.append("</g:HTMLPanel>");

    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException e) {
      assertTrue("Expect no 'from' attribute error",  
          tester.logger.died.contains("Attribute 'from' not found."));
    }
  }  

  public void testNoComputedValue() throws SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:HTMLPanel><ui:text from='nope'/>");
    b.append("</g:HTMLPanel>");

    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException e) {
      assertTrue("Expect no computed value error",  
          tester.logger.died.contains("Attribute 'from' does not have a computed value"));
    }
  }

  public void testTooManyAttributes() throws SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:HTMLPanel><ui:text from='{foo}' foo='' bar='' />");
    b.append("</g:HTMLPanel>");

    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException e) {
      assertTrue("Expect too many attributes attribute error",  
          tester.logger.died.contains("Unexpected attributes"));
    }
  }
  
  public void testHappy() throws Exception {
    String s = "<g:HTMLPanel>\n <ui:text from='{foo}'/>\n </g:HTMLPanel>\n";

    String interpretedValue = new UiTextInterpreter(tester.writer)
      .interpretElement(tester.getElem(tester.wrapXML(s).toString(), "ui:text"));

    assertEquals(interpretedValue, "\" + \"--token--1--token--\" + \"");
  }
}
