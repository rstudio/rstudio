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

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.BeforeSelectionEvent;
import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;

import java.util.Iterator;

/**
 * Tests for {@link StackLayoutPanel}.
 */
public class StackLayoutPanelTest extends WidgetTestBase {

  static class Adder implements HasWidgetsTester.WidgetAdder {
    public void addChild(HasWidgets container, Widget child) {
      ((StackLayoutPanel) container).add(child, new Label("Header"), 1);
    }
  }

  private class TestSelectionHandler implements BeforeSelectionHandler<Integer>, SelectionHandler<Integer> {
    private boolean onBeforeFired;

    public void onBeforeSelection(BeforeSelectionEvent<Integer> event) {
      onBeforeFired = true;
    }

    public void onSelection(SelectionEvent<Integer> event) {
      assertTrue(onBeforeFired);
      finishTest();
    }
  }

  public void testAttachDetachOrder() {
    HasWidgetsTester.testAll(new StackLayoutPanel(Unit.EM), new Adder(), true);
  }

  public void testEmptyAdd() {
    // Issue 4414: Attaching an empty StackLayoutPanel caused an exception.
    StackLayoutPanel p = new StackLayoutPanel(Unit.EM);
    RootPanel.get().add(p);
  }

  public void testInsertMultipleTimes() {
    StackLayoutPanel p = new StackLayoutPanel(Unit.EM);

    TextBox tb = new TextBox();
    p.add(tb, "Title", 1);
    p.add(tb, "Title", 1);
    p.add(tb, "Title3", 1);

    assertEquals(1, p.getWidgetCount());
    assertEquals(0, p.getWidgetIndex(tb));
    Iterator<Widget> i = p.iterator();
    assertTrue(i.hasNext());
    assertTrue(tb.equals(i.next()));
    assertFalse(i.hasNext());

    Label l = new Label();
    p.add(l, "Title", 1);
    p.add(l, "Title", 1);
    p.add(l, "Title3", 1);
    assertEquals(2, p.getWidgetCount());
    assertEquals(0, p.getWidgetIndex(tb));
    assertEquals(1, p.getWidgetIndex(l));

    p.insert(l, "Title", 1, 0);
    assertEquals(2, p.getWidgetCount());
    assertEquals(0, p.getWidgetIndex(l));
    assertEquals(1, p.getWidgetIndex(tb));

    p.insert(l, "Title", 1, 1);
    assertEquals(2, p.getWidgetCount());
    assertEquals(0, p.getWidgetIndex(l));
    assertEquals(1, p.getWidgetIndex(tb));

    p.insert(l, "Title", 1, 2);
    assertEquals(2, p.getWidgetCount());
    assertEquals(0, p.getWidgetIndex(tb));
    assertEquals(1, p.getWidgetIndex(l));
  }

  public void testInsertWithHTML() {
    StackLayoutPanel p = new StackLayoutPanel(Unit.EM);
    Label l = new Label();
    p.add(l, "three", 1);
    p.insert(new HTML("<b>hello</b>"), "two", true, 1, 0);
    p.insert(new HTML("goodbye"), "one", false, 1, 0);
    assertEquals(3, p.getWidgetCount());
  }

  /**
   * Tests to ensure that arbitrary widgets can be added/inserted effectively.
   */
  public void testInsertWithWidgets() {
    StackLayoutPanel p = new StackLayoutPanel(Unit.EM);

    TextBox wa = new TextBox();
    CheckBox wb = new CheckBox();
    VerticalPanel wc = new VerticalPanel();
    wc.add(new Label("First"));
    wc.add(new Label("Second"));

    p.add(new Label("Content C"), wc, 1);
    p.insert(new Label("Content B"), wb, 1, 0);
    p.insert(new Label("Content A"), wa, 1, 0);

    // Call these to ensure we don't throw an exception.
    assertNotNull(p.getHeaderWidget(0));
    assertNotNull(p.getHeaderWidget(1));
    assertNotNull(p.getHeaderWidget(2));
    assertEquals(3, p.getWidgetCount());
  }

  public void testIterator() {
    StackLayoutPanel p = new StackLayoutPanel(Unit.EM);
    HTML foo = new HTML("foo");
    HTML bar = new HTML("bar");
    HTML baz = new HTML("baz");
    p.add(foo, "foo", 1);
    p.add(bar, "bar", 1);
    p.add(baz, "baz", 1);

    // Iterate over the entire set and make sure it stops correctly.
    Iterator<Widget> it = p.iterator();
    assertTrue(it.hasNext());
    assertTrue(it.next() == foo);
    assertTrue(it.hasNext());
    assertTrue(it.next() == bar);
    assertTrue(it.hasNext());
    assertTrue(it.next() == baz);
    assertFalse(it.hasNext());

    // Test removing using the iterator.
    it = p.iterator();
    it.next();
    it.remove();
    assertTrue(it.next() == bar);
    assertTrue(p.getWidgetCount() == 2);
    assertTrue(p.getWidget(0) == bar);
    assertTrue(p.getWidget(1) == baz);
  }

  public void testSelectionEvents() {
    StackLayoutPanel p = new StackLayoutPanel(Unit.EM);
    RootPanel.get().add(p);

    p.add(new Button("foo"), "foo", 1);
    p.add(new Button("bar"), "bar", 1);

    // Make sure selecting a stack fires both events in the right order.
    TestSelectionHandler handler = new TestSelectionHandler();
    p.addBeforeSelectionHandler(handler);
    p.addSelectionHandler(handler);

    delayTestFinish(2000);
    p.showWidget(1);
  }

  public void testVisibleWidget() {
    StackLayoutPanel p = new StackLayoutPanel(Unit.EM);
    assertNull(p.getVisibleWidget());

    Label l0 = new Label("foo");
    Label l1 = new Label("bar");
    Label l2 = new Label("baz");

    // Add one widget, and ensure that it's visible.
    p.add(l0, new Label("header"), 1);
    assertEquals(l0, p.getVisibleWidget());

    // Remove the same, and ensure that there's no visible widget.
    p.remove(l0);
    assertNull(p.getVisibleWidget());

    // Add all three.
    p.add(l0, new Label("header"), 1);
    p.add(l1, new Label("header"), 1);
    p.add(l2, new Label("header"), 1);

    // l0 should be visible by default.
    assertEquals(l0, p.getVisibleWidget());

    // Removing l0 (currently visible) should cause l1 to be shown.
    p.remove(l0);
    assertEquals(l1, p.getVisibleWidget());

    // Now ensure that showing l2 works properly.
    p.showWidget(l2);
    assertEquals(l2, p.getVisibleWidget());
  }
}
