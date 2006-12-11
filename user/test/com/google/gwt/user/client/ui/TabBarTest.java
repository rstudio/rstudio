//Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.user.client.ui;

import com.google.gwt.junit.client.GWTTestCase;

public class TabBarTest extends GWTTestCase {

  int selected;
  int beforeSelection;
  
  public String getModuleName() {
    return "com.google.gwt.user.User";
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
    assertEquals(-1,beforeSelection);
    assertEquals(0,selected);
    assertTrue(result);
    
    result = bar.selectTab(1);
    assertFalse(result);
    assertEquals(0,selected);
    assertEquals(1, beforeSelection);
    
    result = bar.selectTab(2);
    assertTrue(result);
    assertEquals(2,selected);
    assertEquals(2,beforeSelection);
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
