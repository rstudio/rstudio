//Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.user.client.ui;

import com.google.gwt.junit.client.GWTTestCase;

import java.util.Iterator;

public class TabPanelTest extends GWTTestCase {

  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  public void testInsertWithHTML() {
    TabPanel p = new TabPanel();
    Label l = new Label();
    p.add(l, "three");
    p.insert(new HTML("<b>hello</b>"), "two", true, 0);
    p.insert(new HTML("goodbye"), "one", false, 0);
    assertEquals(3, p.getWidgetCount());
  }

  public void testSelectionEvents() {
    TabPanel p = new TabPanel();
    RootPanel.get().add(p);

    p.add(new Button("foo"), "foo");
    p.add(new Button("bar"), "bar");

    // Make sure selecting a tab fires both events in the right order.
    p.addTabListener(new TabListener() {
      private boolean onBeforeFired;

      public void onTabSelected(SourcesTabEvents sender, int tabIndex) {
        assertTrue(onBeforeFired);
        finishTest();
      }

      public boolean onBeforeTabSelected(SourcesTabEvents sender, int tabIndex) {
        onBeforeFired = true;
        return true;
      }
    });

    this.delayTestFinish(1000);
    p.selectTab(1);
  }

  public void testIterator() {
    TabPanel p = new TabPanel();
    HTML foo = new HTML("foo");
    HTML bar = new HTML("bar");
    HTML baz = new HTML("baz");
    p.add(foo, "foo");
    p.add(bar, "bar");
    p.add(baz, "baz");

    // Iterate over the entire set and make sure it stops correctly.
    Iterator it = p.iterator();
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
}
