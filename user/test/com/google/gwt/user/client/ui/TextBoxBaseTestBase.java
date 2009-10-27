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

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Abstract base test for {@link TextBox}, {@link TextArea}, and
 * {@link PasswordTextBox}.
 */
public abstract class TextBoxBaseTestBase extends GWTTestCase {

  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  protected abstract TextBoxBase createTextBoxBase();

  /**
   * Tests that {@link TextArea#setText(String)} appropriately converts nulls to
   * empty strings.
   */
  public void testNullMeansEmptyString() {
    TextBoxBase area = createTextBoxBase();
    area.setText(null);
    assertEquals("setText(null) should result in empty string", "",
        area.getText());
  }

  /**
   * Tests that {@link TextArea#setCursorPos(int)} updates the cursor position
   * correctly.
   */
  @DoNotRunWith({Platform.HtmlUnit})
  public void testMovingCursor() {
    TextBoxBase area = createTextBoxBase();
    RootPanel.get().add(area);
    area.setText("abcd");
    for (int i = 0; i < 4; i++) {
      area.setCursorPos(i);
      assertEquals(i, area.getCursorPos());
    }
  }

  /**
   * Tests various text selection methods in text area.
   */
  public void disabledTestSelection() {
    TextBoxBase area = createTextBoxBase();
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

    // Issue 1996: Cannot select text if TextBox is hidden
    {
      TextBoxBase area2 = createTextBoxBase();
      area2.setVisible(false);
      RootPanel.get().add(area2);
      area.selectAll();
    }
  }
  
  @DoNotRunWith({Platform.HtmlUnit})
  public void testValueChangeEvent() {
    TextBoxBase tb = createTextBoxBase();
    // To work cross-platform, the tb must be added to the root panel.
    RootPanel.get().add(tb);
    
    Handler h = new Handler();
    tb.addValueChangeHandler(h);
    
    tb.setValue(null);
    assertEquals("", tb.getValue());
    assertNull(h.received);
    
    tb.setText("able");
    assertEquals("able", tb.getValue());
    assertNull(h.received);

    tb.setValue("able");
    assertEquals("able", tb.getValue());
    assertNull(h.received);

    tb.setValue("baker");
    assertNull(h.received);

    tb.setValue("baker", true);
    assertEquals("baker", tb.getValue());
    assertNull(h.received);

    tb.setValue("able", true);
    assertEquals("able", h.received);

    tb.setValue("baker", true);
    assertEquals("baker", h.received);
  }
  
  private static class Handler implements ValueChangeHandler<String> {
    String received = null;
    
    public void onValueChange(ValueChangeEvent<String> event) {
      received = event.getValue();
    }
  }
}
