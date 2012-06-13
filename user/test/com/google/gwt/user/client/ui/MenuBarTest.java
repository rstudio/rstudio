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

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.Command;

import java.util.List;

/**
 * Tests the DockPanel widget.
 */
public class MenuBarTest extends WidgetTestBase {

  private static final String html = "<b>hello</b><i>world</i>";

  /**
   * A blank command.
   */
  private static final Command BLANK_COMMAND = new Command() {
    public void execute() {
    }
  };

  /**
   * A blank scheduled command.
   */
  private static final ScheduledCommand BLANK_SCHEDULED_COMMAND = new ScheduledCommand() {
    public void execute() {
    }
  };

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.DebugTest";
  }

  /**
   * Test adding and removing {@link MenuItem}s and {@link MenuItemSeparator}s
   * from a menu.
   */
  public void testAddRemoveItemsWithCommand() {
    // Create a menu bar
    MenuBar bar = new MenuBar(true);

    // Add an item, default to text
    MenuItem item0 = bar.addItem("<b>test</b>", BLANK_COMMAND);
    assertEquals("<b>test</b>", item0.getText());
    assertEquals(BLANK_COMMAND, item0.getCommand());
    assertEquals(bar, item0.getParentMenu());

    // Add a separator
    MenuItemSeparator separator0 = bar.addSeparator();
    assertEquals(bar, separator0.getParentMenu());

    // Add another item, force to html
    MenuItem item1 = bar.addItem("<b>test1</b>", true, BLANK_COMMAND);
    assertEquals("test1", item1.getText());
    assertEquals(BLANK_COMMAND, item1.getCommand());
    assertEquals(bar, item1.getParentMenu());

    // Get all items
    List<MenuItem> items = bar.getItems();
    assertEquals(item0, items.get(0));
    assertEquals(item1, items.get(1));

    // Remove an item
    bar.removeItem(item0);
    assertEquals(item1, items.get(0));
    assertNull(item0.getParentMenu());

    // Remove the separator
    bar.removeSeparator(separator0);
    assertEquals(item1, items.get(0));
    assertNull(separator0.getParentMenu());

    // Add a bunch of items and clear them all
    MenuItem item2 = bar.addItem("test2", true, BLANK_COMMAND);
    MenuItemSeparator separator1 = bar.addSeparator();
    MenuItem item3 = bar.addItem("test3", true, BLANK_COMMAND);
    MenuItemSeparator separator2 = bar.addSeparator();
    MenuItem item4 = bar.addItem("test4", true, BLANK_COMMAND);
    MenuItemSeparator separator3 = bar.addSeparator();
    bar.clearItems();
    assertEquals(0, bar.getItems().size());
    assertNull(item2.getParentMenu());
    assertNull(item3.getParentMenu());
    assertNull(item4.getParentMenu());
    assertNull(separator1.getParentMenu());
    assertNull(separator2.getParentMenu());
    assertNull(separator3.getParentMenu());
  }

  /**
   * Test adding and removing {@link MenuItem}s and {@link MenuItemSeparator}s
   * from a menu.
   */
  public void testAddRemoveItemsWithScheduledCommand() {
    // Create a menu bar
    MenuBar bar = new MenuBar(true);

    // Add an item, default to text
    MenuItem item0 = bar.addItem("<b>test</b>", BLANK_SCHEDULED_COMMAND);
    assertEquals("<b>test</b>", item0.getText());
    assertEquals(BLANK_SCHEDULED_COMMAND, item0.getScheduledCommand());
    assertEquals(bar, item0.getParentMenu());

    // Add a separator
    MenuItemSeparator separator0 = bar.addSeparator();
    assertEquals(bar, separator0.getParentMenu());

    // Add another item, force to html
    MenuItem item1 = bar.addItem("<b>test1</b>", true, BLANK_SCHEDULED_COMMAND);
    assertEquals("test1", item1.getText());
    assertEquals(BLANK_SCHEDULED_COMMAND, item1.getScheduledCommand());
    assertEquals(bar, item1.getParentMenu());

    // Get all items
    List<MenuItem> items = bar.getItems();
    assertEquals(item0, items.get(0));
    assertEquals(item1, items.get(1));

    // Remove an item
    bar.removeItem(item0);
    assertEquals(item1, items.get(0));
    assertNull(item0.getParentMenu());

    // Remove the separator
    bar.removeSeparator(separator0);
    assertEquals(item1, items.get(0));
    assertNull(separator0.getParentMenu());

    // Add a bunch of items and clear them all
    MenuItem item2 = bar.addItem("test2", true, BLANK_SCHEDULED_COMMAND);
    MenuItemSeparator separator1 = bar.addSeparator();
    MenuItem item3 = bar.addItem("test3", true, BLANK_SCHEDULED_COMMAND);
    MenuItemSeparator separator2 = bar.addSeparator();
    MenuItem item4 = bar.addItem("test4", true, BLANK_SCHEDULED_COMMAND);
    MenuItemSeparator separator3 = bar.addSeparator();
    bar.clearItems();
    assertEquals(0, bar.getItems().size());
    assertNull(item2.getParentMenu());
    assertNull(item3.getParentMenu());
    assertNull(item4.getParentMenu());
    assertNull(separator1.getParentMenu());
    assertNull(separator2.getParentMenu());
    assertNull(separator3.getParentMenu());
  }

  public void testAutoHideChildMenuPopup() {
    // Create a menu bar with children.
    MenuBar l0 = new MenuBar();
    l0.setAutoOpen(true);
    MenuBar l1 = new MenuBar();
    l1.setAutoOpen(true);
    MenuBar l2 = new MenuBar();
    l2.setAutoOpen(true);
    MenuItem item2 = l2.addItem("l2", BLANK_COMMAND);
    MenuItem item1 = l1.addItem("l1", l2);
    MenuItem item0 = l0.addItem("l0", l1);
    RootPanel.get().add(l0);

    // Open l2.
    l0.itemOver(item0, true);
    l1.itemOver(item1, true);
    l2.itemOver(item2, true);
    assertTrue(l0.getPopup().isShowing());
    assertEquals(item0, l0.getSelectedItem());
    assertTrue(l1.getPopup().isShowing());
    assertEquals(item1, l1.getSelectedItem());

    // Auto-hide the child popup.
    l1.getPopup().hide(true);
    assertNull(l0.getPopup());
    assertNull(l0.getSelectedItem());
    assertNull(l1.getPopup());

    // Open l2.
    l0.itemOver(item0, true);
    l1.itemOver(item1, true);
    l2.itemOver(item2, true);
    assertTrue(l0.getPopup().isShowing());
    assertEquals(item0, l0.getSelectedItem());
    assertTrue(l1.getPopup().isShowing());
    assertEquals(item1, l1.getSelectedItem());

    // Close child menus below l1.
    l1.closeAllChildren(true);
    assertTrue(l0.getPopup().isShowing());
    assertEquals(item0, l0.getSelectedItem());
    assertNull(l1.getPopup());
    assertNull(l2.getPopup());
  }

  @DoNotRunWith({Platform.HtmlUnitBug})
  public void testBlur() {
    // Create a menu bar with children.
    final MenuBar menu = new MenuBar();
    MenuItem item0 = menu.addItem("item0", BLANK_COMMAND);
    RootPanel.get().add(menu);

    // Select the item.
    menu.focus();
    menu.selectItem(item0);
    assertEquals(item0, menu.getSelectedItem());

    // Blur the menu bar.
    NativeEvent event = Document.get().createBlurEvent();
    menu.getElement().dispatchEvent(event);
    assertNull(menu.getSelectedItem());
  }

  public void testDebugId() {
    // Create a sub menu
    MenuBar subMenu = new MenuBar(true);
    subMenu.addItem("sub0", BLANK_COMMAND);
    subMenu.addItem("sub1", BLANK_COMMAND);
    subMenu.addItem("sub2", BLANK_COMMAND);

    // Create a menu bar
    MenuBar bar = new MenuBar(false);
    bar.setAnimationEnabled(false);
    bar.setAutoOpen(true);
    bar.addItem("top0", BLANK_COMMAND);
    bar.addItem("top1", BLANK_COMMAND);
    MenuItem top2 = bar.addItem("top2", subMenu);
    RootPanel.get().add(bar);

    // Open the item with a submenu
    bar.itemOver(top2, true);

    // Set the Debug Id
    bar.ensureDebugId("myMenu");
    UIObjectTest.assertDebugId("myMenu", bar.getElement());

    delayTestFinish(5000);
    Scheduler.get().scheduleDeferred(new ScheduledCommand() {
      public void execute() {
        UIObjectTest.assertDebugIdContents("myMenu-item0", "top0");
        UIObjectTest.assertDebugIdContents("myMenu-item1", "top1");
        UIObjectTest.assertDebugIdContents("myMenu-item2", "top2");

        UIObjectTest.assertDebugIdContents("myMenu-item2-item0", "sub0");
        UIObjectTest.assertDebugIdContents("myMenu-item2-item1", "sub1");
        UIObjectTest.assertDebugIdContents("myMenu-item2-item2", "sub2");
        finishTest();
      }
    });
  }

  public void testDisabledItem() {
    MenuBar bar = new MenuBar(true);
    MenuItem item1 = new MenuItem("item1", BLANK_COMMAND);
    MenuItem item2 = new MenuItem("item2", BLANK_COMMAND);
    MenuItem item3 = new MenuItem("item3", BLANK_COMMAND);
    bar.addItem(item1);
    bar.addItem(item2);
    bar.addItem(item3);
    RootPanel.get().add(bar);

    item2.setEnabled(false);

    bar.moveSelectionDown();
    assertEquals(item1, bar.getSelectedItem());
    bar.moveSelectionDown();
    assertEquals(item3, bar.getSelectedItem());
    bar.moveSelectionUp();
    assertEquals(item1, bar.getSelectedItem());
  }

  public void testEscapeKey() {
    // Create a menu bar with children.
    MenuBar l0 = new MenuBar();
    l0.setAutoOpen(true);
    MenuBar l1 = new MenuBar();
    l1.setAutoOpen(true);
    MenuBar l2 = new MenuBar();
    l2.setAutoOpen(true);
    MenuItem item2 = l2.addItem("l2", BLANK_COMMAND);
    MenuItem item1 = l1.addItem("l1", l2);
    MenuItem item0 = l0.addItem("l0", l1);
    RootPanel.get().add(l0);

    // Open l2.
    l0.itemOver(item0, true);
    l1.itemOver(item1, true);
    l2.itemOver(item2, true);
    assertTrue(l0.getPopup().isShowing());
    assertEquals(item0, l0.getSelectedItem());
    assertTrue(l1.getPopup().isShowing());
    assertEquals(item1, l1.getSelectedItem());

    // Escape from the menu.
    NativeEvent event = Document.get().createKeyDownEvent(
        false, false, false, false, KeyCodes.KEY_ESCAPE);
    l1.getElement().dispatchEvent(event);
    assertNull(l0.getPopup());
    assertNull(l0.getSelectedItem());
    assertNull(l1.getPopup());
  }

  /**
   * Test inserting {@link MenuItem}s and {@link MenuItemSeparator}s into the
   * menu.
   */
  public void testInsertItems() {
    // Create a menu bar
    MenuBar bar = new MenuBar(true);

    // Insert first item
    MenuItem item0 = bar.insertItem(new MenuItem("test", BLANK_COMMAND), 0);
    assertEquals(bar.getItemIndex(item0), 0);

    // Insert item at 0
    MenuItem item1 = bar.insertItem(new MenuItem("test", BLANK_COMMAND), 0);
    assertEquals(bar.getItemIndex(item1), 0);
    assertEquals(bar.getItemIndex(item0), 1);

    // Insert item at end
    MenuItem item2 = bar.insertItem(new MenuItem("test", BLANK_COMMAND), 2);
    assertEquals(bar.getItemIndex(item1), 0);
    assertEquals(bar.getItemIndex(item0), 1);
    assertEquals(bar.getItemIndex(item2), 2);

    // Insert a separator at 0
    MenuItemSeparator separator0 = bar.insertSeparator(0);
    assertEquals(bar.getSeparatorIndex(separator0), 0);
    assertEquals(bar.getItemIndex(item1), 1);
    assertEquals(bar.getItemIndex(item0), 2);
    assertEquals(bar.getItemIndex(item2), 3);

    // Insert a separator at end
    MenuItemSeparator separator1 = bar.insertSeparator(4);
    assertEquals(bar.getSeparatorIndex(separator0), 0);
    assertEquals(bar.getItemIndex(item1), 1);
    assertEquals(bar.getItemIndex(item0), 2);
    assertEquals(bar.getItemIndex(item2), 3);
    assertEquals(bar.getSeparatorIndex(separator1), 4);

    // Insert a separator at middle
    MenuItemSeparator separator2 = bar.insertSeparator(2);
    assertEquals(bar.getSeparatorIndex(separator0), 0);
    assertEquals(bar.getItemIndex(item1), 1);
    assertEquals(bar.getSeparatorIndex(separator2), 2);
    assertEquals(bar.getItemIndex(item0), 3);
    assertEquals(bar.getItemIndex(item2), 4);
    assertEquals(bar.getSeparatorIndex(separator1), 5);
  }

  /**
   * Test inserting {@link MenuItem}s and {@link MenuItemSeparator}s into the
   * menu at indexes that are out of bounds.
   */
  public void testInsertItemsOutOfBounds() {
    // Create a menu bar
    MenuBar bar = new MenuBar(true);

    // Add some items to the menu
    for (int i = 0; i < 3; i++) {
      bar.addItem("item", BLANK_COMMAND);
    }

    // Add an item at a negative index
    try {
      bar.insertItem(new MenuItem("test", BLANK_COMMAND), -1);
      fail("Expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException e) {
      // Expected exception
    }

    // Add an item at a high index
    try {
      bar.insertItem(new MenuItem("test", BLANK_COMMAND), 4);
      fail("Expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException e) {
      // Expected exception
    }
  }

  public void testSafeHtmlWithCommand() {
    MenuBar bar = new MenuBar(true);

    // ensure safehtml passes through when a command is set.
    MenuItem item1 =
      bar.addItem(SafeHtmlUtils.fromSafeConstant(html), BLANK_COMMAND);
    assertEquals(html, item1.getHTML().toLowerCase());
    assertEquals(BLANK_COMMAND, item1.getCommand());
    assertEquals(bar, item1.getParentMenu());

    // ensure safehtml passes through when a submenu/popup is set.
    MenuBar foo = new MenuBar(true);
    MenuItem item2 = foo.addItem(SafeHtmlUtils.fromSafeConstant(html), bar);
    assertEquals(html, item2.getHTML().toLowerCase());
    assertEquals(bar, item2.getSubMenu());
    assertEquals(foo, item2.getParentMenu());
  }

  public void testSafeHtmlWithScheduledCommand() {
    MenuBar bar = new MenuBar(true);

    // ensure safehtml passes through when a command is set.
    MenuItem item1 =
      bar.addItem(SafeHtmlUtils.fromSafeConstant(html), BLANK_SCHEDULED_COMMAND);
    assertEquals(html, item1.getHTML().toLowerCase());
    assertEquals(BLANK_SCHEDULED_COMMAND, item1.getScheduledCommand());
    assertEquals(bar, item1.getParentMenu());

    // ensure safehtml passes through when a submenu/popup is set.
    MenuBar foo = new MenuBar(true);
    MenuItem item2 = foo.addItem(SafeHtmlUtils.fromSafeConstant(html), bar);
    assertEquals(html, item2.getHTML().toLowerCase());
    assertEquals(bar, item2.getSubMenu());
    assertEquals(foo, item2.getParentMenu());
  }

  /**
   * Test that the selected item points to the correct item.
   */
  public void testSelectedItem() {
    // Create a menu bar
    MenuBar bar = new MenuBar(true);

    // Add some items
    MenuItem item1 = bar.addItem("item1", BLANK_COMMAND);
    MenuItem item2 = bar.addItem("item2", BLANK_COMMAND);
    MenuItem item3 = bar.addItem("item3", BLANK_COMMAND);

    // Test setting the selected item
    assertNull(bar.getSelectedItem());
    bar.selectItem(item1);
    assertEquals(item1, bar.getSelectedItem());

    // Test removing the selected item
    bar.removeItem(item1);
    assertNull(bar.getSelectedItem());

    // Test removing an item that is not selected
    bar.selectItem(item3);
    assertEquals(item3, bar.getSelectedItem());
    bar.removeItem(item2);
    assertEquals(item3, bar.getSelectedItem());

    // Test clearing all items
    bar.clearItems();
    assertNull(bar.getSelectedItem());
  }

  public void testSelectItem() {
    MenuBar bar = new MenuBar(false);
    MenuItem item1 = new MenuItem("item1", BLANK_COMMAND);
    MenuItem item2 = new MenuItem("item2", BLANK_COMMAND);
    MenuItem item3 = new MenuItem("item3", BLANK_COMMAND);
    bar.addItem(item1);
    bar.addItem(item2);
    bar.addItem(item3);
    RootPanel.get().add(bar);

    bar.selectItem(item1);
    assertEquals(item1, bar.getSelectedItem());
    bar.selectItem(item3);
    assertEquals(item3, bar.getSelectedItem());
    bar.selectItem(null);
    assertNull(bar.getSelectedItem());
  }

  public void testTabKey() {
    // Create a menu bar with children.
    MenuBar l0 = new MenuBar();
    l0.setAutoOpen(true);
    MenuBar l1 = new MenuBar();
    l1.setAutoOpen(true);
    MenuBar l2 = new MenuBar();
    l2.setAutoOpen(true);
    MenuItem item2 = l2.addItem("l2", BLANK_COMMAND);
    MenuItem item1 = l1.addItem("l1", l2);
    MenuItem item0 = l0.addItem("l0", l1);
    RootPanel.get().add(l0);

    // Open l2.
    l0.itemOver(item0, true);
    l1.itemOver(item1, true);
    l2.itemOver(item2, true);
    assertTrue(l0.getPopup().isShowing());
    assertEquals(item0, l0.getSelectedItem());
    assertTrue(l1.getPopup().isShowing());
    assertEquals(item1, l1.getSelectedItem());

    // Tab away from the menu.
    NativeEvent event = Document.get().createKeyDownEvent(
        false, false, false, false, KeyCodes.KEY_TAB);
    l1.getElement().dispatchEvent(event);
    assertNull(l0.getPopup());
    assertNull(l0.getSelectedItem());
    assertNull(l1.getPopup());
  }
}
