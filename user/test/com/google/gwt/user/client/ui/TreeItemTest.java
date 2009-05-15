/*
 * Copyright 2009 Google Inc.
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
 * Tests the {@link TreeItem}.
 */
public class TreeItemTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  /**
   * Test that setting the widget to null does not modify the widget. See issue
   * 2297 for more details.
   */
  public void testSetWidgetToNull() {
    Label widget = new Label("Test");
    TreeItem item = new TreeItem(widget);
    assertEquals("Test", widget.getText());
    item.setWidget(null);
    assertEquals("Test", widget.getText());
  }
}
