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
package com.google.gwt.dom.client;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests the {@link TableElement}, {@link TableCaptionElement},
 * {@link TableCellElement}, {@link TableRowElement}, and
 * {@link TableSectionElement} classes.
 */
public class TableTests extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.dom.DOMTest";
  }

  /**
   * insertRow, getRows, rowIndex.
   */
  public void testInsertRow() {
    Document doc = Document.get();
    TableElement table = doc.createTableElement();
    doc.getBody().appendChild(table);

    TableRowElement row1 = table.insertRow(-1);
    TableRowElement row2 = table.insertRow(-1);
    TableRowElement row3 = table.insertRow(-1);

    assertEquals(row1, table.getRows().getItem(0));
    assertEquals(row2, table.getRows().getItem(1));
    assertEquals(row3, table.getRows().getItem(2));

    TableRowElement row0 = table.insertRow(0);
    assertEquals(row0, table.getRows().getItem(0));
    assertEquals(row1, table.getRows().getItem(1));
    assertEquals(row2, table.getRows().getItem(2));
    assertEquals(row3, table.getRows().getItem(3));

    assertEquals(0, row0.getRowIndex());
    assertEquals(1, row1.getRowIndex());
    assertEquals(2, row2.getRowIndex());
    assertEquals(3, row3.getRowIndex());
  }

  /**
   * insertCell, getCells, cellIndex.
   */
  public void testInsertCell() {
    Document doc = Document.get();
    TableElement table = doc.createTableElement();
    doc.getBody().appendChild(table);

    TableRowElement row = table.insertRow(0);

    TableCellElement cell1 = row.insertCell(-1);
    TableCellElement cell2 = row.insertCell(-1);
    TableCellElement cell3 = row.insertCell(-1);

    assertEquals(cell1, row.getCells().getItem(0));
    assertEquals(cell2, row.getCells().getItem(1));
    assertEquals(cell3, row.getCells().getItem(2));

    TableCellElement cell0 = row.insertCell(0);
    assertEquals(cell0, row.getCells().getItem(0));
    assertEquals(cell1, row.getCells().getItem(1));
    assertEquals(cell2, row.getCells().getItem(2));
    assertEquals(cell3, row.getCells().getItem(3));

// TODO: TableCellElement.cellIndex is broken (always 0) on Safari 2 (bug 3295)
//    assertEquals(0, cell0.getCellIndex());
//    assertEquals(1, cell1.getCellIndex());
//    assertEquals(2, cell2.getCellIndex());
//    assertEquals(3, cell3.getCellIndex());
  }

  /**
   * createTHead, thead, tfoot, createTFoot, tBodies, section.insertRow,
   * section.rows, deleteTHead, deleteTFoot.
   */
  public void testSections() {
    TableElement table = Document.get().createTableElement();

    // Put some rows in the body.
    TableRowElement row0 = table.insertRow(-1);
    TableRowElement row1 = table.insertRow(-1);
    TableRowElement row2 = table.insertRow(-1);

    // Add header and footer rows.
    TableSectionElement thead = table.createTHead();
    TableSectionElement tfoot = table.createTFoot();
    TableRowElement headRow = thead.insertRow(-1);
    TableRowElement footRow = tfoot.insertRow(-1);

    // Add a cell to each row (this is technically required).
    row0.insertCell(-1);
    row1.insertCell(-1);
    row2.insertCell(-1);
    headRow.insertCell(-1);
    footRow.insertCell(-1);

    // Check tbodies, thead, tfoot.
    TableSectionElement tbody = table.getTBodies().getItem(0);
    assertEquals("one tbody expected", 1, table.getTBodies().getLength());
    assertEquals("one tbody expected", tbody, table.getTBodies().getItem(0));
    assertEquals("thead should exist", thead, table.getTHead());
    assertEquals("tfoot should exist", tfoot, table.getTFoot());

    assertEquals("<thead> expected", "thead", thead.getTagName().toLowerCase());
    assertEquals("<tbody> expected", "tbody", tbody.getTagName().toLowerCase());
    assertEquals("<tfoot> expected", "tfoot", tfoot.getTagName().toLowerCase());

    // Ensure table row enumeration contains all rows (order of section rows is
    // not guaranteed across browsers).
    NodeList<TableRowElement> allRows = table.getRows();
    
    assertEquals("5 rows expected", 5, allRows.getLength());
    assertContains("[0] == headRow", headRow, allRows);
    assertContains("[1] == footRow", footRow, allRows);
    assertContains("[2] == row0", row0, allRows);
    assertContains("[3] == row1", row1, allRows);
    assertContains("[4] == row2", row2, allRows);

    // Ensure tbody section row enumeration is correct.
    NodeList<TableRowElement> bodyRows = tbody.getRows();
    assertEquals("[0] == row0", row0, bodyRows.getItem(0));
    assertEquals("[1] == row1", row1, bodyRows.getItem(1));
    assertEquals("[2] == row2", row2, bodyRows.getItem(2));

    // Remove the header and footer.
    table.deleteTHead();
    table.deleteTFoot();
    assertNull("no thead expected", table.getTHead());
    assertNull("no tfoot expected", table.getTFoot());
  }

  private void assertContains(String msg, Node n, NodeList<?> list) {
    for (int i = 0; i < list.getLength(); ++i) {
      if (list.getItem(i) == n) {
        return;
      }
    }

    fail(msg);
  }
}
