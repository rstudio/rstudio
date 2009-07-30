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

import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;

import java.util.List;

/**
 * Tests the DockPanel widget.
 */
public class MenuBarTest extends GWTTestCase {
  @Override
  public String getModuleName() {
    return "com.google.gwt.user.DebugTest";
  }

  /**
   * Test adding and removing {@link MenuItem}s and {@link MenuItemSeparator}s
   * from a menu.
   */
  public void testAddRemoveItems() {
    // Create a menu bar
    MenuBar bar = new MenuBar(true);

    // Create a blank command
    Command blankCommand = new Command() {
      public void execute() {
      }
    };

    // Add an item, default to text
    MenuItem item0 = bar.addItem("<b>test</b>", blankCommand);
    assertEquals("<b>test</b>", item0.getText());
    assertEquals(blankCommand, item0.getCommand());
    assertEquals(bar, item0.getParentMenu());

    // Add a separator
    MenuItemSeparator separator0 = bar.addSeparator();
    assertEquals(bar, separator0.getParentMenu());

    // Add another item, force to html
    MenuItem item1 = bar.addItem("<b>test1</b>", true, blankCommand);
    assertEquals("test1", item1.getText());
    assertEquals(blankCommand, item1.getCommand());
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
    MenuItem item2 = bar.addItem("test2", true, blankCommand);
    MenuItemSeparator separator1 = bar.addSeparator();
    MenuItem item3 = bar.addItem("test3", true, blankCommand);
    MenuItemSeparator separator2 = bar.addSeparator();
    MenuItem item4 = bar.addItem("test4", true, blankCommand);
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
   * Test inserting {@link MenuItem}s and {@link MenuItemSeparator}s into the
   * menu.
   */
  public void testInsertItems() {
    // Create a menu bar
    MenuBar bar = new MenuBar(true);

    // Create a blank command
    Command blankCommand = new Command() {
      public void execute() {
      }
    };

    // Insert first item
    MenuItem item0 = bar.insertItem(new MenuItem("test", blankCommand), 0);
    assertEquals(bar.getItemIndex(item0), 0);

    // Insert item at 0
    MenuItem item1 = bar.insertItem(new MenuItem("test", blankCommand), 0);
    assertEquals(bar.getItemIndex(item1), 0);
    assertEquals(bar.getItemIndex(item0), 1);

    // Insert item at end
    MenuItem item2 = bar.insertItem(new MenuItem("test", blankCommand), 2);
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

    // Create a blank command
    Command blankCommand = new Command() {
      public void execute() {
      }
    };

    // Add some items to the menu
    for (int i = 0; i < 3; i++) {
      bar.addItem("item", blankCommand);
    }

    // Add an item at a negative index
    try {
      bar.insertItem(new MenuItem("test", blankCommand), -1);
      fail("Expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException e) {
      // Expected exception
    }

    // Add an item at a high index
    try {
      bar.insertItem(new MenuItem("test", blankCommand), 4);
      fail("Expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException e) {
      // Expected exception
    }
  }

  public void testSelectItem() {
    Command emptyCommand = new Command() {
      public void execute() {
      }
    };

    MenuBar bar = new MenuBar(false);
    MenuItem item1 = new MenuItem("item1", emptyCommand);
    MenuItem item2 = new MenuItem("item2", emptyCommand);
    MenuItem item3 = new MenuItem("item3", emptyCommand);
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

  @DoNotRunWith({Platform.Htmlunit})
  public void testDebugId() {
    Command emptyCommand = new Command() {
      public void execute() {
      }
    };

    // Create a sub menu
    MenuBar subMenu = new MenuBar(true);
    subMenu.addItem("sub0", emptyCommand);
    subMenu.addItem("sub1", emptyCommand);
    subMenu.addItem("sub2", emptyCommand);

    // Create a menu bar
    MenuBar bar = new MenuBar(false);
    bar.setAnimationEnabled(false);
    bar.setAutoOpen(true);
    bar.addItem("top0", emptyCommand);
    bar.addItem("top1", emptyCommand);
    MenuItem top2 = bar.addItem("top2", subMenu);
    RootPanel.get().add(bar);

    // Open the item with a submenu
    bar.itemOver(top2, true);

    // Set the Debug Id
    bar.ensureDebugId("myMenu");
    UIObjectTest.assertDebugId("myMenu", bar.getElement());

    DeferredCommand.addCommand(new Command() {
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
    delayTestFinish(250);
  }

  /**
   * Test that the selected item points to the correct item.
   */
  public void testSelectedItem() {
    // Create a menu bar
    MenuBar bar = new MenuBar(true);

    // Create a blank command
    Command blankCommand = new Command() {
      public void execute() {
      }
    };

    // Add some items
    MenuItem item1 = bar.addItem("item1", blankCommand);
    MenuItem item2 = bar.addItem("item2", blankCommand);
    MenuItem item3 = bar.addItem("item3", blankCommand);

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
}
