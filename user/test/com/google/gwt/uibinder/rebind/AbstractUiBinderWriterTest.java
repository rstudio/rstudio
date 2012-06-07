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

import com.google.gwt.codegen.server.AbortablePrintWriter;
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
import com.google.gwt.uibinder.rebind.messages.MessagesWriter;
import com.google.gwt.uibinder.test.UiJavaResources;

import junit.framework.TestCase;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXParseException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Set;

/**
 * Base class for tests that exercise {@link UiBinderWriter}.
 */
public abstract class AbstractUiBinderWriterTest extends TestCase {

  public static final MockJavaResource CLIENT_BUNDLE = new MockJavaResource(
      "com.google.gwt.resources.client.ClientBundle") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.resources.client;\n");
      code.append("public interface ClientBundle {");
      code.append("}");
      return code;
    }
  };
  public static final MockJavaResource DIV_ELEMENT = new MockJavaResource(
      "com.google.gwt.dom.client.DivElement") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.dom.client;\n");
      code.append("public class DivElement extends Element {");
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

  public static final MockJavaResource RENDERABLE_PANEL = new MockJavaResource(
      "com.google.gwt.user.client.ui.RenderablePanel") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.user.client.ui;\n");
      code.append("public class RenderablePanel {");
      code.append("}");
      return code;
    }
  };

  public static final MockJavaResource SPAN_ELEMENT = new MockJavaResource(
      "com.google.gwt.dom.client.SpanElement") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.dom.client;\n");
      code.append("public class SpanElement extends Element {");
      code.append("}");
      return code;
    }
  };

  public static final MockJavaResource UI_STYLE = new MockJavaResource("foo.UiStyle") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package foo;");
      code.append("public interface UiStyle {");
      code.append("}");
      return code;
    }
  };

  protected static final String BINDER_URI = "urn:ui:com.google.gwt.uibinder";

  protected static final W3cDomHelper docHelper = new W3cDomHelper(TreeLogger.NULL,
        new MockResourceOracle());

  protected static final String RENDERER_BASE_CLASS_NAME = "renderer.OwnerClass.Renderer";

  protected static final String RENDERER_OWNER_CLASS_NAME = "renderer.OwnerClass";

  private static TreeLogger createCompileLogger() {
    PrintWriterTreeLogger logger = new PrintWriterTreeLogger(new PrintWriter(System.err, true));
    logger.setMaxDetail(TreeLogger.ERROR);
    return logger;
  }

  protected Document doc;
  protected PrintWriter printWriter;
  protected UiBinderWriter writer;
  protected UiBinderParser parser;
  protected XMLElementProvider elemProvider;
  protected XMLElement elm;
  protected FieldManager fieldManager;
  protected Element item;
  protected MockMortalLogger logger;
  protected Set<Resource> resources = new HashSet<Resource>();
  protected TypeOracle types;

  public AbstractUiBinderWriterTest() {
    super();
  }

  /**
   * @param name
   */
  public AbstractUiBinderWriterTest(String name) {
    super(name);
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    resources.addAll(UiJavaResources.getUiResources());
    printWriter = new AbortablePrintWriter(new PrintWriter(new StringWriter()));
  }

  protected void init(String domString, MockJavaResource rendererClass,
      MockJavaResource... otherClasses) throws SAXParseException, UnableToCompleteException {
    resources.add(RENDERABLE_PANEL);
    resources.add(CLIENT_BUNDLE);
    resources.add(DIV_ELEMENT);
    resources.add(SPAN_ELEMENT);
    resources.add(UI_STYLE);
    resources.add(FOO);
    resources.add(rendererClass);
    resources.addAll(Arrays.asList(otherClasses));
    CompilationState state = CompilationStateBuilder.buildFrom(createCompileLogger(), resources);
    types = state.getTypeOracle();
    logger = new MockMortalLogger();
    UiBinderContext uiBinderCtx = new UiBinderContext();
    fieldManager = new FieldManager(types, logger, true);
    String baseClass = RENDERER_BASE_CLASS_NAME;
    DesignTimeUtils designTime = DesignTimeUtilsStub.EMPTY;
    elemProvider =
        new XMLElementProviderImpl(new AttributeParsers(types, fieldManager, logger),
            types, logger, designTime);
    doc = docHelper.documentFor(domString, rendererClass.getPath());
    item = (Element) doc.getDocumentElement().getChildNodes().item(0);
    elm = elemProvider.get(item);
    JClassType aClass = types.findType(baseClass);
    MessagesWriter messages =
        new MessagesWriter(types, BINDER_URI, logger, rendererClass.getPath(), "rendererPackage",
            "rendererClassName");
    writer =
        new UiBinderWriter(aClass, "foo", "", types, logger, fieldManager, messages,
            DesignTimeUtilsStub.EMPTY, uiBinderCtx, true, true, BINDER_URI);
    parser = new UiBinderParser(writer, messages, fieldManager, types, null, BINDER_URI, new UiBinderContext());
    designTime.rememberPathForElements(doc);
  }
}