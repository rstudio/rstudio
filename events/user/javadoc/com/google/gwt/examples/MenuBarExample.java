/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.examples;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.RootPanel;

public class MenuBarExample implements EntryPoint {

  public void onModuleLoad() {
    // Make a command that we will execute from all leaves.
    Command cmd = new Command() {
      public void execute() {
        Window.alert("You selected a menu item!");
      }
    };

    // Make some sub-menus that we will cascade from the top menu.
    MenuBar fooMenu = new MenuBar(true);
    fooMenu.addItem("the", cmd);
    fooMenu.addItem("foo", cmd);
    fooMenu.addItem("menu", cmd);

    MenuBar barMenu = new MenuBar(true);
    barMenu.addItem("the", cmd);
    barMenu.addItem("bar", cmd);
    barMenu.addItem("menu", cmd);

    MenuBar bazMenu = new MenuBar(true);
    bazMenu.addItem("the", cmd);
    bazMenu.addItem("baz", cmd);
    bazMenu.addItem("menu", cmd);

    // Make a new menu bar, adding a few cascading menus to it.
    MenuBar menu = new MenuBar();
    menu.addItem("foo", fooMenu);
    menu.addItem("bar", barMenu);
    menu.addItem("baz", bazMenu);

    // Add it to the root panel.
    RootPanel.get().add(menu);
  }
}