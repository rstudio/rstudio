/*
 * Copyright 2008 Google Inc.
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

import com.google.gwt.dom.client.SelectElement;
import com.google.gwt.i18n.client.HasDirection.Direction;
import com.google.gwt.i18n.shared.BidiFormatter;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;

/**
 * Tests {@link ListBox}. Needs many, many more tests.
 */
public class ListBoxTest extends GWTTestCase {

  private final String RTL_TEXT = "\u05e0 \u05e0\u05e0\u05e0\u05e0\u05e0" +
      "\u05e0\u05e0\u05e0 \u05e0\u05e0\u05e0\u05e0\u05e0 \u05e0\u05e0\u05e0" +
      "\u05e0\u05e0\u05e0 \u05e0\u05e0\u05e0 \u05e0\u05e0\u05e0";
  private final String LTR_TEXT = "The quick brown fox jumps over the" +
      "lazy dog. The lazy dog seems quite amused.";

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.DebugTest";
  }

  public void testClear() {
    ListBox lb = new ListBox();
    lb.addItem("a");
    lb.addItem("b");
    lb.addItem("c");
    lb.clear();
    assertEquals(0, lb.getItemCount());
  }

  public void testDebugId() {
    ListBox list = new ListBox();
    list.addItem("option0", "value0");
    list.addItem("option1", "value1");
    list.addItem("option2", "value2");
    list.addItem("option3", "value3");
    RootPanel.get().add(list);

    list.ensureDebugId("myList");
    UIObjectTest.assertDebugId("myList", list.getElement());

    delayTestFinish(5000);
    DeferredCommand.addCommand(new Command() {
      public void execute() {
        UIObjectTest.assertDebugIdContents("myList-item0", "option0");
        UIObjectTest.assertDebugIdContents("myList-item1", "option1");
        UIObjectTest.assertDebugIdContents("myList-item2", "option2");
        UIObjectTest.assertDebugIdContents("myList-item3", "option3");
        finishTest();
      }
    });
  }

  public void testInsert() {

    // Insert in the middle
    {
      ListBox lb = new ListBox();
      lb.addItem("a");
      lb.addItem("c");
      lb.insertItem("b", 1);
      assertEquals("a", lb.getItemText(0));
      assertEquals("b", lb.getItemText(1));
      assertEquals("c", lb.getItemText(2));
    }

    // Insert at the front
    {
      ListBox lb = new ListBox();
      lb.addItem("b");
      lb.addItem("c");
      lb.insertItem("a", 0);
      assertEquals("a", lb.getItemText(0));
      assertEquals("b", lb.getItemText(1));
      assertEquals("c", lb.getItemText(2));
    }

    // Insert at the end by using a negative index
    {
      ListBox lb = new ListBox();
      lb.addItem("a");
      lb.addItem("b");
      lb.insertItem("c", -1);
      assertEquals("a", lb.getItemText(0));
      assertEquals("b", lb.getItemText(1));
      assertEquals("c", lb.getItemText(2));
    }

    // Insert at the end by using an index greater than the length
    // of the list
    {
      ListBox lb = new ListBox();
      lb.addItem("a");
      lb.addItem("b");
      lb.insertItem("c", 2);
      assertEquals("a", lb.getItemText(0));
      assertEquals("b", lb.getItemText(1));
      assertEquals("c", lb.getItemText(2));
    }

    // Insert items of different directions
    {
      // Explicit direction, no direction estimation
      ListBox lb = new ListBox();
      lb.insertItem(RTL_TEXT, Direction.RTL, 0);
      assertEquals(RTL_TEXT, lb.getItemText(0));
      assertOptionText(BidiFormatter.getInstanceForCurrentLocale().unicodeWrap(
          RTL_TEXT, false /* isHtml */, false /* dirReset */), lb, 0);
      lb.insertItem(LTR_TEXT, Direction.LTR, 0);
      assertEquals(LTR_TEXT, lb.getItemText(0));
      assertOptionText(BidiFormatter.getInstanceForCurrentLocale().unicodeWrap(
          LTR_TEXT, false /* isHtml */, false /* dirReset */), lb, 0);
      lb.clear();

      // Direction estimation
      lb.setDirectionEstimator(true);
      lb.addItem(RTL_TEXT);
      assertEquals(RTL_TEXT, lb.getItemText(0));
      assertOptionText(BidiFormatter.getInstanceForCurrentLocale().unicodeWrap(
          RTL_TEXT, false /* isHtml */, false /* dirReset */), lb, 0);
      lb.addItem(LTR_TEXT);
      assertEquals(LTR_TEXT, lb.getItemText(1));
      assertOptionText(BidiFormatter.getInstanceForCurrentLocale().unicodeWrap(
          LTR_TEXT, false /* isHtml */, false /* dirReset */), lb, 1);

      // Explicit direction which is opposite to the estimated direction
      lb.insertItem(RTL_TEXT, Direction.LTR, 0);
      assertEquals(RTL_TEXT, lb.getItemText(0));
      assertOptionText(
          BidiFormatter.getInstanceForCurrentLocale().unicodeWrapWithKnownDir(
          Direction.LTR, RTL_TEXT, false /* isHtml */, false /* dirReset */),
          lb, 0);
      lb.insertItem(LTR_TEXT, Direction.RTL, 1);
      assertEquals(LTR_TEXT, lb.getItemText(1));
      assertOptionText(
          BidiFormatter.getInstanceForCurrentLocale().unicodeWrapWithKnownDir(
          Direction.RTL, LTR_TEXT, false /* isHtml */, false /* dirReset */),
          lb, 1);
    }
  }

  public void testRemove() {
    ListBox lb = new ListBox();
    lb.addItem("a");
    lb.addItem("b");
    lb.addItem("c");
    lb.removeItem(1);
    assertEquals("a", lb.getItemText(0));
    assertEquals("c", lb.getItemText(1));
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

      // Setting the selected index should de-select all other items, except
      // the item at the index.
      box.setSelectedIndex(1);
      assertFalse(box.isItemSelected(0));
      assertTrue(box.isItemSelected(1));

      // Make sure that setting the selected index to -1 de-selects the
      // selected item.
      box.setSelectedIndex(-1);
      assertFalse(box.isItemSelected(1));
    }
  }

  public void testSetStyleNames() {
    ListBox box = new ListBox();

    // Check subset problems.
    box.addStyleName("superset");
    box.addStyleName("super");
    assertEquals("gwt-ListBox superset super", box.getStyleName());

    // Remove a style that doesn't exist.
    box.removeStyleName("sup");
    assertEquals("gwt-ListBox superset super", box.getStyleName());
    box.removeStyleName("super");
    assertEquals("gwt-ListBox superset", box.getStyleName());
    box.addStyleName("two styles");
    assertEquals("gwt-ListBox superset two styles", box.getStyleName());
    box.removeStyleName("superset");
    assertEquals("gwt-ListBox two styles", box.getStyleName());
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
    assertEquals("gwt-ListBox superset two styles", box.getStyleName());
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

    // Text of different directions
    {
      ListBox lb = new ListBox();
      // Explicit direction, no direction estimation
      lb.insertItem(RTL_TEXT, Direction.RTL, 0);
      assertEquals(RTL_TEXT, lb.getItemText(0));
      assertOptionText(BidiFormatter.getInstanceForCurrentLocale().unicodeWrap(
          RTL_TEXT, false /* isHtml */, false /* dirReset */), lb, 0);
      lb.insertItem(LTR_TEXT, Direction.LTR, 0);
      assertEquals(LTR_TEXT, lb.getItemText(0));
      assertOptionText(BidiFormatter.getInstanceForCurrentLocale().unicodeWrap(
          LTR_TEXT, false /* isHtml */, false /* dirReset */), lb, 0);

      // Direction estimation
      lb.setDirectionEstimator(true);
      lb.setItemText(0, RTL_TEXT);
      assertEquals(RTL_TEXT, lb.getItemText(0));
      assertOptionText(BidiFormatter.getInstanceForCurrentLocale().unicodeWrap(
          RTL_TEXT, false /* isHtml */, false /* dirReset */), lb, 0);
      lb.setItemText(0, LTR_TEXT);
      assertEquals(LTR_TEXT, lb.getItemText(0));
      assertOptionText(BidiFormatter.getInstanceForCurrentLocale().unicodeWrap(
          LTR_TEXT, false /* isHtml */, false /* dirReset */), lb, 0);

      // Explicit direction which is opposite to the estimated direction
      lb.setItemText(0, LTR_TEXT, Direction.RTL);
      assertEquals(LTR_TEXT, lb.getItemText(0));
      assertOptionText(
          BidiFormatter.getInstanceForCurrentLocale().unicodeWrapWithKnownDir(
          Direction.RTL, LTR_TEXT, false /* isHtml */, false /* dirReset */),
          lb, 0);
      lb.setItemText(0, RTL_TEXT, Direction.LTR);
      assertEquals(RTL_TEXT, lb.getItemText(0));
      assertOptionText(
          BidiFormatter.getInstanceForCurrentLocale().unicodeWrapWithKnownDir(
          Direction.LTR, RTL_TEXT, false /* isHtml */, false /* dirReset */),
          lb, 0);
    }

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

  private void assertOptionText(String expected, ListBox listBox, int index) {
    SelectElement select = listBox.getElement().cast();
    assertEquals(expected, select.getOptions().getItem(index).getText());
  }
}
