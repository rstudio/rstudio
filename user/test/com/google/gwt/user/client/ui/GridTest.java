/*
 * Copyright 2007 Google Inc.
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

/**
 * TODO: document me.
 */
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

  public void testColumnFormatter() {
    Grid r = new Grid(4, 5);
    Grid.ColumnFormatter columns = r.getColumnFormatter();
    columns.setStyleName(0, "base");
    columns.addStyleName(0, "a");
    assertEquals("base a", columns.getStyleName(0));
    columns.addStyleName(0, "b");
    assertEquals("base a b", getNormalizedStyleName(columns, 0));
    columns.addStyleName(0, "c");
    assertEquals("base a b c", getNormalizedStyleName(columns, 0));
    // Remove first.
    columns.removeStyleName(0, "a");
    assertEquals("base b c", getNormalizedStyleName(columns, 0));

    // Remove last.
    columns.removeStyleName(0, "c");
    assertEquals("base b", getNormalizedStyleName(columns, 0));

    // Only one column should be created.
    Element e = DOM.getChild(r.getElement(), 0);
    assertEquals(1, DOM.getChildCount(e));

    columns.addStyleName(3, "a");
    // Now there shoud be three such columns .
    e = DOM.getChild(r.getElement(), 0);
    assertEquals(4, DOM.getChildCount(e));
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
   * Ensures row and column counts stay in sync during resizing.
   */
  public void testResizing() {
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

  private String getNormalizedStyleName(Grid.ColumnFormatter formatter,
      int index) {
    return formatter.getStyleName(index).replaceAll("  ", " ").trim();
  }
}
