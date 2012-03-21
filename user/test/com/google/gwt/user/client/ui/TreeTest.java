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

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

import java.util.Iterator;

/**
 * Tests the Tree widget.
 */
public class TreeTest extends GWTTestCase {

  private static final String html = "<b>hello</b><i>world</i>";

  static class Adder implements HasWidgetsTester.WidgetAdder {
    @Override
    public void addChild(HasWidgets container, Widget child) {
      ((Tree) container).addItem(child);
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.DebugTest";
  }

  /**
   * Test for {@link Tree#add(IsWidget)}.
   */
  public void testAddAsIsWidget() {
    Tree t = createTree();
    Widget widget = new Label("foo");

    // IsWidget cast to call the overloaded version
    t.add((IsWidget) widget);

    assertEquals("The number of items should be 1", 1, t.getItemCount());
    assertSame(widget, t.getItem(0).getWidget());
  }
  
  /**
   * Ensures that {@link Tree#add(Widget)} does <b>NOT</b> throws a
   * {@link NullPointerException} when the Widget argument is <code>null</code>,
   * for stupidity consistency with add(Widget).
   */
  public void testAddNullAsIsWidget() {
    Tree t = createTree();
    // IsWidget reference to call the overload version
    IsWidget widget = null;
    
    t.add(widget);
    // ta da...
  }

  /**
   * Test for {@link Tree#addItem(IsTreeItem)}.
   */
  public void testAddItemIsTreeItem() {
    Tree t = createTree();
    TreeItem item = new TreeItem("hello");
    t.addItem((IsTreeItem) item);
    assertEquals(1, t.getItemCount());
    assertSame(item, t.getItem(0));
  }

  public void testAddItemSafeHtml() {
    Tree t = createTree();
    TreeItem item = t.addItem(SafeHtmlUtils.fromSafeConstant(html));
    assertEquals(html, item.getHTML().toLowerCase());
  }

  /**
   * Test for {@link Tree#addTextItem(String)}.
   */
  public void testAddTextItem() {
    Tree t = createTree();
    String text = "Some<br>text";
    TreeItem item = t.addTextItem(text);
    assertEquals(text, item.getText());
    // Safari 3 leaves > in the HTML
    String html = item.getHTML().replace(">", "&gt;");
    assertEquals("Some&lt;br&gt;text", html);
  }

  public void testAttachDetachOrder() {
    HasWidgetsTester.testAll(createTree(), new Adder(), true);
  }

  public void testClear() {
    Tree t = createTree();
    // Adding widget to end of tree, widgets still have their parents set
    // correctly.
    TreeItem a = new TreeItem("a");
    TreeItem b = new TreeItem("b");
    TreeItem c = new TreeItem("c");
    TreeItem d = new TreeItem();
    TreeItem e = new TreeItem();
    Label dLabel = new Label("d");
    Label eLabel = new Label("e");
    d.setWidget(dLabel);
    d.addItem(e);
    c.addItem(d);
    b.addItem(c);
    a.addItem(b);
    t.addItem(a);
    t.addItem("hello");
    t.addItem(eLabel);
    t.clear();
    assertFalse(t.treeItemIterator().hasNext());
    assertEquals(0, t.getChildWidgets().size());
  }

  public void testDebugId() {
    Tree tree = createTree();
    TreeItem top0 = tree.addItem("top0");
    TreeItem top1 = tree.addItem("top1");
    TreeItem top2 = tree.addItem("top2");
    TreeItem top3 = tree.addItem("top3");
    TreeItem bottom0 = top3.addItem("bottom0");
    TreeItem bottom1 = top3.addItem("bottom1");
    TreeItem bottom2 = top3.addItem("bottom2");

    // Check tree items deep
    tree.ensureDebugId("myTree");
    UIObjectTest.assertDebugId("myTree", tree.getElement());
    UIObjectTest.assertDebugId("myTree-root-child0", top0.getElement());
    UIObjectTest.assertDebugId("myTree-root-child1", top1.getElement());
    UIObjectTest.assertDebugId("myTree-root-child2", top2.getElement());
    UIObjectTest.assertDebugId("myTree-root-child3", top3.getElement());
    UIObjectTest.assertDebugId("myTree-root-child3-child0",
        bottom0.getElement());
    UIObjectTest.assertDebugId("myTree-root-child3-child1",
        bottom1.getElement());
    UIObjectTest.assertDebugId("myTree-root-child3-child2",
        bottom2.getElement());

    // Check tree item sub elements
    UIObjectTest.assertDebugId("myTree-root-child0-content",
        top0.getContentElem());

    UIObjectTest.assertDebugId("myTree-root-child3-image",
        top3.getImageHolderElement());
  }

