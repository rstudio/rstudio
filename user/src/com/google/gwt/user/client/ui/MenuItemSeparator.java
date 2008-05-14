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

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

/**
 * A separator that can be placed in a
 * {@link com.google.gwt.user.client.ui.MenuBar}.
 */
public class MenuItemSeparator extends UIObject {

  private static final String STYLENAME_DEFAULT = "gwt-MenuItemSeparator";

  private MenuBar parentMenu;

  /**
   * Constructs a new {@link MenuItemSeparator}.
   */
  public MenuItemSeparator() {
    setElement(DOM.createTD());
    setStyleName(STYLENAME_DEFAULT);
    
    // Add an inner element for styling purposes
    Element div = DOM.createDiv();
    DOM.appendChild(getElement(), div);
    setStyleName(div, "menuSeparatorInner");
  }

  /**
   * Gets the menu that contains this item.
   * 
   * @return the parent menu, or <code>null</code> if none exists.
   */
  public MenuBar getParentMenu() {
    return parentMenu;
  }

  void setParentMenu(MenuBar parentMenu) {
    this.parentMenu = parentMenu;
  }
}
