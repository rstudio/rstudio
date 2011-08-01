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
import com.google.gwt.dev.javac.testing.impl.MockResourceOracle;

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
  private final DesignTimeUtils stub = DesignTimeUtilsStub.EMPTY;
  private final DesignTimeUtilsImpl impl = new DesignTimeUtilsImpl();

  /**
   * Test for {@link DesignTimeUtilsImpl#isDesignTime(String)}.
   */
  public void test_isDesignTime_evaluate() {
    // not design time
    {
      Beans.setDesignTime(false);
      assertEquals(false, DesignTimeUtilsImpl.isDesignTime("no.Matter"));
    }
    // not this Binder
    try {
      Beans.setDesignTime(true);
      System.setProperty("gwt.UiBinder.isDesignTime my.Binder", "true");
      assertEquals(false, DesignTimeUtilsImpl.isDesignTime("other.Binder"));
    } finally {
      Beans.setDesignTime(false);
      System.clearProperty("gwt.UiBinder.isDesignTime my.design.Binder");
    }
    // OK
    try {
      Beans.setDesignTime(true);
      System.setProperty("gwt.UiBinder.isDesignTime my.Binder", "true");
      assertEquals(true, DesignTimeUtilsImpl.isDesignTime("my.Binder"));
    } finally {
      Beans.setDesignTime(false);
      System.clearProperty("gwt.UiBinder.isDesignTime my.design.Binder");
    }
  }

  /**
   * Test for {@link DesignTimeUtils#addDeclarations(IndentedWriter)}.
   */
  public void test_addDeclarations_default() {
    String result = call_addDeclarations(stub);
    assertEquals("", result);
  }

  /**
   * Test for {@link DesignTimeUtils#addDeclarations(IndentedWriter)}.
   */
  public void test_addDeclarations_designTime() {
    String result = call_addDeclarations(impl);
    assertContains(result, "public static interface DTObjectHandler");
    assertContains(result, "void handle(String path, Object object)");
    assertContains(result, "public DTObjectHandler dtObjectHandler;");
    assertContains(result,
        "public final java.util.Map dtAttributes = new java.util.HashMap();");
    assertContains(result,
        "private void dtPutAttribute(String key, Object...values) {");
  }

  private static String call_addDeclarations(DesignTimeUtils designTime) {
    StringWriter sw = new StringWriter();
    IndentedWriter indentedWriter = new IndentedWriter(new PrintWriter(sw));
    designTime.addDeclarations(indentedWriter);
    return sw.toString();
  }

  private static void assertContains(String content, String subString) {
    assertTrue(subString, content.contains(subString));
  }

  /**
   * Test for {@link DesignTimeUtils#getImplName(String)}.
   */
  public void test_getImplName_default() {
    String basicName = "MyBinderImpl";
    String result = stub.getImplName(basicName);
    assertEquals(basicName, result);
  }

  /**
   * Test for {@link DesignTimeUtils#getImplName(String)}.
   */
  public void test_getImplName_designTime() {
    String basicName = "MyBinderImpl";
    String result = impl.getImplName(basicName);
    // has "_designTime" substring
    String prefix = basicName + "_designTime";
    assertTrue(result.startsWith(prefix));
    // suffix is current time plus/minus 1 hour, so generates unique names
    long suffix = Long.parseLong(result.substring(prefix.length()));
    long delta = System.currentTimeMillis() - suffix;
    assertTrue(Math.abs(delta) < 1000 * 3600);
  }

  /**
   * Test for {@link DesignTimeUtils#getPath(Element)} and related methods.
   */
  public void test_path_default() throws SAXParseException {
    Document doc = docHelper.documentFor("<root><first/><second/></root>", null);
    stub.rememberPathForElements(doc);
    Element first = getChildElement(doc.getDocumentElement(), "first");
    Element second = getChildElement(doc.getDocumentElement(), "second");
    assertEquals(null, stub.getPath(first));
    assertEquals(null, stub.getPath(second));
  }

  /**
   * Test for {@link DesignTimeUtils#getPath(Element)} and related methods.
   */
  public void test_path_designTime() throws SAXParseException {
    Document doc = docHelper.documentFor(
        "<root><first/><second><subSecond/></second></root>", null);
    impl.rememberPathForElements(doc);
    Element first = getChildElement(doc.getDocumentElement(), "first");
    Element second = getChildElement(doc.getDocumentElement(), "second");
    Element subSecond = getChildElement(second, "subSecond");
    assertEquals("0/0", impl.getPath(first));
    assertEquals("0/1", impl.getPath(second));
    assertEquals("0/1/0", impl.getPath(subSecond));
  }

  /**
   * Test for {@link DesignTimeUtils#getProvidedField(String, String)}.
   */
  public void test_getProvidedField_default() {
    String source = stub.getProvidedField("java.lang.String", "fieldName");
    assertEquals(null, source);
  }

  /**
   * Test for {@link DesignTimeUtils#getProvidedField(String, String)}.
   */
  public void test_getProvidedField_designTime() {
    String source = impl.getProvidedField("java.lang.String", "fieldName");
    assertEquals("(java.lang.String) dtObjectHandler.provideField("
        + "java.lang.String.class, \"fieldName\")", source);
  }

  /**
   * Test for {@link DesignTimeUtils#getProvidedFactory(String, String, String)}
   * .
   */
  public void test_getProvidedFactory_default() {
    String source = stub.getProvidedFactory("java.lang.String", "methodName",
        "false, 1");
    assertEquals(null, source);
  }

  /**
   * Test for {@link DesignTimeUtils#getProvidedFactory(String, String, String)}
   * .
   */
  public void test_getProvidedFactory_designTime() {
    String source = impl.getProvidedFactory("java.lang.String", "methodName",
        "false, 1");
    assertEquals("(java.lang.String) dtObjectHandler.provideFactory("
        + "java.lang.String.class, \"methodName\", new Object[] {false, 1})",
        source);
  }

  /**
   * Test for {@link DesignTimeUtils#getTemplateContent(String)}.
   */
  public void test_getTemplateContent_default() {
    String path = "the/path";
    assertEquals(null, stub.getTemplateContent(path));
  }

  /**
   * Test for {@link DesignTimeUtils#getTemplateContent(String)}.
   */
  public void test_getTemplateContent_designTime() {
    String path = "the/path";
    String key = "gwt.UiBinder.designTime " + path;
    try {
      String content = "myContent";
      System.setProperty(key, content);
      assertEquals(content, impl.getTemplateContent(path));
    } finally {
      System.clearProperty(key);
    }
  }

  /**
   * Test for
   * {@link DesignTimeUtils#handleUIObject(IUiBinderWriterStatements, XMLElement, String)}
   * .
   */
  public void test_handleUIObject_default() {
    WriterStatements writer = new WriterStatements();
    stub.handleUIObject(writer, null, "myField");
    assertEquals(0, writer.statements.size());
  }

  /**
   * Test for
   * {@link DesignTimeUtils#handleUIObject(IUiBinderWriterStatements, XMLElement, String)}
   * .
   */
  public void test_handleUIObject_designTime() throws SAXParseException {
    // prepare XMLElement
    XMLElement element;
    {
      Document doc = docHelper.documentFor("<root><first/></root>", null);
      impl.rememberPathForElements(doc);
      Element first = getChildElement(doc.getDocumentElement(), "first");
      element = createXMLElement(first, impl);
    }
    // prepare statements
    List<String> statements;
    {
      WriterStatements writer = new WriterStatements();
      impl.handleUIObject(writer, element, "myField");
      statements = writer.statements;
    }
    // validate
    assertEquals(1, statements.size());
    assertEquals(
        "if (dtObjectHandler != null) dtObjectHandler.handle(\"0/0\", myField);",
        statements.get(0));
  }

  /**
   * Test for {@link DesignTimeUtils#isDesignTime()}.
   */
  public void test_isDesignTime_default() {
    assertEquals(false, stub.isDesignTime());
  }

  /**
   * Test for {@link DesignTimeUtils#isDesignTime()}.
   */
  public void test_getOwnerCheck_designTime() {
    assertEquals(true, impl.isDesignTime());
  }

  /**
   * Test for {@link DesignTimeUtils#putAttribute(XMLElement, String, String)}
   * and {@link DesignTimeUtils#writeAttributes(Statements)}.
   */
  public void test_putAttribute_default() throws SAXParseException {
    List<String> statements = call_putAttribute(stub);
    // validate
    assertEquals(0, statements.size());
  }

  /**
   * Test for {@link DesignTimeUtils#putAttribute(XMLElement, String, String)}
   * and {@link DesignTimeUtils#writeAttributes(Statements)}.
   */
  public void test_putAttribute_designTime() throws SAXParseException {
    List<String> statements = call_putAttribute(impl);
    // validate
    assertEquals(1, statements.size());
    assertEquals("dtPutAttribute(\"0/0 attr\", val);", statements.get(0));
  }

  private static List<String> call_putAttribute(DesignTimeUtils designTime) throws SAXParseException
      {
    // prepare XMLElement
    XMLElement element;
    {
      Document doc = docHelper.documentFor("<root><first/></root>", null);
      designTime.rememberPathForElements(doc);
      Element first = getChildElement(doc.getDocumentElement(), "first");
      element = createXMLElement(first, designTime);
    }
    // prepare statements
    List<String> statements;
    {
      WriterStatements writer = new WriterStatements();
      designTime.putAttribute(element, "attr", "val");
      designTime.writeAttributes(writer);
      statements = writer.statements;
    }
    // done
    return statements;
  }

  /**
   * Test for {@link DesignTimeUtils#putAttribute(XMLElement, String, String[])}
   * .
   */
  public void test_putAttributeStrings_default() throws SAXParseException {
    List<String> statements = call_putAttributeStrings(new String[]{"a", "b"},
        stub);
    // validate
    assertEquals(0, statements.size());
  }

  /**
   * Test for {@link DesignTimeUtils#putAttribute(XMLElement, String, String[])}
   * .
   */
  public void test_putAttributeStrings_designTime_empty() throws SAXParseException {
    List<String> statements = call_putAttributeStrings(new String[]{}, impl);
    // validate
    assertEquals(0, statements.size());
  }

  /**
   * Test for {@link DesignTimeUtils#putAttribute(XMLElement, String, String[])}
   * .
   */
  public void test_putAttributeStrings_designTime() throws SAXParseException {
    List<String> statements = call_putAttributeStrings(new String[]{"a", "b"},
        impl);
    // validate
    assertEquals(1, statements.size());
    assertEquals("dtPutAttribute(\"0/0 attr\", new String[] {a, b});",
        statements.get(0));
  }

  private static List<String> call_putAttributeStrings(String[] strings,
      DesignTimeUtils designTime) throws SAXParseException {
    // prepare XMLElement
    XMLElement element;
    {
      Document doc = docHelper.documentFor("<root><first/></root>", null);
      designTime.rememberPathForElements(doc);
      Element first = getChildElement(doc.getDocumentElement(), "first");
      element = createXMLElement(first, designTime);
    }
    // prepare statements
    List<String> statements;
    {
      WriterStatements writer = new WriterStatements();
      designTime.putAttribute(element, "attr", strings);
      designTime.writeAttributes(writer);
      statements = writer.statements;
    }
    return statements;
  }

  /**
   * Returns the only child {@link Element} with given tag name.
   */
  private static Element getChildElement(Element parent, String name) {
    NodeList elements = parent.getElementsByTagName(name);
    assertEquals(1, elements.getLength());
    return (Element) elements.item(0);
  }

  /**
   * Returns the {@link XMLElement} wrapper for given {@link Element}.
   */
  private static XMLElement createXMLElement(Element elem,
      DesignTimeUtils designTime) {
    return new XMLElement(elem, null, null, null, designTime, null);
  }

  /**
   * Implementation of {@link IUiBinderWriterStatements} for simple statements.
   */
  static class WriterStatements extends Statements.Empty {
    List<String> statements = new ArrayList<String>();

    @Override
    public void addStatement(String format, Object... args) {
      statements.add(String.format(format, args));
    }
  };
}
