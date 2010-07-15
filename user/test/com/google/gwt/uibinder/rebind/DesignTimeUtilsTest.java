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
package com.google.gwt.uibinder.rebind;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.javac.impl.MockResourceOracle;

import junit.framework.TestCase;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXParseException;

import java.beans.Beans;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests for {@link DesignTimeUtils}.
 */
public class DesignTimeUtilsTest extends TestCase {
  private static final W3cDomHelper docHelper = new W3cDomHelper(
      TreeLogger.NULL, new MockResourceOracle());

  ////////////////////////////////////////////////////////////////////////////
  //
  // Life cycle
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  protected void tearDown() throws Exception {
    Beans.setDesignTime(false);
    super.tearDown();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // isDesignTime()
  //
  ///////////////////////////////////////////////////////////////////////////
  /**
   * Test for {@link DesignTimeUtils#isDesignTime()}.
   */
  public void test_isDesignTime_default() {
    assertFalse(DesignTimeUtils.isDesignTime());
  }

  /**
   * Test for {@link DesignTimeUtils#isDesignTime()}.
   */
  public void test_isDesignTime_designTime() {
    Beans.setDesignTime(true);
    assertTrue(DesignTimeUtils.isDesignTime());
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // addDeclarations()
  //
  ///////////////////////////////////////////////////////////////////////////
  /**
   * Test for {@link DesignTimeUtils#addDeclarations(IndentedWriter)}.
   */
  public void test_addDeclarations_default() {
    String result = call_addDeclarations();
    assertEquals("", result);
  }

  /**
   * Test for {@link DesignTimeUtils#addDeclarations(IndentedWriter)}.
   */
  public void test_addDeclarations_designTime() {
    Beans.setDesignTime(true);
    String result = call_addDeclarations();
    String lineSeparator = System.getProperty("line.separator");
    assertEquals(
        "public static interface DTObjectHandler {void handle(String path, Object object);}"
            + lineSeparator
            + "public DTObjectHandler dtObjectHandler;"
            + lineSeparator
            + "public final java.util.Map dtAttributes = new java.util.HashMap();"
            + lineSeparator, result);
  }

  private static String call_addDeclarations() {
    StringWriter sw = new StringWriter();
    IndentedWriter indentedWriter = new IndentedWriter(new PrintWriter(sw));
    DesignTimeUtils.addDeclarations(indentedWriter);
    return sw.toString();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // getImplName()
  //
  ///////////////////////////////////////////////////////////////////////////
  /**
   * Test for {@link DesignTimeUtils#getImplName(String)}.
   */
  public void test_getImplName_default() {
    String basicName = "MyBinderImpl";
    String result = DesignTimeUtils.getImplName(basicName);
    assertEquals(basicName, result);
  }

  /**
   * Test for {@link DesignTimeUtils#getImplName(String)}.
   */
  public void test_getImplName_designTime() {
    Beans.setDesignTime(true);
    String basicName = "MyBinderImpl";
    String result = DesignTimeUtils.getImplName(basicName);
    // has "_designTime" substring
    String prefix = basicName + "_designTime";
    assertTrue(result.startsWith(prefix));
    // suffix is current time plus/minus 1 hour, so generates unique names
    long suffix = Long.parseLong(result.substring(prefix.length()));
    long delta = System.currentTimeMillis() - suffix;
    assertTrue(Math.abs(delta) < 1000 * 3600);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Path
  //
  ///////////////////////////////////////////////////////////////////////////
  /**
   * Test for {@link DesignTimeUtils#getPath(Element)} and related methods.
   */
  public void test_path_default() throws SAXParseException {
    Document doc = docHelper.documentFor("<root><first/><second/></root>", null);
    DesignTimeUtils.rememberPathForElements(doc);
    Element first = getChildElement(doc.getDocumentElement(), "first");
    Element second = getChildElement(doc.getDocumentElement(), "second");
    assertEquals(null, DesignTimeUtils.getPath(first));
    assertEquals(null, DesignTimeUtils.getPath(second));
  }

  /**
   * Test for {@link DesignTimeUtils#getPath(Element)} and related methods.
   */
  public void test_path_designTime() throws SAXParseException {
    Beans.setDesignTime(true);
    Document doc = docHelper.documentFor(
        "<root><first/><second><subSecond/></second></root>", null);
    DesignTimeUtils.rememberPathForElements(doc);
    Element first = getChildElement(doc.getDocumentElement(), "first");
    Element second = getChildElement(doc.getDocumentElement(), "second");
    Element subSecond = getChildElement(second, "subSecond");
    assertEquals("0/0", DesignTimeUtils.getPath(first));
    assertEquals("0/1", DesignTimeUtils.getPath(second));
    assertEquals("0/1/0", DesignTimeUtils.getPath(subSecond));
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // getTemplateContent()
  //
  ///////////////////////////////////////////////////////////////////////////
  /**
   * Test for {@link DesignTimeUtils#getTemplateContent(String)}.
   */
  public void test_getTemplateContent_default() {
    String path = "the/path";
    assertEquals(null, DesignTimeUtils.getTemplateContent(path));
  }

  /**
   * Test for {@link DesignTimeUtils#getTemplateContent(String)}.
   */
  public void test_getTemplateContent_designTime() {
    Beans.setDesignTime(true);
    String path = "the/path";
    String key = "gwt.UiBinder.designTime " + path;
    try {
      String content = "myContent";
      System.setProperty(key, content);
      assertEquals(content, DesignTimeUtils.getTemplateContent(path));
    } finally {
      System.clearProperty(key);
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // handleUIObject()
  //
  ///////////////////////////////////////////////////////////////////////////
  /**
   * Test for
   * {@link DesignTimeUtils#handleUIObject(Statements, XMLElement, String)}
   * .
   */
  public void test_handleUIObject_default() {
    WriterStatements writer = new WriterStatements();
    DesignTimeUtils.handleUIObject(writer, null, "myField");
    assertEquals(0, writer.statements.size());
  }

  /**
   * Test for
   * {@link DesignTimeUtils#handleUIObject(Statements, XMLElement, String)}
   * .
   */
  public void test_handleUIObject_designTime() throws SAXParseException {
    Beans.setDesignTime(true);
    // prepare XMLElement
    XMLElement element;
    {
      Document doc = docHelper.documentFor("<root><first/></root>", null);
      DesignTimeUtils.rememberPathForElements(doc);
      Element first = getChildElement(doc.getDocumentElement(), "first");
      element = createXMLElement(first);
    }
    // prepare statements
    List<String> statements;
    {
      WriterStatements writer = new WriterStatements();
      DesignTimeUtils.handleUIObject(writer, element, "myField");
      statements = writer.statements;
    }
    // validate
    assertEquals(1, statements.size());
    assertEquals(
        "if (dtObjectHandler != null) dtObjectHandler.handle(\"0/0\", myField);",
        statements.get(0));
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // putAttribute()
  //
  ///////////////////////////////////////////////////////////////////////////
  /**
   * Test for
   * {@link DesignTimeUtils#putAttribute(Statements, XMLElement, String, String)}
   * .
   */
  public void test_putAttribute_default() {
    WriterStatements writer = new WriterStatements();
    DesignTimeUtils.putAttribute(writer, null, "name", "value");
    assertEquals(0, writer.statements.size());
  }

  /**
   * Test for
   * {@link DesignTimeUtils#putAttribute(Statements, XMLElement, String, String)}
   * .
   */
  public void test_putAttribute_designTime() throws SAXParseException {
    Beans.setDesignTime(true);
    // prepare XMLElement
    XMLElement element;
    {
      Document doc = docHelper.documentFor("<root><first/></root>", null);
      DesignTimeUtils.rememberPathForElements(doc);
      Element first = getChildElement(doc.getDocumentElement(), "first");
      element = createXMLElement(first);
    }
    // prepare statements
    List<String> statements;
    {
      WriterStatements writer = new WriterStatements();
      DesignTimeUtils.putAttribute(writer, element, "name", "value");
      statements = writer.statements;
    }
    // validate
    assertEquals(1, statements.size());
    assertEquals("dtAttributes.put(\"0/0 name\", value);", statements.get(0));
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Utilities
  //
  ///////////////////////////////////////////////////////////////////////////

  /**
   * @return the only child {@link Element} with given tag name.
   */
  private static Element getChildElement(Element parent, String name) {
    NodeList elements = parent.getElementsByTagName(name);
    assertEquals(1, elements.getLength());
    return (Element) elements.item(0);
  }

  /**
   * @return the {@link XMLElement} wrapper for given {@link Element}.
   */
  private static XMLElement createXMLElement(Element elem) {
    return new XMLElement(elem, null, null, null, null, null);
  }

  /**
   * Implementation of {@link Statements} for simple statements.
   */
  private static class WriterStatements implements Statements {
    List<String> statements = new ArrayList<String>();

    public void addDetachStatement(String format, Object... args) {
    }

    public void addInitStatement(String format, Object... params) {
    }

    public void addStatement(String format, Object... args) {
      statements.add(String.format(format, args));
    }
  };
}
