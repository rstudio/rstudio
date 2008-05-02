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
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

/**
 * TODO: document me.
 */
public class TabBarTest extends GWTTestCase {
  int selected;
  int beforeSelection;

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.DebugTest";
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
   * Create a new, empty tab bar.
   */
  protected TabBar createTabBar() {
    return new TabBar();
  }
}
