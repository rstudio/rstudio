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
 * Unit test for {@link DialogBox}.
 */
public class DialogBoxTest extends PopupTest {

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.User";
  }
  
  /**
   * Test the accessors.
   */
  public void testAccessors() {
    // Set the widget
    DialogBox box1 = new DialogBox();
    assertNull(box1.getWidget());
    HTML contents1 = new HTML("Contents");
    box1.setWidget(contents1);
    assertEquals(contents1, box1.getWidget());

    // Set widget to null
    box1.setWidget(null);
    assertNull(box1.getWidget());
  }
}