  public void testInsertSameItemRepeatedly() {
    Tree t = createTree();
    TreeItem ti = new TreeItem();
    TreeItem wti = new TreeItem();
    wti.setWidget(new Label("label"));
    for (int i = 0; i < 10; i++) {
      t.addItem(ti);
      t.addItem(wti);
    }
    assertEquals(2, t.getItemCount());
    assertEquals(ti, t.getItem(0));
    assertEquals(wti, t.getItem(1));
  }

  public void testInsertItemSafeHtml() {
    Tree t = createTree();
    TreeItem item = t.insertItem(0, SafeHtmlUtils.fromSafeConstant(html));
    assertEquals(html, item.getHTML().toLowerCase());
  }

  public void testInsertTextItem() {
    Tree t = createTree();
    String text = "Some<br>text";
    TreeItem item = t.insertTextItem(0, text);
    assertEquals(text, item.getText());
    // Safari 3 leaves > in the HTML
    String html = item.getHTML().replace(">", "&gt;");
    assertEquals("Some&lt;br&gt;text", html);
  }

  public void testIterator() {
    Tree tree = createTree();
    Iterator<TreeItem> iter = tree.treeItemIterator();
    assertFalse(iter.hasNext());
    TreeItem a = tree.addItem("a");
    TreeItem b = tree.addItem("b");
    TreeItem c = tree.addItem("c");

    Iterator<TreeItem> iter2 = tree.treeItemIterator();
    assertEquals(a, iter2.next());
    assertEquals(b, iter2.next());
    assertEquals(c, iter2.next());
    assertFalse(iter2.hasNext());

    TreeItem a_a = a.addItem("a_a");
    TreeItem a_a_a = a_a.addItem("a_a_a");
    TreeItem a_a_b = a_a.addItem("a_a_b");

    Iterator<TreeItem> iter3 = tree.treeItemIterator();
    assertEquals(a, iter3.next());
    assertEquals(a_a, iter3.next());
    assertEquals(a_a_a, iter3.next());
    assertEquals(a_a_b, iter3.next());
    assertEquals(b, iter3.next());
    assertEquals(c, iter3.next());
    assertFalse(iter3.hasNext());
  }

  public void testNulls() {
    // Checking for setting the widget null then clearing the tree.
    Tree t = createTree();
    TreeItem item = new TreeItem();
    item.setWidget(null);
    t.clear();

    TreeItem a = t.addItem("");
    TreeItem b = t.addItem(new Label("b"));
    a.setWidget(null);
    b.setWidget(null);
  }

  public void testRemove() {
    Tree t = createTree();
    TreeItem item = t.addItem("a");
    TreeItem itemb = t.addItem("b");
    t.setSelectedItem(item);
    assertEquals(item, t.getSelectedItem());
    item.remove();
    assertNull(t.getSelectedItem());
    Iterator<TreeItem> iter = t.treeItemIterator();
    assertTrue(iter.hasNext());
    iter.next();
    assertFalse(iter.hasNext());
    t.removeItem(itemb);
    assertNull(t.getSelectedItem());
    Iterator<TreeItem> iter2 = t.treeItemIterator();
    assertFalse(iter2.hasNext());
  }

  /**
   * Test for {@link Tree#removeItem(IsTreeItem)}.
   */
  public void testRemoveIsTreeItem() {
    Tree t = createTree();
    TreeItem itemA = t.addItem("a");
    TreeItem itemB = t.addItem("b");
    // initial state
    assertEquals(2, t.getItemCount());
    assertSame(itemA, t.getItem(0));
    assertSame(itemB, t.getItem(1));
    // remove "itemA" as wrapper
    t.removeItem((IsTreeItem) itemA);
    assertEquals(1, t.getItemCount());
    assertSame(itemB, t.getItem(0));
    // ignore null
    t.removeItem((IsTreeItem) null);
  }

  /**
   * Test for {@link Tree#removeItems()}.
   */
  public void testRemoveItems() {
    Tree t = createTree();
    TreeItem itemA = t.addItem("a");
    TreeItem itemB = t.addItem("b");
    // initial state
    assertEquals(2, t.getItemCount());
    assertSame(itemA, t.getItem(0));
    assertSame(itemB, t.getItem(1));
    // do remove
    t.removeItems();
    assertEquals(0, t.getItemCount());
  }

  public void testRootAdd() {
    Tree t = createTree();
    Label l = new Label("hello");
    t.add(l);
    assertEquals(t, l.getParent());
  }

