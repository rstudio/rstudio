//Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.user.client.ui;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

public class LinearPanelTest extends GWTTestCase {

  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  public void testHorizontalAddRemove() {
    HorizontalPanel hp = new HorizontalPanel();
    RootPanel.get().add(hp);

    HTML[] stuff = new HTML[3];
    stuff[0] = new HTML("foo");
    stuff[1] = new HTML("bar");
    stuff[2] = new HTML("baz");

    // Ensure that we can add & remove cleanly.
    hp.add(stuff[0]);
    hp.add(stuff[1]);
    hp.add(stuff[2]);

    assertTrue(hp.getWidgetCount() == 3);
    hp.remove(stuff[1]);
    assertTrue(hp.getWidgetCount() == 2);
    assertTrue(hp.getWidget(0) == stuff[0]);
    assertTrue(hp.getWidget(1) == stuff[2]);

    // Make sure the table structure is still correct (no stuff left hanging
    // around).
    Element elem = hp.getElement();
    Element body = DOM.getFirstChild(elem);
    Element tr = DOM.getFirstChild(body);
    assertTrue(DOM.getChildCount(tr) == 2);
  }

  public void testVerticalAddRemove() {
    VerticalPanel hp = new VerticalPanel();
    RootPanel.get().add(hp);

    HTML[] stuff = new HTML[3];
    stuff[0] = new HTML("foo");
    stuff[1] = new HTML("bar");
    stuff[2] = new HTML("baz");

    // Ensure that we can add & remove cleanly.
    hp.add(stuff[0]);
    hp.add(stuff[1]);
    hp.add(stuff[2]);

    assertTrue(hp.getWidgetCount() == 3);
    hp.remove(stuff[1]);
    assertTrue(hp.getWidgetCount() == 2);
    assertTrue(hp.getWidget(0) == stuff[0]);
    assertTrue(hp.getWidget(1) == stuff[2]);

    // Make sure the table structure is still correct (no stuff left hanging
    // around).
    Element elem = hp.getElement();
    Element body = DOM.getFirstChild(elem);
    assertTrue(DOM.getChildCount(body) == 2);
  }
}
