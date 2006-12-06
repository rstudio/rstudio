// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.user.client.ui;

import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;

public class FlexTableTest extends HTMLTableTestBase {

  public HTMLTable getTable(int row, int column) {
    return new FlexTable();
  }

  public void testInertFirst() {
    FlexTable t = new FlexTable();
    t.insertRow(0);
    t.setWidget(0, 3, new HTML("hello"));

    t.insertRow(1);
    t.setWidget(1, 0, new HTML("goodbye"));
  }

  public void testBounds() {
    HTMLTable t = getTable(3, 3);
    try {
      t.setText(-1, 0, "hello");
      fail("IndexOutOfBoundsException should have been thrown");
    } catch (IndexOutOfBoundsException e) {
      assertEquals("Cannot create a row with a negative index: -1", e
        .getMessage());
    }
    try {
      t.setText(0, -1, "hello");
      fail("IndexOutOfBoundsException should have been thrown");
    } catch (IndexOutOfBoundsException e) {
      assertEquals("Cannot create a column with a negative index: -1", e
        .getMessage());
    }
    try {
      t.clearCell(3, 3);
      fail("IndexOutOfBoundsException should have been thrown");
    } catch (IndexOutOfBoundsException e) {
      assertEquals("Row index: 3, Row size: 1", e.getMessage());
    }

    try {
      t.getText(0, 5);
      fail("IndexOutOfBoundsException should have been thrown");
    } catch (Exception e) {
      // Expected
    }
  }

  public void testNullWidget() {
    FlexTable ft = new FlexTable();
    ft.setText(0, 0, "hello");
    assertNull(ft.getWidget(0, 0));
    ft.setWidget(0, 1, null);
    assertNull(ft.getWidget(0, 1));
    ft.clear();
  }

  public void secondarySetHeightTest() {
    FlexTable ft = new FlexTable();
    FlexCellFormatter cellFormatter = (FlexCellFormatter) ft.getCellFormatter();
    cellFormatter.setHeight(3, 1, "300px");
    cellFormatter.setColSpan(3, 1, 2);
  }

}
