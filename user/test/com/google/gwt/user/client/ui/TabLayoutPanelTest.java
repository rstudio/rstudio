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
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Timer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Tests for {@link TabLayoutPanel}.
 */
public class TabLayoutPanelTest extends GWTTestCase {

  static class Adder implements HasWidgetsTester.WidgetAdder {
    @Override
    public void addChild(HasWidgets container, Widget child) {
      ((TabLayoutPanel) container).add(child, "foo");
    }
  }

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

    @Override
    public void onBeforeSelection(BeforeSelectionEvent<Integer> event) {
      assertFalse(onSelectionFired);
      onBeforeSelectionFired = true;
    }

    @Override
    public void onSelection(SelectionEvent<Integer> event) {
      assertTrue(onBeforeSelectionFired);
      onSelectionFired = true;
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.DebugTest";
  }

  public void testAttachDetachOrder() {
    HasWidgetsTester.testAll(new TabLayoutPanel(1, Unit.EM), new Adder(), true);
  }

  /**
   * Ensures that hidden children are layed out properly when their tabs are
   * selected. This has been a problem on IE6 (see issue 4596).
   */
  @DoNotRunWith({Platform.HtmlUnitBug})
  public void testHiddenChildLayout() {
    final TabLayoutPanel p = new TabLayoutPanel(32, Unit.PX);
    p.setSize("128px", "128px");
    RootPanel.get().add(p);

    final Label foo = new Label("foo");
    final Label bar = new Label("bar");
    p.add(foo, new Label("foo"));
    p.add(bar, new Label("bar"));

    delayTestFinish(2000);
    DeferredCommand.addCommand(new Command() {
      @Override
      public void execute() {
        assertEquals(128, foo.getOffsetWidth());
        assertEquals(128 - 32, foo.getOffsetHeight());

        p.selectTab(1);
        DeferredCommand.addCommand(new Command() {
          @Override
          public void execute() {
            assertEquals(128, bar.getOffsetWidth());
            assertEquals(128 - 32, bar.getOffsetHeight());
            finishTest();
          }
        });
      }
    });
  }

  /**
   * Ensures that hidden children are layed out properly when their tabs are
   * selected, when they're sized in EM units. This has been a problem on IE8
   * (see issue 4694).
   */
  public void testHiddenChildLayoutEM() {
    final TabLayoutPanel p = new TabLayoutPanel(2, Unit.EM);
    p.setSize("128px", "128px");
    RootPanel.get().add(p);

    final Label foo = new Label("foo");
    p.add(foo, new Label("foo"));

    // Add the 'bar' label in a nested LayoutPanel. This is meant to test that
    // layout propagates correctly into nested layout panels (something that
    // should happen naturally, but we have a specific hack for on IE8).
    final DockLayoutPanel inner = new DockLayoutPanel(Unit.EM);
    final Label bar = new Label("bar");
    inner.addSouth(bar, 2);
    inner.add(new Label("center"));
    p.add(inner, new Label("bar"));

    delayTestFinish(2000);
    DeferredCommand.addCommand(new Command() {
      @Override
      public void execute() {
        p.selectTab(1);
        DeferredCommand.addCommand(new Command() {
          @Override
          public void execute() {
            // Assert that the 'bar' label is of non-zero size on both axes.
            // The problem fixed in issue 4694 was causing its height to be
            // zero on IE8, because the EM units weren't being calculated
            // properly when it was initially hidden.
            assertTrue(bar.getOffsetWidth() > 0);
            assertTrue(bar.getOffsetHeight() > 0);

            finishTest();
          }
        });
      }
    });
  }

