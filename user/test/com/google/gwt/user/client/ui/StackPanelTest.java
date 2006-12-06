// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.user.client.ui;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests <code>ListBox</code>. Needs many, many more tests.
 */
public class StackPanelTest extends GWTTestCase {

  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  public String curContents(StackPanel p) {
    String accum = "";
    int size = p.getWidgetCount();
    for (int i = 0; i < size; i++) {
      Label l = (Label) p.getWidget(i);
      if (i != 0) {
        accum += " ";
      }
      accum += l.getText();
    }
    return accum;
  }

  /**
   * Tests new remove implementation for StackPanel
   */
  public void testRemove() {
    StackPanel p = new StackPanel();
    Label a = new Label("a");
    Label b = new Label("b");
    Label c = new Label("c");
    Label d = new Label("d");
    p.add(a);
    p.add(b, "b");
    p.add(c);
    p.add(d, "d");
    assertEquals("a b c d", curContents(p));

    // Remove c
    p.remove(c);
    assertEquals("a b d", curContents(p));

    // Remove b.
    p.remove(1);
    assertEquals("a d", curContents(p));

    // Remove non-existant element
    assertFalse(p.remove(b));

    // Remove a.
    p.remove(a);
    assertEquals("d", curContents(p));

    // Remove d.
    p.remove(a);
    assertEquals("d", curContents(p));

  }

  /**
   * Tests getSelectedStack.
   */
  public void testGetSelectedStack() {
    StackPanel p = new StackPanel();
    assertEquals(-1, p.getSelectedIndex());
    Label a = new Label("a");
    Label b = new Label("b");
    Label c = new Label("c");
    Label d = new Label("d");
    p.add(a);
    p.add(b, "b");
    p.add(c);
    p.add(d, "d");
    assertEquals(0, p.getSelectedIndex());
    p.showStack(2);
    assertEquals(2, p.getSelectedIndex());
    p.showStack(-1);
    assertEquals(-1, p.getSelectedIndex());
  }
  
   
}