  public void testRootInsert() {
    Tree t = createTree();
    TreeItem b = t.addItem("b");
    assertEquals(1, t.getItemCount());
    assertEquals(b, t.getItem(0));

    // Insert at zero.
    TreeItem a = t.insertItem(0, "a");
    assertEquals(2, t.getItemCount());
    assertEquals(a, t.getItem(0));
    assertEquals(b, t.getItem(1));
    assertEquals(a.getElement().getNextSiblingElement(), b.getElement());

    // Insert at end.
    TreeItem d = t.insertItem(2, new Label("d"));
    assertEquals(3, t.getItemCount());
    assertEquals(a, t.getItem(0));
    assertEquals(b, t.getItem(1));
    assertEquals(d, t.getItem(2));
    assertEquals(b.getElement().getNextSiblingElement(), d.getElement());

    // Insert in the middle.
    TreeItem c = new TreeItem("c");
    t.insertItem(2, c);
    assertEquals(4, t.getItemCount());
    assertEquals(a, t.getItem(0));
    assertEquals(b, t.getItem(1));
    assertEquals(c, t.getItem(2));
    assertEquals(d, t.getItem(3));
    assertEquals(b.getElement().getNextSiblingElement(), c.getElement());
  }

  public void testRootInsertInvalidIndex() {
    Tree t = createTree();
    t.addItem("a");
    t.addItem("b");
    t.addItem("c");

    // Insert at -1.
    try {
      t.insertItem(-1, "illegal");
      fail("Expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException e) {
      // Expected.
    }

    // Insert past the end.
    try {
      t.insertItem(4, "illegal");
      fail("Expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException e) {
      // Expected.
    }
  }

  public void testSwap() {
    Tree t = createTree();

    // Start with text.
    TreeItem item = t.addItem("hello");
    String inner = DOM.getInnerHTML(item.getContentElem());
    assertTrue(inner.indexOf("hello") >= 0);
    t.addItem(item);
    Widget goodbyeWidget = new Label("goodbye");
    item.setWidget(goodbyeWidget);
    String innerWidget = DOM.getInnerHTML(item.getContentElem());
    assertFalse(innerWidget.indexOf("hello") >= 0);

    // Start with widget.
    Widget hello = new Label("hello");
    TreeItem widgetItem = t.addItem(hello);
    assertTrue(DOM.getInnerHTML(widgetItem.getContentElem()).indexOf("hello") >= 0);
    widgetItem.setText("goodbye");
    assertFalse(DOM.getInnerHTML(widgetItem.getContentElem()).indexOf("hello") >= 0);
    assertTrue(DOM.getInnerHTML(widgetItem.getContentElem()).indexOf("goodbye") >= 0);
    assertNull(hello.getParent());

    // Move widget.
    widgetItem.addItem(item);
    assertEquals(t, goodbyeWidget.getParent());
    assertEquals(goodbyeWidget, item.getWidget());

    // Set back to text.
    item.setText("aloha");
    assertEquals("aloha", DOM.getInnerHTML(item.getContentElem()));
    assertNull(goodbyeWidget.getParent());
    assertNull(item.getWidget());
  }

  public void testTree() {
    // Simple widget
    Tree t = createTree();
    Label l = new Label("simple widget");
    TreeItem simple = new TreeItem(l);
    t.addItem(simple);

    // Adding test and widget
    TreeItem item = new TreeItem();
    t.addItem(item);
    item.setWidget(new Label("now widget"));
    Element elem = item.getContentElem();
    assertEquals(1, DOM.getChildCount(elem));

    // Add widget to existing tree.
    Label l2 = new Label("next widget");
    simple.setWidget(l2);
    assertEquals(t, l2.getParent());

    // Remove a tree item, make sure widget is removed from tree, but not tree
    // item.
    simple.remove();
    assertEquals(l2, simple.getWidget());
    assertNull(l2.getParent());

    // Adding widget to end of tree, widgets still have their parents set
    // correctly.
    TreeItem a = new TreeItem("a");
    TreeItem b = new TreeItem("b");
    TreeItem c = new TreeItem("c");
    TreeItem d = new TreeItem();
    TreeItem e = new TreeItem();
    Label dLabel = new Label("d");
    Label eLabel = new Label("e");
    d.setWidget(dLabel);
    d.addItem(e);
    c.addItem(d);
    b.addItem(c);
    a.addItem(b);
    t.addItem(a);
    assertEquals(t, dLabel.getParent());
    e.setWidget(eLabel);
    assertEquals(t, eLabel.getParent());

    // Tree inside of Tree.
    Tree childTree = createTree();
    t.addItem(new TreeItem(childTree));

    // Swap TreeItems to new Tree.
    childTree.addItem(c);
    assertEquals(childTree, dLabel.getParent());
    assertEquals(childTree, eLabel.getParent());

    // Make sure remove clears.
    d.remove();
    assertNull(dLabel.getParent());
    assertNull(eLabel.getParent());
    assertFalse(childTree.getChildWidgets().containsKey(eLabel.getParent()));
  }

  private Tree createTree() {
    return new Tree();
  }
}
