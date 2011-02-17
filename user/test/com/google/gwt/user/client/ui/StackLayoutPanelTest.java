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
import com.google.gwt.safehtml.shared.SafeHtmlUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Tests for {@link StackLayoutPanel}.
 */
public class StackLayoutPanelTest extends WidgetTestBase {
  static class Adder implements HasWidgetsTester.WidgetAdder {
    public void addChild(HasWidgets container, Widget child) {
      ((StackLayoutPanel) container).add(child, new Label("Header"), 1);
    }
  }

  private static final String html = "<b>hello</b><i>world</i>";

  private class TestSelectionHandler implements
      BeforeSelectionHandler<Integer>, SelectionHandler<Integer> {
    private boolean onBeforeSelectionFired;
    private boolean onSelectionFired;

    public void assertOnBeforeSelectionFired(boolean expected) {
      assertEquals(expected, onBeforeSelectionFired);
    }

    public void assertOnSelectionFired(boolean expected) {
      assertEquals(expected, onSelectionFired);
    }

    public void onBeforeSelection(BeforeSelectionEvent<Integer> event) {
      assertFalse(onSelectionFired);
      onBeforeSelectionFired = true;
    }

    public void onSelection(SelectionEvent<Integer> event) {
      assertTrue(onBeforeSelectionFired);
      onSelectionFired = true;
    }
  }

  /**
   * Tests {@link StackLayoutPanel#add(Widget, String, boolean, double)}.
   */
  public void testAddWithTextHeader() {
    StackLayoutPanel panel = createStackLayoutPanel(Unit.EM);
    Widget widget = new Label("foo");

    panel.add(widget, "header", false, 1);

    assertLogicalPaternityOfChild(panel, widget);
  }

  /**
   * Tests {@link StackLayoutPanel#add(IsWidget, String, boolean, double)}.
   */
  public void testAddWithTextHeaderAsIsWidget() {
    StackLayoutPanel panel = createStackLayoutPanel(Unit.EM);
    Widget widget = new Label("foo");

    // IsWidget cast to call the overloaded version
    panel.add((IsWidget) widget, "header", false, 1);

    assertLogicalPaternityOfChild(panel, widget);
  }

  /**
   * Tests {@link StackLayoutPanel#add(Widget, Widget, double)}.
   */
  public void testAddWithWidgetHeader() {
    StackLayoutPanel panel = createStackLayoutPanel(Unit.EM);
    Widget header = new Label("foo");
    Widget widget = new Label("bar");

    panel.add(widget, header, 1);

    assertLogicalPaternityOfChild(panel, widget);
    assertLogicalPaternityOfHeader(panel, widget, header);
  }

  /**
   * Tests {@link StackLayoutPanel#add(IsWidget, IsWidget, double)}.
   */
  public void testAddWithWidgetHeaderAsIsWidget() {
    StackLayoutPanel panel = createStackLayoutPanel(Unit.EM);
    Widget header = new Label("foo");
    Widget widget = new Label("bar");

    // IsWidget casts to call the overloaded version
    panel.add((IsWidget) widget, (IsWidget) header, 1);

    assertLogicalPaternityOfChild(panel, widget);
    assertLogicalPaternityOfHeader(panel, widget, header);
  }

