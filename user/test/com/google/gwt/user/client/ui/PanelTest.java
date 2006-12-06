//Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.user.client.ui;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

/**
 * TODO: add circular containment test.
 */
public class PanelTest extends GWTTestCase {

  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  private static class TestComposite extends Composite {

    private Label lbl = new Label("foo");

    public TestComposite() {
      initWidget(lbl);
    }

    public Widget getImplWidget() {
      return lbl;
    }
  }

  public void testAdoptException() {
    // Test to ensure that Panel.adopt() on a composite's implementation widget
    // correctly throws an IllegalStateException. We'll test separately with
    // VerticalPanel, HorizontalPanel, StackPanel, and HTMLTable (Grid), because
    // each of these has the potential to get partially constructed if this
    // exception is not dealt with properly.
    TestComposite comp = new TestComposite();

    // VerticalPanel
    VerticalPanel vp = new VerticalPanel();
    try {
      vp.add(comp.getImplWidget());
      fail("Expection IllegalStateException");
    } catch (IllegalStateException e) {
    }
    Element tbody = DOM.getFirstChild(vp.getElement());
    assertEquals(DOM.getChildCount(tbody), 0);

    // HorizontalPanel
    HorizontalPanel hp = new HorizontalPanel();
    try {
      hp.add(comp.getImplWidget());
      fail("Expection IllegalStateException");
    } catch (IllegalStateException e) {
    }
    tbody = DOM.getFirstChild(hp.getElement());
    Element tr = DOM.getFirstChild(tbody);
    assertEquals(DOM.getChildCount(tr), 0);

    // StackPanel
    StackPanel stack = new StackPanel();
    try {
      stack.add(comp.getImplWidget());
      fail("Expection IllegalStateException");
    } catch (IllegalStateException e) {
    }
    tbody = DOM.getFirstChild(stack.getElement());
    assertEquals(DOM.getChildCount(tbody), 0);

    // Grid (in this case, we want to ensure that the cell's contents are not
    // cleared when the widget cannot be added).
    Grid grid = new Grid(1, 1);
    grid.setText(0, 0, "foo");
    try {
      grid.setWidget(0, 0, comp.getImplWidget());
      fail("Expection IllegalStateException");
    } catch (IllegalStateException e) {
    }
    assertTrue(grid.getText(0, 0).equals("foo"));
  }
}
