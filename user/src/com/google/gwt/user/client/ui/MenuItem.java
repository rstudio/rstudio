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

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.safehtml.client.HasSafeHtml;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;

/**
 * An entry in a
 * {@link com.google.gwt.user.client.ui.MenuBar}. Menu items can either fire a
 * {@link com.google.gwt.core.client.Scheduler.ScheduledCommand} when they are clicked, or open a
 * cascading sub-menu.
 *
 * Each menu item is assigned a unique DOM id in order to support ARIA. See
 * {@link com.google.gwt.user.client.ui.Accessibility} for more information.
 */
public class MenuItem extends UIObject implements HasHTML, HasEnabled, HasSafeHtml {

  private static final String DEPENDENT_STYLENAME_SELECTED_ITEM = "selected";
  private static final String DEPENDENT_STYLENAME_DISABLED_ITEM = "disabled";

  private ScheduledCommand command;
  private MenuBar parentMenu, subMenu;
  private boolean enabled = true;

  /**
   * Constructs a new menu item that fires a command when it is selected.
   *
   * @param html the item's html text
   */
  public MenuItem(SafeHtml html) {
    this(html.asString(), true);
  }

  /**
   * Constructs a new menu item that fires a command when it is selected.
   *
   * @param html the item's text
   * @param cmd the command to be fired when it is selected
   */
  public MenuItem(SafeHtml html, ScheduledCommand cmd) {
    this(html.asString(), true, cmd);
  }

  /**
   * Constructs a new menu item that cascades to a sub-menu when it is selected.
   *
   * @param html the item's text
   * @param subMenu the sub-menu to be displayed when it is selected
   */
  public MenuItem(SafeHtml html, MenuBar subMenu) {
    this(html.asString(), true, subMenu);
  }

  /**
   * Constructs a new menu item that fires a command when it is selected.
   *
   * @param text the item's text
   * @param asHTML <code>true</code> to treat the specified text as html
   * @param cmd the command to be fired when it is selected
   */
  public MenuItem(String text, boolean asHTML, ScheduledCommand cmd) {
    this(text, asHTML);
    setScheduledCommand(cmd);
  }

  /**
   * Constructs a new menu item that cascades to a sub-menu when it is selected.
   *
   * @param text the item's text
   * @param asHTML <code>true</code> to treat the specified text as html
   * @param subMenu the sub-menu to be displayed when it is selected
   */
  public MenuItem(String text, boolean asHTML, MenuBar subMenu) {
    this(text, asHTML);
    setSubMenu(subMenu);
  }

  /**
   * Constructs a new menu item that fires a command when it is selected.
   *
   * @param text the item's text
   * @param cmd the command to be fired when it is selected
   */
  public MenuItem(String text, ScheduledCommand cmd) {
    this(text, false);
    setScheduledCommand(cmd);
  }

  /**
   * Constructs a new menu item that cascades to a sub-menu when it is selected.
   *
   * @param text the item's text
   * @param subMenu the sub-menu to be displayed when it is selected
   */
  public MenuItem(String text, MenuBar subMenu) {
    this(text, false);
    setSubMenu(subMenu);
  }

  MenuItem(String text, boolean asHTML) {
    setElement(DOM.createTD());
    setSelectionStyle(false);

    if (asHTML) {
      setHTML(text);
    } else {
      setText(text);
    }
    setStyleName("gwt-MenuItem");

    DOM.setElementAttribute(getElement(), "id", DOM.createUniqueId());
    // Add a11y role "menuitem"
    Roles.getMenuitemRole().set(getElement());
  }

  /**
   * Gets the command associated with this item.  If a scheduled command
   * is associated with this item a command that can be used to execute the
   * scheduled command will be returned.
   *
   * @return the command
   * @deprecated use {@link #getScheduledCommand()} instead
   */
  @Deprecated
  public Command getCommand() {
    Command rtnVal;

    if (command == null) {
      rtnVal = null;
    } else if (command instanceof Command) {
      rtnVal = (Command) command;
    } else {
      rtnVal = new Command() {
        @Override
        public void execute() {
          if (command != null) {
            command.execute();
          }
        }
      };
    }

    return rtnVal;
  }

  @Override
  public String getHTML() {
    return DOM.getInnerHTML(getElement());
  }

  /**
   * Gets the menu that contains this item.
   *
   * @return the parent menu, or <code>null</code> if none exists.
   */
  public MenuBar getParentMenu() {
    return parentMenu;
  }

  /**
   * Gets the scheduled command associated with this item.
   *
   * @return this item's scheduled command, or <code>null</code> if none exists
   */
  public ScheduledCommand getScheduledCommand() {
    return command;
  }

  /**
   * Gets the sub-menu associated with this item.
   *
   * @return this item's sub-menu, or <code>null</code> if none exists
   */
  public MenuBar getSubMenu() {
    return subMenu;
  }

  @Override
  public String getText() {
    return DOM.getInnerText(getElement());
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Sets the command associated with this item.
   *
   * @param cmd the command to be associated with this item
   * @deprecated use {@link #setScheduledCommand(ScheduledCommand)} instead
   */
  @Deprecated
  public void setCommand(Command cmd) {
    command = cmd;
  }

  @Override
  public void setEnabled(boolean enabled) {
    if (enabled) {
      removeStyleDependentName(DEPENDENT_STYLENAME_DISABLED_ITEM);
    } else {
      addStyleDependentName(DEPENDENT_STYLENAME_DISABLED_ITEM);
    }
    this.enabled = enabled;
  }

  @Override
  public void setHTML(SafeHtml html) {
    setHTML(html.asString());
  }

  @Override
  public void setHTML(String html) {
    DOM.setInnerHTML(getElement(), html);
  }

  /**
   * Sets the scheduled command associated with this item.
   *
   * @param cmd the scheduled command to be associated with this item
   */
  public void setScheduledCommand(ScheduledCommand cmd) {
    command = cmd;
  }

  /**
   * Sets the sub-menu associated with this item.
   *
   * @param subMenu this item's new sub-menu
   */
  public void setSubMenu(MenuBar subMenu) {
    this.subMenu = subMenu;
    if (this.parentMenu != null) {
      this.parentMenu.updateSubmenuIcon(this);
    }

    if (subMenu != null) {
      // Change tab index from 0 to -1, because only the root menu is supposed
      // to be in the tab order
      FocusPanel.impl.setTabIndex(subMenu.getElement(), -1);

      // Update a11y role "haspopup"
      Roles.getMenuitemRole().setAriaHaspopupProperty(getElement(), true);
    } else {
      // Update a11y role "haspopup"
      Roles.getMenuitemRole().setAriaHaspopupProperty(getElement(), false);
    }
  }

  @Override
  public void setText(String text) {
    DOM.setInnerText(getElement(), text);
  }

  /**
   * Also sets the Debug IDs of MenuItems in the submenu of this
   * {@link MenuItem} if a submenu exists.
   *
   * @see UIObject#onEnsureDebugId(String)
   */
  @Override
  protected void onEnsureDebugId(String baseID) {
    super.onEnsureDebugId(baseID);
    if (subMenu != null) {
      subMenu.setMenuItemDebugIds(baseID);
    }
  }

  protected void setSelectionStyle(boolean selected) {
    if (selected) {
      addStyleDependentName(DEPENDENT_STYLENAME_SELECTED_ITEM);
    } else {
      removeStyleDependentName(DEPENDENT_STYLENAME_SELECTED_ITEM);
    }
  }

  void setParentMenu(MenuBar parentMenu) {
    this.parentMenu = parentMenu;
  }
}