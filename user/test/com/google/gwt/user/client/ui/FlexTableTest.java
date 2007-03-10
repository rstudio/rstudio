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

import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;

/**
 * TODO: document me.
 */
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

  public void secondarySetHeightTest() {
    FlexTable ft = new FlexTable();
    FlexCellFormatter cellFormatter = (FlexCellFormatter) ft.getCellFormatter();
    cellFormatter.setHeight(3, 1, "300px");
    cellFormatter.setColSpan(3, 1, 2);
  }

}
