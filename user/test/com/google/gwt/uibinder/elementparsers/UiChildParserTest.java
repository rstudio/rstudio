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
package com.google.gwt.uibinder.elementparsers;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.uibinder.rebind.FieldWriter;
import com.google.gwt.uibinder.rebind.UiBinderContext;

import junit.framework.TestCase;

import org.xml.sax.SAXParseException;

/**
 * Tests {@link UiChildParser}.
 */
public class UiChildParserTest extends TestCase {
  private static final String CHILDREN = "#CHILDREN#";
  private static final String PARSED_TYPE = "com.google.gwt.user.client.ui.HasUiChildren";
  private static final String UIBINDER = "<g:HasUiChildren>" + CHILDREN
      + "</g:HasUiChildren>";

  public void testAddChildWithParameters() throws SAXParseException,
      UnableToCompleteException {
    String b = UIBINDER.replaceAll(CHILDREN,
        "<g:alimitedchild param1=\"1\"> <g:Label/> </g:alimitedchild>");
    ElementParserTester tester = getTester();
    
    FieldWriter w = tester.parse(b.toString());
    assertEquals(1, tester.writer.statements.size());
    assertEquals("Only call was: " + tester.writer.statements.get(0),
        "fieldName.addLimitedChild(<g:Label>, 1);",
        tester.writer.statements.get(0));
    assertNull("Parser should never set an initializer.", w.getInitializer());
  }

  public void testAddChildWithParametersMissing() throws SAXParseException,
      UnableToCompleteException {
    String b = UIBINDER.replaceAll(CHILDREN,
        "<g:alimitedchild> <g:Label/> </g:alimitedchild>");
    ElementParserTester tester = getTester();

    FieldWriter w = tester.parse(b);
    assertEquals(1, tester.writer.statements.size());
    assertEquals("Only call was: " + tester.writer.statements.get(0),
        "fieldName.addLimitedChild(<g:Label>, 0);",
        tester.writer.statements.get(0));
    assertNull("Parser should never set an initializer.", w.getInitializer());
  }

