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
 * A flexible table that creates cells on demand. It can be jagged (that is,
 * each row can contain a different number of cells) and individual cells can be
 * set to span multiple rows or columns.
 * <p>
 * <img class='gallery' src='doc-files/Table.png'/>
 * </p>
 * <p>
 * <h3>Example</h3>
 * {@example com.google.gwt.examples.FlexTableExample}
 * </p>
 */
public class FlexTable extends HTMLTable {

  /**
   * FlexTable-specific implementation of {@link HTMLTable.CellFormatter}. The
   * formatter retrieved from {@link HTMLTable#getCellFormatter()} may be cast
   * to this class.
   */
  public class FlexCellFormatter extends CellFormatter {

    /**
     * Gets the column span for the given cell. This is the number of logical
     * columns covered by the cell.
     * 
     * @param row the cell's row
     * @param column the cell's column
     * @return the cell's column span
     * @throws IndexOutOfBoundsException
     */
    public int getColSpan(int row, int column) {
      return DOM.getElementPropertyInt(getElement(row, column), "colSpan");
    }

    /**
     * Gets the row span for the given cell. This is the number of logical rows
     * covered by the cell.
     * 
     * @param row the cell's row
     * @param column the cell's column
     * @return the cell's row span
     * @throws IndexOutOfBoundsException
     */
    public int getRowSpan(int row, int column) {
      return DOM.getElementPropertyInt(getElement(row, column), "rowSpan");
    }

    /**
     * Sets the column span for the given cell. This is the number of logical
     * columns covered by the cell.
     * 
     * @param row the cell's row
     * @param column the cell's column
     * @param colSpan the cell's column span
     * @throws IndexOutOfBoundsException
     */
    public void setColSpan(int row, int column, int colSpan) {
      DOM.setElementPropertyInt(ensureElement(row, column), "colSpan", colSpan);
    }

    /**
     * Sets the row span for the given cell. This is the number of logical rows
     * covered by the cell.
     * 
     * @param row the cell's row
     * @param column the cell's column
     * @param rowSpan the cell's row span
     * @throws IndexOutOfBoundsException
     */
    public void setRowSpan(int row, int column, int rowSpan) {
      DOM.setElementPropertyInt(ensureElement(row, column), "rowSpan", rowSpan);
    }
  }

  private static native void addCells(Element table, int row, int num)/*-{
    var rowElem = table.rows[row];
    for(var i = 0; i < num; i++){
      var cell = $doc.createElement("td");
      rowElem.appendChild(cell);  
    }
  }-*/;

  public FlexTable() {
    super();
    setCellFormatter(new FlexCellFormatter());
    setRowFormatter(new RowFormatter());
    setColumnFormatter(new ColumnFormatter());
  }

  /**
   * Appends a cell to the specified row.
   * 
   * @param row the row to which the new cell will be added
   * @throws IndexOutOfBoundsException
   */
  public void addCell(int row) {
    insertCell(row, getCellCount(row));
  }

  /**
   * Gets the number of cells on a given row.
   * 
   * @param row the row whose cells are to be counted
   * @return the number of cells present
   * @throws IndexOutOfBoundsException
   */
  @Override
  public int getCellCount(int row) {
    checkRowBounds(row);
    return getDOMCellCount(getBodyElement(), row);
  }

  /**
   * Explicitly gets the {@link FlexCellFormatter}. The results of
   * {@link HTMLTable#getCellFormatter()} may also be downcast to a
   * {@link FlexCellFormatter}.
   * 
   * @return the FlexTable's cell formatter
   */
  public FlexCellFormatter getFlexCellFormatter() {
    return (FlexCellFormatter) getCellFormatter();
  }

  /**
   * Gets the number of rows.
   * 
   * @return number of rows
   */
  @Override
  public int getRowCount() {
    return getDOMRowCount();
  }

  /**
   * Inserts a cell into the FlexTable.
   * 
   * @param beforeRow the cell's row
   * @param beforeColumn the cell's column
   */
  @Override
  public void insertCell(int beforeRow, int beforeColumn) {
    super.insertCell(beforeRow, beforeColumn);
  }

  /**
   * Inserts a row into the FlexTable.
   * 
   * @param beforeRow the row to insert
   */
  @Override
  public int insertRow(int beforeRow) {
    return super.insertRow(beforeRow);
  }

  /**
   * Remove all rows in this table.
   */
  public void removeAllRows() {
    int numRows = getRowCount();
    for (int i = 0; i < numRows; i++) {
      removeRow(0);
    }
  }

  @Override
  public void removeCell(int row, int col) {
    super.removeCell(row, col);
  }

  /**
   * Removes a number of cells from a row in the table.
   * 
   * @param row the row of the cells to be removed
   * @param column the column of the first cell to be removed
   * @param num the number of cells to be removed
   * @throws IndexOutOfBoundsException
   */
  public void removeCells(int row, int column, int num) {
    for (int i = 0; i < num; i++) {
      removeCell(row, column);
    }
  }

  @Override
  public void removeRow(int row) {
    super.removeRow(row);
  }

  /**
   * Ensure that the cell exists.
   * 
   * @param row the row to prepare.
   * @param column the column to prepare.
   * @throws IndexOutOfBoundsException if the row is negative
   */
  @Override
  protected void prepareCell(int row, int column) {
    prepareRow(row);
    if (column < 0) {
      throw new IndexOutOfBoundsException(
          "Cannot create a column with a negative index: " + column);
    }

    // Ensure that the requested column exists.
    int cellCount = getCellCount(row);
    int required = column + 1 - cellCount;
    if (required > 0) {
      addCells(getBodyElement(), row, required);
    }
  }

  /**
   * Ensure that the row exists.
   * 
   * @param row The row to prepare.
   * @throws IndexOutOfBoundsException if the row is negative
   */
  @Override
  protected void prepareRow(int row) {
    if (row < 0) {
      throw new IndexOutOfBoundsException(
          "Cannot create a row with a negative index: " + row);
    }

    // Ensure that the requested row exists.
    int rowCount = getRowCount();
    for (int i = rowCount; i <= row; i++) {
      insertRow(i);
    }
  }
}
