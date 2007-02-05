//Copyright 2006 Google Inc. All Rights Reserved.

package com.google.gwt.user.client.ui;

import com.google.gwt.junit.client.GWTTestCase;

public class TextAreaTest extends GWTTestCase {

  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  public void testMovingCursor() {
    TextArea area = new TextArea();
    RootPanel.get().add(area);
    area.setText("abcd");
    for (int i = 0; i < 4; i++) {
      area.setCursorPos(i);
      assertEquals(i, area.getCursorPos());
    }

  }

  public void disabledTestSelection() {
    TextArea area = new TextArea();
    assertEquals("", area.getSelectedText());
    area.selectAll();
    assertEquals(0, area.getSelectionLength());
    try {
      area.setSelectionRange(0, 1);
      fail("Should have thrown IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException e) {
      // Expected.
    }

    // IE bug: if not attached to dom, setSelectionRange fails.
    RootPanel.get().add(area);
    area.setText("a");

    area.selectAll();    
    assertEquals(1, area.getSelectionLength());
    
    area.setText("");
    assertEquals(0, area.getSelectionLength());
    area.setText("abcde");
    area.setSelectionRange(2, 2);
    assertEquals(2, area.getCursorPos());

    // Check for setting 0;
    area.setSelectionRange(0, 0);
  }
}
