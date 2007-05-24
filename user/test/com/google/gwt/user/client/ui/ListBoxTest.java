/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.user.client.ui;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.DOM;

/**
 * Tests {@link ListBox}. Needs many, many more tests.
 */
public class ListBoxTest extends GWTTestCase {

  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  public void testSelected() {
    ListBox lb = new ListBox();
    lb.clear();
    for (int i = 0; i < 3; i++) {
      lb.addItem(Integer.toString(i), Integer.toString(i));
    }
    lb.setSelectedIndex(2);
    assertEquals(2, lb.getSelectedIndex());
  }

  public void testSelection() {
    {
      ListBox box = new ListBox();
      box.addItem("a");
      box.setSelectedIndex(-1);
      assertEquals(-1, box.getSelectedIndex());
      box.setSelectedIndex(0);
      assertEquals("a", box.getItemText(box.getSelectedIndex()));
    }

    // Testing multiple selection
    {
      ListBox box = new ListBox(true);
      box.setMultipleSelect(true);
      box.addItem("a");
      box.addItem("b");
      box.addItem("c");

      for (int j = 0; j < box.getItemCount(); j++) {
        box.setItemSelected(j, true);
      }

      for (int j = 0; j < box.getItemCount(); j++) {
        assertTrue(box.isItemSelected(j));
      }
    }
  }

  public void testSetStyleNames() {
    ListBox box = new ListBox();
    try {
      box.removeStyleName("gwt-ListBox");
      fail("Should have thrown illegal argument exception");
    } catch (IllegalArgumentException e) {
    }

    // Check subset problems.
    box.addStyleName("superset");
    box.addStyleName("super");
    assertEquals("gwt-ListBox superset super", getNormalizedStyleName(box));

    // Remove a style that doesn't exist.
    box.removeStyleName("sup");
    assertEquals("gwt-ListBox superset super", getNormalizedStyleName(box));
    box.removeStyleName("super");
    assertEquals("gwt-ListBox superset", getNormalizedStyleName(box));
    box.addStyleName("two styles");
    assertEquals("gwt-ListBox superset two styles", getNormalizedStyleName(box));
    box.removeStyleName("superset");
    assertEquals("gwt-ListBox two styles", getNormalizedStyleName(box));
    box.removeStyleName("two styles");
    try {
      box.addStyleName("");
      fail("Should have thrown illegal argument exception");
    } catch (IllegalArgumentException e) {
      // expected
    }
    box.addStyleName("superset");
    box.addStyleName("two");
    box.addStyleName("styles");
    assertEquals("gwt-ListBox superset two styles", getNormalizedStyleName(box));
  }

  public void testText() {
    ListBox box = new ListBox();
    box.addItem("a");
    box.addItem("b", "B");
    assertEquals(2, box.getItemCount());
    box.addItem("c", "C");
    assertEquals("B", box.getValue(1));
    assertEquals("a", box.getItemText(0));
    assertEquals("b", box.getItemText(1));
    assertEquals("c", box.getItemText(2));
    box.setItemText(1, "bb");
    assertEquals("bb", box.getItemText(1));
    box.setItemText(1, "bc");
    assertEquals("bc", box.getItemText(1));
    box.setItemText(0, "");
    assertEquals("", box.getItemText(0));
    try {
      box.setItemText(0, null);
      fail("Should have thrown Null Pointer");
    } catch (NullPointerException e) {
      // expected;
    }
  }

  public void testValues() {
    ListBox box = new ListBox();

    // Test adding an item without a value then modifying it later.
    {
      String text = "no-value item added";

      box.addItem(text);
      assertEquals(text, box.getValue(0));

      box.setValue(0, "foo");
      assertEquals("foo", box.getValue(0));
    }

    // Test inserting an item without a value then modifying it later.
    {
      String text = "no-value item inserted";

      box.insertItem(text, 0);
      assertEquals(text, box.getValue(0));

      box.setValue(0, "bar");
      assertEquals("bar", box.getValue(0));
    }

    // Test inserting an item with a value then modifying it later.
    {
      String text = "value item inserted";
      String value = "value";

      box.insertItem(text, value, 1);
      assertEquals(text, box.getItemText(1));
      assertEquals(value, box.getValue(1));

      box.setValue(1, "bar");
      box.setItemText(1, "item text");
      assertEquals("bar", box.getValue(1));
      assertEquals("item text", box.getItemText(1));
    }
  }

  private String getNormalizedStyleName(ListBox box) {
    return DOM.getElementProperty(box.getElement(), "className").replaceAll("  ", " ").trim();
  }

}
