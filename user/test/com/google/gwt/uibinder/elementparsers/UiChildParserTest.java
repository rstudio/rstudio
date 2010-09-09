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
import com.google.gwt.dev.javac.impl.MockJavaResource;
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
  private ElementParserTester tester;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    MockJavaResource itemSubclass = new MockJavaResource(PARSED_TYPE) {
      @Override
      protected CharSequence getContent() {
        StringBuffer code = new StringBuffer();
        code.append("package com.google.gwt.user.client.ui;\n");
        code.append("import com.google.gwt.uibinder.client.UiChild;\n");
        code.append("public class HasUiChildren {\n");
        code.append("  public HasUiChildren() {\n");
        code.append("  }\n\n");
        code.append("  @UiChild\n");
        code.append("  void addChild(Object child) {\n");
        code.append("  }\n\n");
        code.append("  @UiChild(tagname=\"achild\")\n");
        code.append("  void addNamedChild(Object child) {\n");
        code.append("  }\n\n");
        code.append("  @UiChild(tagname=\"alimitedchild\", limit=1)\n");
        code.append("  void addLimitedChild(Object child, int param1) {\n");
        code.append("  }\n\n");
        code.append("}\n");
        return code;
      }
    };
    UiBinderContext uiBinderCtx = new UiBinderContext();
    tester = new ElementParserTester(PARSED_TYPE,
        new UiChildParser(uiBinderCtx), itemSubclass);
  }

  public void testAddChildWithParameters() throws SAXParseException,
      UnableToCompleteException {
    String b = UIBINDER.replaceAll(CHILDREN,
        "<g:alimitedchild param1=\"1\"> <g:Label/> </g:alimitedchild>");

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
    FieldWriter w = tester.parse(b);
    assertEquals(1, tester.writer.statements.size());
    assertEquals("Only call was: " + tester.writer.statements.get(0),
        "fieldName.addLimitedChild(<g:Label>, 0);",
        tester.writer.statements.get(0));
    assertNull("Parser should never set an initializer.", w.getInitializer());
  }

  public void testAddLimitedChildInvalid() throws SAXParseException {
    String b = UIBINDER.replaceAll(CHILDREN,
        "  <g:alimitedchild param1=\"1\"> <g:Label/> </g:alimitedchild>"
            + "  <g:alimitedchild param1=\"2\"> <g:Label/> </g:alimitedchild>");
    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException exception) {
      assertEquals(1, tester.writer.statements.size());
      assertEquals("Only call was: " + tester.writer.statements.get(0),
          "fieldName.addLimitedChild(<g:Label>, 1);",
          tester.writer.statements.get(0));
    }
  }

  public void testAddNamedChild() throws SAXParseException,
      UnableToCompleteException {
    String b = UIBINDER.replaceAll(CHILDREN,
        "<g:achild > <g:Label/> </g:achild>");

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

  public void testIncompatibleChild() throws SAXParseException,
      UnableToCompleteException {
    String b = UIBINDER.replaceAll(CHILDREN, "<g:child> <div/> </g:child>");

    try {
      FieldWriter w = tester.parse(b);
      fail("Incompatible type should have thrown an error.");
    } catch (UnableToCompleteException exception) {
      assertEquals(0, tester.writer.statements.size());
    }
  }

  public void testMultipleChildrenInvalid() throws SAXParseException,
      UnableToCompleteException {
    String b = UIBINDER.replaceAll(CHILDREN,
        "<g:child ><g:Label/><g:Label/></g:child>");

    try {
      FieldWriter w = tester.parse(b.toString());
      fail("Cannot have multiple children under an @UiChild tag.");
    } catch (UnableToCompleteException exception) {
      assertEquals(0, tester.writer.statements.size());
    }
  }

  public void testNoParameterChild() throws SAXParseException,
      UnableToCompleteException {
    String b = UIBINDER.replaceAll(CHILDREN, "<g:child> <g:Label/> </g:child>");

    FieldWriter w = tester.parse(b);
    assertEquals(1, tester.writer.statements.size());
    assertNull("Parser should never set an initializer.", w.getInitializer());
    assertEquals("fieldName.addChild(<g:Label>);",
        tester.writer.statements.get(0));
  }
}
