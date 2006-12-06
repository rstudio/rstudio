// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.user.client.ui;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests <code>ListBox</code>. Needs many, many more tests.
 */
public class ListBoxTest extends GWTTestCase {

  public String getModuleName() {
    return "com.google.gwt.user.User";
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

  public void testSelection() {
    ListBox box = new ListBox();
    box.addItem("a");
    box.setSelectedIndex(-1);
    assertEquals(-1, box.getSelectedIndex());
    box.setSelectedIndex(0);
    assertEquals("a", box.getItemText(box.getSelectedIndex()));
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

  public void testSetStyleNames() {
    ListBox box = new ListBox();
    box.removeStyleName("gwt-ListBox");
    assertEquals("", box.getStyleName());

    // Check subset problems.
    box.addStyleName("superset");
    box.addStyleName("super");
    assertEquals("superset super", getNormalizedStyleName(box));

    // Remove a style that doesn't exist.
    box.removeStyleName("sup");
    assertEquals("superset super", getNormalizedStyleName(box));
    box.removeStyleName("super");
    assertEquals("superset", getNormalizedStyleName(box));
    box.addStyleName("two styles");
    assertEquals("superset two styles", getNormalizedStyleName(box));
    box.removeStyleName("superset");
    assertEquals("two styles", getNormalizedStyleName(box));
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
    assertEquals("superset two styles", getNormalizedStyleName(box));
   }

  private String getNormalizedStyleName(ListBox box) {
    return box.getStyleName().replaceAll("  ", " ").trim();
  }

}
