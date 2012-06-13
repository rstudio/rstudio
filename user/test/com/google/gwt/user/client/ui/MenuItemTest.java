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

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.Command;

/**
 * Tests for {@link MenuItem}.
 */
public class MenuItemTest extends GWTTestCase {

  private static final String html = "<b>hello</b><i>world</i>";

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.UserTest";
  }

  public void testSafeHtmlWithCommand() {
    Command command = new Command() {
      @Override
      public void execute() {
      }
    };
    MenuItem item = new MenuItem(SafeHtmlUtils.fromSafeConstant(html), command);

    assertEquals(html, item.getHTML().toLowerCase());
    assertEquals(command, item.getCommand());
  }

  public void testSafeHtmlWithScheduledCommand() {
    ScheduledCommand command = new ScheduledCommand() {
      @Override
      public void execute() {
      }
    };
    MenuItem item = new MenuItem(SafeHtmlUtils.fromSafeConstant(html), command);

    assertEquals(html, item.getHTML().toLowerCase());
    assertEquals(command, item.getScheduledCommand());
  }

  public void testSafeHtmlWithSubMenu() {
    MenuBar subMenu = new MenuBar();
    MenuItem item = new MenuItem(SafeHtmlUtils.fromSafeConstant(html), subMenu);

    assertEquals(html, item.getHTML().toLowerCase());
    assertEquals(subMenu, item.getSubMenu());
  }

  public void testSetCommandWithMenuBar() {
    Command command = new Command() {
      @Override
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
      @Override
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

  public void testSetSafeHtmlWithCommand() {
    Command command = new Command() {
      @Override
      public void execute() {
      }
    };
    MenuItem item = new MenuItem("foo", command);
    item.setHTML(SafeHtmlUtils.fromSafeConstant(html));

    assertEquals(html, item.getHTML().toLowerCase());
    assertEquals(command, item.getCommand());
  }

  public void testSetSafeHtmlWithScheduledCommand() {
    ScheduledCommand command = new ScheduledCommand() {
      @Override
      public void execute() {
      }
    };
    MenuItem item = new MenuItem("foo", command);
    item.setHTML(SafeHtmlUtils.fromSafeConstant(html));

    assertEquals(html, item.getHTML().toLowerCase());
    assertEquals(command, item.getScheduledCommand());
  }

  public void testSetScheduledCommandWithMenuBar() {
    ScheduledCommand command = new ScheduledCommand() {
      @Override
      public void execute() {
      }
    };
    MenuBar bar = new MenuBar();
    MenuItem item = bar.addItem("test", command);
    assertEquals(command, item.getScheduledCommand());

    item.setScheduledCommand(null);
    assertNull(item.getScheduledCommand());

    item.setScheduledCommand(command);
    assertEquals(command, item.getScheduledCommand());
  }

  public void testSetScheduledCommandWithoutMenuBar() {
    ScheduledCommand command = new ScheduledCommand() {
      @Override
      public void execute() {
      }
    };
    MenuItem item = new MenuItem("test", command);
    assertEquals(command, item.getScheduledCommand());

    item.setScheduledCommand(null);
    assertNull(item.getScheduledCommand());

    item.setScheduledCommand(command);
    assertEquals(command, item.getScheduledCommand());
  }

  public void testSetSubMenuWithMenuBar() {
    MenuBar bar = new MenuBar();
    MenuBar submenu = new MenuBar();
    MenuItem item = bar.addItem("test", submenu);
    assertEquals(submenu, item.getSubMenu());
    assertEquals(-1, FocusPanel.impl.getTabIndex(submenu.getElement()));

    assertEquals("true", Roles.getMenuitemRole().getAriaHaspopupProperty(item.getElement()));

    item.setSubMenu(null);
    assertNull(item.getSubMenu());
    assertEquals("false", Roles.getMenuitemRole().getAriaHaspopupProperty(item.getElement()));

    item.setSubMenu(submenu);
    assertEquals(submenu, item.getSubMenu());
    assertEquals("true", Roles.getMenuitemRole().getAriaHaspopupProperty(item.getElement()));
  }

  public void testSetSubMenuWithoutMenuBar() {
    MenuBar submenu = new MenuBar();
    MenuItem item = new MenuItem("test", submenu);
    assertEquals(submenu, item.getSubMenu());
    assertEquals(-1, FocusPanel.impl.getTabIndex(submenu.getElement()));
    assertEquals("true", Roles.getMenuitemRole().getAriaHaspopupProperty(item.getElement()));

    item.setSubMenu(null);
    assertNull(item.getSubMenu());
    assertEquals("false", Roles.getMenuitemRole().getAriaHaspopupProperty(item.getElement()));

    item.setSubMenu(submenu);
    assertEquals(submenu, item.getSubMenu());
    assertEquals("true", Roles.getMenuitemRole().getAriaHaspopupProperty(item.getElement()));
  }
}
