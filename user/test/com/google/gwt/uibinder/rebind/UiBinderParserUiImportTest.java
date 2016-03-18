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
import com.google.gwt.dev.CompilerContext;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompilationStateBuilder;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.dev.javac.testing.impl.MockResourceOracle;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.resource.ResourceOracle;
import com.google.gwt.dev.util.collect.HashSet;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.resources.rg.GssResourceGenerator.AutoConversionMode;
import com.google.gwt.resources.rg.GssResourceGenerator.GssOptions;
import com.google.gwt.uibinder.attributeparsers.AttributeParsers;
import com.google.gwt.uibinder.test.UiJavaResources;

import junit.framework.TestCase;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXParseException;

import java.io.PrintWriter;
import java.util.Set;

/**
 * Tests UiBinderParser behavior for <ui:import> tag.
 */
public class UiBinderParserUiImportTest extends TestCase {

  public static final MockJavaResource BAR = new MockJavaResource("bar.Bar") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package bar;");
      code.append("public class Bar {");
      code.append("  public static String s = \"testString\";");
      code.append("  public Bar(int a) { }");
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
      code.append("public class OwnerClass {");
      code.append("  public interface Renderer");
      code.append("      extends UiRenderer {");
      code.append("    public void render(SafeHtmlBuilder sb);");
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
    resources.add(BAR);
    resources.add(RENDERER);
    CompilationState state =
        CompilationStateBuilder.buildFrom(createCompileLogger(), new CompilerContext(), resources);
    types = state.getTypeOracle();
    logger = new MockMortalLogger();
    fieldManager = new FieldManager(types, logger, true);
  }

  public void testUiImportHasTypeImported()
      throws UnableToCompleteException, SAXParseException {
    init("<doc><import field=\"bar.Bar.s\"/></doc>", "renderer.OwnerClass.Renderer");

    assertNotNull(fieldManager.lookup("s"));
    assertTrue("ui:import field must have type IMPORTED",
            fieldManager.lookup("s").getFieldType().equals(FieldWriterType.IMPORTED));
  }

  private void init(String domString, String baseClass) throws SAXParseException,
      UnableToCompleteException {
    DesignTimeUtils designTime = DesignTimeUtilsStub.EMPTY;
    elemProvider =
        new XMLElementProviderImpl(new AttributeParsers(types, null, logger), types,
            logger, designTime);
    doc = docHelper.documentFor(domString, null);
    item = (Element) doc.getDocumentElement().getElementsByTagName("import").item(0);
    elm = elemProvider.get(item);
    JClassType aClass = types.findType(baseClass);
    ResourceOracle resourceOracle = new MockResourceOracle();
    GssOptions gssOptions = new GssOptions(true, AutoConversionMode.OFF, true);
    writer = new UiBinderWriter(aClass, "bar", "", types, logger, fieldManager, null,
        DesignTimeUtilsStub.EMPTY, new UiBinderContext(), true, true, "", resourceOracle,
        gssOptions);
    parser = new UiBinderParser(writer, null, fieldManager, types, null, "", new UiBinderContext(),
        resourceOracle, gssOptions);
    designTime.rememberPathForElements(doc);
    UiBinderParser.Resource.IMPORT.create(parser, elm);
  }
}
