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
package com.google.gwt.user.client;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Label;

/**
 * Test Case for {@link Cookies}.
 */
public class WindowTest extends GWTTestCase {

  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  /**
   * Tests the ability of the Window to get the client size correctly
   * with and without visible scroll bars.
   */
  public void testGetClientSize() {
    // Get the dimensions without any scroll bars
    Window.enableScrolling(false);
    int oldClientHeight = Window.getClientHeight();
    int oldClientWidth = Window.getClientWidth();
    assertTrue(oldClientHeight > 0);
    assertTrue(oldClientWidth > 0);

    // Compare to the dimensions with scroll bars
    Window.enableScrolling(true);
    Label largeDOM = new Label();
    largeDOM.setPixelSize(oldClientWidth + 100, oldClientHeight + 100);
    RootPanel.get().add(largeDOM);
    int newClientHeight = Window.getClientHeight();
    int newClientWidth = Window.getClientWidth();
    assertTrue(newClientHeight < oldClientHeight);
    assertTrue(newClientWidth < oldClientWidth);

    // Increase the size of the contents and verify we still have the same
    // client height and width 
    largeDOM.setPixelSize(oldClientWidth + 500, oldClientHeight + 500);
    int finalClientHeight = Window.getClientHeight();
    int finalClientWidth = Window.getClientWidth();
    assertEquals(newClientHeight, finalClientHeight);
    assertEquals(newClientWidth, finalClientWidth);
  }
}
