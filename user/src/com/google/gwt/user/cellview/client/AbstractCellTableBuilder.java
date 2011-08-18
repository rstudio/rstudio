/*
 * Copyright 2011 Google Inc.
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

import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.cell.client.HasCell;
import com.google.gwt.dom.builder.shared.ElementBuilderBase;
import com.google.gwt.dom.builder.shared.HtmlBuilderFactory;
import com.google.gwt.dom.builder.shared.HtmlTableSectionBuilder;
import com.google.gwt.dom.builder.shared.TableRowBuilder;
import com.google.gwt.dom.builder.shared.TableSectionBuilder;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Builder used to construct a CellTable.
 * 
 * @param <T> the row data type
 */
public abstract class AbstractCellTableBuilder<T> implements CellTableBuilder<T> {
  
  /**
   * The attribute used to indicate that an element contains a cell.
   */
  private static final String CELL_ATTRIBUTE = "__gwt_cell";
  
  /**
   * The attribute used to specify the logical row index.
   */
  private static final String ROW_ATTRIBUTE = "__gwt_row";

  /**
   * The attribute used to specify the subrow within a logical row value.
   */
  private static final String SUBROW_ATTRIBUTE = "__gwt_subrow";
  
  protected final AbstractCellTable<T> cellTable;
  
  /**
   * A mapping of unique cell IDs to the cell.
   */
  private final Map<String, HasCell<T, ?>> idToCellMap = new HashMap<String, HasCell<T, ?>>();
  private final Map<HasCell<T, ?>, String> cellToIdMap = new HashMap<HasCell<T, ?>, String>();

  private HtmlTableSectionBuilder tbody;  
  private int rowIndex;
  private int subrowIndex;
  private Object rowValueKey;

  /**
   * Construct a new table builder.
   * 
   * @param cellTable the table this builder will build rows for
   */
  public AbstractCellTableBuilder(AbstractCellTable<T> cellTable) {
    this.cellTable = cellTable;
  }
  
  /**
   * Build zero or more table rows for the specified row value.
   *
   * @param rowValue the value for the row to render
   * @param absRowIndex the absolute row index
   */
  @Override
  public final void buildRow(T rowValue, int absRowIndex) {
    setRowInfo(absRowIndex, rowValue);
    buildRowImpl(rowValue, absRowIndex);
  }
  
  /**
   * Create the context for a column based on the current table building state.
   * 
   * @param column the column id
   * @return the context that contains the column index, row/subrow indexes, and the row value key
   */
  public final Context createContext(int column) {
    return new Context(rowIndex, column, rowValueKey, subrowIndex);
  }
  
  /**
   * Finish the building and get the {@link TableSectionBuilder} containing the children.
   */
  @Override
  public final TableSectionBuilder finish() {
    // End dangling elements.
    while (tbody.getDepth() > 0) {
      tbody.endTBody();
    }
    return tbody;
  }
  
  /**
   * Return the column containing an element.
   *
   * @param context the context for the element
   * @param rowValue the value for the row corresponding to the element
   * @param elem the element that the column contains
   * @return the immediate column containing the element
   */
  @Override
  public final HasCell<T, ?> getColumn(Context context, T rowValue, Element elem) {
    return getColumn(elem);
  }

  /**
   * Return all the columns that this table builder has renderred.
   */
  @Override
  public final Collection<HasCell<T, ?>> getColumns() {
    return idToCellMap.values();
  }
  
  /**
   * Get the index of the row value from the associated {@link TableRowElement}.
   * 
   * @param row the row element
   * @return the row value index
   */
  @Override
  public final int getRowValueIndex(TableRowElement row) {
    try {
      return Integer.parseInt(row.getAttribute(ROW_ATTRIBUTE));
    } catch (NumberFormatException e) {
      // The attribute doesn't exist. Maybe the user is overriding
      // renderRowValues().
      return row.getSectionRowIndex() + cellTable.getPageStart();
    }
  }
  
  /**
   * Get the index of the subrow value from the associated
   * {@link TableRowElement}. The sub row value starts at 0 for the first row
   * that represents a row value.
   * 
   * @param row the row element
   * @return the subrow value index, or 0 if not found
   */
  @Override
  public final int getSubrowValueIndex(TableRowElement row) {
    try {
      return Integer.parseInt(row.getAttribute(SUBROW_ATTRIBUTE));
    } catch (NumberFormatException e) {
      // The attribute doesn't exist. Maybe the user is overriding
      // renderRowValues() in {@link AbstractCellTable}.
      return 0;
    }
  }
  
