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
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

/**
 * Tests the {@link TabBar} widget.
 */
public class TabBarTest extends GWTTestCase {

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

  private static final String html = "<b>hello</b><i>world</i>";

  int selected;
  int beforeSelection;

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.DebugTest";
  }

  public void testAddTab() {
    TabBar bar = createTabBar();
    bar.addTab(SafeHtmlUtils.fromSafeConstant(html));
    
    assertEquals(html, bar.getTabHTML(0).toLowerCase());
  }

  public void testDebugId() {
    // Create a tab bar with a few tabs
    TabBar bar = createTabBar();
    Label tab0 = new Label("My Tab 0");
    bar.addTab(tab0);
    Label tab1 = new Label("My Tab 1");
    bar.addTab(tab1);
    Label tab2 = new Label("My Tab 2");
    bar.addTab(tab2);

    // Verify the debug IDs are on the correct elements
    bar.ensureDebugId("myBar");
    HorizontalPanel hPanel = (HorizontalPanel) bar.getWidget();
    Element tr = DOM.getFirstChild(hPanel.getBody());
    UIObjectTest.assertDebugId("myBar", bar.getElement());
    UIObjectTest.assertDebugId("myBar-tab0", DOM.getParent(tab0.getElement()));
    UIObjectTest.assertDebugId("myBar-tab-wrapper0", DOM.getChild(tr, 1));
    UIObjectTest.assertDebugId("myBar-tab1", DOM.getParent(tab1.getElement()));
    UIObjectTest.assertDebugId("myBar-tab-wrapper1", DOM.getChild(tr, 2));
    UIObjectTest.assertDebugId("myBar-tab2", DOM.getParent(tab2.getElement()));
    UIObjectTest.assertDebugId("myBar-tab-wrapper2", DOM.getChild(tr, 3));
  }

  public void testEnableDisable() {
    final TabBar bar = createTabBar();
    bar.addTab("foo");
    bar.addTab("bar");
    bar.addTab("baz");

    assertTrue(bar.isTabEnabled(1));
    bar.setTabEnabled(1, false);
    assertFalse(bar.isTabEnabled(1));
    bar.setTabEnabled(1, true);
    assertTrue(bar.isTabEnabled(1));
  }

  public void testInsertTab() {
    TabBar bar = createTabBar();
    bar.insertTab(SafeHtmlUtils.fromSafeConstant(html), 0);
    
    assertEquals(html, bar.getTabHTML(0).toLowerCase());
  }

  public void testSelect() {
    // Create a tab bar with three items.
    final TabBar bar = createTabBar();
    bar.addTab("foo");
    bar.addTab("bar");
    bar.addTab("baz");
    bar.selectTab(1);
    assertEquals(1, bar.getSelectedTab());
    bar.selectTab(2);
    assertEquals(2, bar.getSelectedTab());
    bar.selectTab(-1);
    assertEquals(-1, bar.getSelectedTab());
    TabListener listener = new TabListener() {
      public boolean onBeforeTabSelected(SourcesTabEvents sender, int tabIndex) {
        beforeSelection = tabIndex;
        if (tabIndex == 1) {
          return false;
        } else {
          return true;
        }
      }

      public void onTabSelected(SourcesTabEvents sender, int tabIndex) {
        selected = tabIndex;
      }
    };
    bar.addTabListener(listener);
    boolean result = bar.selectTab(-1);
    assertEquals(-1, beforeSelection);
    assertEquals(0, selected);
    assertTrue(result);

    result = bar.selectTab(1);
    assertFalse(result);
    assertEquals(0, selected);
    assertEquals(1, beforeSelection);

    result = bar.selectTab(2);
    assertTrue(result);
    assertEquals(2, selected);
    assertEquals(2, beforeSelection);
  }

  public void testSelectionEvents() {
    TabBar bar = new TabBar();
    RootPanel.get().add(bar);

    bar.addTab("foo");
    bar.addTab("bar");

    // Make sure selecting a tab fires both events in the right order.
    TestSelectionHandler handler = new TestSelectionHandler();
    bar.addBeforeSelectionHandler(handler);
    bar.addSelectionHandler(handler);
    bar.selectTab(1);
    handler.assertOnBeforeSelectionFired(true);
    handler.assertOnSelectionFired(true);
  }

  public void testSelectionEventsNoFire() {
    TabBar bar = new TabBar();
    RootPanel.get().add(bar);

    bar.addTab("foo");
    bar.addTab("bar");

    TestSelectionHandler handler = new TestSelectionHandler();
    bar.addBeforeSelectionHandler(handler);
    bar.addSelectionHandler(handler);
    bar.selectTab(1, false);
    handler.assertOnBeforeSelectionFired(false);
    handler.assertOnSelectionFired(false);
  }

  public void testSetTabSafeHtml() {
    TabBar bar = createTabBar();
    bar.insertTab("foo", 0);
    bar.setTabHTML(0, SafeHtmlUtils.fromSafeConstant(html));
    
    assertEquals(html, bar.getTabHTML(0).toLowerCase());
  }

  public void testGetHTML() {
    final TabBar bar = createTabBar();
    bar.addTab("foo");
    bar.addTab("<b>bar</b>", true);
    bar.addTab("baz");
    assertEquals("foo", bar.getTabHTML(0));
    assertTrue("<b>bar</b>".equalsIgnoreCase(bar.getTabHTML(1)));
    bar.removeTab(1);
    assertEquals("baz", bar.getTabHTML(1));
  }

  public void testSetTextAndHTML() {
    final TabBar bar = createTabBar();
    bar.addTab("foo");
    bar.addTab("bar");
    bar.addTab("baz");
    bar.addTab(new Grid(0, 0));

    bar.setTabText(1, "w00t");
    assertEquals("w00t", bar.getTabHTML(1));

    // toLowerCase() is necessary in these assertions because IE capitalizes
    // HTML tags read from innerHTML.
    bar.setTabHTML(1, "<i>w00t!</i>");
    assertEquals("<i>w00t!</i>", bar.getTabHTML(1).toLowerCase());

    // Set the text knowing that we currently have an HTML. This should replace
    // the HTML with a Label.
    bar.setTabText(1, "<b>w00t</b>");
    assertEquals("<b>w00t</b>", bar.getTabHTML(1).toLowerCase());

    // Set the text knowing that we currently have a Grid. This should replace
    // the Grid with a Label.
    bar.setTabText(3, "w00t");
    assertEquals("w00t", bar.getTabHTML(3));
  }

  /**
   * Verify that if a tab contains a widget, {@link TabBar#getTabHTML(int)}
   * returns the HTML of the focusable element with the widget inside of it.
   */
  public void testGetHTMLWithWidget() {
    TabBar bar = createTabBar();
    Button myButton = new Button("My Button");
    bar.addTab(myButton);

    Widget focusablePanel = myButton.getParent();
    assertEquals(focusablePanel.getElement().getParentElement().getInnerHTML(),
        bar.getTabHTML(0));
  }

  /**
   * Verify that wordWrap works when adding widgets that implement HasWordWrap.
   */
  public void testWordWrapWithSupport() {
    TabBar bar = createTabBar();
    Label tabContent0 = new Label("Tab 0", false);
    Label tabContent1 = new Label("Tab 1", true);
    bar.addTab(tabContent0);
    bar.addTab(tabContent1);
    bar.addTab("Tab 2");

    // hasWordWrap()
    {
      assertTrue(bar.getTab(0).hasWordWrap());
      assertTrue(bar.getTab(1).hasWordWrap());
      assertTrue(bar.getTab(2).hasWordWrap());
    }

    // getWordWrap()
    {
      assertFalse(bar.getTab(0).getWordWrap());
      assertTrue(bar.getTab(1).getWordWrap());
      assertFalse(bar.getTab(2).getWordWrap());
    }

    // setWordWrap()
    {
      bar.getTab(0).setWordWrap(true);
      bar.getTab(1).setWordWrap(true);
      bar.getTab(2).setWordWrap(true);
      assertTrue(bar.getTab(0).getWordWrap());
      assertTrue(bar.getTab(1).getWordWrap());
      assertTrue(bar.getTab(2).getWordWrap());
      assertTrue(tabContent0.getWordWrap());
    }
  }

  /**
   * Verify that wordWrap works when adding widgets that do not implement
   * HasWordWrap.
   */
  public void testWordWrapWithoutSupport() {
    TabBar bar = createTabBar();
    bar.addTab(new Button("Tab 0"));

    // hasWordWrap
    assertFalse(bar.getTab(0).hasWordWrap());

    // getWordWrap();
    try {
      bar.getTab(0).getWordWrap();
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException e) {
      assertTrue(true);
    }

    // setWordWrap();
    try {
      bar.getTab(0).setWordWrap(true);
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException e) {
      assertTrue(true);
    }
  }

  /**
   * Create a new, empty tab bar.
   */
  protected TabBar createTabBar() {
    return new TabBar();
  }
}
