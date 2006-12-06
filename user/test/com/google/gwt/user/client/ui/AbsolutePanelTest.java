package com.google.gwt.user.client.ui;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Window;

public class AbsolutePanelTest extends GWTTestCase {

  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  public void testPositioning() {
    // Make an absolute panel with a label at (42, 43).
    AbsolutePanel abs = new AbsolutePanel();
    abs.setSize("128px", "128px");
    Label lbl = new Label("foo");
    abs.add(lbl, 3, 7);

    // Put the panel in a grid that will place it at (32, 32) within the grid.
    Grid g = new Grid(2, 2);
    g.setBorderWidth(0);
    g.setCellPadding(0);
    g.setCellSpacing(0);
    g.getCellFormatter().setWidth(0, 0, "100px");
    g.getCellFormatter().setHeight(0, 0, "200px");
    g.setWidget(1, 1, abs);

    // Clear the margin so that absolute position is predictable.
    Window.setMargin("0px");
    RootPanel.get().add(g);

    // Make sure that the label's position, both relative to the absolute panel
    // and relative to the page, is correct. It is important to test both of
    // these, because an incorrectly constructed AbsolutePanel will lead to
    // wacky positioning of its children.
    int x = abs.getWidgetLeft(lbl);
    int y = abs.getWidgetTop(lbl);
    int absX = lbl.getAbsoluteLeft();
    int absY = lbl.getAbsoluteTop();
    assertEquals(x, 3);
    assertEquals(y, 7);
    assertEquals(absX, 3 + 100);
    assertEquals(absY, 7 + 200);
  }
}
