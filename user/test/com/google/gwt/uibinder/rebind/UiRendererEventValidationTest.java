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

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;

import org.xml.sax.SAXParseException;

/**
 * Tests UiBinderWriter validation of UiRender onBrowserEvent methods.
 */
public class UiRendererEventValidationTest extends AbstractUiBinderWriterTest {

  private static String UI_XML = "<ui:UiBinder xmlns:ui='urn:ui:com.google.gwt.uibinder'>"
      + "<ui:with field='withField' />" + "  <div ui:field='root'>"
      + "    <span ui:field='someField'><ui:text from='{withField.toString}'/></span>" + "  </div>"
      + "</ui:UiBinder>";

  private StringBuffer declaredMethods = new StringBuffer();

  public void testAGoodUiRendererInterface() throws UnableToCompleteException, SAXParseException {
    declaredMethods.append("    public void onBrowserEvent(FooReceiver f, NativeEvent e,"
        + "Element p);");

    init(UI_XML, generateRendererResource(declaredMethods), generateReceiverResource("FooReceiver",
        "@UiHandler({\"root\", \"someField\"}) public void receiver1(ClickEvent e,"
            + "    Element p) {", "}"));
    writer.parseDocument(doc, printWriter);
  }

  public void testEventHandlerTooFewParams() throws UnableToCompleteException, SAXParseException {
    declaredMethods.append("    public void onBrowserEvent(FooReceiver f, NativeEvent e,"
        + "Element p);");

    init(UI_XML, generateRendererResource(declaredMethods), generateReceiverResource("FooReceiver",
        "@UiHandler({\"root\", \"someField\"}) public void receiver1(ClickEvent e, Element p,"
            + "    int tooMuch) {", "}"));
    assertParseFailure("Expected failure due to receiver1() having too few parameters.",
        "Too many parameters in \"void receiver1(com.google.gwt.event.dom.client.ClickEvent e,"
            + " com.google.gwt.dom.client.Element p, int tooMuch)\" of renderer.FooReceiver");
  }

  public void testEventHandlerTooManyParamsOk() throws UnableToCompleteException, SAXParseException {
    declaredMethods.append("    public void onBrowserEvent(FooReceiver f, NativeEvent e,"
        + "Element p, int oneMore);");

    init(UI_XML, generateRendererResource(declaredMethods), generateReceiverResource("FooReceiver",
        "@UiHandler({\"root\", \"someField\"}) public void receiver1(ClickEvent e,"
            + "    Element p) {", "}"));
    writer.parseDocument(doc, printWriter);
  }

  public void testEventHandlerTooManyParamsOkEvenNoElement() throws UnableToCompleteException, SAXParseException {
    declaredMethods.append("    public void onBrowserEvent(FooReceiver f, NativeEvent e,"
        + "Element p, int oneMore);");

    init(UI_XML, generateRendererResource(declaredMethods), generateReceiverResource("FooReceiver",
        "@UiHandler({\"root\", \"someField\"}) public void receiver1(ClickEvent e) {",
        "}"));
    writer.parseDocument(doc, printWriter);
  }

  public void testHandlerBadParam1() throws UnableToCompleteException, SAXParseException {
    declaredMethods.append("    public void onBrowserEvent(FooReceiver f, NativeEvent e,"
        + "Element p);");

    init(UI_XML, generateRendererResource(declaredMethods), generateReceiverResource("FooReceiver",
        "@UiHandler({\"root\"}) public void receiver1(String e," + "    Element p) {", "}"));
    assertParseFailure("Expected failure due to handler method with bad first parameter type.",
        "First parameter must be assignable to com.google.gwt.dom.client.DomEvent in"
            + " \"void receiver1(java.lang.String e, com.google.gwt.dom.client.Element p)\""
            + " of renderer.FooReceiver");
  }

