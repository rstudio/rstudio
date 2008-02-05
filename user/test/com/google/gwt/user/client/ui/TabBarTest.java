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
    TabBar bar = new TabBar();
    Label tab0 = new Label("My Tab 0");
    bar.addTab(tab0);
    Label tab1 = new Label("My Tab 1");
    bar.addTab(tab1);
    Label tab2 = new Label("My Tab 2");
    bar.addTab(tab2);

    bar.ensureDebugId("myBar");
    UIObjectTest.assertDebugId("myBar", bar.getElement());
    UIObjectTest.assertDebugId("myBar-tab0", DOM.getParent(tab0.getElement()));
    UIObjectTest.assertDebugId("myBar-tab-wrapper0",
        DOM.getParent(DOM.getParent(tab0.getElement())));
    UIObjectTest.assertDebugId("myBar-tab1", DOM.getParent(tab1.getElement()));
    UIObjectTest.assertDebugId("myBar-tab-wrapper1",
        DOM.getParent(DOM.getParent(tab1.getElement())));
    UIObjectTest.assertDebugId("myBar-tab2", DOM.getParent(tab2.getElement()));
    UIObjectTest.assertDebugId("myBar-tab-wrapper2",
        DOM.getParent(DOM.getParent(tab2.getElement())));
  }

  public void testSelect() {
    // Create a tab bar with three items.
    final TabBar bar = new TabBar();
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
    final TabBar bar = new TabBar();
    bar.addTab("foo");
    bar.addTab("<b>bar</b>", true);
    bar.addTab("baz");
    assertEquals("foo", bar.getTabHTML(0));
    assertTrue("<b>bar</b>".equalsIgnoreCase(bar.getTabHTML(1)));
    bar.removeTab(1);
    assertEquals("baz", bar.getTabHTML(1));
  }
}
