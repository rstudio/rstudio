/*
 * Copyright 2008 Google Inc.
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
 * Tests for {@link SimpleCheckBox}.
 */
public class SimpleCheckBoxTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.UserTest";
  }

  public void testProperties() {
    SimpleCheckBox checkbox = new SimpleCheckBox();

    checkbox.setName("myName");
    assertEquals("myName", checkbox.getName());

    checkbox.setTabIndex(42);
    assertEquals(42, checkbox.getTabIndex());

    checkbox.setEnabled(false);
    assertEquals(false, checkbox.isEnabled());

    // Test the 'checked' state across attachment and detachment
    // (this value has a tendency to get lost on some browsers).
    checkbox.setChecked(true);
    assertEquals(true, checkbox.isChecked());

    RootPanel.get().add(checkbox);
    assertEquals(true, checkbox.isChecked());

    RootPanel.get().remove(checkbox);
    assertEquals(true, checkbox.isChecked());
  }
}
