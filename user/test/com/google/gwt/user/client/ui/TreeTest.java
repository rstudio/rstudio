/*
 * Copyright 2007 Google Inc.
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
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.TreeItem.ContentPanel;

import java.util.Iterator;

/**
 * TODO: document me.
 */
public class TreeTest extends GWTTestCase {
  public void testClear() {
    Tree t = new Tree();
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

  public void testNulls() {
    // Checking for setting the widget null then clearing the tree.
    Tree t = new Tree();
    TreeItem item = new TreeItem();
    item.setWidget(null);
    t.clear();

    TreeItem a = t.addItem("");
    TreeItem b = t.addItem(new Label("b"));
    a.setWidget(null);
    b.setWidget(null);
  }

  public void testSwap() {
    Tree t = new Tree();

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
    assertEquals(t, goodbyeWidget.getParent().getParent());
    assertEquals(goodbyeWidget, item.getWidget());

    // Set back to text.
    item.setText("aloha");
    assertEquals("aloha", DOM.getInnerHTML(item.getContentElem()));
    assertNull(goodbyeWidget.getParent());
    assertNull(item.getWidget());
  }

  public void testRemove() {
    Tree t = new Tree();
    TreeItem item = t.addItem("a");
    TreeItem itemb = t.addItem("b");
    t.setSelectedItem(item);
    assertEquals(item, t.getSelectedItem());
    item.remove();
    assertNull(t.getSelectedItem());
    Iterator iter = t.treeItemIterator();
    assertTrue(iter.hasNext());
    iter.next();
    assertFalse(iter.hasNext());
    t.removeItem(itemb);
    assertNull(t.getSelectedItem());
    Iterator iter2 = t.treeItemIterator();
    assertFalse(iter2.hasNext());
  }

  public void testTree() {
    // Simple widget
    Tree t = new Tree();
    Label l = new Label("simple widget");
    TreeItem simple = new TreeItem(l);
    t.addItem(simple);

    // Testing access for the tree item
    Widget w = simple.getWidget();
    TreeItem.ContentPanel parent = (ContentPanel) w.getParent();
    assertEquals(simple, parent.getTreeItem());

    // Adding test and widget
    TreeItem item = new TreeItem();
    t.addItem(item);
    item.setWidget(new Label("now widget"));
    Element elem = item.getContentElem();
    assertEquals(1, DOM.getChildCount(elem));

    // Add widget to existing tree.
    Label l2 = new Label("next widget");
    simple.setWidget(l2);
    assertEquals(t, l2.getParent().getParent());

    // Remove a tree item, make sure widget is removed from tree, but not tree
    // item.
    simple.remove();
    assertEquals(l2, simple.getWidget());
    assertNull(l2.getParent().getParent());

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
    assertEquals(t, dLabel.getParent().getParent());
    e.setWidget(eLabel);
    assertEquals(t, eLabel.getParent().getParent());

    // Tree inside of Tree.
    Tree childTree = new Tree();
    t.addItem(new TreeItem(childTree));

    // Swap TreeItems to new Tree.
    childTree.addItem(c);
    assertEquals(childTree, dLabel.getParent().getParent());
    assertEquals(childTree, eLabel.getParent().getParent());
    assertTrue(childTree.getChildWidgets().contains(eLabel.getParent()));

    // Make sure remove clears.
    d.remove();
    assertNull(dLabel.getParent().getParent());
    assertNull(eLabel.getParent().getParent());
    assertFalse(childTree.getChildWidgets().contains(eLabel.getParent()));

    // Should not be able to add getParent
    Panel p = new SimplePanel(DOM.createDiv());
    try {
      p.add(dLabel.getParent());
      fail("Should have thrown UnssupportedOperationException");
    } catch (UnsupportedOperationException good) {
      // Expected.
    }
  }

  public void testIterator() {
    Tree tree = new Tree();
    Iterator iter = tree.treeItemIterator();
    assertFalse(iter.hasNext());
    TreeItem a = tree.addItem("a");
    TreeItem b = tree.addItem("b");
    TreeItem c = tree.addItem("c");

    Iterator iter2 = tree.treeItemIterator();
    assertEquals(a, iter2.next());
    assertEquals(b, iter2.next());
    assertEquals(c, iter2.next());
    assertFalse(iter2.hasNext());

    TreeItem a_a = a.addItem("a_a");
    TreeItem a_a_a = a_a.addItem("a_a_a");
    TreeItem a_a_b = a_a.addItem("a_a_b");

    Iterator iter3 = tree.treeItemIterator();
    assertEquals(a, iter3.next());
    assertEquals(a_a, iter3.next());
    assertEquals(a_a_a, iter3.next());
    assertEquals(a_a_b, iter3.next());
    assertEquals(b, iter3.next());
    assertEquals(c, iter3.next());
    assertFalse(iter3.hasNext());
  }

  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  public void testContentPane() {
    // Simple widget
    Tree t = new Tree();
    Label l = new Label("simple widget");
    TreeItem simple = new TreeItem(l);
    t.addItem(simple);
    try {
      RootPanel.get().add(l.getParent());
      fail("Should have thrown UnsupportedOperationException");
    } catch (UnsupportedOperationException e) {
      // Expected.
    }
    assert (t.getItemCount() == 0);
    simple.setWidget(new Label("I'm confused"));
    t.addItem(simple);
  }

  public void testRootAdd() {
    Tree t = new Tree();
    Label l = new Label("hello");
    t.add(l);
    assertEquals(t, l.getParent().getParent());
  }

  public void testInsertSameItemRepeatedly() {
    Tree t = new Tree();
    TreeItem ti = new TreeItem();
    TreeItem wti = new TreeItem();
    wti.setWidget(new Label("label"));
    for (int i = 0; i < 10; i++) {
      t.addItem(ti);
      t.addItem(wti);
    }
    assertEquals(2, t.getItemCount());
  }

}
