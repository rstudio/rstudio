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

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;

/**
 * Tests the {@link TreeItem}.
 */
public class TreeItemTest extends GWTTestCase {

  private static final String HTML = "<b>hello</b><i>world</i>";

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  public void testAddIntoSameItem() {
    TreeItem item = new TreeItem();

    // Add the only child back to its parent.
    TreeItem a = item.addItem("a");
    item.addItem(a);
    assertEquals(1, item.getChildCount());
    assertEquals(a, item.getChild(0));

    // Add a child back to its parent that has multiple children.
    TreeItem b = item.addItem("b");
    item.addItem(a);
    assertEquals(2, item.getChildCount());
    assertEquals(b, item.getChild(0));
    assertEquals(a, item.getChild(1));
  }
  
  /**
   * Test for {@link TreeItem#addItem(IsTreeItem)}.
   */
  public void testAddItemIsTreeItem() {
    TreeItem root = new TreeItem("foo");
    TreeItem item = new TreeItem("hello");
    root.addItem((IsTreeItem) item);
    assertEquals(1, root.getChildCount());
    assertSame(item, root.getChild(0));
  }

  public void testAddItemSafeHtml() {
    TreeItem item = new TreeItem("foo");
    TreeItem child = item.addItem(SafeHtmlUtils.fromSafeConstant(HTML));
    assertEquals(HTML, child.getHTML().toLowerCase());
  }
  
  /**
   * Test for {@link Tree#addTextItem(String)}.
   */
  public void testAddTextItem() {
    TreeItem root = new TreeItem("foo");
    String text = "Some<br>text";
    TreeItem item = root.addTextItem(text);
    assertEquals(text, item.getText());
    // Normalize the html for ancient safari 3
    String html = item.getHTML().replace(">", "&gt;");
    assertEquals("Some&lt;br&gt;text", html);
  }
  
  public void testAsTreeItem() {
    TreeItem item = new TreeItem("foo");
    assertSame(item, item.asTreeItem());
  }

  public void testInsert() {
    TreeItem item = new TreeItem();
    TreeItem b = item.addItem("b");
    assertEquals(1, item.getChildCount());
    assertEquals(b, item.getChild(0));

    // Insert at zero.
    TreeItem a = item.insertItem(0, "a");
    assertEquals(2, item.getChildCount());
    assertEquals(a, item.getChild(0));
    assertEquals(b, item.getChild(1));
    assertEquals(a.getElement().getNextSiblingElement(), b.getElement());

    // Insert at end.
    TreeItem d = item.insertItem(2, new Label("b"));
    assertEquals(3, item.getChildCount());
    assertEquals(a, item.getChild(0));
    assertEquals(b, item.getChild(1));
    assertEquals(d, item.getChild(2));
    assertEquals(b.getElement().getNextSiblingElement(), d.getElement());

    // Insert in the middle.
    TreeItem c = new TreeItem("c");
    item.insertItem(2, c);
    assertEquals(4, item.getChildCount());
    assertEquals(a, item.getChild(0));
    assertEquals(b, item.getChild(1));
    assertEquals(c, item.getChild(2));
    assertEquals(d, item.getChild(3));
    assertEquals(b.getElement().getNextSiblingElement(), c.getElement());
  }

  /**
   * Make sure that we can reinsert a child item into its tree.
   */
  public void testInsertIntoSameItem() {
    TreeItem item = new TreeItem();
    TreeItem a = item.addItem("a");
    item.addItem("b");
    item.addItem("c");

    // Reinsert at the end.
    item.insertItem(2, a);
    assertNull(a.getElement().getNextSiblingElement());

    // Reinsert past the end. Index 3 is normally valid, but not in this case.
    try {
      item.insertItem(3, a);
      fail("Expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException e) {
      // Expected.
    }
  }

  public void testInsertInvalidIndex() {
    TreeItem item = new TreeItem();
    item.addItem("a");
    item.addItem("b");
    item.addItem("c");

    // Insert at -1.
    try {
      item.insertItem(-1, "illegal");
      fail("Expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException e) {
      // Expected.
    }

    // Insert past the end.
    try {
      item.insertItem(4, "illegal");
      fail("Expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException e) {
      // Expected.
    }
  }

  public void testInsertItemSafeHtml() {
    TreeItem item = new TreeItem("foo");
    TreeItem child = item.insertItem(0, SafeHtmlUtils.fromSafeConstant(HTML));
    assertEquals(HTML, child.getHTML().toLowerCase());
  }

  public void testInsertTextItem() {
    TreeItem root = new TreeItem("foo");
    String text = "Some<br>text";
    TreeItem item = root.insertTextItem(0, text);
    assertEquals(text, item.getText());
    // Normalize the html for ancient safari 3
    String html = item.getHTML().replace(">", "&gt;");
    assertEquals("Some&lt;br&gt;text", html);
  }

  /**
   * Test for {@link TreeItem#removeItem(IsTreeItem)}.
   */
  public void testRemoveIsTreeItem() {
    TreeItem root = new TreeItem("root");
    TreeItem itemA = root.addItem("a");
    TreeItem itemB = root.addItem("b");
    // initial state
    assertEquals(2, root.getChildCount());
    assertSame(itemA, root.getChild(0));
    assertSame(itemB, root.getChild(1));
    // remove "itemA" as wrapper
    root.removeItem((IsTreeItem) itemA);
    assertEquals(1, root.getChildCount());
    assertSame(itemB, root.getChild(0));
    // ignore null
    root.removeItem((IsTreeItem) null);
  }
  
  /**
   * Test for {@link TreeItem#removeItems()}.
   */
  public void testRemoveItems() {
    TreeItem root = new TreeItem("root");
    TreeItem itemA = root.addItem("a");
    TreeItem itemB = root.addItem("b");
    // initial state
    assertEquals(2, root.getChildCount());
    assertSame(itemA, root.getChild(0));
    assertSame(itemB, root.getChild(1));
    // do remove
    root.removeItems();
    assertEquals(0, root.getChildCount());
  }

  public void testSafeHtmlConstructor() {
    TreeItem item = new TreeItem(SafeHtmlUtils.fromSafeConstant(HTML));
    
    assertEquals(HTML, item.getHTML().toLowerCase());
  }

  public void testSetSafeHtml() {
    TreeItem item = new TreeItem("foo");
    item.setHTML(SafeHtmlUtils.fromSafeConstant(HTML));
    assertEquals(HTML, item.getHTML().toLowerCase());
  }

  /**
   * Test that setting the widget to null does not modify the widget. See issue
   * 2297 for more details.
   */
  public void testSetWidgetToNull() {
    Label widget = new Label("Test");
    TreeItem item = new TreeItem(widget);
    assertEquals("Test", widget.getText());
    item.setWidget(null);
    assertEquals("Test", widget.getText());
  }

  public void testSetWidgetNullWithError() {
    // Create a widget that will throw an exception onUnload.
    BadWidget badWidget = new BadWidget();
    badWidget.setFailOnUnload(true);

    // Add the widget to a panel.
    TreeItem item = new TreeItem(badWidget);
    assertFalse(badWidget.isAttached());

    // Attach the widget.
    Tree tree = new Tree();
    tree.addItem(item);
    RootPanel.get().add(tree);
    assertTrue(badWidget.isAttached());

    // Remove the widget from the panel.
    try {
      item.setWidget(null);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // Expected.
    }
    assertFalse(badWidget.isAttached());
    assertNull(badWidget.getParent());
    assertNull(badWidget.getElement().getParentElement());
    assertNull(item.getWidget());

    // Detach the panel.
    RootPanel.get().remove(tree);
  }
}
