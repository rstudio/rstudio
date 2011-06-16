/*
 * Copyright 2009 Google Inc.
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
import com.google.gwt.user.client.ui.DialogBox;

import junit.framework.TestCase;

import org.xml.sax.SAXException;

import java.util.Iterator;

/**
 * A unit test. Guess what of.
 */
public class DialogBoxParserTest extends TestCase {

  private static final String PARSED_TYPE = "com.google.gwt.user.client.ui.DialogBox";
  private static final MockJavaResource CAPTION_SUBCLASS = new MockJavaResource(
      "my.MyCaption") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package my;\n");
      code.append("import com.google.gwt.user.client.ui.DialogBox;\n");
      code.append("import com.google.gwt.user.client.ui.Widget;\n");
      code.append("public class MyCaption extends Widget implements DialogBox.Caption {\n");
      code.append("  public MyCaption() { super(); } \n");;
      code.append("}\n");
      return code;
    }
  };
  private static final MockJavaResource DIALOG_SUBCLASS = new MockJavaResource(
      "my.MyDialogBox") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package my;\n");
      code.append("import com.google.gwt.user.client.ui.DialogBox;\n");
      code.append("public class MyDialogBox extends DialogBox {\n");
      code.append("  public MyDialogBox() { super(false, true); }");
      code.append("}\n");
      return code;
    }
  };

  private ElementParserTester tester;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    tester = new ElementParserTester(PARSED_TYPE, new DialogBoxParser());
  }

  public void testHappy() throws UnableToCompleteException, SAXException {
    StringBuffer b = new StringBuffer();

    b.append("<g:DialogBox autoHide='true' modal='true'>");
    b.append("  <g:caption>Hello, I <b>caption</b>you.</g:caption>");
    b.append("  <g:Label>And your little dog, too!</g:Label>");
    b.append("</g:DialogBox> ");

    String[] expected = {
        "fieldName.setHTML(\"@mockToken-" + ElementParserTester.FIELD_NAME
            + "-Hello, I <b>caption</b>you.\");",
        "fieldName.setWidget(<g:Label>);",};

    FieldWriter w = tester.parse(b.toString());
    assertEquals("new " + PARSED_TYPE + "(true, true)", w.getInitializer());

    Iterator<String> i = tester.writer.statements.iterator();
    for (String e : expected) {
      assertEquals(e, i.next());
    }
    assertFalse(i.hasNext());
    assertNull(tester.logger.died);
  }

  public void testNoCaption() throws UnableToCompleteException, SAXException {
    StringBuffer b = new StringBuffer();

    b.append("<g:DialogBox autoHide='true' modal='true'>");
    b.append("  <g:Label>And your little dog, too!</g:Label>");
    b.append("</g:DialogBox> ");

    FieldWriter w = tester.parse(b.toString());
    assertEquals("new " + PARSED_TYPE + "(true, true)", w.getInitializer());

    Iterator<String> i = tester.writer.statements.iterator();
    assertEquals("fieldName.setWidget(<g:Label>);", i.next());
    assertFalse(i.hasNext());
    assertNull(tester.logger.died);
  }

  public void testDefaults() throws UnableToCompleteException, SAXException {
    StringBuffer b = new StringBuffer();

    b.append("<g:DialogBox/> ");

    FieldWriter w = tester.parse(b.toString());
    assertEquals("new " + PARSED_TYPE + "(false, true)", w.getInitializer());

    Iterator<String> i = tester.writer.statements.iterator();
    assertFalse(i.hasNext());
    assertNull(tester.logger.died);
  }

  public void testCustomCaptionAndCaption() throws SAXException, 
      UnableToCompleteException {
    StringBuffer b = new StringBuffer();

    DialogBoxParser parser = new DialogBoxParser();
    String typeName = DialogBox.class.getCanonicalName();
    tester = new ElementParserTester(typeName, parser, CAPTION_SUBCLASS);

    b.append("<ui:UiBinder xmlns:ui='" + ElementParserTester.BINDER_URI + "'");
    b.append("    xmlns:my='urn:import:my'");
    b.append("    xmlns:g='urn:import:com.google.gwt.user.client.ui'>");
    b.append("  <g:DialogBox autoHide='true' modal='true'>");
    b.append("    <g:caption>Hello, I <b>caption</b>you.</g:caption>");
    b.append("    <g:customCaption><my:MyCaption/></g:customCaption>");
    b.append("    <g:Label>And your little dog, too!</g:Label>");
    b.append("  </g:DialogBox> ");
    b.append("</ui:UiBinder>");

    try {
      parser.parse(tester.getElem(b.toString(), "g:DialogBox"), "fieldName",
          tester.parsedType, tester.writer);
      fail();
    } catch (UnableToCompleteException e) {
      assertNotNull(tester.logger.died);
    }
  }

  public void testWidgetInCaption() throws SAXException {
    StringBuffer b = new StringBuffer();

    b.append("<g:DialogBox autoHide='true' modal='true'>");
    b.append("  <g:caption> ");
    b.append("    <g:Label>And your little dog, too!</g:Label>");
    b.append("  </g:caption> ");
    b.append("</g:DialogBox> ");

    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException e) {
      assertNotNull(tester.logger.died);
    }
  }

  public void testTooManyCaptionWidgets() throws SAXException,
      UnableToCompleteException {

    StringBuffer b = new StringBuffer();
    DialogBoxParser parser = new DialogBoxParser();
    String typeName = DialogBox.class.getCanonicalName();
    tester = new ElementParserTester(typeName, parser, CAPTION_SUBCLASS);

    b.append("<ui:UiBinder xmlns:ui='" + ElementParserTester.BINDER_URI + "'");
    b.append("    xmlns:my='urn:import:my'");
    b.append("    xmlns:g='urn:import:com.google.gwt.user.client.ui'>");
    b.append("  <g:DialogBox autoHide='true' modal='true'>");
    b.append("    <g:customCaption><my:MyCaption/></g:customCaption>");
    b.append("    <g:customCaption><my:MyCaption/></g:customCaption>");
    b.append("  </g:DialogBox> ");
    b.append("</ui:UiBinder>");

    try {
      parser.parse(tester.getElem(b.toString(), "g:DialogBox"), "fieldName",
          tester.parsedType, tester.writer);
      fail();
    } catch (UnableToCompleteException e) {
      assertNotNull(tester.logger.died);
    }
  }

  public void testCustomCaption() throws SAXException, UnableToCompleteException {
    StringBuffer b = new StringBuffer();

    DialogBoxParser parser = new DialogBoxParser();
    String typeName = DialogBox.class.getCanonicalName();
    tester = new ElementParserTester(typeName, parser, CAPTION_SUBCLASS);

    b.append("<ui:UiBinder xmlns:ui='" + ElementParserTester.BINDER_URI + "'");
    b.append("    xmlns:my='urn:import:my'");
    b.append("    xmlns:g='urn:import:com.google.gwt.user.client.ui'>");
    b.append("  <g:DialogBox autoHide='true' modal='true'>");
    b.append("    <g:customCaption><my:MyCaption/></g:customCaption>");
    b.append("    <g:Label>And your little dog, too!</g:Label>");
    b.append("  </g:DialogBox> ");
    b.append("</ui:UiBinder>");

    parser.parse(tester.getElem(b.toString(), "g:DialogBox"), "fieldName",
        tester.parsedType, tester.writer);

    String[] expected = {"fieldName.setWidget(<g:Label>);",};

    FieldWriter w = tester.fieldManager.lookup("fieldName");

    assertEquals("new " + PARSED_TYPE + "(true, true, <my:MyCaption>)",
        w.getInitializer());

    Iterator<String> i = tester.writer.statements.iterator();
    for (String e : expected) {
      assertEquals(e, i.next());
    }
    assertFalse(i.hasNext());
    assertNull(tester.logger.died);
  }

  public void testCaptionWidgetDoesntImplementCaption() throws SAXException {

    StringBuffer b = new StringBuffer();

    b.append("<g:DialogBox autoHide='true' modal='true'>");
    b.append("  <g:customCaption> ");
    b.append("    <g:Label>And your little dog, too!</g:Label>");
    b.append("  </g:customCaption> ");
    b.append("</g:DialogBox> ");

    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException e) {
      assertNotNull(tester.logger.died);
    }
  }

  public void testTooManyBodies() throws SAXException {
    StringBuffer b = new StringBuffer();

    b.append("<g:DialogBox autoHide='true' modal='true'>");
    b.append("  <g:Label>And your little dog, too!</g:Label>");
    b.append("  <g:Label>And your little dog, too!</g:Label>");
    b.append("</g:DialogBox> ");

    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException e) {
      assertNotNull(tester.logger.died);
    }
  }

  public void testTooManyCaptions() throws SAXException {
    StringBuffer b = new StringBuffer();

    b.append("<g:DialogBox autoHide='true' modal='true'>");
    b.append("  <g:caption>Hello, I <b>caption</b>you.</g:caption>");
    b.append("  <g:caption>Hello, I <b>caption</b>you.</g:caption>");
    b.append("</g:DialogBox> ");

    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException e) {
      assertNotNull(tester.logger.died);
    }
  }

  public void testBadCaptionWidgetContent() throws SAXException {

    StringBuffer b = new StringBuffer();

    b.append("<g:DialogBox autoHide='true' modal='true'>");
    b.append("  <g:customCaption>");
    b.append("    <div>Oops</div>");
    b.append("  </g:customCaption>");
    b.append("</g:DialogBox> ");

    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException e) {
      assertNotNull(tester.logger.died);
    }
  }

  public void testBadElemContent() throws SAXException {
    StringBuffer b = new StringBuffer();

    b.append("<g:DialogBox autoHide='true' modal='true'>");
    b.append("  <div>Oops</div>");
    b.append("</g:DialogBox> ");

    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException e) {
      assertNotNull(tester.logger.died);
    }
  }

  public void testBadTextContent() throws SAXException {
    StringBuffer b = new StringBuffer();

    b.append("<g:DialogBox autoHide='true' modal='true'>");
    b.append("  Oops");
    b.append("  <g:caption>Hello, I <b>caption</b>you.</g:caption>");
    b.append("  <g:Label>And your little dog, too!</g:Label>");
    b.append("</g:DialogBox> ");

    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException e) {
      assertNotNull(tester.logger.died);
    }
  }

  public void testSubclassOkay() throws UnableToCompleteException,
      SAXException {
    DialogBoxParser parser = new DialogBoxParser();
    String typeName = "my.MyDialogBox";
    tester = new ElementParserTester(typeName, parser, DIALOG_SUBCLASS);

    StringBuffer b = new StringBuffer();

    b.append("<ui:UiBinder xmlns:ui='" + ElementParserTester.BINDER_URI + "'");
    b.append("    xmlns:my='urn:import:my'");
    b.append("    xmlns:g='urn:import:com.google.gwt.user.client.ui'>");
    b.append("  <my:MyDialogBox>");
    b.append("    <g:caption>Hello, I <b>caption</b>you.</g:caption>");
    b.append("    <g:Label>And your little dog, too!</g:Label>");
    b.append("  </my:MyDialogBox> ");
    b.append("</ui:UiBinder>");

    String[] expected = {
        "fieldName.setHTML(\"@mockToken-" + ElementParserTester.FIELD_NAME
            + "-Hello, I <b>caption</b>you.\");",
        "fieldName.setWidget(<g:Label>);",};

    parser.parse(tester.getElem(b.toString(), "my:MyDialogBox"), "fieldName",
        tester.parsedType, tester.writer);
    FieldWriter w = tester.fieldManager.lookup("fieldName");

    assertNull(w.getInitializer());

    Iterator<String> i = tester.writer.statements.iterator();
    for (String e : expected) {
      assertEquals(e, i.next());
    }
    assertFalse(i.hasNext());
    assertNull(tester.logger.died);
  }
}
