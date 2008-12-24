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
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.Widget;

/**
 * Verify that IE returns the correct value for getAbsoluteLeft() when zoomed
 * in. The absolute left coordinate should NOT depend on the zoom. That is, an
 * elements absoluteLeft position should be the same regardless of zoom (IE
 * automatically multiplies it by the zoom).
 */
public class Issue3172 extends AbstractIssue {

  @Override
  public Widget createIssue() {
    // Create a command that will execute on menu item selection
    Command emptyCommand = new Command() {
      public void execute() {
      }
    };

    // Create a menu bar
    MenuBar menu = new MenuBar();
    menu.setWidth("500px");
    menu.setAutoOpen(true);

    // Create a sub menu of recent documents
    MenuBar recentDocsMenu = new MenuBar(true);
    recentDocsMenu.addItem("Document 0", emptyCommand);
    recentDocsMenu.addItem("Document 1", emptyCommand);
    recentDocsMenu.addItem("Document 2", emptyCommand);

    // Create the file menu
    MenuBar fileMenu = new MenuBar(true);
    menu.addItem(new MenuItem("File", fileMenu));
    fileMenu.addItem("New", emptyCommand);
    fileMenu.addItem("Print", emptyCommand);
    fileMenu.addItem("Recent Docs", recentDocsMenu);

    // Create the edit menu
    MenuBar editMenu = new MenuBar(true);
    menu.addItem(new MenuItem("Edit", editMenu));
    editMenu.addItem("Cut", emptyCommand);
    editMenu.addItem("Copy", emptyCommand);
    editMenu.addItem("Paste", emptyCommand);

    // Create the help menu
    MenuBar helpMenu = new MenuBar(true);
    menu.addItem(new MenuItem("Help", helpMenu));
    helpMenu.addItem("Settings", emptyCommand);
    helpMenu.addItem("About", emptyCommand);

    return menu;
  }

  @Override
  public String getInstructions() {
    return "In IE, press Ctrl++ to zoom in, then verify that the sub menus open"
        + " in the correct locations.";
  }

  @Override
  public String getSummary() {
    return "getAbsoluteLeft() with zoom in IE";
  }

  @Override
  public boolean hasCSS() {
    return false;
  }
}
