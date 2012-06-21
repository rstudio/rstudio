/*
 * Copyright 2009 Google Inc.
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

import com.google.gwt.aria.client.IdReference;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.HasCloseHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.ui.PopupPanel.AnimationType;

import java.util.ArrayList;
import java.util.List;

/**
 * A standard menu bar widget. A menu bar can contain any number of menu items,
 * each of which can either fire a {@link com.google.gwt.core.client.Scheduler.ScheduledCommand} or
 * open a cascaded menu bar.
 *
 * <p>
 * <img class='gallery' src='doc-files/MenuBar.png'/>
 * </p>
 *
 * <h3>CSS Style Rules</h3>
 * <dl>
 * <dt>.gwt-MenuBar</dt>
 * <dd>the menu bar itself</dd>
 * <dt>.gwt-MenuBar-horizontal</dt>
 * <dd>dependent style applied to horizontal menu bars</dd>
 * <dt>.gwt-MenuBar-vertical</dt>
 * <dd>dependent style applied to vertical menu bars</dd>
 * <dt>.gwt-MenuBar .gwt-MenuItem</dt>
 * <dd>menu items</dd>
 * <dt>.gwt-MenuBar .gwt-MenuItem-selected</dt>
 * <dd>selected menu items</dd>
 * <dt>.gwt-MenuBar .gwt-MenuItemSeparator</dt>
 * <dd>section breaks between menu items</dd>
 * <dt>.gwt-MenuBar .gwt-MenuItemSeparator .menuSeparatorInner</dt>
 * <dd>inner component of section separators</dd>
 * <dt>.gwt-MenuBarPopup .menuPopupTopLeft</dt>
 * <dd>the top left cell</dd>
 * <dt>.gwt-MenuBarPopup .menuPopupTopLeftInner</dt>
 * <dd>the inner element of the cell</dd>
 * <dt>.gwt-MenuBarPopup .menuPopupTopCenter</dt>
 * <dd>the top center cell</dd>
 * <dt>.gwt-MenuBarPopup .menuPopupTopCenterInner</dt>
 * <dd>the inner element of the cell</dd>
 * <dt>.gwt-MenuBarPopup .menuPopupTopRight</dt>
 * <dd>the top right cell</dd>
 * <dt>.gwt-MenuBarPopup .menuPopupTopRightInner</dt>
 * <dd>the inner element of the cell</dd>
 * <dt>.gwt-MenuBarPopup .menuPopupMiddleLeft</dt>
 * <dd>the middle left cell</dd>
 * <dt>.gwt-MenuBarPopup .menuPopupMiddleLeftInner</dt>
 * <dd>the inner element of the cell</dd>
 * <dt>.gwt-MenuBarPopup .menuPopupMiddleCenter</dt>
 * <dd>the middle center cell</dd>
 * <dt>.gwt-MenuBarPopup .menuPopupMiddleCenterInner</dt>
 * <dd>the inner element of the cell</dd>
 * <dt>.gwt-MenuBarPopup .menuPopupMiddleRight</dt>
 * <dd>the middle right cell</dd>
 * <dt>.gwt-MenuBarPopup .menuPopupMiddleRightInner</dt>
 * <dd>the inner element of the cell</dd>
 * <dt>.gwt-MenuBarPopup .menuPopupBottomLeft</dt>
 * <dd>the bottom left cell</dd>
 * <dt>.gwt-MenuBarPopup .menuPopupBottomLeftInner</dt>
 * <dd>the inner element of the cell</dd>
 * <dt>.gwt-MenuBarPopup .menuPopupBottomCenter</dt>
 * <dd>the bottom center cell</dd>
 * <dt>.gwt-MenuBarPopup .menuPopupBottomCenterInner</dt>
 * <dd>the inner element of the cell</dd>
 * <dt>.gwt-MenuBarPopup .menuPopupBottomRight</dt>
 * <dd>the bottom right cell</dd>
 * <dt>.gwt-MenuBarPopup .menuPopupBottomRightInner</dt>
 * <dd>the inner element of the cell</dd>
 * </dl>
 *
 * <p>
 * <h3>Example</h3>
 * {@example com.google.gwt.examples.MenuBarExample}
 * </p>
 *
 * <h3>Use in UiBinder Templates</h3>
 * <p>
 * MenuBar elements in UiBinder template files can have a <code>vertical</code>
 * boolean attribute (which defaults to false), and may have only MenuItem
 * elements as children. MenuItems may contain HTML and MenuBars.
 * <p>
 * For example:
 *
 * <pre>
 * &lt;g:MenuBar>
 *   &lt;g:MenuItem>Higgledy
 *     &lt;g:MenuBar vertical="true">
 *       &lt;g:MenuItem>able&lt;/g:MenuItem>
 *       &lt;g:MenuItem>baker&lt;/g:MenuItem>
 *       &lt;g:MenuItem>charlie&lt;/g:MenuItem>
 *     &lt;/g:MenuBar>
 *   &lt;/g:MenuItem>
 *   &lt;g:MenuItem>Piggledy
 *     &lt;g:MenuBar vertical="true">
 *       &lt;g:MenuItem>foo&lt;/g:MenuItem>
 *       &lt;g:MenuItem>bar&lt;/g:MenuItem>
 *       &lt;g:MenuItem>baz&lt;/g:MenuItem>
 *     &lt;/g:MenuBar>
 *   &lt;/g:MenuItem>
 *   &lt;g:MenuItem>&lt;b>Pop!&lt;/b>
 *     &lt;g:MenuBar vertical="true">
 *       &lt;g:MenuItem>uno&lt;/g:MenuItem>
 *       &lt;g:MenuItem>dos&lt;/g:MenuItem>
 *       &lt;g:MenuItem>tres&lt;/g:MenuItem>
 *     &lt;/g:MenuBar>
 *   &lt;/g:MenuItem>
 * &lt;/g:MenuBar>
 * </pre>
 */
