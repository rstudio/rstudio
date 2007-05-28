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
import com.google.gwt.user.client.Timer;

/**
 * Tests the {@link RichTextArea} widget.
 */
public class RichTextAreaTest extends GWTTestCase {

  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  /**
   * Test that a delayed set of HTML is reflected. Some platforms have timing
   * subtleties that need to be tested.
   */
  public void testSetHTMLAfterInit() {
    final RichTextArea richTextArea = new RichTextArea();
    RootPanel.get().add(richTextArea);
    new Timer() {
      public void run() {
        richTextArea.setHTML("<b>foo</b>");
        assertEquals("<b>foo</b>", richTextArea.getHTML().toLowerCase());
        finishTest();
      }
    }.schedule(200);
    delayTestFinish(1000);
  }

  /**
   * Test that an immediate set of HTML is reflected immediately and after a
   * delay. Some platforms have timing subtleties that need to be tested.
   */
  public void testSetHTMLBeforeInit() {
    final RichTextArea richTextArea = new RichTextArea();
    RootPanel.get().add(richTextArea);
    richTextArea.setHTML("<b>foo</b>");
    assertEquals("<b>foo</b>", richTextArea.getHTML().toLowerCase());
    new Timer() {
      public void run() {
        assertEquals("<b>foo</b>", richTextArea.getHTML().toLowerCase());
        finishTest();
      }
    }.schedule(200);
    delayTestFinish(1000);
  }

  /**
   * Test that delayed set of text is reflected. Some platforms have timing
   * subtleties that need to be tested.
   */
  public void testSetTextAfterInit() {
    final RichTextArea richTextArea = new RichTextArea();
    RootPanel.get().add(richTextArea);
    new Timer() {
      public void run() {
        richTextArea.setText("foo");
        assertEquals("foo", richTextArea.getText());
        finishTest();
      }
    }.schedule(200);
    delayTestFinish(1000);
  }

  /**
   * Test that an immediate set of text is reflected immediately and after a
   * delay. Some platforms have timing subtleties that need to be tested.
   */
  public void testSetTextBeforeInit() {
    final RichTextArea richTextArea = new RichTextArea();
    RootPanel.get().add(richTextArea);
    richTextArea.setText("foo");
    assertEquals("foo", richTextArea.getText());
    new Timer() {
      public void run() {
        assertEquals("foo", richTextArea.getText());
        finishTest();
      }
    }.schedule(200);
    delayTestFinish(1000);
  }
}
