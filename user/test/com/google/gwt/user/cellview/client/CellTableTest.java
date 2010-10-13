/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.user.cellview.client;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.dom.client.TableSectionElement;
import com.google.gwt.user.cellview.client.CellTable.Resources;
import com.google.gwt.user.cellview.client.CellTable.Style;

/**
 * Tests for {@link CellTable}.
 */
public class CellTableTest extends AbstractHasDataTestBase {

  /**
   * Test headers that span multiple columns.
   */
  public void testMultiColumnHeader() {
    Resources res = GWT.create(Resources.class);
    CellTable<String> table = new CellTable<String>(10, res);
    TextHeader header = new TextHeader("Hello");

    // Get the style information.
    Style style = res.cellTableStyle();
    String styleHeader = style.cellTableHeader();
    String styleFirstColumn = style.cellTableFirstColumnHeader();
    String styleLastColumn = style.cellTableLastColumnHeader();

    // No header.
    table.redraw();
    assertEquals(0, getHeaderCount(table));

    // Single column.
    table.addColumn(new TextColumn<String>() {
      @Override
      public String getValue(String object) {
        return null;
      }
    }, header);
    table.redraw();
    assertEquals(1, getHeaderCount(table));
    assertEquals(1, getHeaderElement(table, 0).getColSpan());
    assertTrue(getHeaderElement(table, 0).getClassName().contains(styleHeader));
    assertTrue(getHeaderElement(table, 0).getClassName().contains(
        styleFirstColumn));
    assertTrue(getHeaderElement(table, 0).getClassName().contains(
        styleLastColumn));

    // Header spans both columns.
    table.addColumn(new TextColumn<String>() {
      @Override
      public String getValue(String object) {
        return null;
      }
    }, header);
    table.redraw();
    assertEquals(1, getHeaderCount(table));
    assertEquals(2, getHeaderElement(table, 0).getColSpan());
    assertTrue(getHeaderElement(table, 0).getClassName().contains(styleHeader));
    assertTrue(getHeaderElement(table, 0).getClassName().contains(
        styleFirstColumn));
    assertTrue(getHeaderElement(table, 0).getClassName().contains(
        styleLastColumn));

    // Header spans all three columns.
    table.addColumn(new TextColumn<String>() {
      @Override
      public String getValue(String object) {
        return null;
      }
    }, header);
    table.redraw();
    assertEquals(1, getHeaderCount(table));
    assertEquals(3, getHeaderElement(table, 0).getColSpan());
    assertTrue(getHeaderElement(table, 0).getClassName().contains(styleHeader));
    assertTrue(getHeaderElement(table, 0).getClassName().contains(
        styleFirstColumn));
    assertTrue(getHeaderElement(table, 0).getClassName().contains(
        styleLastColumn));

    // New header at fourth column.
    table.addColumn(new TextColumn<String>() {
      @Override
      public String getValue(String object) {
        return null;
      }
    }, "New Header");
    table.redraw();
    assertEquals(2, getHeaderCount(table));
    assertEquals(3, getHeaderElement(table, 0).getColSpan());
    assertTrue(getHeaderElement(table, 0).getClassName().contains(styleHeader));
    assertTrue(getHeaderElement(table, 0).getClassName().contains(
        styleFirstColumn));
    assertEquals(1, getHeaderElement(table, 1).getColSpan());
    assertTrue(getHeaderElement(table, 1).getClassName().contains(styleHeader));
    assertTrue(getHeaderElement(table, 1).getClassName().contains(
        styleLastColumn));

    // Two separate spans of same header: HHHXHH.
    table.addColumn(new TextColumn<String>() {
      @Override
      public String getValue(String object) {
        return null;
      }
    }, header);
    table.addColumn(new TextColumn<String>() {
      @Override
      public String getValue(String object) {
        return null;
      }
    }, header);
    table.redraw();
    assertEquals(3, getHeaderCount(table));
    assertEquals(3, getHeaderElement(table, 0).getColSpan());
    assertTrue(getHeaderElement(table, 0).getClassName().contains(styleHeader));
    assertTrue(getHeaderElement(table, 0).getClassName().contains(
        styleFirstColumn));
    assertEquals(1, getHeaderElement(table, 1).getColSpan());
    assertTrue(getHeaderElement(table, 1).getClassName().contains(styleHeader));
    assertEquals(2, getHeaderElement(table, 2).getColSpan());
    assertTrue(getHeaderElement(table, 2).getClassName().contains(styleHeader));
    assertTrue(getHeaderElement(table, 2).getClassName().contains(
        styleLastColumn));
  }

  @Override
  protected CellTable<String> createAbstractHasData() {
    CellTable<String> table = new CellTable<String>();
    table.addColumn(new Column<String, String>(new TextCell()) {
      @Override
      public String getValue(String object) {
        return object;
      }
    });
    table.addColumn(new Column<String, String>(new TextCell()) {
      @Override
      public String getValue(String object) {
        return object + "-2";
      }
    });
    return table;
  }

  /**
   * Get the number of column headers in the table.
   * 
   * @param table the {@link CellTable}
   * @return the number of column headers
   */
  private int getHeaderCount(CellTable<?> table) {
    TableElement tableElem = table.getElement().cast();
    TableSectionElement thead = tableElem.getTHead();
    TableRowElement tr = thead.getRows().getItem(0);
    return tr.getCells().getLength();
  }

  /**
   * Get a column header from the table.
   * 
   * @param table the {@link CellTable}
   * @param column the column index
   * @return the column header
   */
  private TableCellElement getHeaderElement(CellTable<?> table, int column) {
    TableElement tableElem = table.getElement().cast();
    TableSectionElement thead = tableElem.getTHead();
    TableRowElement tr = thead.getRows().getItem(0);
    return tr.getCells().getItem(column);
  }
}
