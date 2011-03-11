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

/**
 * A unit test. Guess what of.
 */
public class IsEmptyParserTest extends TestCase {
  private static final String PARSED_TYPE = "com.google.gwt.user.client.ui.UIObject";

  private ElementParserTester tester;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    tester = new ElementParserTester(PARSED_TYPE, new IsEmptyParser());
  }

  public void testExtraText() throws SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:UIObject>");
    b.append("  I have some extra");
    b.append("</g:UIObject>");

    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException e) {
      assertTrue("Expect extra text echo",  
          tester.logger.died.contains("I have some extra"));
    }
  }

  public void testExtraChildren() throws SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:UIObject>");
    b.append("  <blorp />");
    b.append("</g:UIObject>");

    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException e) {
      assertTrue("Expect extra text child",  
          tester.logger.died.contains("<blorp>"));
    }
  }
  
  public void testExtraAttributes() throws SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:UIObject blip='blap' blorp='bloop'>");
    b.append("</g:UIObject>");

    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException e) {
      assertTrue("Expect extra attributes list",
          tester.logger.died.contains("\"blip\", \"blorp\""));
    }
  }
}