  public void testHandlerBadParam2() throws UnableToCompleteException, SAXParseException {
    declaredMethods.append("    public void onBrowserEvent(FooReceiver f, NativeEvent e,"
        + "Element p);");

    init(UI_XML, generateRendererResource(declaredMethods), generateReceiverResource("FooReceiver",
        "@UiHandler({\"root\"}) public void receiver1(ClickEvent e," + "    int j) {", "}"));
    assertParseFailure(
        "Expected failure due to handler method with  bad second parameter type.",
        "Parameter j in \"void receiver1(com.google.gwt.event.dom.client.ClickEvent e, int j)\""
            + " of renderer.FooReceiver is not of the same type as parameter p in"
            + " \"void onBrowserEvent(renderer.FooReceiver f, com.google.gwt.dom.client.NativeEvent e,"
            + " com.google.gwt.dom.client.Element p)\" of renderer.OwnerClass.Renderer");
  }

  public void testNoUnknownFieldInUiHandler() throws UnableToCompleteException, SAXParseException {
    declaredMethods.append("    public void onBrowserEvent(FooReceiver f, NativeEvent e,"
        + "Element p);");

    init(UI_XML, generateRendererResource(declaredMethods), generateReceiverResource("FooReceiver",
        "@UiHandler({\"unknown\"}) public void receiver1(ClickEvent e," + "    Element p) {", "}"));
    assertParseFailure("Expected failure due to @UiHabndler containing unkown field.",
        "\"unknown\" is not a known field name as listed in the @UiHandler annotation in"
            + " \"void receiver1(com.google.gwt.event.dom.client.ClickEvent e,"
            + " com.google.gwt.dom.client.Element p)\" of renderer.FooReceiver");
  }

  public void testOnBrowserEventBadSignatureOneParam() throws UnableToCompleteException,
      SAXParseException {
    declaredMethods.append("    public void onBrowserEvent(FooReceiver f);");

    init(UI_XML, generateRendererResource(declaredMethods), generateReceiverResource("FooReceiver"));
    assertParseFailure("Expected failure due to onBrowserEvent() having too few parameters.",
        "Too few parameters in \"void onBrowserEvent(renderer.FooReceiver f)\" of"
            + " renderer.OwnerClass.Renderer");
  }

  public void testOnBrowserEventBadSignatureParamType1() throws UnableToCompleteException,
      SAXParseException {
    declaredMethods.append("    public void onBrowserEvent(int f, NativeEvent e,"
        + "    Element p);");

    init(UI_XML, generateRendererResource(declaredMethods), generateReceiverResource("FooReceiver"));
    assertParseFailure("Expected failure due to onBrowserEvent() having bad second parameter.",
        "First parameter must be a class or interface in"
            + " \"void onBrowserEvent(int f, com.google.gwt.dom.client.NativeEvent e,"
            + " com.google.gwt.dom.client.Element p)\" of renderer.OwnerClass.Renderer");
  }

  public void testOnBrowserEventBadSignatureParamType2() throws UnableToCompleteException,
      SAXParseException {
    declaredMethods.append("    public void onBrowserEvent(FooReceiver f, String e,"
        + "    Element p);");

    init(UI_XML, generateRendererResource(declaredMethods), generateReceiverResource("FooReceiver"));
    assertParseFailure("Expected failure due to onBrowserEvent()  having bad first parameter.",
        "Second parameter must be of type com.google.gwt.dom.client.NativeEvent in"
            + " \"void onBrowserEvent(renderer.FooReceiver f, java.lang.String e,"
            + " com.google.gwt.dom.client.Element p)\" of renderer.OwnerClass.Renderer");
  }

  public void testOnBrowserEventBadSignatureParamType3() throws UnableToCompleteException,
      SAXParseException {
    declaredMethods.append("    public void onBrowserEvent(FooReceiver f, NativeEvent e,"
        + "    String p);");

    init(UI_XML, generateRendererResource(declaredMethods), generateReceiverResource("FooReceiver"));
    assertParseFailure(
        "Expected failure due to onBrowserEvent() having bad third parameter.",
        "Third parameter must be of type com.google.gwt.dom.client.Element in"
            + " \"void onBrowserEvent(renderer.FooReceiver f, com.google.gwt.dom.client.NativeEvent"
            + " e, java.lang.String p)\" of renderer.OwnerClass.Renderer");
  }

