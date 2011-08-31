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
import com.google.gwt.dom.builder.shared.TableSectionBuilder;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableRowElement;

import java.util.Collection;

/**
 * Builder used to construct a CellTable.
 * 
 * <p>
 * The default implementation used by cell widths is
 * {@link DefaultCellTableBuilder}.
 * </p>
 * 
 * @param <T> the row data type
 */
public interface CellTableBuilder<T> {

  /**
   * Build zero or more table rows for the specified row value.
   * 
   * @param rowValue the value for the row to render
   * @param absRowIndex the absolute row index
   */
  void buildRow(T rowValue, int absRowIndex);

  /**
   * Finish the building of rows and return the table section builder. Currently
   * only {@link com.google.gwt.dom.builder.shared.HtmlTableSectionBuilder} and
   * its subclasses are supported.
   */
  TableSectionBuilder finish();

  /**
   * Return the column containing an element.
   * 
   * @param context the context for the element
   * @param rowValue the value for the row corresponding to the element
   * @param elem the elm that the column contains
   * @return the immediate column containing the element
   */
  HasCell<T, ?> getColumn(Context context, T rowValue, Element elem);

  /**
   * Return all the columns that this table builder has rendered.
   */
  Collection<HasCell<T, ?>> getColumns();

  /**
   * Get the index of the primary row from the associated
   * {@link TableRowElement} (an TR element).
   * 
   * @param row the row element
   * @return the row value index
   */
  int getRowValueIndex(TableRowElement row);

  /**
   * Get the index of the subrow value from the associated
   * {@link TableRowElement} (an TR element). The sub row value starts at 0 for
   * the first row that represents a row value.
   * 
   * @param row the row element
   * @return the subrow value index, or 0 if not found
   */
  int getSubrowValueIndex(TableRowElement row);

  /**
   * Return if an element contains a cell. This may be faster to execute than
   * {@link #getColumn(Context, Object, Element)}.
   * 
   * @param elem the element of interest
   */
  boolean isColumn(Element elem);

  /**
   * Start building rows. User may want to reset the internal state of the table
   * builder (e.g., reset the internal table section builder). A flag
   * isRebuildingAllRows is used to mark whether the builder is going to rebuild
   * all rows. User may want to have different reset logic given this flag.
   * 
   * @param isRebuildingAllRows is this start intended for rebuilding all rows
   */
  void start(boolean isRebuildingAllRows);
}
