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

import junit.framework.TestCase;

import org.xml.sax.SAXException;

/**
 * Eponymous unit test.
 */
public class ImageParserTest extends TestCase {
  private static final String PARSED_TYPE = "com.google.gwt.user.client.ui.Image";

  private static final MockJavaResource IMAGE_SUBCLASS_NO_CONSTRUCTOR = new MockJavaResource(
      "my.MyImage") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package my;\n");
      code.append("import com.google.gwt.user.client.ui.Image;\n");
      code.append("public class MyImage extends Image {\n");
      code.append("}\n");
      return code;
    }
  };
  private static final MockJavaResource IMAGE_SUBCLASS_RESOURCE_CONSTRUCTOR = new MockJavaResource(
      "my.MyConstructedImage") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package my;\n");
      code.append("import com.google.gwt.user.client.ui.Image;\n");
      code.append("import com.google.gwt.resources.client.ImageResource;\n");
      code.append("public class MyConstructedImage extends Image {\n");
      code.append("  public MyConstructedImage(ImageResource r) { super(r); }");
      code.append("}\n");
      return code;
    }
  };
  private ElementParserTester tester;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    tester = new ElementParserTester(PARSED_TYPE, new ImageParser());
  }

  public void testHappyWithDefaultInstantiableSubclass()
      throws UnableToCompleteException, SAXException {
    tester = new ElementParserTester("my.MyImage", new ImageParser(),
        IMAGE_SUBCLASS_NO_CONSTRUCTOR, IMAGE_SUBCLASS_RESOURCE_CONSTRUCTOR);
    ImageParser parser = new ImageParser();
    StringBuffer b = new StringBuffer();

    b.append("<ui:UiBinder xmlns:ui='" + ElementParserTester.BINDER_URI + "'");
    b.append("    xmlns:my='urn:import:my'");
    b.append("    xmlns:g='urn:import:com.google.gwt.user.client.ui'>");
    b.append("  <my:MyImage resource='{someImageResource}'/> ");
    b.append("</ui:UiBinder>");

    parser.parse(tester.getElem(b.toString(), "my:MyImage"), "fieldName", 
        tester.parsedType, tester.writer);
    FieldWriter w = tester.fieldManager.lookup("fieldName");
    assertNull(w.getInitializer());

    assertTrue(tester.writer.statements.isEmpty());
    assertNull(tester.logger.died);
  }

  public void testHappyWithSubclassWithImageResourceConstructor()
      throws UnableToCompleteException, SAXException {
    ImageParser parser = new ImageParser();
    tester = new ElementParserTester("my.MyConstructedImage", new ImageParser(),
        IMAGE_SUBCLASS_NO_CONSTRUCTOR, IMAGE_SUBCLASS_RESOURCE_CONSTRUCTOR);

    StringBuffer b = new StringBuffer();

    b.append("<ui:UiBinder xmlns:ui='" + ElementParserTester.BINDER_URI + "'");
    b.append("    xmlns:my='urn:import:my'");
    b.append("    xmlns:g='urn:import:com.google.gwt.user.client.ui'>");
    b.append("  <my:MyConstructedImage resource='{someImageResource}'/> ");
    b.append("</ui:UiBinder>");

    parser.parse(tester.getElem(b.toString(), "my:MyConstructedImage"), "fieldName", 
        tester.parsedType, tester.writer);
    FieldWriter w = tester.fieldManager.lookup("fieldName");
    assertEquals("new my.MyConstructedImage(someImageResource)",
        w.getInitializer());

    assertTrue(tester.writer.statements.isEmpty());
    assertNull(tester.logger.died);
  }

  public void testHappyWithResource() throws UnableToCompleteException,
      SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<ui:Image field='someImageResource' />");
    b.append("<g:Image resource='{someImageResource}' >");
    b.append("</g:Image>");

    FieldWriter w = tester.parse(b.toString());
    assertEquals("new " + PARSED_TYPE + "(someImageResource)",
        w.getInitializer());

    assertTrue(tester.writer.statements.isEmpty());
    assertNull(tester.logger.died);
  }

  public void testHappyWithNoResource() throws UnableToCompleteException,
      SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:Image>");
    b.append("</g:Image>");

    FieldWriter w = tester.parse(b.toString());
    assertNull(w.getInitializer());

    assertTrue(tester.writer.statements.isEmpty());
    assertNull(tester.logger.died);
  }

  public void testChokeOnNonResource() throws SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:Image resource='someString' >");
    b.append("</g:Image>");

    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException e) {
      assertTrue("Expect to hear about ImageResource",
          tester.logger.died.contains("ImageResource"));
    }
  }
}