  public void testOnBrowserEventBadSignatureTwoParams() throws UnableToCompleteException,
      SAXParseException {
    declaredMethods.append("    public void onBrowserEvent(FooReceiver f, NativeEvent e);");

    init(UI_XML, generateRendererResource(declaredMethods), generateReceiverResource("FooReceiver"));
    assertParseFailure("Expected failure due to onBrowserEvent() having too few parameters.",
        "Too few parameters in \"void onBrowserEvent(renderer.FooReceiver f,"
            + " com.google.gwt.dom.client.NativeEvent e)\" of renderer.OwnerClass.Renderer");
  }

  public void testTwoHandlers() throws UnableToCompleteException, SAXParseException {
    declaredMethods.append("    public void onBrowserEvent(FooReceiver f, NativeEvent e,"
        + "Element p);");
    declaredMethods.append("    public void onBrowserEvent(BarReceiver b, NativeEvent e,"
        + "Element p);");

    init(UI_XML, generateRendererResource(declaredMethods), generateReceiverResource("FooReceiver",
        "@UiHandler({\"root\", \"someField\"}) public void receiver1(ClickEvent e,"
            + "    Element p) {", "}"), generateReceiverResource("BarReceiver",
        "@UiHandler({\"root\", \"someField\"}) public void receiver1(ClickEvent e,"
            + "    Element p) {", "}"));
    writer.parseDocument(doc, printWriter);
  }

  private void assertParseFailure(String message, String expectedMessage) {
    try {
      writer.parseDocument(doc, printWriter);
      fail(message);
    } catch (UnableToCompleteException e) {
      if (expectedMessage != null) {
        assertEquals(expectedMessage, logger.died);
      }
    }
  }

  private MockJavaResource generateReceiverResource(final String className,
      final String... contents) {
    return new MockJavaResource("renderer." + className) {
      @Override
      public CharSequence getContent() {
        StringBuffer code = new StringBuffer();
        code.append("package renderer;\n");
        code.append("import com.google.gwt.safehtml.shared.SafeHtmlBuilder;\n");
        code.append("import com.google.gwt.uibinder.client.UiRenderer;\n");
        code.append("import com.google.gwt.dom.client.Element;\n");
        code.append("import com.google.gwt.uibinder.client.UiHandler;\n");
        code.append("import com.google.gwt.event.dom.client.ClickEvent;\n");
        code.append("import foo.Foo;\n");
        code.append("public class " + className + " {\n");
        for (String statement : contents) {
          code.append(statement);
          code.append("\n");
        }
        code.append("}\n");
        return code;
      }
    };
  }

  private MockJavaResource generateRendererResource(final StringBuffer declarations) {
    return new MockJavaResource("renderer.OwnerClass") {
      @Override
      public CharSequence getContent() {
        StringBuffer code = new StringBuffer();
        code.append("package renderer;\n");
        code.append("import com.google.gwt.safehtml.shared.SafeHtmlBuilder;\n");
        code.append("import com.google.gwt.uibinder.client.UiRenderer;\n");
        code.append("import com.google.gwt.dom.client.DivElement;\n");
        code.append("import com.google.gwt.dom.client.Element;\n");
        code.append("import com.google.gwt.dom.client.NativeEvent;\n");
        code.append("import com.google.gwt.dom.client.SpanElement;\n");
        code.append("import foo.Foo;\n");
        code.append("public class OwnerClass {");
        code.append("  public interface Renderer");
        code.append("      extends UiRenderer {");
        code.append("    public void render(SafeHtmlBuilder sb, foo.Foo withField);");
        code.append("    public DivElement getRoot(Element foo);");
        code.append("    public SpanElement getSomeField(Element bar);");
        code.append(declarations);
        code.append("  }");
        code.append("}");
        return code;
      }
    };
  }

}