  public void testAddLimitedChildInvalid() throws SAXParseException, UnableToCompleteException {
    String b = UIBINDER.replaceAll(CHILDREN,
        "  <g:alimitedchild param1=\"1\"> <g:Label/> </g:alimitedchild>"
            + "  <g:alimitedchild param1=\"2\"> <g:Label/> </g:alimitedchild>");
    ElementParserTester tester = getTester();

    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException exception) {
      assertEquals(1, tester.writer.statements.size());
      assertEquals("Only call should be: " + tester.writer.statements.get(0),
          "fieldName.addLimitedChild(<g:Label>, 1);",
          tester.writer.statements.get(0));
    }
  }

  public void testAddNamedChild() throws SAXParseException,
      UnableToCompleteException {
    String b = UIBINDER.replaceAll(CHILDREN,
        "<g:achild > <g:Label/> </g:achild>");
    ElementParserTester tester = getTester();

    FieldWriter w = tester.parse(b.toString());
    assertEquals(1, tester.writer.statements.size());
    assertEquals("fieldName.addNamedChild(<g:Label>);",
        tester.writer.statements.get(0));
    assertNull("Parser should never set an initializer.", w.getInitializer());
  }

  public void testBadChild() throws SAXParseException,
      UnableToCompleteException {
    String b = UIBINDER.replaceAll(CHILDREN,
        "<g:badtag> <g:Label/> </g:badtag>");
    ElementParserTester tester = getTester();

    FieldWriter w = tester.parse(b);
    assertEquals("No children should have been consumed.", 0,
        tester.writer.statements.size());
    assertNull("Parser should never set an initializer.", w.getInitializer());

    // children tags should be lowercase only
    b = UIBINDER.replaceAll(CHILDREN, "<g:Child />");
    w = tester.parse(b);
    assertEquals("No children should have been consumed.", 0,
        tester.writer.statements.size());
    assertNull("Parser should never set an initializer.", w.getInitializer());
  }

  public void testBadParamType() throws SAXParseException, UnableToCompleteException {
    String b =
      UIBINDER.replaceAll(CHILDREN, "<g:aIntChild > <g:Widget/> </g:aIntChild>");
    ElementParserTester tester = getBadParamTester();

    try {
      tester.parse(b.toString());
      fail("Expected to choke on non-object param type.");
    } catch (UnableToCompleteException exception) {
      assertEquals(0, tester.writer.statements.size());
    }
  }

  public void testComposedParamTypeChild() throws SAXParseException, UnableToCompleteException {
    String b =
        UIBINDER.replaceAll(CHILDREN, "<g:aComposedParamTypeChild > <g:CheckBox/> </g:aComposedParamTypeChild>");
    ElementParserTester tester = getTester();

    FieldWriter w = tester.parse(b.toString());
    assertEquals(1, tester.writer.statements.size());
    assertEquals("fieldName.addComposedParamTypeChild(<g:CheckBox>);", tester.writer.statements.get(0));
    assertNull("Parser should never set an initializer.", w.getInitializer());
  }

  public void testIncompatibleChild() throws SAXParseException, UnableToCompleteException {
    String b = UIBINDER.replaceAll(CHILDREN, "<g:child> <div/> </g:child>");
    ElementParserTester tester = getTester();

    try {
      tester.parse(b);
      fail("Incompatible type should have thrown an error.");
    } catch (UnableToCompleteException exception) {
      assertEquals(0, tester.writer.statements.size());
    }
  }

  public void testInterfaceChild() throws SAXParseException, UnableToCompleteException {
    String b =
        UIBINDER.replaceAll(CHILDREN, "<g:anInterfaceChild > <g:IsWidget/> </g:anInterfaceChild>");
    ElementParserTester tester = getTester();

    FieldWriter w = tester.parse(b.toString());
    assertEquals(1, tester.writer.statements.size());
    assertEquals("fieldName.addInterfaceChild(<g:IsWidget>);", tester.writer.statements.get(0));
    assertNull("Parser should never set an initializer.", w.getInitializer());
  }

  public void testMultipleChildrenInvalid() throws SAXParseException, UnableToCompleteException {
    String b = UIBINDER.replaceAll(CHILDREN,
        "<g:child ><g:Label/><g:Label/></g:child>");
    ElementParserTester tester = getTester();

    try {
      tester.parse(b.toString());
      fail("Cannot have multiple children under an @UiChild tag.");
    } catch (UnableToCompleteException exception) {
      assertEquals(0, tester.writer.statements.size());
    }
  }

  public void testNonWidgetChild() throws SAXParseException, UnableToCompleteException {
    String b =
        UIBINDER.replaceAll(CHILDREN, "<g:aSpecificChild > <g:MenuItem/> </g:aSpecificChild>");
    ElementParserTester tester = getTester();

    FieldWriter w = tester.parse(b.toString());
    assertEquals(1, tester.writer.statements.size());
    assertEquals("fieldName.addSpecificTypeOfChild(<g:MenuItem>);", tester.writer.statements.get(0));
    assertNull("Parser should never set an initializer.", w.getInitializer());
  }

  public void testNoAttributes() throws SAXParseException, UnableToCompleteException {
    String b = UIBINDER.replaceAll(CHILDREN, "<g:achild ui:field='oops'> <g:Label/> </g:achild>");
    ElementParserTester tester = getTester();

    try {
      tester.parse(b.toString());
      fail("Expected to choke on disallowed parameter.");
    } catch (UnableToCompleteException exception) {
      assertEquals(0, tester.writer.statements.size());
    }
  }
  
  public void testNoParameterChild() throws SAXParseException,
      UnableToCompleteException {
    String b = UIBINDER.replaceAll(CHILDREN, "<g:child> <g:Label/> </g:child>");
    ElementParserTester tester = getTester();

    FieldWriter w = tester.parse(b);
    assertEquals(1, tester.writer.statements.size());
    assertNull("Parser should never set an initializer.", w.getInitializer());
    assertEquals("fieldName.addChild(<g:Label>);",
        tester.writer.statements.get(0));
  }
  
  public void testParamTypeChild() throws SAXParseException, UnableToCompleteException {
    String b =
        UIBINDER.replaceAll(CHILDREN, "<g:aParamTypeChild > <g:ParamTypeImpl/> </g:aParamTypeChild>");
    ElementParserTester tester = getTester();

    FieldWriter w = tester.parse(b.toString());
    assertEquals(1, tester.writer.statements.size());
    assertEquals("fieldName.addParamTypeChild(<g:ParamTypeImpl>);", tester.writer.statements.get(0));
    assertNull("Parser should never set an initializer.", w.getInitializer());
  }

  public void testTypeMismatchChild() throws SAXParseException, UnableToCompleteException {
    String b =
      UIBINDER.replaceAll(CHILDREN, "<g:aSpecificChild > <g:Widget/> </g:aSpecificChild>");
    ElementParserTester tester = getTester();

    try {
      tester.parse(b.toString());
      fail("Expected to choke on type mismatch.");
    } catch (UnableToCompleteException exception) {
      assertEquals(0, tester.writer.statements.size());
    }
  }
  
  private ElementParserTester getBadParamTester() throws UnableToCompleteException {
    MockJavaResource invalidParamType = new MockJavaResource(PARSED_TYPE) {
      @Override
      public CharSequence getContent() {
        StringBuffer code = new StringBuffer();
        code.append("package com.google.gwt.user.client.ui;\n");
        code.append("import com.google.gwt.uibinder.client.UiChild;\n");
        code.append("public class HasUiChildren {\n");
        code.append("  @UiChild\n");
        code.append("  void addIntChild(int child) {\n");
        code.append("  }\n\n");
        code.append("}\n");
        return code;
      }
    };
    return new ElementParserTester(PARSED_TYPE,
        new UiChildParser(new UiBinderContext()), invalidParamType);
  }

  private ElementParserTester getTester() throws UnableToCompleteException {
    MockJavaResource editor = new MockJavaResource("com.google.gwt.user.client.ui.Editor") {
      @Override
      public CharSequence getContent() {
        StringBuffer code = new StringBuffer();
        code.append("package com.google.gwt.user.client.ui;\n");
        code.append("public interface Editor<T>{\n");
        code.append("}\n");
        return code;
      }
    };
    MockJavaResource checkBox = new MockJavaResource("com.google.gwt.user.client.ui.CheckBox") {
      @Override
      public CharSequence getContent() {
        StringBuffer code = new StringBuffer();
        code.append("package com.google.gwt.user.client.ui;\n");
        code.append("public class CheckBox extends Widget implements Editor<Boolean> {");
        code.append("}\n");
        return code;
      }
    };
    MockJavaResource paramType = new MockJavaResource("com.google.gwt.user.client.ui.ParamType") {
      @Override
      public CharSequence getContent() {
        StringBuffer code = new StringBuffer();
        code.append("package com.google.gwt.user.client.ui;\n");
        code.append("public interface ParamType<T> {}\n");
        return code;
      }
    };

    MockJavaResource paramTypeImpl =
        new MockJavaResource("com.google.gwt.user.client.ui.ParamTypeImpl") {
          @Override
          public CharSequence getContent() {
            StringBuffer code = new StringBuffer();
            code.append("package com.google.gwt.user.client.ui;\n");
            code.append("public class ParamTypeImpl<T> implements ParamType<T> {}\n");
            return code;
          }
        };

    MockJavaResource hasUiChildren = new MockJavaResource(PARSED_TYPE) {
      @Override
      public CharSequence getContent() {
        StringBuffer code = new StringBuffer();
        code.append("package com.google.gwt.user.client.ui;\n");
        code.append("import com.google.gwt.uibinder.client.UiChild;\n");
        code.append("import com.google.gwt.event.dom.client.ClickHandler;\n");
        code.append("public class HasUiChildren<T> {\n");

        code.append("  @UiChild\n");
        code.append("  void addChild(Object child) {}\n\n");

        code.append("  @UiChild(tagname=\"achild\")\n");
        code.append("  void addNamedChild(Object child) {}\n\n");

        code.append("  @UiChild(tagname=\"alimitedchild\", limit=1)\n");
        code.append("  void addLimitedChild(Object child, int param1) {}\n\n");

        code.append("  @UiChild(tagname=\"aSpecificChild\")\n");
        code.append("  void addSpecificTypeOfChild(MenuItem child) {}\n\n");

        code.append("  @UiChild(tagname=\"anInterfaceChild\")\n");
        code.append("  void addInterfaceChild(IsWidget child) {}\n\n");

        code.append("  @UiChild(tagname=\"aParamTypeChild\")\n");
        code.append("  void addParamTypeChild(ParamType<T> child) {}\n\n");

        code.append("  @UiChild(tagname=\"aComposedParamTypeChild\")\n");
        code.append("  <W extends IsWidget & Editor<T>> void \n");
        code.append("  addComposedParamTypeChild(W child) {}\n\n");
        code.append("}\n");
        return code;
      }
    };

    return new ElementParserTester(PARSED_TYPE, new UiChildParser(new UiBinderContext()),
        paramType, paramTypeImpl, editor, checkBox, hasUiChildren);
  }
}
