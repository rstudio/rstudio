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
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;

/**
 * TODO: document me.
 */
public class FlexTableTest extends HTMLTableTestBase {

  public HTMLTable getTable(int row, int column) {
    return new FlexTable();
  }

  public void testWidgetPos() {
    FlexTable t = new FlexTable();
    HTML widget_3_0 = new HTML("3,0");
    HTML widget_3_1 = new HTML("3,1");
    HTML widget_1_2 = new HTML("1,2");

    t.setWidget(0, 0, widget_3_1);
    t.insertRow(0);
    t.insertCell(1, 0);
    t.setWidget(1, 0, widget_3_0);
    t.insertRow(0);
    t.setWidget(0, 0, widget_1_2);
    t.insertCells(0, 0, 2);
    t.insertRow(0);
    int hit = 0;
    for (int row = 0; row < 4; row++) {
      int colBounds = t.getCellCount(row);
      for (int col = 0; col < colBounds; col++) {
        Widget widget = t.getWidget(row, col);
        if (row == 3 && col == 0) {
          ++hit;
          assertEquals(widget_3_0, widget);
        } else if (row == 3 && col == 1) {
          ++hit;
          assertEquals(widget_3_1, widget);
        } else if (row == 1 && col == 2) {
          ++hit;
          assertEquals(widget_1_2, widget);
        } else {
          if (widget != null) {
            System.err.println("row: " + row + ", col: " + col + ", widget: "
                + DOM.toString(widget.getElement()));
          }
          assertNull(widget);
        }
      }
    }
    assertEquals(3, hit);

    // Move widget.
    t.setWidget(3, 2, widget_1_2);
    assertEquals(widget_1_2, t.getWidget(3, 2));
    assertNull(t.getWidget(1, 2));

    // Remove by widget.
    t.remove(widget_3_0);
    assertNull(t.getWidget(3, 0));
    assertEquals(widget_3_1, t.getWidget(3, 1));

    // Remove by cell.
    t.removeCell(3, 1);
    assertEquals(widget_1_2, t.getWidget(3, 1));
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
      assertEquals("Cannot create a row with a negative index: -1",
          e.getMessage());
    }
    try {
      t.setText(0, -1, "hello");
      fail("IndexOutOfBoundsException should have been thrown");
    } catch (IndexOutOfBoundsException e) {
      assertEquals("Cannot create a column with a negative index: -1",
          e.getMessage());
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

  public void testRemoveAllRows() {
    FlexTable table = new FlexTable();
    for (int row = 0; row < 4; row++) {
      table.setHTML(row, 0, row + ":0");
      table.setHTML(row, 1, row + ":1");
      table.setWidget(row, 2, new Button(row + ":2"));
      table.setWidget(row, 3, new Button(row + ":3"));
    }

    assertEquals(4, table.getRowCount());
    table.removeAllRows();
    assertEquals(0, table.getRowCount());
  }

  public void secondarySetHeightTest() {
    FlexTable ft = new FlexTable();
    FlexCellFormatter cellFormatter = (FlexCellFormatter) ft.getCellFormatter();
    cellFormatter.setHeight(3, 1, "300px");
    cellFormatter.setColSpan(3, 1, 2);
  }

}
