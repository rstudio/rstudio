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

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * TODO: document me.
 */
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

    public Iterator<Widget> iterator() {
      return null;
    }

    public boolean remove(Widget w) {
      if (!collection.contains(w)) {
        return false;
      }
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

    Iterator<Widget> it = wc.iterator();
    assertTrue(it.hasNext());
    assertEquals(it.next(), l0);
    it.remove();
    assertTrue(it.hasNext());
    assertEquals(it.next(), l1);
    assertEquals(it.next(), l2);
    assertFalse(it.hasNext());
  }

  public void testExceptionInIteratorNextOnEmpty() {
    // empty collection - next
    try {
      new Container().collection.iterator().next();
      fail("expected NoSuchElementException");
    } catch (NoSuchElementException expected) {
    }
  }

  public void testExceptionInIteratorRemoveOnEmpty() {
    // empty collection - remove
    try {
      new Container().collection.iterator().remove();
      fail("expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
  }

  public void testExceptionInIteratorNextOnCollection() {
    WidgetCollection wc = new Container().collection;

    wc.add(new Button("a"));
    wc.add(new Button("b"));

    Iterator<Widget> iter = wc.iterator();
    iter.next();
    iter.next();
    try {
      iter.next();
      fail("expected NoSuchElementException");
    } catch (NoSuchElementException expected) {
    }
  }

  public void testExceptionInIteratorOnRemoveBeforeNext() {
    // test remove before next
    WidgetCollection wc = new Container().collection;
    wc.add(new Button("a"));
    wc.add(new Button("b"));

    Iterator<Widget> iter = wc.iterator();
    try {
      iter.remove();
      fail("expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
  }

  public void testExceptionInIteratorOnDoubleRemove() {
    // test remove two times
    WidgetCollection wc = new Container().collection;
    wc.add(new Button("a"));
    wc.add(new Button("b"));

    Iterator<Widget> iter = wc.iterator();
    iter.next();
    iter.remove();
    try {
      iter.remove();
      fail("expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
  }
}
