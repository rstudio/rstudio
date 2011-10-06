/*
 * Copyright 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.uibinder.rebind;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompilationStateBuilder;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.dev.javac.testing.impl.MockResourceOracle;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.collect.HashSet;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.uibinder.attributeparsers.AttributeParsers;
import com.google.gwt.uibinder.test.UiJavaResources;

import junit.framework.TestCase;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXParseException;

import java.io.PrintWriter;
import java.util.Set;

/**
 * Tests UiBinderParser behavior for <ui:with> tag.
 */
public class UiBinderParserUiWithTest extends TestCase {

  public static final MockJavaResource BAR = new MockJavaResource("bar.Bar") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package bar;");
      code.append("public class Bar {");
      code.append("  public Bar(int a) { }");
      code.append("}");
      return code;
    }
  };

  public static final MockJavaResource BINDER = new MockJavaResource("binder.OwnerClass") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package binder;\n");
      code.append("import com.google.gwt.uibinder.client.UiBinder;\n");
      code.append("import com.google.gwt.uibinder.client.UiField;\n");
      code.append("import com.google.gwt.uibinder.client.UiFactory;\n");
      code.append("import bar.Bar;\n");
      code.append("import foo.Foo;\n");
      code.append("public class OwnerClass {");
      code.append("  public interface Binder");
      code.append("      extends UiBinder<java.lang.String, OwnerClass> {");
      code.append("  }");
      code.append("  @UiField foo.Foo fieldName;");
      code.append("  @UiFactory bar.Bar aFactory() { return new Bar(1); }");
      code.append("}");
      return code;
    }
  };

  public static final MockJavaResource FOO = new MockJavaResource("foo.Foo") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package foo;");
      code.append("public class Foo {");
      code.append("}");
      return code;
    }
  };

  public static final MockJavaResource FOOISH = new MockJavaResource("foo.Fooish") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package foo;");
      code.append("public class Fooish extends Foo {");
      code.append("}");
      return code;
    }
  };

  public static final MockJavaResource RENDERER = new MockJavaResource("renderer.OwnerClass") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package renderer;\n");
      code.append("import com.google.gwt.safehtml.shared.SafeHtmlBuilder;\n");
      code.append("import com.google.gwt.uibinder.client.UiRenderer;\n");
      code.append("import foo.Foo;\n");
      code.append("public class OwnerClass {");
      code.append("  public interface Renderer");
      code.append("      extends UiRenderer {");
      code.append("    public void render(SafeHtmlBuilder sb, foo.Fooish fieldName);");
      code.append("  }");
      code.append("}");
      return code;
    }
  };

  private static final W3cDomHelper docHelper = new W3cDomHelper(TreeLogger.NULL,
      new MockResourceOracle());

  private static TreeLogger createCompileLogger() {
    PrintWriterTreeLogger logger = new PrintWriterTreeLogger(new PrintWriter(System.err, true));
    logger.setMaxDetail(TreeLogger.ERROR);
    return logger;
  }

  UiBinderParser parser;
  private Document doc;
  private XMLElementProvider elemProvider;

  private XMLElement elm;

  private FieldManager fieldManager;

  private Element item;

  private MockMortalLogger logger;

  private Set<Resource> resources = new HashSet<Resource>();

  private TypeOracle types;

  private UiBinderWriter writer;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    resources.addAll(UiJavaResources.getUiResources());
    resources.add(FOO);
    resources.add(FOOISH);
    resources.add(BAR);
    resources.add(RENDERER);
    resources.add(BINDER);
    CompilationState state = CompilationStateBuilder.buildFrom(createCompileLogger(), resources);
    types = state.getTypeOracle();
    logger = new MockMortalLogger();
    fieldManager = new FieldManager(types, logger, true);
  }

  public void testWithErrorMisTyped() throws SAXParseException {
    try {
      init("<doc><with field=\"fieldName\" type=\"bar.Bar\" bloop=\"\"/></doc>",
          "renderer.OwnerClass.Renderer");
      fail("Expecting UnabletoCompleteException");
    } catch (UnableToCompleteException e) {
      assertNotNull(logger.died);
    }
  }

  public void testWithErrorNoField() throws SAXParseException {
    try {
      init("<doc><with type=\"bar.Bar\"/></doc>", "renderer.OwnerClass.Renderer");
      fail("Expecting UnabletoCompleteException");
    } catch (UnableToCompleteException e) {
      assertNotNull(logger.died);
    }
  }

  public void testWithErrorUiBinderBadUiFieldType() throws SAXParseException {
    try {
      init("<doc><with field=\"someName\" type=\"foo.Unknown\"/></doc>",
          "binder.OwnerClass.Binder");
      fail("Expecting UnabletoCompleteException");
    } catch (UnableToCompleteException e) {
      assertNotNull(logger.died);
    }
  }

  public void testWithErrorUiBinderMisTypedUiField() throws SAXParseException {
    try {
      init("<doc><with field=\"fieldName\" type=\"bar.Bar\"/></doc>", "binder.OwnerClass.Binder");
      fail("Expecting UnabletoCompleteException");
    } catch (UnableToCompleteException e) {
      assertNotNull(logger.died);
    }
  }

  public void testWithErrorUiRendererMisTypedImport() throws SAXParseException {
    try {
      init("<doc><with field=\"fieldName\" type=\"bar.Bar\"/></doc>",
          "renderer.OwnerClass.Renderer");
      fail("Expecting UnabletoCompleteException");
    } catch (UnableToCompleteException e) {
      assertNotNull(logger.died);
    }
  }

  public void testWithErrorUiRendererUnTypedNonExistingFieldName() throws SAXParseException {
    try {
      init("<doc><with field=\"nonExisting\"/></doc>", "renderer.OwnerClass.Renderer");
      fail("Expecting UnabletoCompleteException");
    } catch (UnableToCompleteException e) {
      assertNotNull(logger.died);
    }
  }

  public void testWithGwtCreated() throws UnableToCompleteException, SAXParseException {
    init("<doc><with field=\"notAField\" type=\"foo.Foo\"/></doc>", "renderer.OwnerClass.Renderer");

    assertNotNull(fieldManager.lookup("notAField"));
    assertEquals("foo.Foo", fieldManager.lookup("notAField").getAssignableType()
        .getQualifiedSourceName());
  }

  public void testWithUiBinderTypedUiField() throws UnableToCompleteException, SAXParseException {
    init("<doc><with field=\"fieldName\" type=\"foo.Foo\"/></doc>", "binder.OwnerClass.Binder");

    assertNotNull(fieldManager.lookup("fieldName"));
    assertEquals("foo.Foo", fieldManager.lookup("fieldName").getAssignableType()
        .getQualifiedSourceName());
  }

  public void testWithUiBinderUiFactory() throws UnableToCompleteException, SAXParseException {
    init("<doc><with field=\"factoryProvided\" type=\"bar.Bar\"/></doc>",
        "binder.OwnerClass.Binder");

    assertNotNull(fieldManager.lookup("factoryProvided"));
    assertEquals("bar.Bar", fieldManager.lookup("factoryProvided").getAssignableType()
        .getQualifiedSourceName());
  }

  public void testWithUiBinderUntypedUiField() throws UnableToCompleteException, SAXParseException {
    init("<doc><with field=\"fieldName\"/></doc>", "binder.OwnerClass.Binder");

    assertNotNull(fieldManager.lookup("fieldName"));
    assertEquals("foo.Foo", fieldManager.lookup("fieldName").getAssignableType()
        .getQualifiedSourceName());
  }

  public void testWithUiRendererTypedImport() throws UnableToCompleteException, SAXParseException {
    init("<doc><with field=\"fieldName\" type=\"foo.Foo\"/></doc>", "renderer.OwnerClass.Renderer");

    assertNotNull(fieldManager.lookup("fieldName"));
    assertEquals("foo.Fooish", fieldManager.lookup("fieldName").getAssignableType()
        .getQualifiedSourceName());
  }

  public void testWithUiRendererUntypedImport()
      throws UnableToCompleteException, SAXParseException {
    init("<doc><with field=\"fieldName\"/></doc>", "renderer.OwnerClass.Renderer");

    assertNotNull(fieldManager.lookup("fieldName"));
    assertEquals("foo.Fooish", fieldManager.lookup("fieldName").getAssignableType()
        .getQualifiedSourceName());
  }

  private void init(String domString, String baseClass) throws SAXParseException,
      UnableToCompleteException {
    DesignTimeUtils designTime = DesignTimeUtilsStub.EMPTY;
    elemProvider =
        new XMLElementProviderImpl(new AttributeParsers(types, null, logger), types,
            logger, designTime);
    doc = docHelper.documentFor(domString, null);
    item = (Element) doc.getDocumentElement().getElementsByTagName("with").item(0);
    elm = elemProvider.get(item);
    JClassType aClass = types.findType(baseClass);
    writer =
        new UiBinderWriter(aClass, "foo", "", types, logger, fieldManager, null,
            DesignTimeUtilsStub.EMPTY, new UiBinderContext(), true, true, "");
    parser = new UiBinderParser(writer, null, fieldManager, types, null, "", new UiBinderContext());
    designTime.rememberPathForElements(doc);
    UiBinderParser.Resource.WITH.create(parser, elm);
  }
}
