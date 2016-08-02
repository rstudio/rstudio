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
import com.google.gwt.dom.builder.shared.TableCellBuilder;
import com.google.gwt.dom.builder.shared.TableRowBuilder;
import com.google.gwt.user.cellview.client.AbstractCellTable.Style;
import com.google.gwt.user.cellview.client.ColumnSortList.ColumnSortInfo;

/**
 * Default implementation of {@link HeaderBuilder} that renders columns.
 * 
 * @param <T> the data type of the table
 */
public class DefaultHeaderOrFooterBuilder<T> extends AbstractHeaderOrFooterBuilder<T> {

  /**
   * Create a new DefaultHeaderBuilder for the header of footer section.
   * 
   * @param table the table being built
   * @param isFooter true if building the footer, false if the header
   */
  public DefaultHeaderOrFooterBuilder(AbstractCellTable<T> table, boolean isFooter) {
    super(table, isFooter);
  }

  @Override
  protected boolean buildHeaderOrFooterImpl() {
    AbstractCellTable<T> table = getTable();
    boolean isFooter = isBuildingFooter();

    // Early exit if there aren't any columns to render.
    int columnCount = table.getColumnCount();
    if (columnCount == 0) {
      // Nothing to render;
      return false;
    }

    // Early exit if there aren't any headers in the columns to render.
    boolean hasHeader = false;
    for (int i = 0; i < columnCount; i++) {
      if (getHeader(i) != null) {
        hasHeader = true;
        break;
      }
    }
    if (hasHeader == false) {
      return false;
    }

    // Get information about the sorted column.
    ColumnSortList sortList = table.getColumnSortList();
    ColumnSortInfo sortedInfo = (sortList.size() == 0) ? null : sortList.get(0);
    Column<?, ?> sortedColumn = (sortedInfo == null) ? null : sortedInfo.getColumn();
    boolean isSortAscending = (sortedInfo == null) ? false : sortedInfo.isAscending();

    // Get the common style names.
    Style style = getTable().getResources().style();
    String className = isBuildingFooter() ? style.footer() : style.header();
    String sortableStyle = " " + style.sortableHeader();
    String sortedStyle =
        " " + (isSortAscending ? style.sortedHeaderAscending() : style.sortedHeaderDescending());

    // Setup the first column.
    Header<?> prevHeader = getHeader(0);
    Column<T, ?> column = getTable().getColumn(0);
    int prevColspan = 1;
    boolean isSortable = false;
    boolean isSorted = false;
    StringBuilder classesBuilder = new StringBuilder(className);
    classesBuilder.append(" " + (isFooter ? style.firstColumnFooter() : style.firstColumnHeader()));
    if (!isFooter && column.isSortable()) {
      isSortable = true;
      isSorted = (column == sortedColumn);
    }

    // Loop through all column headers.
    TableRowBuilder tr = startRow();
    int curColumn;
    for (curColumn = 1; curColumn < columnCount; curColumn++) {
      Header<?> header = getHeader(curColumn);

      if (header != prevHeader) {
        // The header has changed, so append the previous one.
        buildTableHeader(tr, column, prevHeader, isSortable, isSorted, isSortAscending,
            classesBuilder, sortableStyle, sortedStyle, prevColspan, curColumn);

        // Reset the previous header.
        prevHeader = header;
        prevColspan = 1;
        classesBuilder = new StringBuilder(className);
        isSortable = false;
        isSorted = false;
      } else {
        // Increment the colspan if the headers == each other.
        prevColspan++;
      }

      // Update the sorted state.
      column = table.getColumn(curColumn);
      if (!isFooter && column.isSortable()) {
        isSortable = true;
        isSorted = (column == sortedColumn);
      }
    }

    // The first and last columns could be the same column.
    classesBuilder.append(" ").append(
        isFooter ? style.lastColumnFooter() : style.lastColumnHeader());

    // Render the last header.
    buildTableHeader(tr, column, prevHeader, isSortable, isSorted, isSortAscending,
        classesBuilder, sortableStyle, sortedStyle, prevColspan, curColumn);

    // End the row.
    tr.endTR();

    return true;
  }

  /**
   * Build the table header for the column.
   */
  private void buildTableHeader(TableRowBuilder tr, Column<T, ?> column, Header<?> header,
      boolean isSortable, boolean isSorted, boolean isSortAscending, StringBuilder classesBuilder,
      String sortableStyle, String sortedStyle, int prevColspan, int curColumn) {

    if (isSortable) {
      classesBuilder.append(sortableStyle);
    }
    if (isSorted) {
      classesBuilder.append(sortedStyle);
    }
    appendExtraStyles(header, classesBuilder);

    TableCellBuilder th = tr.startTH().colSpan(prevColspan).className(classesBuilder.toString());
    enableColumnHandlers(th, column);
    if (header != null) {
      // Build the header.
      Context context = new Context(0, curColumn - prevColspan, header.getKey());
      if (isSortable) {
        // TODO: Figure out aria-label and translation of label text
        th.attribute("role", "button");
        th.tabIndex(-1);
      }
      renderSortableHeader(th, context, header, isSorted, isSortAscending);
    }
    th.endTH();
  }
  
  /**
   * Append the extra style names for the header.
   * @param header the header that may contain extra styles, it can be null
   * @param classesBuilder the string builder for the TD classes
   */  
  private <H> void appendExtraStyles(Header<H> header, StringBuilder classesBuilder) {
    if (header == null) {
      return;
    }
    String headerStyleNames = header.getHeaderStyleNames();
    if (headerStyleNames != null) {
      classesBuilder.append(" ");
      classesBuilder.append(headerStyleNames);
    }
  }
}
