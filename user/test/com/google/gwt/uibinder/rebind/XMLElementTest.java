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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompilationStateBuilder;
import com.google.gwt.dev.javac.testing.impl.MockResourceOracle;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.uibinder.attributeparsers.AttributeParsers;
import com.google.gwt.uibinder.test.UiJavaResources;

import junit.framework.TestCase;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.xml.sax.SAXParseException;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Tests XMLElement.
 */
public class XMLElementTest extends TestCase {
  private static final String STRING_WITH_DOUBLEQUOTE = "I have a \" quote in me";

  private static final W3cDomHelper docHelper = new W3cDomHelper(
      TreeLogger.NULL, new MockResourceOracle());

  private static TreeLogger createCompileLogger() {
    PrintWriterTreeLogger logger = new PrintWriterTreeLogger(new PrintWriter(
        System.err, true));
    logger.setMaxDetail(TreeLogger.ERROR);
    return logger;
  }

  private TypeOracle types;
  private MockMortalLogger logger;

  private Document doc;
  private XMLElementProvider elemProvider;
  private XMLElement elm;
  private Element item;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    CompilationState state = CompilationStateBuilder.buildFrom(
        createCompileLogger(), UiJavaResources.getUiResources());
    types = state.getTypeOracle();
    logger = new MockMortalLogger();
    init(DesignTimeUtilsStub.EMPTY,
        "<doc><elm attr1=\"attr1Value\" attr2=\"attr2Value\"/></doc>");
  }

  public void testAssertNoAttributes() throws SAXParseException {
    init("<doc>\n\n<elm yes='true' no='false'>Blah <blah/> blah</elm></doc>");
    assertNull(logger.died);
    try {
      elm.assertNoAttributes();
      fail();
    } catch (UnableToCompleteException e) {
      assertTrue("Expect extra attributes list",
          logger.died.contains("\"yes\""));
      assertTrue("Expect extra attributes list", logger.died.contains("\"no\""));
      assertTrue("Expect line number " + logger.died,
          logger.died.contains("Unknown:3"));
    }
  }

  public void testAssertNoBody() throws SAXParseException {
    init("<doc>\n\n<elm yes='true' no='false'>Blah <blah/> blah</elm></doc>");
    assertNull(logger.died);
    try {
      elm.assertNoBody();
      fail();
    } catch (UnableToCompleteException e) {
      assertTrue("Expect extra child " + logger.died,
          logger.died.contains("<blah>"));
      assertTrue("Expect line number", logger.died.contains("Unknown:3"));
    }
  }

  public void testAssertNoText() throws SAXParseException {
    init("<doc>\n\n<elm yes='true' no='false'>Blah <blah/> blah</elm></doc>");
    assertNull(logger.died);
    try {
      elm.assertNoText();
      fail();
    } catch (UnableToCompleteException e) {
      assertTrue("Expect extra text", logger.died.contains("Blah"));
      assertTrue("Expect extra text", logger.died.contains("blah"));
      assertTrue("Expect line number", logger.died.contains("Unknown:3"));
    }
  }

  public void testConsumeBoolean() throws SAXParseException,
      UnableToCompleteException {
    init("<doc>\n\n<elm yes='true' no='false' "
        + "fnord='fnord' ref='{foo.bar.baz}'/></doc>");

    assertNull(elm.consumeBooleanAttribute("foo"));

    assertEquals("true", elm.consumeBooleanAttribute("yes"));
    assertNull(elm.consumeBooleanAttribute("yes"));

    assertEquals("false", elm.consumeBooleanAttribute("no"));
    assertNull(elm.consumeBooleanAttribute("no"));

    assertEquals("foo.bar().baz()", elm.consumeBooleanAttribute("ref"));

    assertNull(logger.died);
    try {
      elm.consumeBooleanAttribute("fnord");
      fail("Should throw UnableToCompleteException on misparse");
    } catch (UnableToCompleteException c) {
      assertNotNull(logger.died);
      assertTrue("Expect line number", logger.died.contains("Unknown:3"));
    }
  }

  public void testConsumeBooleanConstant() throws SAXParseException,
      UnableToCompleteException {
    init("<doc>\n\n<elm yes='true' no='false' "
        + "fnord='fnord' ref='{foo.bar.baz}' empty=''/></doc>");

    assertNull(elm.consumeBooleanConstantAttribute("foo"));

    assertTrue(elm.consumeBooleanConstantAttribute("yes"));
    assertNull(elm.consumeBooleanConstantAttribute("yes"));

    assertFalse(elm.consumeBooleanConstantAttribute("no"));
    assertNull(elm.consumeBooleanConstantAttribute("no"));

    assertNull(logger.died);
    try {
      elm.consumeBooleanConstantAttribute("empty");
    } catch (UnableToCompleteException c) {
      assertNotNull(logger.died);
      assertTrue("Expect line number", logger.died.contains("Unknown:3"));
    }

    logger.died = null;
    try {
      elm.consumeBooleanConstantAttribute("ref");
      fail("Should throw UnableToCompleteException on field ref");
    } catch (UnableToCompleteException c) {
      assertNotNull(logger.died);
      assertTrue("Expect line number", logger.died.contains("Unknown:3"));
    }

    logger.died = null;
    try {
      elm.consumeBooleanConstantAttribute("fnord");
      fail("Should throw UnableToCompleteException on misparse");
    } catch (UnableToCompleteException c) {
      assertNotNull(logger.died);
      assertTrue("Expect line number", logger.died.contains("Unknown:3"));
    }
  }

  public void testConsumeBooleanDefault() throws SAXParseException,
      UnableToCompleteException {
    init("<doc>\n\n<elm yes='true' no='false' "
        + "fnord='fnord' ref='{foo.bar.baz}'/></doc>");

    assertEquals("false", elm.consumeBooleanAttribute("foo", false));
    assertEquals("true", elm.consumeBooleanAttribute("foo", true));

    assertEquals("true", elm.consumeBooleanAttribute("yes", false));
    assertEquals("false", elm.consumeBooleanAttribute("yes", false));

    assertEquals("false", elm.consumeBooleanAttribute("no", true));
    assertEquals("true", elm.consumeBooleanAttribute("no", true));

    assertEquals("foo.bar().baz()", elm.consumeBooleanAttribute("ref", true));
    assertEquals("true", elm.consumeBooleanAttribute("ref", true));

    assertNull(logger.died);
    try {
      elm.consumeBooleanAttribute("fnord");
      fail("Should throw UnableToCompleteException on misparse");
    } catch (UnableToCompleteException c) {
      assertNotNull(logger.died);
      assertTrue("Expect line number", logger.died.contains("Unknown:3"));
    }
  }

  /**
   * Test that {@link XMLElement#consumeBooleanAttribute(String, boolean)} calls
   * {@link DesignTimeUtils}.
   */
  public void testConsumeBooleanDefault_designTime() throws Exception {
    DesignTimeUtilsImpl designTime = new DesignTimeUtilsImpl();
    init(designTime,
        "<doc><elm yes='true' no='false' ref='{foo.bar.baz}'/></doc>");
    // consume attributes
    assertEquals("true", elm.consumeBooleanAttribute("foo", true));
    assertEquals("true", elm.consumeBooleanAttribute("yes"));
    assertEquals("false", elm.consumeBooleanAttribute("no"));
    assertEquals("foo.bar().baz()", elm.consumeBooleanAttribute("ref"));
    // validate
    Map<String, String> attributes = designTime.getAttributes();
    assertEquals(4, attributes.size());
    assertEquals("true", attributes.get("0/0 foo.default"));
    assertEquals("true", attributes.get("0/0 yes"));
    assertEquals("false", attributes.get("0/0 no"));
    assertEquals("foo.bar().baz()", attributes.get("0/0 ref"));
  }

  /**
   * Test for {@link XMLElement#consumeStringArrayAttribute(String)}.
   */
  public void testConsumeStringArray() throws Exception {
    init("<doc>\n\n<elm spaces='a b c' commas='a,b,c' mixed='a, b, c'/></doc>");

    String[] expected = new String[]{"\"a\"", "\"b\"", "\"c\""};
    assertTrue(Arrays.equals(new String[]{},
        elm.consumeStringArrayAttribute("no-such-attribute")));
    assertTrue(Arrays.equals(expected,
        elm.consumeStringArrayAttribute("spaces")));
    assertTrue(Arrays.equals(expected,
        elm.consumeStringArrayAttribute("commas")));
    assertTrue(Arrays.equals(expected, elm.consumeStringArrayAttribute("mixed")));
  }

  /**
   * Test that {@link XMLElement#consumeStringArrayAttribute(String)} calls
   * {@link DesignTimeUtils}.
   */
  public void testConsumeStringArray_designTime() throws Exception {
    DesignTimeUtilsImpl designTime = new DesignTimeUtilsImpl();
    init(designTime, "<doc>\n\n<elm attr='a b c'/></doc>");
    // consume attributes
    String[] expected = new String[]{"\"a\"", "\"b\"", "\"c\""};
    assertTrue(Arrays.equals(expected, elm.consumeStringArrayAttribute("attr")));
    // validate
    Map<String, String> attributes = designTime.getAttributes();
    assertEquals(1, attributes.size());
    assertEquals("new String[] {\"a\", \"b\", \"c\"}",
        attributes.get("0/0 attr"));
  }

  public void testConsumeChildrenNoTextAllowed() throws SAXParseException {
    init("<doc>\n\n<elm><child>Hi.</child> Stray text is bad</elm></doc>");
    assertNull(logger.died);
    try {
      elm.consumeChildElements();
      fail();
    } catch (UnableToCompleteException e) {
      assertNotNull(logger.died);
      assertTrue("Expect line number", logger.died.contains("Unknown:3"));
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

  public void testConsumeRawAttribute() {
    assertEquals("attr1Value", elm.consumeRawAttribute("attr1"));
    assertNull(elm.consumeRawAttribute("attr1"));
  }

  public void testConsumeRawAttributeWithDefault() {
    assertEquals("attr1Value", elm.consumeRawAttribute("attr1", "default"));
    assertEquals("default", elm.consumeRawAttribute("attr1", "default"));
    assertEquals("otherDefault",
        elm.consumeRawAttribute("unsetthing", "otherDefault"));
  }

  public void testConsumeRequiredRaw() throws UnableToCompleteException {
    assertEquals("attr1Value", elm.consumeRequiredRawAttribute("attr1"));
    assertNull(logger.died);
    try {
      elm.consumeRequiredRawAttribute("unsetthing");
      fail("Should have thrown UnableToCompleteException");
    } catch (UnableToCompleteException e) {
      assertNotNull(logger.died);
    }
  }

  public void testConsumeRequired() throws UnableToCompleteException {
    assertEquals(
        "\"attr1Value\"",
        elm.consumeRequiredAttribute("attr1",
            types.findType("java.lang.String")));
  }

  /**
   * Test that {@link XMLElement#consumeRequiredAttribute(String)} calls
   * {@link DesignTimeUtils}.
   */
  public void testConsumeRequired_designTime() throws Exception {
    DesignTimeUtilsImpl designTime = new DesignTimeUtilsImpl();
    init(designTime, "<doc><elm attr='val'/></doc>");
    // consume attributes
    assertEquals(
        "\"val\"",
        elm.consumeRequiredAttribute("attr", types.findType("java.lang.String")));
    // validate
    Map<String, String> attributes = designTime.getAttributes();
    assertEquals(1, attributes.size());
    assertEquals("\"val\"", attributes.get("0/0 attr"));
  }

  public void testConsumeRequiredDouble() throws UnableToCompleteException,
      SAXParseException {
    init("<doc>\n\n<elm minus='-123.45' plus='123.45' minus-one='-1' "
        + "plus-one='1' fnord='fnord' ref='{foo.bar.baz}'/></doc>");
    assertEquals("1", elm.consumeRequiredDoubleAttribute("plus-one"));
    assertEquals("-1", elm.consumeRequiredDoubleAttribute("minus-one"));
    assertEquals("123.45", elm.consumeRequiredDoubleAttribute("plus"));
    assertEquals("-123.45", elm.consumeRequiredDoubleAttribute("minus"));
    assertEquals("(double)foo.bar().baz()",
        elm.consumeRequiredDoubleAttribute("ref"));

    assertNull(logger.died);
    try {
      elm.consumeRequiredDoubleAttribute("fnord");
      fail("Should throw UnableToCompleteException on misparse");
    } catch (UnableToCompleteException c) {
      assertNotNull(logger.died);
      assertTrue("Expect line number", logger.died.contains("Unknown:3"));
    }

    logger.died = null;
    try {
      elm.consumeRequiredDoubleAttribute("plus-one");
      fail("Should throw UnableToCompleteException consumed attribute");
    } catch (UnableToCompleteException c) {
      assertNotNull(logger.died);
      assertTrue("Expect line number", logger.died.contains("Unknown:3"));
    }

    logger.died = null;
    try {
      elm.consumeRequiredDoubleAttribute("empty");
      fail("Should throw UnableToCompleteException on no such attribute");
    } catch (UnableToCompleteException c) {
      assertNotNull(logger.died);
      assertTrue("Expect line number", logger.died.contains("Unknown:3"));
    }
  }

  /**
   * Test for {@link XMLElement#consumeRequiredIntAttribute(String)}.
   */
  public void testConsumeRequiredInt() throws UnableToCompleteException,
      SAXParseException {
    init("<doc>\n\n<elm plus='123' minus='-123'"
        + " fnord='fnord' ref='{foo.bar.baz}'/></doc>");
    assertEquals("123", elm.consumeRequiredIntAttribute("plus"));
    assertEquals("-123", elm.consumeRequiredIntAttribute("minus"));
    assertEquals("(int)foo.bar().baz()", elm.consumeRequiredIntAttribute("ref"));

    assertNull(logger.died);
    try {
      elm.consumeRequiredDoubleAttribute("fnord");
      fail("Should throw UnableToCompleteException on misparse");
    } catch (UnableToCompleteException c) {
      assertNotNull(logger.died);
      assertTrue("Expect line number", logger.died.contains("Unknown:3"));
    }

    logger.died = null;
    try {
      elm.consumeRequiredDoubleAttribute("empty");
      fail("Should throw UnableToCompleteException on no such attribute");
    } catch (UnableToCompleteException c) {
      assertNotNull(logger.died);
      assertTrue("Expect line number", logger.died.contains("Unknown:3"));
    }
  }

  public void testConsumeSingleChildElementEmpty() throws SAXParseException,
      UnableToCompleteException {
    assertNull(logger.died);
    try {
      elm.consumeSingleChildElement();
      fail("Should throw on single child element");
    } catch (UnableToCompleteException e) {
      assertNotNull(logger.died);
      assertTrue("Expect line number", logger.died.contains("Unknown:1"));
    }

    init("<doc><elm><child>Hi.</child></elm></doc>");
    assertEquals("Hi.",
        elm.consumeSingleChildElement().consumeUnescapedInnerText());

    logger.died = null;
    init("<doc>\n\n<elm id='elm'><child>Hi.</child><child>Ho.</child></elm></doc>");
    assertNull(logger.died);
    try {
      elm.consumeSingleChildElement();
      fail("Should throw on too many children");
    } catch (UnableToCompleteException e) {
      assertNotNull(logger.died);
      assertTrue("Expect line number", logger.died.contains("Unknown:3"));
    }
  }

  public void testConsumeUnescapedInnerText() throws UnableToCompleteException {
    appendText(STRING_WITH_DOUBLEQUOTE);
    assertEquals(STRING_WITH_DOUBLEQUOTE, elm.consumeUnescapedInnerText());
  }

  public void testConsumeUnescapedInnerTextEmpty()
      throws UnableToCompleteException {
    assertEquals("", elm.consumeUnescapedInnerText());
  }

  public void testNullOnMissingAttribute() {
    assertNull(elm.consumeRawAttribute("fnord"));
  }

  public void testIterator() {
    String[] expecteds = {"attr1", "attr2"};
    Set<String> seen = new HashSet<String>();
    for (int i = elm.getAttributeCount() - 1; i >= 0; i--) {
      XMLAttribute attr = elm.getAttribute(i);
      String expected = expecteds[i];
      assertEquals(expected, attr.getLocalName());
      assertFalse(attr.isConsumed());
      assertEquals(expected + "Value", attr.consumeRawValue());
      assertTrue(attr.isConsumed());
      seen.add(expected);
    }
    assertEquals(2, seen.size());
  }

  public void testNoEndTags() throws SAXParseException {
    doc = docHelper.documentFor("<doc><br/></doc>", null);
    Element documentElement = doc.getDocumentElement();
    Element item = (Element) documentElement.getElementsByTagName("br").item(0);
    XMLElement elm = elemProvider.get(item);
    assertEquals("br", item.getTagName());
    assertEquals("", elm.getClosingTag());
  }

  private void appendText(final String text) {
    Text t = doc.createTextNode(text);
    item.appendChild(t);
  }

  /**
   * Initializes {@link #elemProvider} and parses DOM string.
   */
  private void init(DesignTimeUtils designTime, String domString)
      throws SAXParseException {
    elemProvider = new XMLElementProviderImpl(new AttributeParsers(types, null,
        logger), types, logger, designTime);
    init(domString);
    designTime.rememberPathForElements(doc);
  }

  /**
   * Parses specified DOM string into {@link #doc}, extract first element into
   * {@link #item} and prepares its {@link XMLElement} into {@link #elm}.
   */
  private void init(String domString) throws SAXParseException {
    doc = docHelper.documentFor(domString, null);
    item = (Element) doc.getDocumentElement().getElementsByTagName("elm").item(
        0);
    elm = elemProvider.get(item);
  }
}
