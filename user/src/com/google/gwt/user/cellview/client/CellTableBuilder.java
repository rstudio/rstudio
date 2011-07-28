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
import com.google.gwt.dom.builder.shared.TableRowBuilder;

/**
 * Builder used to construct a CellTable.
 * 
 * @param <T> the row data type
 */
public interface CellTableBuilder<T> {

  /**
   * Utility to help build a table.
   * 
   * @param <T> the row data type
   */
  abstract static class Utility<T> {

    /**
     * Only instantiable by CellTable implementation.
     */
    Utility() {
    }

    /**
     * Create a {@link Context} object for the specific column index that can be
     * passed to a Cell.
     * 
     * @param column the column index of the context
     * @return a {@link Context} object
     */
    public abstract Context createContext(int column);

    /**
     * Render a Cell into the specified {@link ElementBuilderBase}. Use this
     * method to ensure that the Cell Widget properly handles events originating
     * in the Cell.
     * 
     * <p>
     * The {@link ElementBuilderBase} must be in a state where attributes and
     * html can be appended. If the builder already contains a child element,
     * this method will fail.
     * </p>
     * 
     * @param <C> the data type of the cell
     * @param builder the {@link ElementBuilderBase} to render into
     * @param context the {@link Context} of the cell
     * @param column the column or {@link HasCell} to render
     * @param rowValue the row value to render
     * @see #createContext(int)
     */
    public abstract <C> void renderCell(ElementBuilderBase<?> builder, Context context,
        HasCell<T, C> column, T rowValue);

    /**
     * Add a row to the table.
     * 
     * @return the row to add
     */
    public abstract TableRowBuilder startRow();
  }

  /**
   * Build zero or more table rows for the specified row value using the
   * {@link Utility}.
   * 
   * @param rowValue the value for the row to render
   * @param absRowIndex the absolute row index
   * @param utility the utility used to build the table
   */
  void buildRow(T rowValue, int absRowIndex, Utility<T> utility);
}
