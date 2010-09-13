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

import com.google.gwt.event.logical.shared.BeforeSelectionEvent;
import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

import java.util.Iterator;

/**
 * Tests the TabPanel.
 */
@SuppressWarnings("deprecation")
public class TabPanelTest extends GWTTestCase {

  static class Adder implements HasWidgetsTester.WidgetAdder {
    public void addChild(HasWidgets container, Widget child) {
      ((TabPanel) container).add(child, "foo");
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

    public void onBeforeSelection(BeforeSelectionEvent<Integer> event) {
      assertFalse(onSelectionFired);
      onBeforeSelectionFired = true;
    }

    public void onSelection(SelectionEvent<Integer> event) {
      assertTrue(onBeforeSelectionFired);
      onSelectionFired = true;
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.DebugTest";
  }

  /**
   * Test that methods associated with animations delegate to the
   * {@link DeckPanel}.
   */
  public void testAnimationDelegation() {
    TabPanel tabPanel = createTabPanel();
    DeckPanel deck = tabPanel.getDeckPanel();

    tabPanel.setAnimationEnabled(true);
    assertTrue(tabPanel.isAnimationEnabled());
    assertTrue(deck.isAnimationEnabled());

    tabPanel.setAnimationEnabled(false);
    assertFalse(tabPanel.isAnimationEnabled());
    assertFalse(deck.isAnimationEnabled());

    deck.setAnimationEnabled(true);
    assertTrue(tabPanel.isAnimationEnabled());
    assertTrue(deck.isAnimationEnabled());
  }

  public void testAttachDetachOrder() {
    HasWidgetsTester.testAll(createTabPanel(), new Adder(), true);
  }

  public void testDebugId() {
    TabPanel panel = createTabPanel();
    Label content0 = new Label("content0");
    Label tab0 = new Label("tab0");
    panel.add(content0, tab0);
    Label content1 = new Label("content1");
    Label tab1 = new Label("tab1");
    panel.add(content1, tab1);
    Label content2 = new Label("content2");
    Label tab2 = new Label("tab2");
    panel.add(content2, tab2);

    // Set the Debug ID
    panel.ensureDebugId("myPanel");
    UIObjectTest.assertDebugId("myPanel", panel.getElement());
    UIObjectTest.assertDebugId("myPanel-bar", panel.getTabBar().getElement());
    UIObjectTest.assertDebugId("myPanel-bottom",
        panel.getDeckPanel().getElement());

    // Check the tabs
    HorizontalPanel hPanel = (HorizontalPanel) panel.getTabBar().getWidget();
    Element tr = DOM.getFirstChild(hPanel.getBody());
    UIObjectTest.assertDebugId("myPanel-bar-tab0",
        DOM.getParent(tab0.getElement()));
    UIObjectTest.assertDebugId("myPanel-bar-tab-wrapper0", DOM.getChild(tr, 1));
    UIObjectTest.assertDebugId("myPanel-bar-tab1",
        DOM.getParent(tab1.getElement()));
    UIObjectTest.assertDebugId("myPanel-bar-tab-wrapper1", DOM.getChild(tr, 2));
    UIObjectTest.assertDebugId("myPanel-bar-tab2",
        DOM.getParent(tab2.getElement()));
    UIObjectTest.assertDebugId("myPanel-bar-tab-wrapper2", DOM.getChild(tr, 3));
  }

  public void testInsertMultipleTimes() {
    TabPanel p = createTabPanel();

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
    TabPanel p = createTabPanel();
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
    TabPanel p = createTabPanel();

    TextBox wa = new TextBox();
    CheckBox wb = new CheckBox();
    VerticalPanel wc = new VerticalPanel();
    wc.add(new Label("First"));
    wc.add(new Label("Second"));

    p.add(new Label("Content C"), wc);
    p.insert(new Label("Content B"), wb, 0);
    p.insert(new Label("Content A"), wa, 0);

    // Call these to ensure we don't throw an exception.
    assertTrue(p.getTabBar().getTabHTML(0).length() > 0);
    assertTrue(p.getTabBar().getTabHTML(1).length() > 0);
    assertTrue(p.getTabBar().getTabHTML(2).length() > 0);
    assertEquals(3, p.getWidgetCount());
  }

  public void testIsWidget() {
    TabPanel p = createTabPanel();

    IsWidgetImpl addText = new IsWidgetImpl(new Label("addText"));
    IsWidgetImpl addHtml = new IsWidgetImpl(new Label("addHtml"));
    IsWidgetImpl addWidget = new IsWidgetImpl(new Label("addWidget"));
    IsWidgetImpl added = new IsWidgetImpl(new Label("added"));

    IsWidgetImpl insText = new IsWidgetImpl(new Label("insText"));
    IsWidgetImpl insHtml = new IsWidgetImpl(new Label("insHtml"));
    IsWidgetImpl insWidget = new IsWidgetImpl(new Label("insWidget"));
    IsWidgetImpl inserted = new IsWidgetImpl(new Label("inserted"));

    p.add(addText, "added text");
    p.add(addHtml, "<b>added html</b>", true);
    p.add(addWidget, added);

    p.insert(insText, "inserted text", 0);
    p.insert(insHtml, "<b>inserted html</b>", true, 2);
    p.insert(insWidget, inserted, 4);

    assertEquals(6, p.getWidgetCount());
    assertEquals(0, p.getWidgetIndex(insText));
    assertEquals(1, p.getWidgetIndex(addText));
    assertEquals(2, p.getWidgetIndex(insHtml));
    assertEquals(3, p.getWidgetIndex(addHtml));
    assertEquals(4, p.getWidgetIndex(insWidget));
    assertEquals(5, p.getWidgetIndex(addWidget));
  }

  public void testIterator() {
    TabPanel p = createTabPanel();
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
    TabPanel p = createTabPanel();
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
    TabPanel p = createTabPanel();
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

  public void testUnmodifiableDeckPanelSubclasses() {
    TabPanel p = createTabPanel();
    DeckPanel d = p.getDeckPanel();

    try {
      d.add(new Label("No"));
      fail("Internal DeckPanel should not allow add() method");
    } catch (UnsupportedOperationException e) {
      // Expected behavior
    }

    try {
      d.insert(new Label("No"), 0);
      fail("Internal DeckPanel should not allow insert() method");
    } catch (UnsupportedOperationException e) {
      // Expected behavior
    }

    try {
      d.clear();
      fail("Internal DeckPanel should not allow clear() method");
    } catch (UnsupportedOperationException e) {
      // Expected behavior
    }
  }

  public void testUnmodifiableTabBarSubclasses() {
    TabPanel p = createTabPanel();
    TabBar b = p.getTabBar();

    try {
      b.addTab("no");
      fail("Internal TabBar should not allow addTab() method");
    } catch (UnsupportedOperationException e) {
      // Expected behavior
    }

    try {
      b.addTab("no", true);
      fail("Internal TabBar should not allow addTab() method");
    } catch (UnsupportedOperationException e) {
      // Expected behavior
    }

    try {
      b.addTab(new Label("no"));
      fail("Internal TabBar should not allow addTab() method");
    } catch (UnsupportedOperationException e) {
      // Expected behavior
    }

    try {
      b.insertTab("no", 0);
      fail("Internal TabBar should not allow insertTab() method");
    } catch (UnsupportedOperationException e) {
      // Expected behavior
    }

    try {
      b.insertTab("no", true, 0);
      fail("Internal TabBar should not allow insertTab() method");
    } catch (UnsupportedOperationException e) {
      // Expected behavior
    }

    try {
      b.insertTab(new Label("no"), 0);
      fail("Internal TabBar should not allow insertTab() method");
    } catch (UnsupportedOperationException e) {
      // Expected behavior
    }

    try {
      b.removeTab(0);
      fail("Internal TabBar should not allow removeTab() method");
    } catch (UnsupportedOperationException e) {
      // Expected behavior
    }
  }

  /**
   * Create a new, empty tab panel.
   */
  protected TabPanel createTabPanel() {
    return new TabPanel();
  }
}
