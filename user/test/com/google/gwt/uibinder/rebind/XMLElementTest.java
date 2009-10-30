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

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.uibinder.parsers.NullInterpreter;

import junit.framework.TestCase;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Tests XMLElement.
 */
public class XMLElementTest extends TestCase {
  private static final String STRING_WITH_DOUBLEQUOTE = "I have a \" quote in me";
  private Document doc;
  private Element item;
  private XMLElement elm;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    init("<doc><elm attr1=\"attr1Value\" attr2=\"attr2Value\"/></doc>");
  }

  public void testConsumeAttribute() {
    assertEquals("attr1Value", elm.consumeAttribute("attr1"));
    assertEquals("", elm.consumeAttribute("attr1"));
  }

  public void testConsumeAttributeWithDefault() {
    assertEquals("attr1Value", elm.consumeAttribute("attr1", "default"));
    assertEquals("default", elm.consumeAttribute("attr1", "default"));
    assertEquals("otherDefault", elm.consumeAttribute("unsetthing",
        "otherDefault"));
  }

  public void testConsumeRequired() throws UnableToCompleteException {
    assertEquals("attr1Value", elm.consumeRequiredAttribute("attr1"));
    try {
      elm.consumeRequiredAttribute("unsetthing");
      fail("Should have thrown UnableToCompleteException");
    } catch (UnableToCompleteException e) {
      /* pass */
    }
  }

  public void testConsumeInnerTextEscapedAsHtmlStringLiteral()
      throws UnableToCompleteException {
    appendText(STRING_WITH_DOUBLEQUOTE);
    assertEquals(
        UiBinderWriter.escapeTextForJavaStringLiteral(STRING_WITH_DOUBLEQUOTE),
        elm.consumeInnerTextEscapedAsHtmlStringLiteral(new NullInterpreter<String>()));
  }

  public void testConsumeInnerTextEscapedAsHtmlStringLiteralEmpty()
      throws UnableToCompleteException {
    assertEquals(
        "",
        elm.consumeInnerTextEscapedAsHtmlStringLiteral(new NullInterpreter<String>()));
  }

  public void testConsumeSingleChildElementEmpty()
      throws ParserConfigurationException, SAXException, IOException,
      UnableToCompleteException {
    try {
      elm.consumeSingleChildElement();
      fail("Should throw on single child element");
    } catch (UnableToCompleteException e) {
      /* pass */
    }

    init("<doc><elm><child>Hi.</child></elm></doc>");
    assertEquals("Hi.",
        elm.consumeSingleChildElement().consumeUnescapedInnerText());
    
    init("<doc><elm id='elm'><child>Hi.</child><child>Ho.</child></elm></doc>");
    try {
      elm.consumeSingleChildElement();
      fail("Should throw on too many children");
    } catch (UnableToCompleteException e) {
      /* pass */
    }
 }

  private void init(final String domString)
      throws ParserConfigurationException, SAXException, IOException {
    doc = DocumentTestHelp.documentForString(domString);
    item = (Element) doc.getDocumentElement().getElementsByTagName("elm").item(
        0);
    elm = new XMLElement(item, new UiBinderWriter());
  }

  private void appendText(final String text) {
    Text t = doc.createTextNode(text);
    item.appendChild(t);
  }

  public void testConsumeUnescapedInnerText() throws UnableToCompleteException {
    appendText(STRING_WITH_DOUBLEQUOTE);
    assertEquals(STRING_WITH_DOUBLEQUOTE, elm.consumeUnescapedInnerText());
  }

  public void testConsumeUnescapedInnerTextEmpty()
      throws UnableToCompleteException {
    assertEquals("", elm.consumeUnescapedInnerText());
  }

  public void testEmptyStringOnMissingAttribute() {
    assertEquals("", elm.consumeAttribute("fnord"));
  }

  public void testIterator() {
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
    Document doc = DocumentTestHelp.documentForString("<doc><br/></doc>");

    Element item = (Element) doc.getDocumentElement().getElementsByTagName("br").item(
        0);
    XMLElement elm = new XMLElement(item, null);
    assertEquals("br", item.getTagName());
    assertEquals("", elm.getClosingTag());
  }
}
