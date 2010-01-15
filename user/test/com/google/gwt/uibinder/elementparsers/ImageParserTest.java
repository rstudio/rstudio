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

import java.io.IOException;

/**
 * Eponymous unit test.
 */
public class ImageParserTest extends TestCase {
  private static final String PARSED_TYPE = "com.google.gwt.user.client.ui.Image";

  private ElementParserTester tester;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    tester = new ElementParserTester(PARSED_TYPE, new ImageParser());
  }

  public void testHappyWithResource() throws UnableToCompleteException,
      SAXException, IOException {
    StringBuffer b = new StringBuffer();
    b.append("<ui:Image field='someImageResource' />");
    b.append("<g:Image resource='{someImageResource}' >");
    b.append("</g:Image>");

    FieldWriter w = tester.parse(b.toString());
    assertEquals("new " + PARSED_TYPE + "(someImageResource)",
        w.getInitializer());

    assertTrue(tester.writer.statements.isEmpty());
    assertNull(tester.logger.died);
  }

  public void testHappyWithNoResource() throws UnableToCompleteException,
      SAXException, IOException {
    StringBuffer b = new StringBuffer();
    b.append("<g:Image>");
    b.append("</g:Image>");

    FieldWriter w = tester.parse(b.toString());
    assertNull(w.getInitializer());

    assertTrue(tester.writer.statements.isEmpty());
    assertNull(tester.logger.died);
  }

  public void testChokeOnNonResource() throws SAXException, IOException {
    StringBuffer b = new StringBuffer();
    b.append("<g:Image resource='someString' >");
    b.append("</g:Image>");

    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException e) {
      assertTrue("Expect to hear about ImageResource",
          tester.logger.died.contains("ImageResource"));
    }
  }
}