  public void testAddWithSafeHtml() {
    StackLayoutPanel panel = new StackLayoutPanel(Unit.EM);
    panel.add(new HTML("foo"), SafeHtmlUtils.fromSafeConstant(html), 1.0);

    assertEquals(1, panel.getWidgetCount());
    assertEquals(html,
        panel.getHeaderWidget(0).getElement().getInnerHTML().toLowerCase());
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

  public void testInsertSafeHtml() {
    StackLayoutPanel panel = new StackLayoutPanel(Unit.EM);
    panel.insert(new HTML("foo"), SafeHtmlUtils.fromSafeConstant(html), 1.0, 0);

    assertEquals(1, panel.getWidgetCount());
    assertEquals(html,
        panel.getHeaderWidget(0).getElement().getInnerHTML().toLowerCase());
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

    Label contentA = new Label("Content A");
    Label contentB = new Label("Content B");
    Label contentC = new Label("Content C");

    p.add(contentC, wc, 1);
    p.insert(contentB, wb, 1, 0);
    p.showWidget(1);

    // Insert before the visible widget.
    p.insert(contentA, wa, 1, 0);

    // Check that the visible widget index has been incremented. 
    assertEquals(2, p.getVisibleIndex());

    // Call these to ensure we don't throw an exception.
    assertEquals(wa, p.getHeaderWidget(0));
    assertEquals(wb, p.getHeaderWidget(1));
    assertEquals(wc, p.getHeaderWidget(2));
    assertEquals(contentA, p.getWidget(0));
    assertEquals(contentB, p.getWidget(1));
    assertEquals(contentC, p.getWidget(2));
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

  public void testRemoveBeforeSelectedWidget() {
    StackLayoutPanel p = new StackLayoutPanel(Unit.EM);
    p.add(new Label("Content 0"), "Header 0", 1);
    p.add(new Label("Content 1"), "Header 0", 1);
    p.add(new Label("Content 2"), "Header 2", 1);
    p.showWidget(2);
    assertEquals(2, p.getVisibleIndex());
    
    // Remove a widget before the selected index.
    p.remove(1);
    assertEquals(1, p.getVisibleIndex());
  }

  public void testRemoveSelectedWidget() {
    StackLayoutPanel p = new StackLayoutPanel(Unit.EM);
    p.add(new Label("Content 0"), "Header 0", 1);
    p.add(new Label("Content 1"), "Header 0", 1);
    p.add(new Label("Content 2"), "Header 2", 1);
    p.showWidget(1);
    assertEquals(1, p.getVisibleIndex());
    
    // Remove the selected widget.
    p.remove(1);
    assertEquals(0, p.getVisibleIndex());
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
    p.showWidget(1);
    handler.assertOnBeforeSelectionFired(true);
    handler.assertOnSelectionFired(true);
  }

  public void testSelectionEventsNoFire() {
    StackLayoutPanel p = new StackLayoutPanel(Unit.EM);
    RootPanel.get().add(p);

    p.add(new Button("foo"), "foo", 1);
    p.add(new Button("bar"), "bar", 1);

    TestSelectionHandler handler = new TestSelectionHandler();
    p.addBeforeSelectionHandler(handler);
    p.addSelectionHandler(handler);
    p.showWidget(1, false);
    handler.assertOnBeforeSelectionFired(false);
    handler.assertOnSelectionFired(false);
  }

  public void testSetHeaderSafeHtml() {
    StackLayoutPanel panel = new StackLayoutPanel(Unit.PX);
    RootPanel.get().add(panel);
    panel.add(new HTML("bar"), "foo", 1.0);
    panel.setHeaderHTML(0, SafeHtmlUtils.fromSafeConstant(html));
    Widget header = panel.getHeaderWidget(0);

    assertEquals(html, header.getElement().getInnerHTML().toLowerCase());
  }

  /**
   * For legacy reasons, {@link StackLayoutPanel#showWidget(Widget)} should call
   * {@link StackLayoutPanel#showWidget(int)}.
   */
  public void testShowWidgetLegacy() {
    final List<Integer> called = new ArrayList<Integer>();
    StackLayoutPanel panel = new StackLayoutPanel(Unit.PX) {
      @Override
      public void showWidget(int index) {
        called.add(index);
        super.showWidget(index);
      }
    };
    Label stack1 = new Label("Stack 1");
    panel.add(new Label("Stack 0"), "Stack 0", 100);
    panel.add(stack1, "Stack 1", 100);
    panel.add(new Label("Stack 2"), "Stack 2", 100);
    called.clear();

    panel.showWidget(stack1);
    assertEquals(1, called.size());
    assertEquals(1, called.get(0).intValue());
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

  /**
   * Asserts that <b>widget</b> is attached to <b>panel</b> as a child in the
   * logical representation of <b>panel</b>.
   * 
   * @param panel the parent panel
   * @param child a expected child of <b>panel</b>
   */
  private void assertLogicalPaternityOfChild(StackLayoutPanel panel,
      Widget child) {
    assertTrue("The widget should be a child of the panel",
        panel.getWidgetIndex(child) >= 0);
  }

  /**
   * Asserts that <b>header</b> is attached to <b>panel</b> as a header of
   * <b>widget</b> in the logical representation of <b>panel</b>.
   * 
   * @param panel the parent panel
   * @param child the child whose header is being tested
   * @param header the expected header of <b>child</b>
   */
  private void assertLogicalPaternityOfHeader(StackLayoutPanel panel,
      Widget child, Widget header) {
    assertSame("The header should be attached to the panel.", header,
        panel.getHeaderWidget(child));
  }

  private StackLayoutPanel createStackLayoutPanel(Unit unit) {
    return new StackLayoutPanel(unit);
  }
}
