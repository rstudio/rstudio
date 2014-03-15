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
 * Tests UiBinderWriter validation of UiRender getters and render method.
 */
public class UiRendererValidationTest extends AbstractUiBinderWriterTest {

  private static String UI_XML = "<ui:UiBinder xmlns:ui='urn:ui:com.google.gwt.uibinder'>"
      + "<ui:style field='styleField'>.foo {color:black;}</ui:style>"
      + "<ui:with field='withField' />"
      + "  <div ui:field='root'>"
      + "    <span ui:field='someField'><ui:text from='{withField.toString}'/></span>"
      + "  </div>"
      + "</ui:UiBinder>";

  private static String UI_XML_TYPED_WITH = "<ui:UiBinder xmlns:ui='urn:ui:com.google.gwt.uibinder'>"
    + "<ui:with field='withField' type='foo.Foo' />"
    + "  <div ui:field='root'>"
    + "    <span ui:field='someField'><ui:text from='{withField.toString}'/></span>"
    + "  </div>"
    + "</ui:UiBinder>";

  private StringBuffer declaredMethods = new StringBuffer();

  public void testAGoodUiRendererInterface() throws UnableToCompleteException, SAXParseException {
    declaredMethods.append("    public void render(SafeHtmlBuilder sb, foo.Foo withField);");
    declaredMethods.append("    public DivElement getRoot(Element foo);");
    declaredMethods.append("    public SpanElement getSomeField(Element bar);");
    declaredMethods.append("    public UiStyle getStyleField();");
    init(UI_XML, generateRendererResource(declaredMethods));
    writer.parseDocument(doc, printWriter);
  }

  public void testGetterBadParameterType() throws SAXParseException, UnableToCompleteException {
    initGetterTest();
    declaredMethods.append("    public DivElement getRoot(String bar);");
    init(UI_XML, generateRendererResource(declaredMethods));

    assertParseFailure("Expected failure due to getter parameter not of type Element.",
        "Getter getRoot must have exactly one parameter of type assignable to "
            + "com.google.gwt.dom.client.Element in renderer.OwnerClass.Renderer");
  }

  public void testGetterParameterAssignableToElement()
      throws SAXParseException, UnableToCompleteException {
    initGetterTest();
    declaredMethods.append("    public DivElement getRoot(DivElement bar);");
    init(UI_XML, generateRendererResource(declaredMethods));

    writer.parseDocument(doc, printWriter);
  }

  public void testGetterNoParameters() throws SAXParseException, UnableToCompleteException {
    initGetterTest();
    declaredMethods.append("    public DivElement getRoot();");
    init(UI_XML, generateRendererResource(declaredMethods));

    assertParseFailure("Expected failure due to getter with no parameters.",
        "Field getter getRoot must have exactly one parameter in renderer.OwnerClass.Renderer");
  }

  public void testStyleGetterWithParameter() throws SAXParseException, UnableToCompleteException {
    initGetterTest();
    declaredMethods.append("    public UiStyle getStyleField(DivElement bar);");
    init(UI_XML, generateRendererResource(declaredMethods));

    assertParseFailure("Expected failure due to style getter with a parameter.",
        "Style getter getStyleField must have no parameters in renderer.OwnerClass.Renderer");
  }

  public void testGetterTooManyParameters() throws SAXParseException, UnableToCompleteException {
    initGetterTest();
    declaredMethods.append("    public DivElement getRoot(Element parent, Integer i);");
    init(UI_XML, generateRendererResource(declaredMethods));

    assertParseFailure("Expected failure due to bad getter signature.",
        "Field getter getRoot must have exactly one parameter in renderer.OwnerClass.Renderer");
  }

  public void testGetterUnknownField() throws SAXParseException, UnableToCompleteException {
    initGetterTest();
    declaredMethods.append("    public DivElement getQuux(Element parent);");
    init(UI_XML, generateRendererResource(declaredMethods));
    assertParseFailure("Expected failure due to getter for an unexpected field name.",
        "getQuux does not match a \"ui:field='quux'\" declaration in renderer.OwnerClass.Renderer,"
            + " or 'quux' refers to something other than a ui:style or an HTML element in"
            + " the template");
  }

