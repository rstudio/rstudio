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
import com.google.gwt.dom.builder.shared.DivBuilder;
import com.google.gwt.dom.builder.shared.ElementBuilderBase;
import com.google.gwt.dom.builder.shared.StylesBuilder;
import com.google.gwt.dom.builder.shared.TableCellBuilder;
import com.google.gwt.dom.builder.shared.TableRowBuilder;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.AbstractCellTable.Style;
import com.google.gwt.user.cellview.client.ColumnSortList.ColumnSortInfo;
import com.google.gwt.user.client.ui.AbstractImagePrototype;

/**
 * Default implementation of {@link HeaderCreator} that renders columns.
 * 
 * <p>
 * WARNING: This API is experimental and may change without warning.
 * </p>
 * 
 * @param <T> the data type of the table
 */
public class DefaultHeaderCreator<T> implements HeaderCreator<T> {

  private static final int ICON_PADDING = 6;

  private final boolean isFooter;
  private boolean isSortIconStartOfLine = true;
  private final int sortAscIconHalfHeight;
  private SafeHtml sortAscIconHtml;
  private final int sortAscIconWidth;
  private final int sortDescIconHalfHeight;
  private SafeHtml sortDescIconHtml;
  private final int sortDescIconWidth;
  private final AbstractCellTable<T> table;

  /**
   * Create a new DefaultHeaderBuilder for the header of footer section.
   * 
   * @param table the table being built
   * @param isFooter true if building the footer, false if the header
   */
  public DefaultHeaderCreator(AbstractCellTable<T> table, boolean isFooter) {
    this.isFooter = isFooter;
    this.table = table;

    /*
     * Cache the height and width of the sort icons. We do not cache the
     * rendered image source so the compiler can optimize it out if the user
     * overrides renderHeader and does not use the sort icon.
     */
    ImageResource asc = table.getResources().sortAscending();
    ImageResource desc = table.getResources().sortDescending();
    if (asc != null) {
      sortAscIconWidth = asc.getWidth() + ICON_PADDING;
      sortAscIconHalfHeight = (int) Math.round(asc.getHeight() / 2.0);
    } else {
      sortAscIconWidth = 0;
      sortAscIconHalfHeight = 0;
    }
    if (desc != null) {
      sortDescIconWidth = desc.getWidth() + ICON_PADDING;
      sortDescIconHalfHeight = (int) Math.round(desc.getHeight() / 2.0);
    } else {
      sortDescIconWidth = 0;
      sortDescIconHalfHeight = 0;
    }
  }

