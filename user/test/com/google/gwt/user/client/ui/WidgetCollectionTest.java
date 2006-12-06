//Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.user.client.ui;

import com.google.gwt.junit.client.GWTTestCase;

import java.util.Iterator;

public class WidgetCollectionTest extends GWTTestCase {

  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  private static class Container implements HasWidgets {

    public WidgetCollection collection = new WidgetCollection(this);

    public void add(Widget w) {
    }

    public void clear() {
    }

    public Iterator iterator() {
      return null;
    }

    public boolean remove(Widget w) {
      if (!collection.contains(w))
        return false;
      collection.remove(w);
      return true;
    }
  }

  public void testAddRemove() {
    Container c = new Container();
    WidgetCollection wc = c.collection;

    Label l0 = new Label("foo");
    Label l1 = new Label("bar");
    Label l2 = new Label("baz");

    wc.add(l0);
    wc.add(l1);
    wc.add(l2);

    assertEquals(wc.size(), 3);
    assertEquals(wc.get(1), l1);

    wc.remove(l1);
    assertEquals(wc.size(), 2);
    assertEquals(wc.get(1), l2);
    assertFalse(wc.contains(l1));

    wc.remove(0);
    assertFalse(wc.contains(l0));
    assertEquals(wc.indexOf(l2), 0);
  }

  public void testIterator() {
    Container c = new Container();
    WidgetCollection wc = c.collection;

    Label l0 = new Label("foo");
    Label l1 = new Label("bar");
    Label l2 = new Label("baz");

    wc.add(l0);
    wc.add(l1);
    wc.add(l2);

    Iterator it = wc.iterator();
    assertTrue(it.hasNext());
    assertEquals(it.next(), l0);
    it.remove();
    assertTrue(it.hasNext());
    assertEquals(it.next(), l1);
    assertEquals(it.next(), l2);
    assertFalse(it.hasNext());
  }
}
