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
import com.google.gwt.dom.builder.shared.HtmlBuilderFactory;
import com.google.gwt.dom.builder.shared.HtmlTableSectionBuilder;
import com.google.gwt.dom.builder.shared.StylesBuilder;
import com.google.gwt.dom.builder.shared.TableRowBuilder;
import com.google.gwt.dom.builder.shared.TableSectionBuilder;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.AbstractImagePrototype;

import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of {@link HeaderBuilder} that renders columns.
 * 
 * @param <T> the data type of the table
 */
public abstract class AbstractHeaderOrFooterBuilder<T> implements HeaderBuilder<T>,
    FooterBuilder<T> {

  /**
   * A map that provides O(1) access to a value given the key, or to the key
   * given the value.
   */
  private static class TwoWayHashMap<K, V> {
    private final Map<K, V> keyToValue = new HashMap<K, V>();
    private final Map<V, K> valueToKey = new HashMap<V, K>();

    void clear() {
      keyToValue.clear();
      valueToKey.clear();
    }

    K getKey(V value) {
      return valueToKey.get(value);
    }

    V getValue(K key) {
      return keyToValue.get(key);
    }

    void put(K key, V value) {
      keyToValue.put(key, value);
      valueToKey.put(value, key);
    }
  }

  /**
   * The attribute used to indicate that an element contains a Column.
   */
  private static final String COLUMN_ATTRIBUTE = "__gwt_column";

  /**
   * The attribute used to indicate that an element contains a header.
   */
  private static final String HEADER_ATTRIBUTE = "__gwt_header";
  
  /**
   * The attribute used to specify the row index of a TR element in the header.
   */
  private static final String ROW_ATTRIBUTE = "__gwt_header_row";

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
  private int rowIndex;

  // The following fields are reset on every build.
  private HtmlTableSectionBuilder section;
  private final Map<String, Column<T, ?>> idToColumnMap = new HashMap<String, Column<T, ?>>();
  private final TwoWayHashMap<String, Header<?>> idToHeaderMap =
      new TwoWayHashMap<String, Header<?>>();

  /**
   * Create a new DefaultHeaderBuilder for the header of footer section.
   * 
   * @param table the table being built
   * @param isFooter true if building the footer, false if the header
   */
  public AbstractHeaderOrFooterBuilder(AbstractCellTable<T> table, boolean isFooter) {
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
  public final TableSectionBuilder buildFooter() {
    if (!isFooter) {
      throw new UnsupportedOperationException(
          "Cannot build footer because this builder is designated to build a header");
    }
    return buildHeaderOrFooter();
  }

  @Override
  public final TableSectionBuilder buildHeader() {
    if (isFooter) {
      throw new UnsupportedOperationException(
          "Cannot build header because this builder is designated to build a footer");
    }
    return buildHeaderOrFooter();
  }

  @Override
  public Column<T, ?> getColumn(Element elem) {
    String cellId = getColumnId(elem);
    return (cellId == null) ? null : idToColumnMap.get(cellId);
  }

  @Override
  public Header<?> getHeader(Element elem) {
    String headerId = getHeaderId(elem);
    return (headerId == null) ? null : idToHeaderMap.getValue(headerId);
  }

  @Override
  public int getRowIndex(TableRowElement row) {
    return Integer.parseInt(row.getAttribute(ROW_ATTRIBUTE));
  }
  
  /**
   * Check if this builder is building a header or footer table.
   * 
   * @return true if a footer, false if a header
   */
  public boolean isBuildingFooter() {
    return isFooter;
  }

  @Override
  public boolean isColumn(Element elem) {
    return getColumnId(elem) != null;
  }

  @Override
  public boolean isHeader(Element elem) {
    return getHeaderId(elem) != null;
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
   * Implementation that builds the header or footer using the convenience
   * methods in this class.
   * 
   * @return true if the header contains content, false if empty
   */
  protected abstract boolean buildHeaderOrFooterImpl();

  /**
   * Enables column-specific event handling for the specified element. If a
   * column is sortable, then clicking on the element or a child of the element
   * will trigger a sort event.
   * 
   * @param builder the builder to associate with the column. The builder should
   *          be a child element of a row returned by {@link #startRow} and must
   *          be in a state where an attribute can be added.
   * @param column the column to associate
   */
  protected final void enableColumnHandlers(ElementBuilderBase<?> builder, Column<T, ?> column) {
    String columnId = "column-" + Document.get().createUniqueId();
    idToColumnMap.put(columnId, column);
    builder.attribute(COLUMN_ATTRIBUTE, columnId);
  }

  /**
   * Get the header or footer at the specified index.
   * 
   * @param index the column index of the header
   * @return the header or footer, depending on the value of isFooter
   */
  protected final Header<?> getHeader(int index) {
    return isFooter ? getTable().getFooter(index) : getTable().getHeader(index);
  }

  protected AbstractCellTable<T> getTable() {
    return table;
  }

  /**
   * Renders a given Header into a given ElementBuilderBase. This method ensures
   * that the CellTable widget will handle events events originating in the
   * Header.
   * 
   * @param <H> the data type of the header
   * @param out the {@link ElementBuilderBase} to render into. The builder
   *          should be a child element of a row returned by {@link #startRow}
   *          and must be in a state that allows both attributes and elements to
   *          be added
   * @param context the {@link Context} of the header being rendered
   * @param header the {@link Header} to render
   */
  protected final <H> void renderHeader(ElementBuilderBase<?> out, Context context, Header<H> header) {
    // Generate a unique ID for the header.
    String headerId = idToHeaderMap.getKey(header);
    if (headerId == null) {
      headerId = "header-" + Document.get().createUniqueId();
      idToHeaderMap.put(headerId, header);
    }
    out.attribute(HEADER_ATTRIBUTE, headerId);

    // Render the cell into the builder.
    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    header.render(context, sb);
    out.html(sb.toSafeHtml());
  }

  /**
   * Render a header, including a sort icon if the column is sortable and
   * sorted.
   * 
   * @param out the builder to render into
   * @param header the header to render
   * @param context the context of the header
   * @param isSorted true if the column is sorted
   * @param isSortAscending indicated the sort order, if sorted
   */
  protected final void renderSortableHeader(ElementBuilderBase<?> out, Context context,
      Header<?> header, boolean isSorted, boolean isSortAscending) {
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
    renderHeader(headerContainer, context, header);

    // Close the elements used for the sort icon.
    if (isSorted) {
      headerContainer.endDiv(); // headerContainer.
      headerContainer.endDiv(); // outerDiv
    }
  }

  /**
   * Add a header (or footer) row to the table, below any rows previously added.
   * 
   * @return the row to add
   */
  protected final TableRowBuilder startRow() {
    // End any dangling rows.
    while (section.getDepth() > 1) {
      section.end();
    }

    // Verify the depth.
    if (section.getDepth() < 1) {
      throw new IllegalStateException(
          "Cannot start a row.  Did you call TableRowBuilder.end() too many times?");
    }

    // Start the next row.
    TableRowBuilder row = section.startTR();
    row.attribute(ROW_ATTRIBUTE, rowIndex);
    rowIndex++;
    return row;
  }

  private TableSectionBuilder buildHeaderOrFooter() {
    // Reset the state of the header.
    section =
        isFooter ? HtmlBuilderFactory.get().createTFootBuilder() : HtmlBuilderFactory.get()
            .createTHeadBuilder();
    idToHeaderMap.clear();
    idToColumnMap.clear();
    rowIndex = 0;

    // Build the header.
    if (!buildHeaderOrFooterImpl()) {
      // The header is empty.
      return null;
    }

    // End dangling elements.
    while (section.getDepth() > 0) {
      section.end();
    }

    // Return the section.
    return section;
  }

  /**
   * Check if an element is the parent of a rendered header.
   * 
   * @param elem the element to check
   * @return the id if a header parent, null if not
   */
  private String getColumnId(Element elem) {
    return getElementAttribute(elem, COLUMN_ATTRIBUTE);
  }

  private String getElementAttribute(Element elem, String attribute) {
    if (elem == null) {
      return null;
    }
    String value = elem.getAttribute(attribute);
    return (value == null) || (value.length() == 0) ? null : value;
  }

  /**
   * Check if an element is the parent of a rendered header.
   * 
   * @param elem the element to check
   * @return the id if a header parent, null if not
   */
  private String getHeaderId(Element elem) {
    return getElementAttribute(elem, HEADER_ATTRIBUTE);
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
