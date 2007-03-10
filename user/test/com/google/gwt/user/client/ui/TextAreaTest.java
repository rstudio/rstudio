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

/**
 * TODO: document me.
 */
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
