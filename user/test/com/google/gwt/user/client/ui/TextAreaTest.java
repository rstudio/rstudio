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

/**
 * Tests a {@link TextArea}.
 */
public class TextAreaTest extends TextBoxBaseTestBase {

  /**
   * Most browsers strip \r from newlines, but IE adds them in. IE's TextRange
   * also truncates the \r\n from the end of the selected range. This test is
   * designed to work on all browsers and verifies that the newlines are
   * accounted for in all browsers.
   */
  public void testNewline() {
    testNewline("Hello World\r\n\r\n\r\n\r\n\r\n", 15, 6, 15);
    testNewline("Hello\r\n\r\n\r\n\r\nWorld, My name is John.", 7, 3, 15);
    testNewline("\r\n\r\n\r\n\r\n\r\nHello World", 4, 4, 13);
    testNewline("\r\n\r\n\r\n\r\n\r\n", 2, 2, 4);
  }

  @Override
  protected TextBoxBase createTextBoxBase() {
    return new TextArea();
  }

  /**
   * Test the handling of newline characters.
   * 
   * @param text the text to test
   * @param cursorPos the cursor position within the newlines
   * @param startRange the start of a range that includes newlines
   * @param endRange the end of a range that includes newlines
   */
  private void testNewline(String text, int cursorPos, int startRange,
      int endRange) {
    TextBoxBase box = createTextBoxBase();
    RootPanel.get().add(box);

    // Browsers will manipulate the text when attached to the DOM, so we need
    // to get the new value. Safari 4 delays the manipulation if the text was
    // set before attaching the TextArea to the DOM, so we attach first and set
    // the text second.
    box.setText(text);
    text = box.getText();

    // Position the cursor in the newlines
    box.setCursorPos(cursorPos);
    assertEquals(cursorPos, box.getCursorPos());

    // Select newlines
    box.setSelectionRange(startRange, endRange - startRange);
    assertEquals(text.substring(startRange, endRange), box.getSelectedText());

    RootPanel.get().remove(box);
  }
}
