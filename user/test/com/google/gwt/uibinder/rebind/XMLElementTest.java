/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.uibinder.rebind;

import com.google.gwt.uibinder.testing.UiBinderTesting;

import junit.framework.TestCase;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Tests XMLElement.
 */
public class XMLElementTest extends TestCase {
  private static final String dom =
    "<doc><elm attr1=\"attr1Value\" attr2=\"attr2Value\"/></doc>";
  private Document doc;
  private Element item;
  private XMLElement elm;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    doc = UiBinderTesting.documentForString(dom);
    item = (Element) doc.getDocumentElement().getElementsByTagName(
        "elm").item(0);
    elm = new XMLElement(item, null);
  }

  public void testConsumeAttribute() {
    assertEquals("attr1Value", elm.consumeAttribute("attr1"));
    assertEquals("", elm.consumeAttribute("attr1"));
  }
  
  public void testEmptyStringOnMissingAttribute()
      throws ParserConfigurationException, SAXException, IOException {
    assertEquals("", elm.consumeAttribute("fnord"));
  }

  public void testIterator() throws ParserConfigurationException, SAXException,
      IOException {
    String[] expecteds = {"attr1", "attr2"};
    Set<String> seen = new HashSet<String>();
    for (int i = elm.getAttributeCount() - 1; i >= 0; i--) {
      XMLAttribute attr = elm.getAttribute(i);
      String expected = expecteds[i];
      assertEquals(expected, attr.getLocalName());
      assertFalse(attr.isConsumed());
      assertEquals(expected + "Value", attr.consumeValue());
      assertTrue(attr.isConsumed());
      seen.add(expected);
    }
    assertEquals(2, seen.size());
  }

  public void testNoEndTags() throws Exception {
    Document doc = UiBinderTesting.documentForString("<doc><br/></doc>");

    Element item = (Element) doc.getDocumentElement().getElementsByTagName("br").item(
        0);
    XMLElement elm = new XMLElement(item, null);
    assertEquals("br", item.getTagName());
    assertEquals("", elm.getClosingTag());
  }
}