  public void testInsertBeforeSelected() {
    TabLayoutPanel p = new TabLayoutPanel(2, Unit.EM);
    p.add(new Label("foo"), "foo");
    p.selectTab(0);
    p.insert(new Label("bar"), "bar", 0);
    assertEquals(1, p.getSelectedIndex());
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

  public void testIsWidget() {
    TabLayoutPanel p = new TabLayoutPanel(2, Unit.EM);

    IsWidgetImpl add = new IsWidgetImpl(new Label("add"));
    IsWidgetImpl addText = new IsWidgetImpl(new Label("addText"));
    IsWidgetImpl addHtml = new IsWidgetImpl(new Label("addHtml"));
    IsWidgetImpl addWidget = new IsWidgetImpl(new Label("addWidget"));
    IsWidgetImpl added = new IsWidgetImpl(new Label("added widget"));

    IsWidgetImpl insert = new IsWidgetImpl(new Label("insText"));
    IsWidgetImpl insText = new IsWidgetImpl(new Label("insText"));
    IsWidgetImpl insHtml = new IsWidgetImpl(new Label("insHtml"));
    IsWidgetImpl insWidget = new IsWidgetImpl(new Label("insWidget"));
    IsWidgetImpl inserted = new IsWidgetImpl(new Label("inserted widget"));

    p.add(add);
    p.add(addText, "added text");
    p.add(addHtml, "<b>added html</b>", true);
    p.add(addWidget, added);

    p.insert(insert, 0);
    p.insert(insText, "inserted text", 2);
    p.insert(insHtml, "<b>inserted html</b>", true, 4);
    p.insert(insWidget, inserted, 6);

    assertEquals(8, p.getWidgetCount());
    assertEquals(0, p.getWidgetIndex(insert));
    assertEquals(1, p.getWidgetIndex(add));
    assertEquals(2, p.getWidgetIndex(insText));
    assertEquals(3, p.getWidgetIndex(addText));
    assertEquals(4, p.getWidgetIndex(insHtml));
    assertEquals(5, p.getWidgetIndex(addHtml));
    assertEquals(6, p.getWidgetIndex(insWidget));
    assertEquals(7, p.getWidgetIndex(addWidget));

    assertEquals("", p.getTabWidget(insert).getElement().getInnerHTML());
    assertEquals("", p.getTabWidget(add).getElement().getInnerHTML());
    assertEquals("inserted text",
        p.getTabWidget(insText).getElement().getInnerHTML());
    assertEquals("added text",
        p.getTabWidget(addText).getElement().getInnerHTML());
    assertEquals("<b>inserted html</b>",
        p.getTabWidget(insHtml).getElement().getInnerHTML().toLowerCase(Locale.ROOT));
    assertEquals("<b>added html</b>",
        p.getTabWidget(addHtml).getElement().getInnerHTML().toLowerCase(Locale.ROOT));
    assertEquals(inserted.w, p.getTabWidget(insWidget));
    assertEquals(added.w, p.getTabWidget(addWidget));

    class Handler implements SelectionHandler<Integer> {
      boolean fired = false;

      @Override
      public void onSelection(SelectionEvent<Integer> event) {
        fired = true;
      }
    }

    Handler handler = new Handler();
    p.addSelectionHandler(handler);

    p.selectTab(addText, true);
    assertTrue(handler.fired);
    assertEquals(3, p.getSelectedIndex());

    handler.fired = false;
    p.selectTab(insWidget);
    assertTrue(handler.fired);
    assertEquals(6, p.getSelectedIndex());
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

  /**
   * Test that removing a widget removes the associated tab.
   */
  public void testRemoveWidgetFromParent() {
    TabLayoutPanel p = new TabLayoutPanel(2, Unit.EM);

    Label content0 = new Label("Content 0");
    Label header0 = new Label("Header 0");
    p.add(content0, header0);
    Label content1 = new Label("Content 1");
    Label header1 = new Label("Header 1");
    p.add(content1, header1);
    Label content2 = new Label("Content 2");
    Label header2 = new Label("Header 2");
    p.add(content2, header2);
    assertEquals(3, p.getWidgetCount());
    assertEquals(content0, p.getWidget(0));
    assertEquals(content1, p.getWidget(1));
    assertEquals(content2, p.getWidget(2));
    assertEquals(header0, p.getTabWidget(0));
    assertEquals(header1, p.getTabWidget(1));
    assertEquals(header2, p.getTabWidget(2));

    // Remove content.
    content1.removeFromParent();
    assertNull(content1.getParent());
    assertNull(header1.getParent());
    assertEquals(2, p.getWidgetCount());
    assertEquals(content0, p.getWidget(0));
    assertEquals(content2, p.getWidget(1));
    assertEquals(header0, p.getTabWidget(0));
    assertEquals(header2, p.getTabWidget(1));

    // Remove a header.
    header2.removeFromParent();
    assertNull(content2.getParent());
    assertNull(header2.getParent());
    assertEquals(1, p.getWidgetCount());
    assertEquals(content0, p.getWidget(0));
    assertEquals(header0, p.getTabWidget(0));
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
    p.selectTab(1);
    handler.assertOnBeforeSelectionFired(true);
    handler.assertOnSelectionFired(true);
  }

  public void testSelectionEventsNoFire() {
    TabLayoutPanel p = new TabLayoutPanel(2, Unit.EM);
    RootPanel.get().add(p);

    p.add(new Button("foo"), "foo");
    p.add(new Button("bar"), "bar");

    TestSelectionHandler handler = new TestSelectionHandler();
    p.addBeforeSelectionHandler(handler);
    p.addSelectionHandler(handler);
    p.selectTab(1, false);
    handler.assertOnBeforeSelectionFired(false);
    handler.assertOnSelectionFired(false);
  }

  /**
   * Issue 6034: setTabText() removes the tab.
   */
  public void testSetTabText() {
    TabLayoutPanel p = new TabLayoutPanel(1, Unit.EM);
    Label[] labels = new Label[3];
    for (int i = 0; i < labels.length; i++) {
      labels[i] = new Label("content" + i);
      p.add(labels[i]);
    }

    // Set the text of tab 1.
    p.setTabText(1, "new text");
    assertEquals(3, p.getWidgetCount());
    assertEquals("new text", ((HasText) p.getTabWidget(1)).getText());
  }

  /**
   * Test that {@link TabLayoutPanel} calls widget.setVisible(true/false) on
   * each widget, when it is shown/hidden.
   */
  public void testSetWidgetVisible() {
    TabLayoutPanel p = new TabLayoutPanel(1, Unit.EM);
    Label[] labels = new Label[3];
    for (int i = 0; i < labels.length; i++) {
      labels[i] = new Label("content" + i);
      p.add(labels[i]);
    }

    // Initially, the first widget should be visible.
    p.forceLayout();
    assertTrue(labels[0].isVisible());
    assertFalse(labels[1].isVisible());
    assertFalse(labels[2].isVisible());

    // Show widget at index 1, make sure it becomes visible, and the one at
    // index 0 is hidden.
    p.selectTab(1);
    p.forceLayout();
    assertFalse(labels[0].isVisible());
    assertTrue(labels[1].isVisible());
    assertFalse(labels[2].isVisible());

    // Show widget at index 0, make sure it changed back to the initial state.
    p.selectTab(0);
    p.forceLayout();
    assertTrue(labels[0].isVisible());
    assertFalse(labels[1].isVisible());
    assertFalse(labels[2].isVisible());
  }

  /**
   * For legacy reasons, {@link TabLayoutPanel#selectTab(Widget)} should call
   * {@link TabLayoutPanel#selectTab(int)}.
   */
  public void testSelectTabLegacy() {
    final List<Integer> called = new ArrayList<Integer>();
    TabLayoutPanel panel = new TabLayoutPanel(100.0, Unit.PX) {
      @Override
      public void selectTab(int index) {
        called.add(index);
        super.selectTab(index);
      }
    };
    Label tab1 = new Label("Tab 1");
    panel.add(new Label("Tab 0"), "Tab 0");
    panel.add(tab1, "Tab 1");
    panel.add(new Label("Tab 2"), "Tab 2");
    called.clear();

    panel.selectTab(tab1);
    assertEquals(1, called.size());
    assertEquals(1, called.get(0).intValue());
  }

  /**
   * Tests that tabs actually line up properly (see issue 4447).
   */
  public void testTabLayout() {
    final TabLayoutPanel p = new TabLayoutPanel(2, Unit.EM);
    RootPanel.get().add(p);

    p.add(new Button("foo"), new Label("foo"));
    p.add(new Button("bar"), new Label("bar"));

    // Give the browser some time to layout
    new Timer() {

      @Override
      public void run() {
        assertEquals(p.getTabWidget(0).getElement().getOffsetTop(),
            p.getTabWidget(1).getElement().getOffsetTop());
        finishTest();
      }

    }.schedule(100);

    delayTestFinish(200);
  }

  /**
   * Test that forcing layout will call onResize only once.
   */
  public void testForceLayoutNoRedundantOnResize() {
    final List<Boolean> called = new ArrayList<>();
    final TabLayoutPanel panel = new TabLayoutPanel(2, Unit.EM);
    SimpleLayoutPanel child = new SimpleLayoutPanel() {
      @Override
      public void onResize() {
        super.onResize();
        called.add(true);
      }
    };
    panel.add(child,"Tab1");
    panel.forceLayout();
    assertEquals(1,called.size());
  }
}
