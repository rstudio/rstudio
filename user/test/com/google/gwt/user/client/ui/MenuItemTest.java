/*
 * Copyright 2010 Google Inc.
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
import com.google.gwt.user.client.Command;

/**
 * Tests for {@link MenuItem}.
 */
public class MenuItemTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.UserTest";
  }

  public void testSetCommandWithMenuBar() {
    Command command = new Command() {
      public void execute() {
      }
    };
    MenuBar bar = new MenuBar();
    MenuItem item = bar.addItem("test", command);
    assertEquals(command, item.getCommand());

    item.setCommand(null);
    assertNull(item.getCommand());

    item.setCommand(command);
    assertEquals(command, item.getCommand());
  }

  public void testSetCommandWithoutMenuBar() {
    Command command = new Command() {
      public void execute() {
      }
    };
    MenuItem item = new MenuItem("test", command);
    assertEquals(command, item.getCommand());

    item.setCommand(null);
    assertNull(item.getCommand());

    item.setCommand(command);
    assertEquals(command, item.getCommand());
  }

  public void testSetSubMenuWithMenuBar() {
    MenuBar bar = new MenuBar();
    MenuBar submenu = new MenuBar();
    MenuItem item = bar.addItem("test", submenu);
    assertEquals(submenu, item.getSubMenu());
    assertEquals(-1, FocusPanel.impl.getTabIndex(submenu.getElement()));
    assertEquals("true", Accessibility.getState(item.getElement(),
        Accessibility.STATE_HASPOPUP));

    item.setSubMenu(null);
    assertNull(item.getSubMenu());
    assertEquals("false", Accessibility.getState(item.getElement(),
        Accessibility.STATE_HASPOPUP));

    item.setSubMenu(submenu);
    assertEquals(submenu, item.getSubMenu());
    assertEquals("true", Accessibility.getState(item.getElement(),
        Accessibility.STATE_HASPOPUP));
  }

  public void testSetSubMenuWithoutMenuBar() {
    MenuBar submenu = new MenuBar();
    MenuItem item = new MenuItem("test", submenu);
    assertEquals(submenu, item.getSubMenu());
    assertEquals(-1, FocusPanel.impl.getTabIndex(submenu.getElement()));
    assertEquals("true", Accessibility.getState(item.getElement(),
        Accessibility.STATE_HASPOPUP));

    item.setSubMenu(null);
    assertNull(item.getSubMenu());
    assertEquals("false", Accessibility.getState(item.getElement(),
        Accessibility.STATE_HASPOPUP));

    item.setSubMenu(submenu);
    assertEquals(submenu, item.getSubMenu());
    assertEquals("true", Accessibility.getState(item.getElement(),
        Accessibility.STATE_HASPOPUP));
  }
}
