/*
 * Copyright 2006 Google Inc.
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
 * A rectangular grid that can contain text, html, or a child
 * {@link com.google.gwt.user.client.ui.Widget} within its cells. It must be
 * resized explicitly to the desired number of rows and columns.
 * <p>
 * <img class='gallery' src='Table.png'/>
 * </p>
 * <p>
 * <h3>Example</h3>
 * {@example com.google.gwt.examples.GridExample}
 * </p>
 */
public class Grid extends HTMLTable {

  /**
   * Native method to add rows into a table with a given number of columns.
   * 
   * @param table the table element
   * @param rows number of rows to add
   * @param columns the number of columns per row
   */
  private static native void addRows(Element table, int rows, int columns) /*-{
     var td = $doc.createElement("td");
     td.innerHTML = "&nbsp;";
     var row = $doc.createElement("tr");
     for(var cellNum = 0; cellNum < columns; cellNum++) {
       var cell = td.cloneNode(true);
       row.appendChild(cell);
     }
     table.appendChild(row);
     for(var rowNum = 1; rowNum < rows; rowNum++) {  
       table.appendChild(row.cloneNode(true));
     }
   }-*/;

  /**
   * Number of columns in the current grid.
   */
  protected int numColumns;

  /**
   * Number of rows in the current grid.
   */
  protected int numRows;

  /**
   * Constructor for <code>Grid</code>.
   */
  public Grid() {
    super();
    setCellFormatter(new CellFormatter());
    setRowFormatter(new RowFormatter());
    setColumnFormatter(new ColumnFormatter());
  }

  /**
   * Constructs a grid with the requested size.
   * 
   * @param rows the number of rows
   * @param columns the number of columns
   * @throws IndexOutOfBoundsException
   */
  public Grid(int rows, int columns) {
    this();
    resize(rows, columns);
  }

  /**
   * Replaces the contents of the specified cell with a single space.
   * 
   * @param row the cell's row
   * @param column the cell's column
   * @throws IndexOutOfBoundsException
   */
  public boolean clearCell(int row, int column) {
    Element td = getCellFormatter().getElement(row, column);
    boolean b = super.internalClearCell(td, false);
    DOM.setInnerHTML(td, "&nbsp;");
    return b;
  }

  /**
   * Return number of columns. For grid, row argument is ignored as all grids
   * are rectangular.
   */
  public int getCellCount(int row) {
    return numColumns;
  }

  /**
   * Gets the number of columns in this grid.
   * 
   * @return the number of columns
   */
  public int getColumnCount() {
    return numColumns;
  }

  /**
   * Return number of rows.
   */
  public int getRowCount() {
    return numRows;
  }

  /**
   * Resizes the grid.
   * 
   * @param rows the number of rows
   * @param columns the number of columns
   * @throws IndexOutOfBoundsException
   */
  public void resize(int rows, int columns) {
    resizeColumns(columns);
    resizeRows(rows);
  }

  /**
   * Resizes the grid to the specified number of columns.
   * 
   * @param columns the number of columns
   * @throws IndexOutOfBoundsException
   */
  public void resizeColumns(int columns) {
    if (numColumns == columns) {
      return;
    }
    if (columns <= 0) {
      throw new IndexOutOfBoundsException("Cannot set number of columns to "
          + columns);
    }

    if (numColumns > columns) {
      // Fewer columns. Remove extraneous cells.
      for (int i = 0; i < numRows; i++) {
        for (int j = numColumns - 1; j >= columns; j--) {
          removeCell(i, j);
        }
      }
    } else {
      // More columns. add cells where necessary.
      for (int i = 0; i < numRows; i++) {
        for (int j = numColumns; j < columns; j++) {
          insertCell(i, j);
        }
      }
    }
    numColumns = columns;
  }

  /**
   * Resizes the grid to the specified number of rows.
   * 
   * @param rows the number of rows
   * @throws IndexOutOfBoundsException
   */
  public void resizeRows(int rows) {
    if (numRows == rows) {
      return;
    }
    if (rows <= 0) {
      throw new IndexOutOfBoundsException("Cannot set number of rows to "
          + rows);
    }
    if (numRows < rows) {
      addRows(getBodyElement(), rows - numRows, numColumns);
      numRows = rows;
    } else {
      while (numRows > rows) {
        // Fewer rows. Remove extraneous ones.
        removeRow(--numRows);
      }
    }
  }

  /**
   * Creates a new, empty cell.
   */
  protected Element createCell() {
    Element td = super.createCell();

    // Add a non-breaking space to the TD. This ensures that the cell is
    // displayed.
    DOM.setInnerHTML(td, "&nbsp;");
    return td;
  }

  /**
   * Checks that a cell is a valid cell in the table.
   * 
   * @param row the cell's row
   * @param column the cell's column
   * @throws IndexOutOfBoundsException
   */
  protected void prepareCell(int row, int column) {
    // Ensure that the indices are not negative.
    prepareRow(row);
    if (column < 0) {
      throw new IndexOutOfBoundsException(
          "Cannot access a column with a negative index: " + column);
    }

    if (column >= numColumns) {
      throw new IndexOutOfBoundsException("Column index: " + column
          + ", Column size: " + numColumns);
    }
  }

  /**
   * Checks that the column index is valid.
   * 
   * @param column The column index to be checked
   * @throws IndexOutOfBoundsException if the column is negative
   */
  protected void prepareColumn(int column) {
    // Ensure that the indices are not negative.
    if (column < 0) {
      throw new IndexOutOfBoundsException(
          "Cannot access a column with a negative index: " + column);
    }

    /**
     * Grid does not lazily create cells, so simply ensure that the requested
     * column and column are valid
     */
    if (column >= numColumns) {
      throw new IndexOutOfBoundsException("Column index: " + column
          + ", Column size: " + numColumns);
    }
  }

  /**
   * Checks that the row index is valid.
   * 
   * @param row The row index to be checked
   * @throws IndexOutOfBoundsException if the row is negative
   */
  protected void prepareRow(int row) {
    // Ensure that the indices are not negative.
    if (row < 0) {
      throw new IndexOutOfBoundsException(
          "Cannot access a row with a negative index: " + row);
    }

    /**
     * Grid does not lazily create cells, so simply ensure that the requested
     * row and column are valid
     */
    if (row >= numRows) {
      throw new IndexOutOfBoundsException("Row index: " + row + ", Row size: "
          + numRows);
    }
  }
}
