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

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.BeforeSelectionEvent;
import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.junit.client.GWTTestCase;

import java.util.Iterator;

/**
 * Tests for {@link TabLayoutPanel}.
 */
public class TabLayoutPanelTest extends GWTTestCase {

  static class Adder implements HasWidgetsTester.WidgetAdder {
    public void addChild(HasWidgets container, Widget child) {
      ((TabPanel) container).add(child, "foo");
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

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.DebugTest";
  }

  public void testAttachDetachOrder() {
    HasWidgetsTester.testAll(new TabPanel(), new Adder(), true);
  }

  public void testInsertMultipleTimes() {
    TabLayoutPanel p = new TabLayoutPanel(2, Unit.EM);

    TextBox tb = new TextBox();
    p.add(tb, "Title");
    p.add(tb, "Title");
    p.add(tb, "Title3");

    assertEquals(1, p.getWidgetCount());
    assertEquals(0, p.getWidgetIndex(tb));
    Iterator<Widget> i = p.iterator();
    assertTrue(i.hasNext());
    assertTrue(tb.equals(i.next()));
    assertFalse(i.hasNext());

    Label l = new Label();
    p.add(l, "Title");
    p.add(l, "Title");
    p.add(l, "Title3");
    assertEquals(2, p.getWidgetCount());
    assertEquals(0, p.getWidgetIndex(tb));
    assertEquals(1, p.getWidgetIndex(l));

    p.insert(l, "Title", 0);
    assertEquals(2, p.getWidgetCount());
    assertEquals(0, p.getWidgetIndex(l));
    assertEquals(1, p.getWidgetIndex(tb));

    p.insert(l, "Title", 1);
    assertEquals(2, p.getWidgetCount());
    assertEquals(0, p.getWidgetIndex(l));
    assertEquals(1, p.getWidgetIndex(tb));

    p.insert(l, "Title", 2);
    assertEquals(2, p.getWidgetCount());
    assertEquals(0, p.getWidgetIndex(tb));
    assertEquals(1, p.getWidgetIndex(l));
  }

  public void testInsertWithHTML() {
    TabLayoutPanel p = new TabLayoutPanel(2, Unit.EM);
    Label l = new Label();
    p.add(l, "three");
    p.insert(new HTML("<b>hello</b>"), "two", true, 0);
    p.insert(new HTML("goodbye"), "one", false, 0);
    assertEquals(3, p.getWidgetCount());
  }

  /**
   * Tests to ensure that arbitrary widgets can be added/inserted effectively.
   */
  public void testInsertWithWidgets() {
    TabLayoutPanel p = new TabLayoutPanel(2, Unit.EM);

    TextBox wa = new TextBox();
    CheckBox wb = new CheckBox();
    VerticalPanel wc = new VerticalPanel();
    wc.add(new Label("First"));
    wc.add(new Label("Second"));

    p.add(new Label("Content C"), wc);
    p.insert(new Label("Content B"), wb, 0);
    p.insert(new Label("Content A"), wa, 0);

    // Call these to ensure we don't throw an exception.
    assertNotNull(p.getTabWidget(0));
    assertNotNull(p.getTabWidget(1));
    assertNotNull(p.getTabWidget(2));
    assertEquals(3, p.getWidgetCount());
  }

  public void testIterator() {
    TabLayoutPanel p = new TabLayoutPanel(2, Unit.EM);
    HTML foo = new HTML("foo");
    HTML bar = new HTML("bar");
    HTML baz = new HTML("baz");
    p.add(foo, "foo");
    p.add(bar, "bar");
    p.add(baz, "baz");

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
    TabLayoutPanel p = new TabLayoutPanel(2, Unit.EM);
    RootPanel.get().add(p);

    p.add(new Button("foo"), "foo");
    p.add(new Button("bar"), "bar");

    // Make sure selecting a tab fires both events in the right order.
    TestSelectionHandler handler = new TestSelectionHandler();
    p.addBeforeSelectionHandler(handler);
    p.addSelectionHandler(handler);

    delayTestFinish(1000);
    p.selectTab(1);
  }
}
