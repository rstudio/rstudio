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
import com.google.gwt.uibinder.rebind.XMLElement;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;

import junit.framework.TestCase;

import java.util.Iterator;

/**
 * Test for {@link MenuItemParser}.
 */
public class MenuItemParserTest extends TestCase {

  private static final String PARSED_TYPE = "com.google.gwt.user.client.ui.MenuItem";
  private static final String STD_ITEM = "new com.google.gwt.user.client.ui.MenuItem(\"\","
      + " (com.google.gwt.user.client.Command) null)";

  private ElementParser parser;
  private ElementParserTester tester;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    parser = new MenuItemParser();
    tester = new ElementParserTester(PARSED_TYPE, parser);
  }

  public void test_empty() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:MenuItem text='My Item'/>");
    // parse
    FieldWriter w = tester.parse(b.toString());
    assertEquals(STD_ITEM, w.getInitializer());
    // no statements
    assertStatements();
  }

  /**
   * Test for case when child {@link XMLElement} is not {@link MenuBar}.
   */
  public void test_notMenuBar() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:MenuItem>");
    b.append("  <div/>");
    b.append("</g:MenuItem>");
    // parse
    FieldWriter w = tester.parse(b.toString());
    assertEquals(STD_ITEM, w.getInitializer());
    // no statements
    assertStatements();
  }

  public void test_hasMenuBar() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:MenuItem>");
    b.append("  <g:MenuBar id='1'/>");
    b.append("</g:MenuItem>");
    // parse
    FieldWriter w = tester.parse(b.toString());
    assertEquals(STD_ITEM, w.getInitializer());
    // usual statement
    assertStatements("fieldName.setSubMenu(<g:MenuBar id='1'>);");
  }

  public void test_twoMenuBar() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:MenuItem text='My Item'>");
    b.append("  <g:MenuBar id='1'/>");
    b.append("  <g:MenuBar id='2'/>");
    b.append("</g:MenuItem>");
    // parse failed
    try {
      tester.parse(b.toString());
    } catch (UnableToCompleteException e) {
      String died = tester.logger.died;
      assertTrue(died.contains("Only one MenuBar may be contained in a MenuItem"));
    }
  }

  /**
   * Test for using subclass of {@link MenuItem}.
   * <p>
   * Custom {@link MenuItem} should have default constructor.
   * <p>
   * See http://code.google.com/p/google-web-toolkit/issues/detail?id=4550
   */
  public void test_customMenuItem() throws Exception {
    MockJavaResource itemSubclass = new MockJavaResource(
        "com.google.gwt.user.client.ui.MyItem") {
      @Override
      public CharSequence getContent() {
        String superName = MenuItem.class.getCanonicalName();
        StringBuffer code = new StringBuffer();
        code.append("package com.google.gwt.user.client.ui;\n");
        code.append("import com.google.gwt.user.client.Command;\n");
        code.append("public class MyItem extends " + superName + " {\n");
        code.append("  public MyItem() {\n");
        code.append("    super(\"\", (Command) null);\n");
        code.append("  }\n");
        code.append("}\n");
        return code;
      }
    };
    tester = new ElementParserTester("com.google.gwt.user.client.ui.MyItem",
        parser, itemSubclass);
    // prepare source
    StringBuffer b = new StringBuffer();
    b.append("<g:MyItem text='My Item'/>");
    // parse
    FieldWriter w = tester.parse(b.toString());
    assertEquals(null, w.getInitializer());
    // no statements
    assertStatements();
  }

  public void test_customMenuBar() throws Exception {
    MockJavaResource barSubclass = new MockJavaResource(
        "com.google.gwt.user.client.ui.MyMenuBar") {
      @Override
      public CharSequence getContent() {
        String superName = MenuBar.class.getCanonicalName();
        StringBuffer code = new StringBuffer();
        code.append("package com.google.gwt.user.client.ui;\n");
        code.append("public class MyMenuBar extends " + superName + " {\n");
        code.append("}\n");
        return code;
      }
    };
    tester = new ElementParserTester(PARSED_TYPE, parser, barSubclass);
    // prepare source
    StringBuffer b = new StringBuffer();
    b.append("<g:MenuItem>");
    b.append("  <g:MyMenuBar id='1'/>");
    b.append("</g:MenuItem>");
    // parse
    FieldWriter w = tester.parse(b.toString());
    assertEquals(STD_ITEM, w.getInitializer());
    // usual statement
    assertStatements("fieldName.setSubMenu(<g:MyMenuBar id='1'>);");
  }

  private void assertStatements(String... expected) {
    Iterator<String> i = tester.writer.statements.iterator();
    for (String e : expected) {
      assertEquals(e, i.next());
    }
    assertFalse(i.hasNext());
    assertNull(tester.logger.died);
  }

  /**
   * Containers method to reference types referenced only from JavaDoc, used to
   * prevent CheckStyle errors.
   */
  public void unusedReferences(XMLElement p1, MenuBar p2) {
    p1.hashCode();
    p2.hashCode();
  }
}