  @Override
  public void buildHeader(Helper<T> utility) {
    // Early exit if there aren't any columns to render.
    int columnCount = table.getColumnCount();
    if (columnCount == 0) {
      // Nothing to render;
      return;
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
      return;
    }

    // Get information about the sorted column.
    ColumnSortList sortList = table.getColumnSortList();
    ColumnSortInfo sortedInfo = (sortList.size() == 0) ? null : sortList.get(0);
    Column<?, ?> sortedColumn = (sortedInfo == null) ? null : sortedInfo.getColumn();
    boolean isSortAscending = (sortedInfo == null) ? false : sortedInfo.isAscending();

    // Get the common style names.
    Style style = table.getResources().style();
    String className = isFooter ? style.footer() : style.header();
    String sortableStyle = " " + style.sortableHeader();
    String sortedStyle =
        " " + (isSortAscending ? style.sortedHeaderAscending() : style.sortedHeaderDescending());

    // Setup the first column.
    Header<?> prevHeader = getHeader(0);
    Column<T, ?> column = table.getColumn(0);
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
    TableRowBuilder tr = utility.startRow();
    int curColumn;
    for (curColumn = 1; curColumn < columnCount; curColumn++) {
      Header<?> header = getHeader(curColumn);

      if (header != prevHeader) {
        // The header has changed, so append the previous one.
        if (isSortable) {
          classesBuilder.append(sortableStyle);
        }
        if (isSorted) {
          classesBuilder.append(sortedStyle);
        }

        // Render the header.
        TableCellBuilder th =
            tr.startTH().colSpan(prevColspan).className(classesBuilder.toString());
        utility.enableColumnHandlers(th, column);
        if (prevHeader != null) {
          // Build the header.
          Context context = new Context(0, curColumn - prevColspan, prevHeader.getKey());
          renderHeader(th, context, prevHeader, utility, isSorted, isSortAscending);
        }
        th.endTH();

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

    // Append the last header.
    if (isSortable) {
      classesBuilder.append(sortableStyle);
    }
    if (isSorted) {
      classesBuilder.append(sortedStyle);
    }

    // The first and last columns could be the same column.
    classesBuilder.append(" ").append(
        isFooter ? style.lastColumnFooter() : style.lastColumnHeader());

    // Render the last header.
    TableCellBuilder th = tr.startTH().colSpan(prevColspan).className(classesBuilder.toString());
    utility.enableColumnHandlers(th, column);
    if (prevHeader != null) {
      Context context = new Context(0, curColumn - prevColspan, prevHeader.getKey());
      renderHeader(th, context, prevHeader, utility, isSorted, isSortAscending);
    }
    th.endTH();

    // End the row.
    tr.endTR();
  }

  /**
   * Check if the icon is located at the start or end of the line. The start of
   * the line refers to the left side in LTR mode and the right side in RTL
   * mode. The default location is the start of the line.
   */
  public boolean isSortIconStartOfLine() {
    return isSortIconStartOfLine;
  }

  /**
   * Set the position of the sort icon to the start or end of the line. The
   * start of the line refers to the left side in LTR mode and the right side in
   * RTL mode. The default location is the start of the line.
   */
  public void setSortIconStartOfLine(boolean isStartOfLine) {
    this.isSortIconStartOfLine = isStartOfLine;
  }

  /**
   * Get the header or footer at the specified index.
   * 
   * @param index the column index of the header
   * @return the header or footer, depending on the value of isFooter
   */
  protected Header<?> getHeader(int index) {
    return isFooter ? getTable().getFooter(index) : getTable().getHeader(index);
  }

  protected AbstractCellTable<?> getTable() {
    return table;
  }

  /**
   * Render a header.
   * 
   * @param out the builder to render into
   * @param header the header to render
   * @param context the context of the header
   * @param utility the utility used to render the header
   * @param isSorted true if the column is sorted
   * @param isSortAscending indicated the sort order, if sorted
   */
  protected void renderHeader(ElementBuilderBase<?> out, Context context, Header<?> header,
      Helper<T> utility, boolean isSorted, boolean isSortAscending) {
    ElementBuilderBase<?> headerContainer = out;

    // Wrap the header in a sort icon if sorted.
    isSorted = isSorted && !isFooter;
    if (isSorted) {
      // Determine the position of the sort icon.
      boolean posRight =
          LocaleInfo.getCurrentLocale().isRTL() ? isSortIconStartOfLine : !isSortIconStartOfLine;

      // Create an outer container to hold the icon and the header.
      int iconWidth = isSortAscending ? sortAscIconWidth : sortDescIconWidth;
      int halfHeight = isSortAscending ? sortAscIconHalfHeight : sortDescIconHalfHeight;
      DivBuilder outerDiv = out.startDiv();
      StylesBuilder style =
          outerDiv.style().position(Position.RELATIVE).trustedProperty("zoom", "1");
      if (posRight) {
        style.paddingRight(iconWidth, Unit.PX);
      } else {
        style.paddingLeft(iconWidth, Unit.PX);
      }
      style.endStyle();

      // Add the icon.
      DivBuilder imageHolder = outerDiv.startDiv();
      style =
          outerDiv.style().position(Position.ABSOLUTE).top(50.0, Unit.PCT).lineHeight(0.0, Unit.PX)
              .marginTop(-halfHeight, Unit.PX);
      if (posRight) {
        style.right(0, Unit.PX);
      } else {
        style.left(0, Unit.PX);
      }

      style.endStyle();
      imageHolder.html(getSortIcon(isSortAscending));
      imageHolder.endDiv();

      // Create the header wrapper.
      headerContainer = outerDiv.startDiv();
    }

    // Build the header.
    utility.renderHeader(headerContainer, context, header);

    // Close the elements used for the sort icon.
    if (isSorted) {
      headerContainer.endDiv(); // headerContainer.
      headerContainer.endDiv(); // outerDiv
    }
  }

  /**
   * Get the HTML representation of the sort icon. These are loaded lazily so
   * the compiler has a chance to strip this method, and the icon source code,
   * if the user overrides renderHeader.
   * 
   * @param isAscending true for the ascending icon, false for descending
   * @return the rendered HTML
   */
  private SafeHtml getSortIcon(boolean isAscending) {
    if (isAscending) {
      if (sortAscIconHtml == null) {
        AbstractImagePrototype proto =
            AbstractImagePrototype.create(table.getResources().sortAscending());
        sortAscIconHtml = SafeHtmlUtils.fromTrustedString(proto.getHTML());
      }
      return sortAscIconHtml;
    } else {
      if (sortDescIconHtml == null) {
        AbstractImagePrototype proto =
            AbstractImagePrototype.create(table.getResources().sortDescending());
        sortDescIconHtml = SafeHtmlUtils.fromTrustedString(proto.getHTML());
      }
      return sortDescIconHtml;
    }
  }
}
