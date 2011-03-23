/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.widget.client;

import com.google.gwt.dom.client.ButtonElement;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Base tests for subclasses of {@link ButtonBase}.
 * 
 * @param <T> the value type
 */
public abstract class ButtonBaseTest<T> extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.widget.Widget";
  }

  public void testSetAccessKey() {
    // No access key by default.
    ButtonBase<T> button = createButton();
    assertFalse(button.getElement().getInnerHTML().contains("accessKey"));

    // Set access key to safe value.
    button.setAccessKey('a');
    assertEquals("a", getButtonElement(button).getAccessKey());
  }

  public void testSetEnabled() {
    // Default is enabled.
    ButtonBase<T> button = createButton();
    assertTrue(button.isEnabled());
    assertFalse(getButtonElement(button).isDisabled());

    // Set tab index to 0.
    button.setEnabled(false);
    assertFalse(button.isEnabled());
    assertTrue(getButtonElement(button).isDisabled());
  }

  public void testSetTabIndex() {
    // Default tab index is 0;
    ButtonBase<T> button = createButton();
    assertEquals(0, button.getTabIndex());
    assertEquals(0, getButtonElement(button).getTabIndex());

    // Set tab index to 0.
    button.setTabIndex(-1);
    assertEquals(-1, getButtonElement(button).getTabIndex());
  }

  protected abstract ButtonBase<T> createButton();

  /**
   * Get the {@link ButtonElement} from the widget.
   */
  private ButtonElement getButtonElement(ButtonBase<T> widget) {
    return widget.getElement().getFirstChildElement().cast();
  }
}
