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
 * Tests the RadioButton class.
 */
public class RadioButtonTest extends GWTTestCase {

  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  /**
   * Test the name and grouping methods.
   */
  public void testGrouping() {
    // Create some radio buttons
    RadioButton r1 = new RadioButton("group1", "Radio 1");
    RadioButton r2 = new RadioButton("group1", "Radio 2");
    RadioButton r3 = new RadioButton("group2", "Radio 3");
    RootPanel.get().add(r1);
    RootPanel.get().add(r2);
    RootPanel.get().add(r3);

    // Check one button in each group
    r2.setChecked(true);
    r3.setChecked(true);

    // Move a button over
    r2.setName("group2");

    // Check that the correct buttons are checked
    assertTrue(r2.isChecked());
    assertFalse(r3.isChecked());

    r1.setChecked(true);
    assertTrue(r1.isChecked());
    assertTrue(r2.isChecked());

    r3.setChecked(true);
    assertTrue(r1.isChecked());
    assertFalse(r2.isChecked());
    assertTrue(r3.isChecked());
  }
}
