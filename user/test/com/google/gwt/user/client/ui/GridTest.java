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

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.HTMLTable.ColumnFormatter;

/**
 * Tests for {@link Grid}.
 */
public class GridTest extends HTMLTableTestBase {

  @Override
  public HTMLTable getTable(int row, int column) {
    return new Grid(row, column);
  }

  public void testBounds() {
    HTMLTable t = getTable(3, 3);
    try {
      t.setText(-1, 0, "hello");
      fail("IndexOutOfBoundsException should have been thrown");
    } catch (IndexOutOfBoundsException e) {
      assertEquals("Cannot access a row with a negative index: -1",
          e.getMessage());
    }
    try {
      t.setText(0, -1, "hello");
      fail("IndexOutOfBoundsException should have been thrown");
    } catch (IndexOutOfBoundsException e) {
      assertEquals("Cannot access a column with a negative index: -1",
          e.getMessage());
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
      // Success.
    }
  }

  public void testColumnFormatterIndexTooHigh() {
    HTMLTable table = getTable(4, 4);
    ColumnFormatter formatter = table.getColumnFormatter();
    try {
      formatter.getElement(4);
      fail("Expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException e) {
      // Expected.
    }
  }

  public void testColumnFormatterStyleName() {
    Grid r = new Grid(4, 5);
    Grid.ColumnFormatter columns = r.getColumnFormatter();
    columns.setStyleName(0, "base");
    columns.addStyleName(0, "a");
    assertEquals("base a", columns.getStyleName(0));
    columns.addStyleName(0, "b");
    assertEquals("base a b", columns.getStyleName(0));
    columns.addStyleName(0, "c");
    assertEquals("base a b c", columns.getStyleName(0));
    // Remove first.
    columns.removeStyleName(0, "a");
    assertEquals("base b c", columns.getStyleName(0));

    // Remove last.
    columns.removeStyleName(0, "c");
    assertEquals("base b", columns.getStyleName(0));

    // All five cols should be created.
    Element e = DOM.getChild(r.getElement(), 0);
    assertEquals(5, DOM.getChildCount(e));

    columns.addStyleName(3, "a");
    // There should still be five columns.
    e = DOM.getChild(r.getElement(), 0);
    assertEquals(5, DOM.getChildCount(e));

    // Querying column 0 should not invalidate column 3.
    assertEquals("base b", columns.getStyleName(0));
    assertEquals("a", columns.getStyleName(3));
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

  /**
   * Verify that rows can be inserted.
   */
  public void testInsertion() {
    Grid r = new Grid(4, 3);
    assertEquals(4, r.getRowCount());
    assertEquals(3, r.getColumnCount());

    int index = r.insertRow(2);
    assertEquals(index, 2);
    assertEquals(5, r.getRowCount());
    assertEquals(3, r.getColumnCount());
    assertEquals(5, r.getDOMRowCount());
    assertEquals(3, r.getDOMCellCount(2));
  }

  /**
   * Ensures row and column counts stay in sync during resizing.
   */
  public void testResizing() {
    {
      // Resize using resize/resizeRows/resizeColumns
      Grid r = new Grid(4, 1);
      assertEquals(4, r.getRowCount());
      assertEquals(1, r.getColumnCount());
      r.resizeRows(0);
      assertEquals(0, r.getRowCount());
      r.resizeColumns(0);
      assertEquals(0, r.getColumnCount());
      r.resize(3, 2);
      assertEquals(3, r.getRowCount());
      assertEquals(2, r.getColumnCount());
    }

    {
      // Resize using removeRow
      Grid r = new Grid(4, 1);
      assertEquals(4, r.getRowCount());
      assertEquals(1, r.getColumnCount());
      r.removeRow(2);
      assertEquals(3, r.getRowCount());
      assertEquals(1, r.getColumnCount());
      assertEquals(3, r.getDOMRowCount());
    }
  }

  public void testResizeColumnGroup() {
    Grid grid = new Grid(2, 2);
    Element colGroup = grid.getColumnFormatter().columnGroup;
    assertEquals(2, grid.getColumnCount());
    assertEquals(2, colGroup.getChildCount());

    grid.resizeColumns(5);
    assertEquals(5, grid.getColumnCount());
    assertEquals(5, colGroup.getChildCount());

    grid.resizeColumns(1);
    assertEquals(1, grid.getColumnCount());
    assertEquals(1, colGroup.getChildCount());
  }
}
