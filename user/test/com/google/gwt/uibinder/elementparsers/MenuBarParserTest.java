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
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.MenuItemSeparator;

import junit.framework.TestCase;

import java.util.Iterator;

/**
 * Test for {@link MenuBarParser}.
 */
public class MenuBarParserTest extends TestCase {

  private static final String PARSED_TYPE = "com.google.gwt.user.client.ui.MenuBar";

  private MenuBarParser parser;
  private ElementParserTester tester;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    parser = new MenuBarParser();
    tester = new ElementParserTester(PARSED_TYPE, parser);
  }

  public void testBadChild_namespace() throws Exception {
    checkBadLine("<ui:blah/>", MenuBarParser.BAD_CHILD);
  }

  public void testBadChild_name() throws Exception {
    checkBadLine("<g:Button/>", MenuBarParser.BAD_CHILD);
  }

  private void checkBadLine(String badLine, String expectedDied)
      throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:MenuBar>");
    b.append("  " + badLine);
    b.append("</g:MenuBar>");
    // parse failed
    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException e) {
      String died = tester.logger.died;
      assertTrue(died, died.contains(expectedDied));
    }
  }

  public void test_verticalMenuBar() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:MenuBar vertical='true'>");
    b.append("  <g:MenuItem/>");
    b.append("</g:MenuBar>");
    // parse
    FieldWriter w = tester.parse(b.toString());
    // special initializer for MenuBar
    assertEquals("new com.google.gwt.user.client.ui.MenuBar(true)",
        w.getInitializer());
    // usual statement
    assertStatements("fieldName.addItem(<g:MenuItem>);");
  }

  public void test_MenuItem() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:MenuBar>");
    b.append("  <g:MenuItem text='1'/>");
    b.append("  <g:MenuItem text='2'/>");
    b.append("</g:MenuBar>");
    // parse
    tester.parse(b.toString());
    // has items
    assertStatements("fieldName.addItem(<g:MenuItem text='1'>);",
        "fieldName.addItem(<g:MenuItem text='2'>);");
  }

  public void test_MenuItemSeparator() throws Exception {
    StringBuffer b = new StringBuffer();
    b.append("<g:MenuBar>");
    b.append("  <g:MenuItemSeparator title='1'/>");
    b.append("  <g:MenuItemSeparator title='2'/>");
    b.append("</g:MenuBar>");
    // parse
    tester.parse(b.toString());
    // has separators
    assertStatements(
        "fieldName.addSeparator(<g:MenuItemSeparator title='1'>);",
        "fieldName.addSeparator(<g:MenuItemSeparator title='2'>);");
  }

  /**
   * Test for using subclass of {@link MenuItem}.
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
    tester = new ElementParserTester(PARSED_TYPE, parser, itemSubclass);
    // prepare source
    StringBuffer b = new StringBuffer();
    b.append("<g:MenuBar>");
    b.append("  <g:MyItem/>");
    b.append("</g:MenuBar>");
    // parse
    tester.parse(b.toString());
    // success
    assertStatements("fieldName.addItem(<g:MyItem>);");
  }

  /**
   * Test for using subclass of {@link MenuItemSeparator}.
   * <p>
   * See http://code.google.com/p/google-web-toolkit/issues/detail?id=4550
   */
  public void test_customMenuItemSeparator() throws Exception {
    MockJavaResource itemSubclass = new MockJavaResource(
        "com.google.gwt.user.client.ui.MySeparator") {
      @Override
      public CharSequence getContent() {
        String superName = MenuItemSeparator.class.getCanonicalName();
        StringBuffer code = new StringBuffer();
        code.append("package com.google.gwt.user.client.ui;\n");
        code.append("public class MySeparator extends " + superName + " {\n");
        code.append("}\n");
        return code;
      }
    };
    tester = new ElementParserTester(PARSED_TYPE, parser, itemSubclass);
    // prepare source
    StringBuffer b = new StringBuffer();
    b.append("<g:MenuBar>");
    b.append("  <g:MySeparator/>");
    b.append("</g:MenuBar>");
    // parse
    tester.parse(b.toString());
    // success
    assertStatements("fieldName.addSeparator(<g:MySeparator>);");
  }

  private void assertStatements(String... expected) {
    Iterator<String> i = tester.writer.statements.iterator();
    for (String e : expected) {
      assertEquals(e, i.next());
    }
    assertFalse(i.hasNext());
    assertNull(tester.logger.died);
  }
}