// Nothing we can do about MenuBar implementing PopupListener until next
// release.
@SuppressWarnings("deprecation")
public class MenuBar extends Widget implements PopupListener, HasAnimation,
    HasCloseHandlers<PopupPanel> {

  /**
   * An {@link ImageBundle} that provides images for {@link MenuBar}.
   *
   * @deprecated replaced by {@link Resources}
   */
  @Deprecated
  public interface MenuBarImages extends ImageBundle {
    /**
     * An image indicating a {@link MenuItem} has an associated submenu.
     *
     * @return a prototype of this image
     */
    AbstractImagePrototype menuBarSubMenuIcon();
  }

  /**
   * A ClientBundle that contains the default resources for this widget.
   */
  public interface Resources extends ClientBundle {
    /**
     * An image indicating a {@link MenuItem} has an associated submenu.
     */
    @ImageOptions(flipRtl = true)
    ImageResource menuBarSubMenuIcon();
  }

  private static final String STYLENAME_DEFAULT = "gwt-MenuBar";

  /**
   * List of all {@link MenuItem}s and {@link MenuItemSeparator}s.
   */
  private ArrayList<UIObject> allItems = new ArrayList<UIObject>();

  /**
   * List of {@link MenuItem}s, not including {@link MenuItemSeparator}s.
   */
  private ArrayList<MenuItem> items = new ArrayList<MenuItem>();

  private Element body;

  private AbstractImagePrototype subMenuIcon = null;
  private boolean isAnimationEnabled = false;
  private MenuBar parentMenu;
  private PopupPanel popup;
  private MenuItem selectedItem;
  private MenuBar shownChildMenu;
  private boolean vertical, autoOpen;
  private boolean focusOnHover = true;

  /**
   * Creates an empty horizontal menu bar.
   */
  public MenuBar() {
    this(false);
  }

  /**
   * Creates an empty menu bar.
   *
   * @param vertical <code>true</code> to orient the menu bar vertically
   */
  public MenuBar(boolean vertical) {
    this(vertical, GWT.<Resources> create(Resources.class));
  }

  /**
   * Creates an empty menu bar that uses the specified image bundle for menu
   * images.
   *
   * @param vertical <code>true</code> to orient the menu bar vertically
   * @param images a bundle that provides images for this menu
   * @deprecated replaced by {@link #MenuBar(boolean, Resources)}
   */
  @Deprecated
  public MenuBar(boolean vertical, MenuBarImages images) {
    init(vertical, images.menuBarSubMenuIcon());
  }

  /**
   * Creates an empty menu bar that uses the specified ClientBundle for menu
   * images.
   *
   * @param vertical <code>true</code> to orient the menu bar vertically
   * @param resources a bundle that provides images for this menu
   */
  public MenuBar(boolean vertical, Resources resources) {
    init(vertical,
        AbstractImagePrototype.create(resources.menuBarSubMenuIcon()));
  }

  /**
   * Creates an empty horizontal menu bar that uses the specified image bundle
   * for menu images.
   *
   * @param images a bundle that provides images for this menu
   * @deprecated replaced by {@link #MenuBar(Resources)}
   */
  @Deprecated
  public MenuBar(MenuBarImages images) {
    this(false, images);
  }

  /**
   * Creates an empty horizontal menu bar that uses the specified ClientBundle
   * for menu images.
   *
   * @param resources a bundle that provides images for this menu
   */
  public MenuBar(Resources resources) {
    this(false, resources);
  }

  @Override
  public HandlerRegistration addCloseHandler(CloseHandler<PopupPanel> handler) {
    return addHandler(handler, CloseEvent.getType());
  }

  /**
   * Adds a menu item to the bar.
   *
   * @param item the item to be added
   * @return the {@link MenuItem} object
   */
  public MenuItem addItem(MenuItem item) {
    return insertItem(item, allItems.size());
  }

  /**
   * Adds a menu item to the bar containing SafeHtml, that will fire the given
   * command when it is selected.
   *
   * @param html the item's html text
   * @param cmd the command to be fired
   * @return the {@link MenuItem} object created
   */
  public MenuItem addItem(SafeHtml html, ScheduledCommand cmd) {
    return addItem(new MenuItem(html, cmd));
  }

  /**
   * Adds a menu item to the bar, that will fire the given command when it is
   * selected.
   *
   * @param text the item's text
   * @param asHTML <code>true</code> to treat the specified text as html
   * @param cmd the command to be fired
   * @return the {@link MenuItem} object created
   */
  public MenuItem addItem(String text, boolean asHTML, ScheduledCommand cmd) {
    return addItem(new MenuItem(text, asHTML, cmd));
  }

  /**
   * Adds a menu item to the bar, that will open the specified menu when it is
   * selected.
   *
   * @param html the item's html text
   * @param popup the menu to be cascaded from it
   * @return the {@link MenuItem} object created
   */
  public MenuItem addItem(SafeHtml html, MenuBar popup) {
    return addItem(new MenuItem(html, popup));
  }

  /**
   * Adds a menu item to the bar, that will open the specified menu when it is
   * selected.
   *
   * @param text the item's text
   * @param asHTML <code>true</code> to treat the specified text as html
   * @param popup the menu to be cascaded from it
   * @return the {@link MenuItem} object created
   */
  public MenuItem addItem(String text, boolean asHTML, MenuBar popup) {
    return addItem(new MenuItem(text, asHTML, popup));
  }

  /**
   * Adds a menu item to the bar, that will fire the given command when it is
   * selected.
   *
   * @param text the item's text
   * @param cmd the command to be fired
   * @return the {@link MenuItem} object created
   */
  public MenuItem addItem(String text, ScheduledCommand cmd) {
    return addItem(new MenuItem(text, cmd));
  }

  /**
   * Adds a menu item to the bar, that will open the specified menu when it is
   * selected.
   *
   * @param text the item's text
   * @param popup the menu to be cascaded from it
   * @return the {@link MenuItem} object created
   */
  public MenuItem addItem(String text, MenuBar popup) {
    return addItem(new MenuItem(text, popup));
  }

  /**
   * Adds a thin line to the {@link MenuBar} to separate sections of
   * {@link MenuItem}s.
   *
   * @return the {@link MenuItemSeparator} object created
   */
  public MenuItemSeparator addSeparator() {
    return addSeparator(new MenuItemSeparator());
  }

  /**
   * Adds a thin line to the {@link MenuBar} to separate sections of
   * {@link MenuItem}s.
   *
   * @param separator the {@link MenuItemSeparator} to be added
   * @return the {@link MenuItemSeparator} object
   */
  public MenuItemSeparator addSeparator(MenuItemSeparator separator) {
    return insertSeparator(separator, allItems.size());
  }

  /**
   * Removes all menu items from this menu bar.
   */
  public void clearItems() {
    // Deselect the current item
    selectItem(null);

    Element container = getItemContainerElement();
    while (DOM.getChildCount(container) > 0) {
      DOM.removeChild(container, DOM.getChild(container, 0));
    }

    // Set the parent of all items to null
    for (UIObject item : allItems) {
      setItemColSpan(item, 1);
      if (item instanceof MenuItemSeparator) {
        ((MenuItemSeparator) item).setParentMenu(null);
      } else {
        ((MenuItem) item).setParentMenu(null);
      }
    }

    // Clear out all of the items and separators
    items.clear();
    allItems.clear();
  }

  /**
   * Closes this menu and all child menu popups.
   *
   * @param focus true to move focus to the parent
   */
  public void closeAllChildren(boolean focus) {
    if (shownChildMenu != null) {
      // Hide any open submenus of this item
      shownChildMenu.onHide(focus);
      shownChildMenu = null;
      selectItem(null);
    }
    // Close the current popup
    if (popup != null) {
      popup.hide();
    }
    // If focus is true, set focus to parentMenu
    if (focus && parentMenu != null) {
      parentMenu.focus();
    }
  }

  /**
   * Give this MenuBar focus.
   */
  public void focus() {
    FocusPanel.impl.focus(getElement());
  }

  /**
   * Gets whether this menu bar's child menus will open when the mouse is moved
   * over it.
   *
   * @return <code>true</code> if child menus will auto-open
   */
  public boolean getAutoOpen() {
    return autoOpen;
  }

  /**
   * Get the index of a {@link MenuItem}.
   *
   * @return the index of the item, or -1 if it is not contained by this MenuBar
   */
  public int getItemIndex(MenuItem item) {
    return allItems.indexOf(item);
  }

  /**
   * Get the index of a {@link MenuItemSeparator}.
   *
   * @return the index of the separator, or -1 if it is not contained by this
   *         MenuBar
   */
  public int getSeparatorIndex(MenuItemSeparator item) {
    return allItems.indexOf(item);
  }

  /**
   * Adds a menu item to the bar at a specific index.
   *
   * @param item the item to be inserted
   * @param beforeIndex the index where the item should be inserted
   * @return the {@link MenuItem} object
   * @throws IndexOutOfBoundsException if <code>beforeIndex</code> is out of
   *           range
   */
  public MenuItem insertItem(MenuItem item, int beforeIndex)
      throws IndexOutOfBoundsException {
    // Check the bounds
    if (beforeIndex < 0 || beforeIndex > allItems.size()) {
      throw new IndexOutOfBoundsException();
    }

    // Add to the list of items
    allItems.add(beforeIndex, item);
    int itemsIndex = 0;
    for (int i = 0; i < beforeIndex; i++) {
      if (allItems.get(i) instanceof MenuItem) {
        itemsIndex++;
      }
    }
    items.add(itemsIndex, item);

    // Setup the menu item
    addItemElement(beforeIndex, item.getElement());
    item.setParentMenu(this);
    item.setSelectionStyle(false);
    updateSubmenuIcon(item);
    return item;
  }

  /**
   * Adds a thin line to the {@link MenuBar} to separate sections of
   * {@link MenuItem}s at the specified index.
   *
   * @param beforeIndex the index where the separator should be inserted
   * @return the {@link MenuItemSeparator} object
   * @throws IndexOutOfBoundsException if <code>beforeIndex</code> is out of
   *           range
   */
  public MenuItemSeparator insertSeparator(int beforeIndex) {
    return insertSeparator(new MenuItemSeparator(), beforeIndex);
  }

  /**
   * Adds a thin line to the {@link MenuBar} to separate sections of
   * {@link MenuItem}s at the specified index.
   *
   * @param separator the {@link MenuItemSeparator} to be inserted
   * @param beforeIndex the index where the separator should be inserted
   * @return the {@link MenuItemSeparator} object
   * @throws IndexOutOfBoundsException if <code>beforeIndex</code> is out of
   *           range
   */
  public MenuItemSeparator insertSeparator(MenuItemSeparator separator,
      int beforeIndex) throws IndexOutOfBoundsException {
    // Check the bounds
    if (beforeIndex < 0 || beforeIndex > allItems.size()) {
      throw new IndexOutOfBoundsException();
    }

    if (vertical) {
      setItemColSpan(separator, 2);
    }
    addItemElement(beforeIndex, separator.getElement());
    separator.setParentMenu(this);
    allItems.add(beforeIndex, separator);
    return separator;
  }

  @Override
  public boolean isAnimationEnabled() {
    return isAnimationEnabled;
  }

  /**
   * Check whether or not this widget will steal keyboard focus when the mouse
   * hovers over it.
   *
   * @return true if enabled, false if disabled
   */
  public boolean isFocusOnHoverEnabled() {
    return focusOnHover;
  }

  /**
   * Moves the menu selection down to the next item. If there is no selection,
   * selects the first item. If there are no items at all, does nothing.
   */
  public void moveSelectionDown() {
    if (selectFirstItemIfNoneSelected()) {
      return;
    }

    if (vertical) {
      selectNextItem();
    } else {
      if (selectedItem.getSubMenu() != null
          && !selectedItem.getSubMenu().getItems().isEmpty()
          && (shownChildMenu == null || shownChildMenu.getSelectedItem() == null)) {
        if (shownChildMenu == null) {
          doItemAction(selectedItem, false, true);
        }
        selectedItem.getSubMenu().focus();
      } else if (parentMenu != null) {
        if (parentMenu.vertical) {
          parentMenu.selectNextItem();
        } else {
          parentMenu.moveSelectionDown();
        }
      }
    }
  }

  /**
   * Moves the menu selection up to the previous item. If there is no selection,
   * selects the first item. If there are no items at all, does nothing.
   */
  public void moveSelectionUp() {
    if (selectFirstItemIfNoneSelected()) {
      return;
    }

    if ((shownChildMenu == null) && vertical) {
      selectPrevItem();
    } else if ((parentMenu != null) && parentMenu.vertical) {
      parentMenu.selectPrevItem();
    } else {
      close(true);
    }
  }

  @Override
  public void onBrowserEvent(Event event) {
    MenuItem item = findItem(DOM.eventGetTarget(event));
    switch (DOM.eventGetType(event)) {
      case Event.ONCLICK: {
        FocusPanel.impl.focus(getElement());
        // Fire an item's command when the user clicks on it.
        if (item != null) {
          doItemAction(item, true, true);
        }
        break;
      }

      case Event.ONMOUSEOVER: {
        if (item != null) {
          itemOver(item, true);
        }
        break;
      }

      case Event.ONMOUSEOUT: {
        if (item != null) {
          itemOver(null, true);
        }
        break;
      }

      case Event.ONFOCUS: {
        selectFirstItemIfNoneSelected();
        break;
      }

      case Event.ONKEYDOWN: {
        int keyCode = DOM.eventGetKeyCode(event);
        switch (keyCode) {
          case KeyCodes.KEY_LEFT:
            if (LocaleInfo.getCurrentLocale().isRTL()) {
              moveToNextItem();
            } else {
              moveToPrevItem();
            }
            eatEvent(event);
            break;
          case KeyCodes.KEY_RIGHT:
            if (LocaleInfo.getCurrentLocale().isRTL()) {
              moveToPrevItem();
            } else {
              moveToNextItem();
            }
            eatEvent(event);
            break;
          case KeyCodes.KEY_UP:
            moveSelectionUp();
            eatEvent(event);
            break;
          case KeyCodes.KEY_DOWN:
            moveSelectionDown();
            eatEvent(event);
            break;
          case KeyCodes.KEY_ESCAPE:
            closeAllParentsAndChildren();
            eatEvent(event);
            break;
          case KeyCodes.KEY_TAB:
            closeAllParentsAndChildren();
            break;
          case KeyCodes.KEY_ENTER:
            if (!selectFirstItemIfNoneSelected()) {
              doItemAction(selectedItem, true, true);
              eatEvent(event);
            }
            break;
        } // end switch(keyCode)

        break;
      } // end case Event.ONKEYDOWN
    } // end switch (DOM.eventGetType(event))
    super.onBrowserEvent(event);
  }

  /**
   * Closes the menu bar.
   *
   * @deprecated Use {@link #addCloseHandler(CloseHandler)} instead
   */
  @Override
  @Deprecated
  public void onPopupClosed(PopupPanel sender, boolean autoClosed) {
    // If the menu popup was auto-closed, close all of its parents as well.
    if (autoClosed) {
      closeAllParents();
    }

    // When the menu popup closes, remember that no item is
    // currently showing a popup menu.
    onHide(!autoClosed);
    CloseEvent.fire(MenuBar.this, sender);
    shownChildMenu = null;
    popup = null;
    if (parentMenu != null && parentMenu.popup != null) {
      parentMenu.popup.setPreviewingAllNativeEvents(true);
    }
  }

  /**
   * Removes the specified menu item from the bar.
   *
   * @param item the item to be removed
   */
  public void removeItem(MenuItem item) {
    // Unselect if the item is currently selected
    if (selectedItem == item) {
      selectItem(null);
    }

    if (removeItemElement(item)) {
      setItemColSpan(item, 1);
      items.remove(item);
      item.setParentMenu(null);
    }
  }

  /**
   * Removes the specified {@link MenuItemSeparator} from the bar.
   *
   * @param separator the separator to be removed
   */
  public void removeSeparator(MenuItemSeparator separator) {
    if (removeItemElement(separator)) {
      separator.setParentMenu(null);
    }
  }

  /**
   * Select the given MenuItem, which must be a direct child of this MenuBar.
   *
   * @param item the MenuItem to select, or null to clear selection
   */
  public void selectItem(MenuItem item) {
    assert item == null || item.getParentMenu() == this;

    if (item == selectedItem) {
      return;
    }

    if (selectedItem != null) {
      selectedItem.setSelectionStyle(false);
      // Set the style of the submenu indicator
      if (vertical) {
        Element tr = DOM.getParent(selectedItem.getElement());
        if (DOM.getChildCount(tr) == 2) {
          Element td = DOM.getChild(tr, 1);
          setStyleName(td, "subMenuIcon-selected", false);
        }
      }
    }

    if (item != null) {
      item.setSelectionStyle(true);

      // Set the style of the submenu indicator
      if (vertical) {
        Element tr = DOM.getParent(item.getElement());
        if (DOM.getChildCount(tr) == 2) {
          Element td = DOM.getChild(tr, 1);
          setStyleName(td, "subMenuIcon-selected", true);
        }
      }

      Roles.getMenubarRole().setAriaActivedescendantProperty(getElement(),
          IdReference.of(DOM.getElementAttribute(item.getElement(), "id")));
    }

    selectedItem = item;
  }

  @Override
  public void setAnimationEnabled(boolean enable) {
    isAnimationEnabled = enable;
  }

  /**
   * Sets whether this menu bar's child menus will open when the mouse is moved
   * over it.
   *
   * @param autoOpen <code>true</code> to cause child menus to auto-open
   */
  public void setAutoOpen(boolean autoOpen) {
    this.autoOpen = autoOpen;
  }

  /**
   * Enable or disable auto focus when the mouse hovers over the MenuBar. This
   * allows the MenuBar to respond to keyboard events without the user having to
   * click on it, but it will steal focus from other elements on the page.
   * Enabled by default.
   *
   * @param enabled true to enable, false to disable
   */
  public void setFocusOnHoverEnabled(boolean enabled) {
    focusOnHover = enabled;
  }

  /**
   * Returns a list containing the <code>MenuItem</code> objects in the menu
   * bar. If there are no items in the menu bar, then an empty <code>List</code>
   * object will be returned.
   *
   * @return a list containing the <code>MenuItem</code> objects in the menu bar
   */
  protected List<MenuItem> getItems() {
    return this.items;
  }

  /**
   * Returns the <code>MenuItem</code> that is currently selected (highlighted)
   * by the user. If none of the items in the menu are currently selected, then
   * <code>null</code> will be returned.
   *
   * @return the <code>MenuItem</code> that is currently selected, or
   *         <code>null</code> if no items are currently selected
   */
  protected MenuItem getSelectedItem() {
    return this.selectedItem;
  }

  @Override
  protected void onDetach() {
    // When the menu is detached, make sure to close all of its children.
    if (popup != null) {
      popup.hide();
    }

    super.onDetach();
  }

  /**
   * <b>Affected Elements:</b>
   * <ul>
   * <li>-item# = the {@link MenuItem} at the specified index.</li>
   * </ul>
   *
   * @see UIObject#onEnsureDebugId(String)
   */
  @Override
  protected void onEnsureDebugId(String baseID) {
    super.onEnsureDebugId(baseID);
    setMenuItemDebugIds(baseID);
  }

  /*
   * Closes all parent menu popups.
   */
  void closeAllParents() {
    if (parentMenu != null) {
      // The parent menu will recursively call closeAllParents.
      close(false);
    } else {
      // If this is the top most menu, deselect the current item.
      selectItem(null);
    }
  }

  /**
   * Closes all parent and child menu popups.
   */
  void closeAllParentsAndChildren() {
    closeAllParents();
    // Ensure the popup is closed even if it has not been enetered
    // with the mouse or key navigation
    if (parentMenu == null && popup != null) {
      popup.hide();
    }
  }

  /*
   * Performs the action associated with the given menu item. If the item has a
   * popup associated with it, the popup will be shown. If it has a command
   * associated with it, and 'fireCommand' is true, then the command will be
   * fired. Popups associated with other items will be hidden.
   *
   * @param item the item whose popup is to be shown. @param fireCommand
   * <code>true</code> if the item's command should be fired, <code>false</code>
   * otherwise.
   */
  void doItemAction(final MenuItem item, boolean fireCommand, boolean focus) {
    // Should not perform any action if the item is disabled
    if (!item.isEnabled()) {
      return;
    }

    // Ensure that the item is selected.
    selectItem(item);

    // if the command should be fired and the item has one, fire it
    if (fireCommand && item.getScheduledCommand() != null) {
      // Close this menu and all of its parents.
      closeAllParents();

      // Fire the item's command. The command must be fired in the same event
      // loop or popup blockers will prevent popups from opening.
      final ScheduledCommand cmd = item.getScheduledCommand();
      Scheduler.get().scheduleFinally(new Scheduler.ScheduledCommand() {
        @Override
        public void execute() {
          cmd.execute();
        }
      });

      // hide any open submenus of this item
      if (shownChildMenu != null) {
        shownChildMenu.onHide(focus);
        popup.hide();
        shownChildMenu = null;
        selectItem(null);
      }
    } else if (item.getSubMenu() != null) {
      if (shownChildMenu == null) {
        // open this submenu
        openPopup(item);
      } else if (item.getSubMenu() != shownChildMenu) {
        // close the other submenu and open this one
        shownChildMenu.onHide(focus);
        popup.hide();
        openPopup(item);
      } else if (fireCommand && !autoOpen) {
        // close this submenu
        shownChildMenu.onHide(focus);
        popup.hide();
        shownChildMenu = null;
        selectItem(item);
      }
    } else if (autoOpen && shownChildMenu != null) {
      // close submenu
      shownChildMenu.onHide(focus);
      popup.hide();
      shownChildMenu = null;
    }
  }

  /**
   * Visible for testing.
   */
  PopupPanel getPopup() {
    return popup;
  }

  void itemOver(MenuItem item, boolean focus) {
    if (item == null) {
      // Don't clear selection if the currently selected item's menu is showing.
      if ((selectedItem != null)
          && (shownChildMenu == selectedItem.getSubMenu())) {
        return;
      }
    }

    if (item != null && !item.isEnabled()) {
      return;
    }

    // Style the item selected when the mouse enters.
    selectItem(item);
    if (focus && focusOnHover) {
      focus();
    }

    // If child menus are being shown, or this menu is itself
    // a child menu, automatically show an item's child menu
    // when the mouse enters.
    if (item != null) {
      if ((shownChildMenu != null) || (parentMenu != null) || autoOpen) {
        doItemAction(item, false, focusOnHover);
      }
    }
  }

  /**
   * Set the IDs of the menu items.
   *
   * @param baseID the base ID
   */
  void setMenuItemDebugIds(String baseID) {
    int itemCount = 0;
    for (MenuItem item : items) {
      item.ensureDebugId(baseID + "-item" + itemCount);
      itemCount++;
    }
  }

  /**
   * Show or hide the icon used for items with a submenu.
   *
   * @param item the item with or without a submenu
   */
  void updateSubmenuIcon(MenuItem item) {
    // The submenu icon only applies to vertical menus
    if (!vertical) {
      return;
    }

    // Get the index of the MenuItem
    int idx = allItems.indexOf(item);
    if (idx == -1) {
      return;
    }

    Element container = getItemContainerElement();
    Element tr = DOM.getChild(container, idx);
    int tdCount = DOM.getChildCount(tr);
    MenuBar submenu = item.getSubMenu();
    if (submenu == null) {
      // Remove the submenu indicator
      if (tdCount == 2) {
        DOM.removeChild(tr, DOM.getChild(tr, 1));
      }
      setItemColSpan(item, 2);
    } else if (tdCount == 1) {
      // Show the submenu indicator
      setItemColSpan(item, 1);
      Element td = DOM.createTD();
      DOM.setElementProperty(td, "vAlign", "middle");
      td.setInnerSafeHtml(subMenuIcon.getSafeHtml());
      setStyleName(td, "subMenuIcon");
      DOM.appendChild(tr, td);
    }
  }

  /**
   * Physically add the td element of a {@link MenuItem} or
   * {@link MenuItemSeparator} to this {@link MenuBar}.
   *
   * @param beforeIndex the index where the separator should be inserted
   * @param tdElem the td element to be added
   */
  private void addItemElement(int beforeIndex, Element tdElem) {
    if (vertical) {
      Element tr = DOM.createTR();
      DOM.insertChild(body, tr, beforeIndex);
      DOM.appendChild(tr, tdElem);
    } else {
      Element tr = DOM.getChild(body, 0);
      DOM.insertChild(tr, tdElem, beforeIndex);
    }
  }

  /**
   * Closes this menu (if it is a popup).
   *
   * @param focus true to move focus to the parent
   */
  private void close(boolean focus) {
    if (parentMenu != null) {
      parentMenu.popup.hide(!focus);
      if (focus) {
        parentMenu.focus();
      }
    }
  }

  private void eatEvent(Event event) {
    DOM.eventCancelBubble(event, true);
    DOM.eventPreventDefault(event);
  }

  private MenuItem findItem(Element hItem) {
    for (MenuItem item : items) {
      if (DOM.isOrHasChild(item.getElement(), hItem)) {
        return item;
      }
    }
    return null;
  }

  private Element getItemContainerElement() {
    if (vertical) {
      return body;
    } else {
      return DOM.getChild(body, 0);
    }
  }

  private void init(boolean vertical, AbstractImagePrototype subMenuIcon) {
    this.subMenuIcon = subMenuIcon;

    Element table = DOM.createTable();
    body = DOM.createTBody();
    DOM.appendChild(table, body);

    if (!vertical) {
      Element tr = DOM.createTR();
      DOM.appendChild(body, tr);
    }

    this.vertical = vertical;

    Element outer = FocusPanel.impl.createFocusable();
    DOM.appendChild(outer, table);
    setElement(outer);

    Roles.getMenubarRole().set(getElement());

    sinkEvents(Event.ONCLICK | Event.ONMOUSEOVER | Event.ONMOUSEOUT
        | Event.ONFOCUS | Event.ONKEYDOWN);

    setStyleName(STYLENAME_DEFAULT);
    if (vertical) {
      addStyleDependentName("vertical");
    } else {
      addStyleDependentName("horizontal");
    }

    // Hide focus outline in Mozilla/Webkit/Opera
    DOM.setStyleAttribute(getElement(), "outline", "0px");

    // Hide focus outline in IE 6/7
    DOM.setElementAttribute(getElement(), "hideFocus", "true");

    // Deselect items when blurring without a child menu.
    addDomHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        if (shownChildMenu == null) {
          selectItem(null);
        }
      }
    }, BlurEvent.getType());
  }

  private void moveToNextItem() {
    if (selectFirstItemIfNoneSelected()) {
      return;
    }

    if (!vertical) {
      selectNextItem();
    } else {
      if (selectedItem.getSubMenu() != null
          && !selectedItem.getSubMenu().getItems().isEmpty()
          && (shownChildMenu == null || shownChildMenu.getSelectedItem() == null)) {
        if (shownChildMenu == null) {
          doItemAction(selectedItem, false, true);
        }
        selectedItem.getSubMenu().focus();
      } else if (parentMenu != null) {
        if (!parentMenu.vertical) {
          parentMenu.selectNextItem();
        } else {
          parentMenu.moveToNextItem();
        }
      }
    }
  }

  private void moveToPrevItem() {
    if (selectFirstItemIfNoneSelected()) {
      return;
    }

    if (!vertical) {
      selectPrevItem();
    } else {
      if ((parentMenu != null) && (!parentMenu.vertical)) {
        parentMenu.selectPrevItem();
      } else {
        close(true);
      }
    }
  }

  /*
   * This method is called when a menu bar is hidden, so that it can hide any
   * child popups that are currently being shown.
   */
  private void onHide(boolean focus) {
    if (shownChildMenu != null) {
      shownChildMenu.onHide(focus);
      popup.hide();
      if (focus) {
        focus();
      }
    }
  }

  /*
   * This method is called when a menu bar is shown.
   */
  private void onShow() {
    // clear the selection; a keyboard user can cursor down to the first item
    selectItem(null);
  }

  private void openPopup(final MenuItem item) {
    // Only the last popup to be opened should preview all event
    if (parentMenu != null && parentMenu.popup != null) {
      parentMenu.popup.setPreviewingAllNativeEvents(false);
    }

    // Create a new popup for this item, and position it next to
    // the item (below if this is a horizontal menu bar, to the
    // right if it's a vertical bar).
    popup = new DecoratedPopupPanel(true, false, "menuPopup") {
      {
        setWidget(item.getSubMenu());
        setPreviewingAllNativeEvents(true);
        item.getSubMenu().onShow();
      }

      @Override
      protected void onPreviewNativeEvent(NativePreviewEvent event) {
        // Hook the popup panel's event preview. We use this to keep it from
        // auto-hiding when the parent menu is clicked.
        if (!event.isCanceled()) {

          switch (event.getTypeInt()) {
            case Event.ONMOUSEDOWN:
              // If the event target is part of the parent menu, suppress the
              // event altogether.
              EventTarget target = event.getNativeEvent().getEventTarget();
              Element parentMenuElement = item.getParentMenu().getElement();
              if (parentMenuElement.isOrHasChild(Element.as(target))) {
                event.cancel();
                return;
              }
              super.onPreviewNativeEvent(event);
              if (event.isCanceled()) {
                selectItem(null);
              }
              return;
          }
        }
        super.onPreviewNativeEvent(event);
      }
    };
    popup.setAnimationType(AnimationType.ONE_WAY_CORNER);
    popup.setAnimationEnabled(isAnimationEnabled);
    popup.setStyleName(STYLENAME_DEFAULT + "Popup");
    String primaryStyleName = getStylePrimaryName();
    if (!STYLENAME_DEFAULT.equals(primaryStyleName)) {
      popup.addStyleName(primaryStyleName + "Popup");
    }
    popup.addPopupListener(this);

    shownChildMenu = item.getSubMenu();
    item.getSubMenu().parentMenu = this;

    // Show the popup, ensuring that the menubar's event preview remains on top
    // of the popup's.
    popup.setPopupPositionAndShow(new PopupPanel.PositionCallback() {

      @Override
      public void setPosition(int offsetWidth, int offsetHeight) {

        // depending on the bidi direction position a menu on the left or right
        // of its base item
        if (LocaleInfo.getCurrentLocale().isRTL()) {
          if (vertical) {
            popup.setPopupPosition(MenuBar.this.getAbsoluteLeft() - offsetWidth
                + 1, item.getAbsoluteTop());
          } else {
            popup.setPopupPosition(item.getAbsoluteLeft()
                + item.getOffsetWidth() - offsetWidth,
                MenuBar.this.getAbsoluteTop() + MenuBar.this.getOffsetHeight()
                    - 1);
          }
        } else {
          if (vertical) {
            popup.setPopupPosition(MenuBar.this.getAbsoluteLeft()
                + MenuBar.this.getOffsetWidth() - 1, item.getAbsoluteTop());
          } else {
            popup.setPopupPosition(item.getAbsoluteLeft(),
                MenuBar.this.getAbsoluteTop() + MenuBar.this.getOffsetHeight()
                    - 1);
          }
        }
      }
    });
  }

  /**
   * Removes the specified item from the {@link MenuBar} and the physical DOM
   * structure.
   *
   * @param item the item to be removed
   * @return true if the item was removed
   */
  private boolean removeItemElement(UIObject item) {
    int idx = allItems.indexOf(item);
    if (idx == -1) {
      return false;
    }

    Element container = getItemContainerElement();
    DOM.removeChild(container, DOM.getChild(container, idx));
    allItems.remove(idx);
    return true;
  }

  /**
   * Selects the first item in the menu if no items are currently selected. Has
   * no effect if there are no items.
   *
   * @return true if no item was previously selected, false otherwise
   */
  private boolean selectFirstItemIfNoneSelected() {
    if (selectedItem == null) {
      for (MenuItem nextItem : items) {
        if (nextItem.isEnabled()) {
          selectItem(nextItem);
          break;
        }
      }
      return true;
    }
    return false;
 }

  private void selectNextItem() {
    if (selectedItem == null) {
      return;
    }

    int index = items.indexOf(selectedItem);
    // We know that selectedItem is set to an item that is contained in the
    // items collection.
    // Therefore, we know that index can never be -1.
    assert (index != -1);

    MenuItem itemToBeSelected;

    int firstIndex = index;
    while (true) {
      index = index + 1;
      if (index == items.size()) {
        // we're at the end, loop around to the start
        index = 0;
      }
      if (index == firstIndex) {
        itemToBeSelected = items.get(firstIndex);
        break;
      } else {
        itemToBeSelected = items.get(index);
        if (itemToBeSelected.isEnabled()) {
          break;
        }
      }
    }

    selectItem(itemToBeSelected);
    if (shownChildMenu != null) {
      doItemAction(itemToBeSelected, false, true);
    }
  }

  private void selectPrevItem() {
    if (selectedItem == null) {
      return;
    }

    int index = items.indexOf(selectedItem);
    // We know that selectedItem is set to an item that is contained in the
    // items collection.
    // Therefore, we know that index can never be -1.
    assert (index != -1);

    MenuItem itemToBeSelected;

    int firstIndex = index;
    while (true) {
      index = index - 1;
      if (index < 0) {
        // we're at the start, loop around to the end
        index = items.size() - 1;
      }
      if (index == firstIndex) {
        itemToBeSelected = items.get(firstIndex);
        break;
      } else {
        itemToBeSelected = items.get(index);
        if (itemToBeSelected.isEnabled()) {
          break;
        }
      }
    }

    selectItem(itemToBeSelected);
    if (shownChildMenu != null) {
      doItemAction(itemToBeSelected, false, true);
    }
  }

  /**
   * Set the colspan of a {@link MenuItem} or {@link MenuItemSeparator}.
   *
   * @param item the {@link MenuItem} or {@link MenuItemSeparator}
   * @param colspan the colspan
   */
  private void setItemColSpan(UIObject item, int colspan) {
    DOM.setElementPropertyInt(item.getElement(), "colSpan", colspan);
  }
}
