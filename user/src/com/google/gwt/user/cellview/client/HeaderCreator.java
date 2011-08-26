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
import com.google.gwt.dom.builder.shared.ElementBuilderBase;
import com.google.gwt.dom.builder.shared.TableRowBuilder;

/**
 * Creates the DOM elements for the header or footer section of a CellTable.
 * 
 * <p>
 * WARNING: This API is experimental and may change without warning.
 * </p>
 * 
 * <p>
 * The default implementation used by cell widgets is
 * {@link DefaultHeaderCreator}.
 * </p>
 * 
 * @param <T> the row data type
 */
// TODO(jlabanca): Refactor to mirror CellTableBuilder API.
// TODO(jlabanca): Add API to handle events in the header.
public interface HeaderCreator<T> {

  /**
   * Contains methods that {@link #buildHeader} can call while building a header
   * or footer.
   * 
   * <p>
   * The cell table being rendered will define the Helper implementation and use
   * it to ensure that events from the rendered table are handled correctly.
   * </p>
   * 
   * @param <T> the row data type
   */
  // TODO(jlabanca): Refactor Helper out of existence.
  public abstract class Helper<T> {

    private final AbstractCellTable<T> cellTable;

    /**
     * Only instantiable by CellTable implementation.
     */
    /*
     * The helper class is defined by the CellTable implementation to register
     * headers and columns with the table. The constructor is package protected
     * so we can add methods in the future without breaking the API. Only
     * serious power users who are creating their own AbstractCellTable
     * implementation that uses some other method to register headers should
     * ever need to implement Helper.
     */
    Helper(AbstractCellTable<T> cellTable) {
      this.cellTable = cellTable;
    }

    /**
     * Enables column-specific event handling for the specified element. If a
     * column is sortable, then clicking on the element or a child of the
     * element will trigger a sort event.
     * 
     * @param builder the builder to associate with the column. The builder
     *          should be a child element of a row returned by {@link #startRow}
     *          and must be in a state where an attribute can be added.
     * @param column the column to associate
     */
    public abstract void enableColumnHandlers(ElementBuilderBase<?> builder, Column<T, ?> column);

    /**
     * Get the cell table that is being built.
     */
    public AbstractCellTable<T> getTable() {
      return cellTable;
    }

    /**
     * Renders a given Header into a given ElementBuilderBase. This method
     * ensures that the CellTable widget will handle events events originating
     * in the Header.
     * 
     * @param <H> the data type of the header
     * @param out the {@link ElementBuilderBase} to render into. The builder
     *          should be a child element of a row returned by {@link #startRow}
     *          and must be in a state that allows both attributes and elements
     *          to be added
     * @param context the {@link Context} of the header being rendered
     * @param header the {@link Header} to render
     */
    public abstract <H> void renderHeader(ElementBuilderBase<?> out, Context context,
        Header<H> header);

    /**
     * Add a header (or footer) row to the table, below any rows previously
     * added.
     * 
     * @return the row to add
     */
    public abstract TableRowBuilder startRow();
  }

  /**
   * Build the entire header table.
   * 
   * <p>
   * The {@link Helper} should be used to add rows to the table, associate
   * elements with a {@link Column}, and render {@link Header}s into elements.
   * </p>
   * 
   * @param helper a {@link Helper} for adding headers
   */
  void buildHeader(Helper<T> helper);
}