  /**
   * Return if an element contains a cell. This may be faster to execute than {@link getColumn}.
   *
   * @param elem the element of interest
   */
  @Override
  public final boolean isColumn(Element elem) {
    return getCellId(elem) != null;
  }
  
  /**
   * Render the cell into an {@link ElementBuilderBase}.
   * 
   * @param builder the {@link ElementBuilderBase} that cell contents append to
   * @param context the context for the element
   * @param column the column containing the cell
   * @param rowValue the value for the row corresponding to the element
   */
  public final <C> void renderCell(ElementBuilderBase<?> builder, Context context,
      HasCell<T, C> column, T rowValue) {
    // Generate a unique ID for the cell.
    String cellId = cellToIdMap.get(column);
    if (cellId == null) {
      cellId = "cell-" + Document.get().createUniqueId();
      idToCellMap.put(cellId, column);
      cellToIdMap.put(column, cellId);
    }
    builder.attribute(CELL_ATTRIBUTE, cellId);

    // Render the cell into the builder.
    SafeHtmlBuilder cellBuilder = new SafeHtmlBuilder();
    if (column instanceof Column) {
      /*
       * If the HasCell is a Column, let it render the Cell itself. This is
       * here for legacy support.
       */
      Column<T, C> theColumn = (Column<T, C>) column;
      theColumn.render(context, rowValue, cellBuilder);
    } else {
      column.getCell().render(context, column.getValue(rowValue), cellBuilder);
    }
    builder.html(cellBuilder.toSafeHtml());
  }
  
  /**
   * 
   */
  /**
   * Start building rows. Reset the internal table section builder. If the table builder is going
   * to re-build all rows, the internal the maps associating the cells and ids will be cleared.
   *
   * @param isRebuildingAllRows is this start intended for rebuilding all rows
   */
  @Override
  public final void start(boolean isRebuildingAllRows) {
    /*
     * TODO(jlabanca): Test with DomBuilder.
     * 
     * DOM manipulation is sometimes faster than String concatenation and
     * innerHTML, but not when mixing the two. Cells render as HTML strings,
     * so its faster to render the entire table as a string.
     */
    tbody = HtmlBuilderFactory.get().createTBodyBuilder();
    if (isRebuildingAllRows) {
      cellToIdMap.clear();
      idToCellMap.clear();
    }
  }

  /**
   * Start a row and return the {@link TableRowBuilder} for this row.
   */
  public final TableRowBuilder startRow() {
    // End any dangling rows.
    while (tbody.getDepth() > 1) {
      tbody.end();
    }

    // Verify the depth.
    if (tbody.getDepth() < 1) {
      throw new IllegalStateException(
          "Cannot start a row.  Did you call TableRowBuilder.end() too many times?");
    }

    // Start the next row.
    TableRowBuilder row = tbody.startTR();
    row.attribute(ROW_ATTRIBUTE, rowIndex);
    row.attribute(SUBROW_ATTRIBUTE, subrowIndex);
    subrowIndex++;
    return row;
  }
    
  /**
   * Build zero or more table rows for the specified row value.
   * 
   * @param rowValue the value for the row to render
   * @param absRowIndex the absolute row index
   */
  protected abstract void buildRowImpl(T rowValue, int absRowIndex);
  
  /**
   * Check if an element is the parent of a rendered cell.
   * 
   * @param elem the element to check
   * @return the cellId if a cell parent, null if not
   */
  private String getCellId(Element elem) {
    if (elem == null) {
      return null;
    }
    String cellId = elem.getAttribute(CELL_ATTRIBUTE);
    return (cellId == null) || (cellId.length() == 0) ? null : cellId;
  }

  /**
   * Return the column containing an element.
   *
   * @param elem the elm that the column contains
   * @return the column containing the element.
   */
  private HasCell<T, ?> getColumn(Element elem) {
    String cellId = getCellId(elem);
    return (cellId == null) ? null : idToCellMap.get(cellId);
  }

  /**
   * Set the information for the current row to build.
   *
   * @param rowIndex the index of the row
   * @param rowValue the value of this row
   */
  private void setRowInfo(int rowIndex, T rowValue) {
    this.rowIndex = rowIndex;
    this.rowValueKey = cellTable.getValueKey(rowValue);
    this.subrowIndex = 0; // Reset the subrow.
  }
}
