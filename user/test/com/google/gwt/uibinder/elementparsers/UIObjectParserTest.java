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
public class UIObjectParserTest extends TestCase {
  private static final String PARSED_TYPE = "com.google.gwt.user.client.ui.UIObject";

  private ElementParserTester tester;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    tester = new ElementParserTester(PARSED_TYPE, new UIObjectParser());
  }

  public void testHappy() throws UnableToCompleteException, SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:UIObject debugId='blat' styleName='primary' "
        + "addStyleNames='foo, bar baz'");
    b.append("    addStyleDependentNames='able, baker charlie' >");
    b.append("</g:UIObject>");

    String[] expected = {
        "fieldName.ensureDebugId(\"blat\");",
        "fieldName.setStyleName(\"primary\");",
        "fieldName.addStyleName(\"foo\");", "fieldName.addStyleName(\"bar\");",
        "fieldName.addStyleName(\"baz\");",
        "fieldName.addStyleDependentName(\"able\");",
        "fieldName.addStyleDependentName(\"baker\");",
        "fieldName.addStyleDependentName(\"charlie\");",};

    tester.parse(b.toString());

    Iterator<String> i = tester.writer.statements.iterator();
    for (String e : expected) {
      assertEquals(e, i.next());
    }
    assertFalse(i.hasNext());
    assertNull(tester.logger.died);
  }

  public void testSetPrimary() throws UnableToCompleteException, SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:UIObject stylePrimaryName='primary' >");
    b.append("</g:UIObject>");

    String[] expected = {"fieldName.setStylePrimaryName(\"primary\");",};

    tester.parse(b.toString());

    Iterator<String> i = tester.writer.statements.iterator();
    for (String e : expected) {
      assertEquals(e, i.next());
    }
    assertFalse(i.hasNext());
    assertNull(tester.logger.died);
  }

  public void testStyleConflict() throws SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:UIObject stylePrimaryName='primary' styleName='otherPrimary'>");
    b.append("</g:UIObject>");

    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException e) {
      assertTrue("Expect to hear about bad styleName usage",
          tester.logger.died.contains("\"styleName\" and \"stylePrimaryName\""));
    }
  }
}