  public void testRenderBadReturnType() throws SAXParseException, UnableToCompleteException {
    declaredMethods.append("    public String render(SafeHtmlBuilder sb, foo.Foo withField);");
    init(UI_XML, generateRendererResource(declaredMethods));
    assertParseFailure("Expected failure due to bad render() return type.",
        "renderer.OwnerClass.Renderer#render(SafeHtmlBuilder ...) does not return void");
  }

  public void testRenderExtraParametersOk() throws SAXParseException, UnableToCompleteException {
    declaredMethods.append("    public void render("
        + "SafeHtmlBuilder sb, foo.Foo withField, foo.Foo withUnknownField);");
    init(UI_XML, generateRendererResource(declaredMethods));
    writer.parseDocument(doc, printWriter);
  }

  public void testRenderMethodRepeated() throws SAXParseException, UnableToCompleteException {
    declaredMethods.append("    public void render(SafeHtmlBuilder sb, foo.Foo withField);");
    declaredMethods.append("    public void render(SafeHtmlBuilder sb);");
    init(UI_XML, generateRendererResource(declaredMethods));
    assertParseFailure("Expected failure due to more than one render method.",
        "renderer.OwnerClass.Renderer declares more than one method named render");
  }

  public void testRenderMissing() throws SAXParseException, UnableToCompleteException {
    // No render method here
    init(UI_XML_TYPED_WITH, generateRendererResource(declaredMethods));
    assertParseFailure("Expected failure due to missing render method.",
        "renderer.OwnerClass.Renderer does not declare a render(SafeHtmlBuilder ...) method");
  }

  public void testRenderNoParameters() throws SAXParseException, UnableToCompleteException {
    declaredMethods.append("    public void render();");
    init(UI_XML_TYPED_WITH, generateRendererResource(declaredMethods));
    assertParseFailure("Expected failure due to render with no parameters.",
        "renderer.OwnerClass.Renderer does not declare a render(SafeHtmlBuilder ...) method");
  }

  public void testRenderNoParametersRepeated() throws SAXParseException, UnableToCompleteException {
    declaredMethods.append("    public void render(SafeHtmlBuilder sb, foo.Foo withField);");
    declaredMethods.append("    public void render();");
    init(UI_XML_TYPED_WITH, generateRendererResource(declaredMethods));
    assertParseFailure("Expected failure due to render method repeated.",
        "renderer.OwnerClass.Renderer declares more than one method named render");
  }

  public void testUnknownMethodDeclaration() throws SAXParseException, UnableToCompleteException {
    initGetterTest();
    declaredMethods.append("    public void quux();");
    init(UI_XML_TYPED_WITH, generateRendererResource(declaredMethods));
    assertParseFailure("Expected failure due to unexpected method.",
        "Unexpected method \"quux\" found in renderer.OwnerClass.Renderer");
  }

  private void assertParseFailure(String message, String expectedMessage) {
    try {
      writer.parseDocument(doc, printWriter);
      fail(message);
    } catch (UnableToCompleteException e) {
      if (expectedMessage != null) {
        assertEquals(expectedMessage, logger.died);
      } else {
        fail("No message was received. " + message);
      }
    }
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
        code.append("import com.google.gwt.dom.client.SpanElement;\n");
        code.append("import foo.Foo;\n");
        code.append("import foo.UiStyle;\n");
        code.append("public class OwnerClass {");
        code.append("  public interface Renderer");
        code.append("      extends UiRenderer {");
        code.append(declarations);
        code.append("  }");
        code.append("}");
        return code;
      }
    };
  }

  private void initGetterTest() {
    // Required, not part of test
    declaredMethods.append("    public void render(SafeHtmlBuilder sb, foo.Foo withField);");
  }
}
