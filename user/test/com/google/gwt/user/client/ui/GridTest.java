// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.user.client.ui;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

public class GridTest extends HTMLTableTestBase {

  public HTMLTable getTable(int row, int column) {
    return new Grid(row, column);
  }

  public void testBounds() {
    HTMLTable t = getTable(3, 3);
    try {
      t.setText(-1, 0, "hello");
      fail("IndexOutOfBoundsException should have been thrown");
    } catch (IndexOutOfBoundsException e) {
      assertEquals("Cannot access a row with a negative index: -1", e
        .getMessage());
    }
    try {
      t.setText(0, -1, "hello");
      fail("IndexOutOfBoundsException should have been thrown");
    } catch (IndexOutOfBoundsException e) {
      assertEquals("Cannot access a column with a negative index: -1", e
        .getMessage());
    }
    try {
      t.clearCell(3, 3);
      fail("IndexOutOfBoundsException should have been thrown");
    } catch (IndexOutOfBoundsException e) {
      assertEquals("Row index: 3, Row size: 3", e.getMessage());
    }

    try {
      t.getText(0, 5);
      fail("IndexOutOfBoundsException should have been thrown");
    } catch (Exception e) {

    }
  }

  public void testColumnFormatter() {
    Grid r = new Grid(4, 5);
    Grid.ColumnFormatter columns = r.getColumnFormatter();
    columns.setStyleName(0, "a");
    assertEquals("a", columns.getStyleName(0));
    columns.addStyleName(0, "b");
    assertEquals("a b", getNormalizedStyleName(columns, 0));
    columns.addStyleName(0, "c");
    assertEquals("a b c", getNormalizedStyleName(columns, 0));
    // Remove first.
    columns.removeStyleName(0, "a");
    assertEquals("b c", getNormalizedStyleName(columns, 0));

    // Remove last.
    columns.removeStyleName(0, "c");
    assertEquals("b", getNormalizedStyleName(columns, 0));

    // Only one column should be created.
    Element e = DOM.getChild(r.getElement(), 0);
    assertEquals(1, DOM.getChildCount(e));

    columns.addStyleName(3,  "a");
    // Now there shoud be three such columns .
    e = DOM.getChild(r.getElement(), 0);
    assertEquals(4, DOM.getChildCount(e));
  }

  private String getNormalizedStyleName(Grid.ColumnFormatter formatter,
      int index) {
    return formatter.getStyleName(index).replaceAll("  ", " ").trim();
  }

  public void testColumnMessage() {

    Grid r = new Grid(1, 1);

    try {
      r.setWidget(0, 2, new Label("hello"));
      fail("Expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException e) {
      assertEquals("Column index: 2, Column size: 1", e.getMessage());
    }

    try {
      r.setWidget(2, 0, new Label("hello"));
      fail("Expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException e) {
      assertEquals("Row index: 2, Row size: 1", e.getMessage());
    }

  }

}
