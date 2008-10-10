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
package com.google.gwt.museum.client.defaultmuseum;

import com.google.gwt.museum.client.common.AbstractIssue;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.Widget;

/**
 * The {@link com.google.gwt.user.client.ui.PopupPanel} used to display sub
 * menus in a {@link MenuBar} is not accessible, nor is it under the
 * {@link MenuBar MenuBar's} DOM structure, so it cannot be uniquely styled.
 */
public class Issue1169 extends AbstractIssue {
  /**
   * A command that does not do anything.
   */
  private static Command emptyCommand = new Command() {
    public void execute() {
    }
  };

  @Override
  public Widget createIssue() {
    // Create the main menu bar
    MenuBar menuBar = new MenuBar();
    menuBar.setAutoOpen(true);

    // Change the primary style name
    menuBar.setStylePrimaryName("myMenuBar");

    // Add the original style names so the default styles apply
    menuBar.addStyleName("gwt-MenuBar gwt-MenuBar-horizontal");

    // Add some sub menus, each with a unique style name
    for (int i = 0; i < 3; i++) {
      MenuBar subMenu = new MenuBar(true);
      subMenu.addItem("Item 1", emptyCommand);
      subMenu.addItem("Item 2", emptyCommand);
      subMenu.addItem("Item 3", emptyCommand);
      menuBar.addItem("Option " + i, subMenu);
    }

    return menuBar;
  }

  @Override
  public String getInstructions() {
    return "Verify that each submenu has a red border, but no shadow.";
  }

  @Override
  public String getSummary() {
    return "Cannot apply unique styles to MenuBar popups";
  }

  @Override
  public boolean hasCSS() {
    return true;
  }
}
