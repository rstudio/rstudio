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

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.cell.client.IconCellDecorator;
import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.dom.client.TableSectionElement;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.ColumnSortList.ColumnSortInfo;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.HasHorizontalAlignment.HorizontalAlignmentConstant;
import com.google.gwt.user.client.ui.HasVerticalAlignment.VerticalAlignmentConstant;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Abstract base class for tabular views that supports paging and columns.
 * 
 * <p>
 * <h3>Columns</h3> The {@link Column} class defines the {@link Cell} used to
 * render a column. Implement {@link Column#getValue(Object)} to retrieve the
 * field value from the row object that will be rendered in the {@link Cell}.
 * </p>
 * 
 * <p>
 * <h3>Headers and Footers</h3> A {@link Header} can be placed at the top
 * (header) or bottom (footer) of the {@link AbstractCellTable}. You can specify
 * a header as text using {@link #addColumn(Column, String)}, or you can create
 * a custom {@link Header} that can change with the value of the cells, such as
 * a column total. The {@link Header} will be rendered every time the row data
 * changes or the table is redrawn. If you pass the same header instance (==)
 * into adjacent columns, the header will span the columns.
 * </p>
 * 
 * @param <T> the data type of each row
 */
public abstract class AbstractCellTable<T> extends AbstractHasData<T> {

  /**
   * A ClientBundle that provides images for this widget.
   */
  public interface Resources {
    /**
     * Icon used when a column is sorted in ascending order.
     */
    ImageResource sortAscending();

    /**
     * Icon used when a column is sorted in descending order.
     */
    ImageResource sortDescending();

    /**
     * The styles used in this widget.
     */
    Style style();
  }

  /**
   * Styles used by this widget.
   */
  public interface Style {
    /**
     * Applied to every cell.
     */
    String cell();

    /**
     * Applied to even rows.
     */
    String evenRow();

    /**
     * Applied to cells in even rows.
     */
    String evenRowCell();

    /**
     * Applied to the first column.
     */
    String firstColumn();

    /**
     * Applied to the first column footers.
     */
    String firstColumnFooter();

    /**
     * Applied to the first column headers.
     */
    String firstColumnHeader();

    /**
     * Applied to footers cells.
     */
    String footer();

    /**
     * Applied to headers cells.
     */
    String header();

    /**
     * Applied to the hovered row.
     */
    String hoveredRow();

    /**
     * Applied to the cells in the hovered row.
     */
    String hoveredRowCell();

    /**
     * Applied to the keyboard selected cell.
     */
    String keyboardSelectedCell();

    /**
     * Applied to the keyboard selected row.
     */
    String keyboardSelectedRow();

    /**
     * Applied to the cells in the keyboard selected row.
     */
    String keyboardSelectedRowCell();

    /**
     * Applied to the last column.
     */
    String lastColumn();

    /**
     * Applied to the last column footers.
     */
    String lastColumnFooter();

    /**
     * Applied to the last column headers.
     */
    String lastColumnHeader();

    /**
     * Applied to odd rows.
     */
    String oddRow();

    /**
     * Applied to cells in odd rows.
     */
    String oddRowCell();

    /**
     * Applied to selected rows.
     */
    String selectedRow();

    /**
     * Applied to cells in selected rows.
     */
    String selectedRowCell();

    /**
     * Applied to header cells that are sortable.
     */
    String sortableHeader();

    /**
     * Applied to header cells that are sorted in ascending order.
     */
    String sortedHeaderAscending();

    /**
     * Applied to header cells that are sorted in descending order.
     */
    String sortedHeaderDescending();

    /**
     * Applied to the table.
     */
    String widget();
  }

  interface Template extends SafeHtmlTemplates {
    @SafeHtmlTemplates.Template("<div style=\"outline:none;\">{0}</div>")
    SafeHtml div(SafeHtml contents);

    @SafeHtmlTemplates.Template("<div style=\"outline:none;\" tabindex=\"{0}\">{1}</div>")
    SafeHtml divFocusable(int tabIndex, SafeHtml contents);

    @SafeHtmlTemplates.Template("<div style=\"outline:none;\" tabindex=\"{0}\" accessKey=\"{1}\">{2}</div>")
    SafeHtml divFocusableWithKey(int tabIndex, char accessKey, SafeHtml contents);

    @SafeHtmlTemplates.Template("<div class=\"{0}\"></div>")
    SafeHtml loading(String loading);

    @SafeHtmlTemplates.Template("<table><tbody>{0}</tbody></table>")
    SafeHtml tbody(SafeHtml rowHtml);

    @SafeHtmlTemplates.Template("<td class=\"{0}\">{1}</td>")
    SafeHtml td(String classes, SafeHtml contents);

    @SafeHtmlTemplates.Template("<td class=\"{0}\" align=\"{1}\" valign=\"{2}\">{3}</td>")
    SafeHtml tdBothAlign(String classes, String hAlign, String vAlign, SafeHtml contents);

    @SafeHtmlTemplates.Template("<td class=\"{0}\" align=\"{1}\">{2}</td>")
    SafeHtml tdHorizontalAlign(String classes, String hAlign, SafeHtml contents);

    @SafeHtmlTemplates.Template("<td class=\"{0}\" valign=\"{1}\">{2}</td>")
    SafeHtml tdVerticalAlign(String classes, String vAlign, SafeHtml contents);

    @SafeHtmlTemplates.Template("<table><tfoot>{0}</tfoot></table>")
    SafeHtml tfoot(SafeHtml rowHtml);

    @SafeHtmlTemplates.Template("<th colspan=\"{0}\" class=\"{1}\">{2}</th>")
    SafeHtml th(int colspan, String classes, SafeHtml contents);

    @SafeHtmlTemplates.Template("<table><thead>{0}</thead></table>")
    SafeHtml thead(SafeHtml rowHtml);

    @SafeHtmlTemplates.Template("<tr onclick=\"\" class=\"{0}\">{1}</tr>")
    SafeHtml tr(String classes, SafeHtml contents);
  }

  /**
   * Implementation of {@link AbstractCellTable}.
   */
  private static class Impl {

    private final com.google.gwt.user.client.Element tmpElem = Document.get().createDivElement()
        .cast();

    /**
     * Convert the rowHtml into Elements wrapped by the specified table section.
     * 
     * @param table the {@link AbstractCellTable}
     * @param sectionTag the table section tag
     * @param rowHtml the Html for the rows
     * @return the section element
     */
    protected TableSectionElement convertToSectionElement(AbstractCellTable<?> table,
        String sectionTag, SafeHtml rowHtml) {
      // Attach an event listener so we can catch synchronous load events from
      // cached images.
      DOM.setEventListener(tmpElem, table);

      /*
       * Render the rows into a table.
       * 
       * IE doesn't support innerHtml on a TableSection or Table element, so we
       * generate the entire table. We do the same for all browsers to avoid any
       * future bugs, since setting innerHTML on a table section seems brittle.
       */
      sectionTag = sectionTag.toLowerCase();
      if ("tbody".equals(sectionTag)) {
        tmpElem.setInnerHTML(template.tbody(rowHtml).asString());
      } else if ("thead".equals(sectionTag)) {
        tmpElem.setInnerHTML(template.thead(rowHtml).asString());
      } else if ("tfoot".equals(sectionTag)) {
        tmpElem.setInnerHTML(template.tfoot(rowHtml).asString());
      } else {
        throw new IllegalArgumentException("Invalid table section tag: " + sectionTag);
      }
      TableElement tableElem = tmpElem.getFirstChildElement().cast();

      // Detach the event listener.
      DOM.setEventListener(tmpElem, null);

      // Get the section out of the table.
      if ("tbody".equals(sectionTag)) {
        return tableElem.getTBodies().getItem(0);
      } else if ("thead".equals(sectionTag)) {
        return tableElem.getTHead();
      } else if ("tfoot".equals(sectionTag)) {
        return tableElem.getTFoot();
      } else {
        throw new IllegalArgumentException("Invalid table section tag: " + sectionTag);
      }
    }

    /**
     * Detach a table section element from its parent.
     * 
     * @param section the element to detach
     */
    protected void detachSectionElement(TableSectionElement section) {
      section.removeFromParent();
    }

    /**
     * Reattach a table section element from its parent.
     * 
     * @param parent the parent element
     * @param section the element to reattach
     * @param nextSection the next section
     */
    protected void reattachSectionElement(Element parent, TableSectionElement section,
        Element nextSection) {
      parent.insertBefore(section, nextSection);
    }

    /**
     * Render a table section in the table.
     * 
     * @param table the {@link AbstractCellTable}
     * @param section the {@link TableSectionElement} to replace
     * @param html the html to render
     */
    protected void replaceAllRows(AbstractCellTable<?> table, TableSectionElement section,
        SafeHtml html) {
      // If the widget is not attached, attach an event listener so we can catch
      // synchronous load events from cached images.
      if (!table.isAttached()) {
        DOM.setEventListener(table.getElement(), table);
      }

      // Remove the section from the tbody.
      Element parent = section.getParentElement();
      Element nextSection = section.getNextSiblingElement();
      detachSectionElement(section);

      // Render the html.
      section.setInnerHTML(html.asString());

      /*
       * Reattach the section. If next section is null, the section will be
       * appended instead.
       */
      reattachSectionElement(parent, section, nextSection);

      // Detach the event listener.
      if (!table.isAttached()) {
        DOM.setEventListener(table.getElement(), null);
      }
    }
  }

  /**
   * Implementation of {@link CellTable} used by Firefox.
   */
  @SuppressWarnings("unused")
  private static class ImplMozilla extends Impl {
    /**
     * Firefox 3.6 and earlier convert td elements to divs if the tbody is
     * removed from the table element.
     */
    @Override
    protected void detachSectionElement(TableSectionElement section) {
      if (isGecko192OrBefore()) {
        return;
      }
      super.detachSectionElement(section);
    }

    @Override
    protected void reattachSectionElement(Element parent, TableSectionElement section,
        Element nextSection) {
      if (isGecko192OrBefore()) {
        return;
      }
      super.reattachSectionElement(parent, section, nextSection);
    }

    /**
     * Return true if using Gecko 1.9.2 (Firefox 3.6) or earlier.
     */
    private native boolean isGecko192OrBefore() /*-{
      return @com.google.gwt.dom.client.DOMImplMozilla::isGecko192OrBefore()();
    }-*/;
  }

  /**
   * Implementation of {@link AbstractCellTable} used by IE.
   */
  @SuppressWarnings("unused")
  private static class ImplTrident extends Impl {

    /**
     * IE doesn't support innerHTML on tbody, nor does it support removing or
     * replacing a tbody. The only solution is to remove and replace the rows
     * themselves.
     */
    @Override
    protected void replaceAllRows(AbstractCellTable<?> table, TableSectionElement section,
        SafeHtml html) {
      // Remove all children.
      Element child = section.getFirstChildElement();
      while (child != null) {
        Element next = child.getNextSiblingElement();
        section.removeChild(child);
        child = next;
      }

      // Add new child elements.
      TableSectionElement newSection = convertToSectionElement(table, section.getTagName(), html);
      child = newSection.getFirstChildElement();
      while (child != null) {
        Element next = child.getNextSiblingElement();
        section.appendChild(child);
        child = next;
      }
    }
  }

  /**
   * The table specific {@link Impl}.
   */
  private static Impl TABLE_IMPL;

  static Template template;

  private boolean cellIsEditing;
  private final List<Column<T, ?>> columns = new ArrayList<Column<T, ?>>();
  private final Map<Column<T, ?>, String> columnWidths = new HashMap<Column<T, ?>, String>();

  /**
   * Indicates that at least one column depends on selection.
   */
  private boolean dependsOnSelection;

  private Widget emptyTableWidget;
  private final List<Header<?>> footers = new ArrayList<Header<?>>();

  /**
   * Indicates that at least one column handles selection.
   */
  private boolean handlesSelection;

  private final List<Header<?>> headers = new ArrayList<Header<?>>();

  private TableRowElement hoveringRow;

  /**
   * Indicates that at least one column is interactive.
   */
  private boolean isInteractive;

  private int keyboardSelectedColumn = 0;
  private Widget loadingIndicator;
  private final Resources resources;
  private RowStyles<T> rowStyles;
  private IconCellDecorator<SafeHtml> sortAscDecorator;
  private IconCellDecorator<SafeHtml> sortDescDecorator;
  private final ColumnSortList sortList = new ColumnSortList(new ColumnSortList.Delegate() {
    @Override
    public void onModification() {
      if (!updatingSortList) {
        createHeaders(false);
      }
    }
  });
  private final Style style;
  private boolean updatingSortList;

  /**
   * Constructs a table with the given page size, the specified {@link Style},
   * and the given key provider.
   * 
   * @param elem the parent {@link Element}
   * @param pageSize the page size
   * @param resources the resources to apply to the widget
   * @param keyProvider an instance of ProvidesKey<T>, or null if the record
   *          object should act as its own key
   */
  public AbstractCellTable(Element elem, final int pageSize, Resources resources,
      ProvidesKey<T> keyProvider) {
    super(elem, pageSize, keyProvider);
    this.resources = resources;
    this.style = resources.style();
    init();
  }

  /**
   * Constructs a table with the given page size, the specified {@link Style},
   * and the given key provider.
   * 
   * @param widget the parent widget
   * @param pageSize the page size
   * @param resources the resources to apply to the widget
   * @param keyProvider an instance of ProvidesKey<T>, or null if the record
   *          object should act as its own key
   */
  public AbstractCellTable(Widget widget, final int pageSize, Resources resources,
      ProvidesKey<T> keyProvider) {
    super(widget, pageSize, keyProvider);
    this.resources = resources;
    this.style = resources.style();
    init();
  }

  /**
   * Adds a column to the end of the table.
   * 
   * @param col the column to be added
   */
  public void addColumn(Column<T, ?> col) {
    insertColumn(getColumnCount(), col);
  }

  /**
   * Adds a column to the end of the table with an associated header.
   * 
   * @param col the column to be added
   * @param header the associated {@link Header}
   */
  public void addColumn(Column<T, ?> col, Header<?> header) {
    insertColumn(getColumnCount(), col, header);
  }

  /**
   * Adds a column to the end of the table with an associated header and footer.
   * 
   * @param col the column to be added
   * @param header the associated {@link Header}
   * @param footer the associated footer (as a {@link Header} object)
   */
  public void addColumn(Column<T, ?> col, Header<?> header, Header<?> footer) {
    insertColumn(getColumnCount(), col, header, footer);
  }

  /**
   * Adds a column to the end of the table with an associated String header.
   * 
   * @param col the column to be added
   * @param headerString the associated header text, as a String
   */
  public void addColumn(Column<T, ?> col, String headerString) {
    insertColumn(getColumnCount(), col, headerString);
  }

  /**
   * Adds a column to the end of the table with an associated {@link SafeHtml}
   * header.
   * 
   * @param col the column to be added
   * @param headerHtml the associated header text, as safe HTML
   */
  public void addColumn(Column<T, ?> col, SafeHtml headerHtml) {
    insertColumn(getColumnCount(), col, headerHtml);
  }

  /**
   * Adds a column to the end of the table with an associated String header and
   * footer.
   * 
   * @param col the column to be added
   * @param headerString the associated header text, as a String
   * @param footerString the associated footer text, as a String
   */
  public void addColumn(Column<T, ?> col, String headerString, String footerString) {
    insertColumn(getColumnCount(), col, headerString, footerString);
  }

  /**
   * Adds a column to the end of the table with an associated {@link SafeHtml}
   * header and footer.
   * 
   * @param col the column to be added
   * @param headerHtml the associated header text, as safe HTML
   * @param footerHtml the associated footer text, as safe HTML
   */
  public void addColumn(Column<T, ?> col, SafeHtml headerHtml, SafeHtml footerHtml) {
    insertColumn(getColumnCount(), col, headerHtml, footerHtml);
  }

  /**
   * Add a handler to handle {@link ColumnSortEvent}s.
   * 
   * @param handler the {@link ColumnSortEvent.Handler} to add
   * @return a {@link HandlerRegistration} to remove the handler
   */
  public HandlerRegistration addColumnSortHandler(ColumnSortEvent.Handler handler) {
    return addHandler(handler, ColumnSortEvent.getType());
  }

  /**
   * Add a style name to the <code>col</code> element at the specified index,
   * creating it if necessary.
   * 
   * @param index the column index
   * @param styleName the style name to add
   */
  public abstract void addColumnStyleName(int index, String styleName);

  /**
   * Clear the width of the specified {@link Column}.
   * 
   * @param column the column
   */
  public void clearColumnWidth(Column<T, ?> column) {
    columnWidths.remove(column);
    refreshColumnWidths();
  }

  /**
   * Flush all pending changes to the table and render immediately.
   * 
   * <p>
   * Modifications to the table, such as adding columns or setting data, are not
   * rendered immediately. Instead, changes are coalesced at the end of the
   * current event loop to avoid rendering the table multiple times. Use this
   * method to force the table to render all pending modifications immediately.
   * </p>
   */
  public void flush() {
    getPresenter().flush();
  }

  /**
   * Get the column at the specified index.
   * 
   * @param col the index of the column to retrieve
   * @return the {@link Column} at the index
   */
  public Column<T, ?> getColumn(int col) {
    checkColumnBounds(col);
    return columns.get(col);
  }

  /**
   * Get the number of columns in the table.
   * 
   * @return the column count
   */
  public int getColumnCount() {
    return columns.size();
  }

  /**
   * Get the index of the specified column.
   * 
   * @param column the column to search for
   * @return the index of the column, or -1 if not found
   */
  public int getColumnIndex(Column<T, ?> column) {
    return columns.indexOf(column);
  }

  /**
   * Get the {@link ColumnSortList} that specifies which columns are sorted.
   * Modifications to the {@link ColumnSortList} will be reflected in the table
   * header.
   * 
   * @return the {@link ColumnSortList}
   */
  public ColumnSortList getColumnSortList() {
    return sortList;
  }

  /**
   * Get the width of a {@link Column}.
   * 
   * @param column the column
   * @return the width of the column, or null if not set
   * @see #setColumnWidth(Column, double, Unit)
   */
  public String getColumnWidth(Column<T, ?> column) {
    return columnWidths.get(column);
  }

  /**
   * Get the widget displayed when the table has no rows.
   * 
   * @return the empty table widget
   */
  public Widget getEmptyTableWidget() {
    return emptyTableWidget;
  }

  /**
   * Get the widget displayed when the data is loading.
   * 
   * @return the loading indicator
   */
  public Widget getLoadingIndicator() {
    return loadingIndicator;
  }

  /**
   * Get the {@link TableRowElement} for the specified row. If the row element
   * has not been created, null is returned.
   * 
   * @param row the row index
   * @return the row element, or null if it doesn't exists
   * @throws IndexOutOfBoundsException if the row index is outside of the
   *           current page
   */
  public TableRowElement getRowElement(int row) {
    flush();
    checkRowBounds(row);
    NodeList<TableRowElement> rows = getTableBodyElement().getRows();
    return rows.getLength() > row ? rows.getItem(row) : null;
  }

  /**
   * Inserts a column into the table at the specified index.
   * 
   * @param beforeIndex the index to insert the column
   * @param col the column to be added
   */
  public void insertColumn(int beforeIndex, Column<T, ?> col) {
    insertColumn(beforeIndex, col, (Header<?>) null, (Header<?>) null);
  }

  /**
   * Inserts a column into the table at the specified index with an associated
   * header.
   * 
   * @param beforeIndex the index to insert the column
   * @param col the column to be added
   * @param header the associated {@link Header}
   */
  public void insertColumn(int beforeIndex, Column<T, ?> col, Header<?> header) {
    insertColumn(beforeIndex, col, header, null);
  }

  /**
   * Inserts a column into the table at the specified index with an associated
   * header and footer.
   * 
   * @param beforeIndex the index to insert the column
   * @param col the column to be added
   * @param header the associated {@link Header}
   * @param footer the associated footer (as a {@link Header} object)
   * @throws IndexOutOfBoundsException if the index is out of range
   */
  public void insertColumn(int beforeIndex, Column<T, ?> col, Header<?> header, Header<?> footer) {
    // Allow insert at the end.
    if (beforeIndex != getColumnCount()) {
      checkColumnBounds(beforeIndex);
    }

    headers.add(beforeIndex, header);
    footers.add(beforeIndex, footer);
    columns.add(beforeIndex, col);
    boolean wasinteractive = isInteractive;
    coalesceCellProperties();

    // Move the keyboard selected column if the current column is not
    // interactive.
    if (!wasinteractive && isInteractive) {
      keyboardSelectedColumn = beforeIndex;
    }

    // Sink events used by the new column.
    Set<String> consumedEvents = new HashSet<String>();
    {
      Set<String> cellEvents = col.getCell().getConsumedEvents();
      if (cellEvents != null) {
        consumedEvents.addAll(cellEvents);
      }
    }
    if (header != null) {
      Set<String> headerEvents = header.getCell().getConsumedEvents();
      if (headerEvents != null) {
        consumedEvents.addAll(headerEvents);
      }
    }
    if (footer != null) {
      Set<String> footerEvents = footer.getCell().getConsumedEvents();
      if (footerEvents != null) {
        consumedEvents.addAll(footerEvents);
      }
    }
    CellBasedWidgetImpl.get().sinkEvents(this, consumedEvents);

    redraw();
  }

  /**
   * Inserts a column into the table at the specified index with an associated
   * String header.
   * 
   * @param beforeIndex the index to insert the column
   * @param col the column to be added
   * @param headerString the associated header text, as a String
   */
  public void insertColumn(int beforeIndex, Column<T, ?> col, String headerString) {
    insertColumn(beforeIndex, col, new TextHeader(headerString), null);
  }

  /**
   * Inserts a column into the table at the specified index with an associated
   * {@link SafeHtml} header.
   * 
   * @param beforeIndex the index to insert the column
   * @param col the column to be added
   * @param headerHtml the associated header text, as safe HTML
   */
  public void insertColumn(int beforeIndex, Column<T, ?> col, SafeHtml headerHtml) {
    insertColumn(beforeIndex, col, new SafeHtmlHeader(headerHtml), null);
  }

  /**
   * Inserts a column into the table at the specified index with an associated
   * String header and footer.
   * 
   * @param beforeIndex the index to insert the column
   * @param col the column to be added
   * @param headerString the associated header text, as a String
   * @param footerString the associated footer text, as a String
   */
  public void insertColumn(int beforeIndex, Column<T, ?> col, String headerString,
      String footerString) {
    insertColumn(beforeIndex, col, new TextHeader(headerString), new TextHeader(footerString));
  }

  /**
   * Inserts a column into the table at the specified index with an associated
   * {@link SafeHtml} header and footer.
   * 
   * @param beforeIndex the index to insert the column
   * @param col the column to be added
   * @param headerHtml the associated header text, as safe HTML
   * @param footerHtml the associated footer text, as safe HTML
   */
  public void insertColumn(int beforeIndex, Column<T, ?> col, SafeHtml headerHtml,
      SafeHtml footerHtml) {
    insertColumn(beforeIndex, col, new SafeHtmlHeader(headerHtml), new SafeHtmlHeader(footerHtml));
  }

  @Override
  public void redraw() {
    refreshColumnWidths();
    super.redraw();
  }

  /**
   * Redraw the table's footers.
   */
  public void redrawFooters() {
    createHeaders(true);
  }

  /**
   * Redraw the table's headers.
   */
  public void redrawHeaders() {
    createHeaders(false);
  }

  /**
   * Remove a column.
   * 
   * @param col the column to remove
   */
  public void removeColumn(Column<T, ?> col) {
    int index = columns.indexOf(col);
    if (index < 0) {
      throw new IllegalArgumentException("The specified column is not part of this table.");
    }
    removeColumn(index);
  }

  /**
   * Remove a column.
   * 
   * @param index the column index
   */
  public void removeColumn(int index) {
    if (index < 0 || index >= columns.size()) {
      throw new IndexOutOfBoundsException("The specified column index is out of bounds.");
    }
    columns.remove(index);
    headers.remove(index);
    footers.remove(index);
    coalesceCellProperties();

    // Find an interactive column. Stick with 0 if no column is interactive.
    if (index <= keyboardSelectedColumn) {
      keyboardSelectedColumn = 0;
      if (isInteractive) {
        for (int i = 0; i < columns.size(); i++) {
          if (isColumnInteractive(columns.get(i))) {
            keyboardSelectedColumn = i;
            break;
          }
        }
      }
    }

    // Redraw the table asynchronously.
    redraw();

    // We don't unsink events because other handlers or user code may have sunk
    // them intentionally.
  }

  /**
   * Remove a style from the <code>col</code> element at the specified index.
   * 
   * @param index the column index
   * @param styleName the style name to remove
   */
  public abstract void removeColumnStyleName(int index, String styleName);

  /**
   * Set the width of a {@link Column}.
   * 
   * @param column the column
   * @param width the width of the column
   */
  public void setColumnWidth(Column<T, ?> column, String width) {
    columnWidths.put(column, width);
    refreshColumnWidths();
  }

  /**
   * Set the width of a {@link Column}.
   * 
   * @param column the column
   * @param width the width of the column
   * @param unit the {@link Unit} of measurement
   */
  public void setColumnWidth(Column<T, ?> column, double width, Unit unit) {
    setColumnWidth(column, width + unit.getType());
  }

  /**
   * Set the widget to display when the table has no rows.
   * 
   * @param widget the empty table widget, or null to disable
   */
  public void setEmptyTableWidget(Widget widget) {
    this.emptyTableWidget = widget;
  }

  /**
   * Set the widget to display when the data is loading.
   * 
   * @param widget the loading indicator, or null to disable
   */
  public void setLoadingIndicator(Widget widget) {
    loadingIndicator = widget;
  }

  /**
   * Sets the object used to determine how a row is styled; the change will take
   * effect the next time that the table is rendered.
   * 
   * @param rowStyles a {@link RowStyles} object
   */
  public void setRowStyles(RowStyles<T> rowStyles) {
    this.rowStyles = rowStyles;
  }

  @Override
  protected Element convertToElements(SafeHtml html) {
    return TABLE_IMPL.convertToSectionElement(AbstractCellTable.this, "tbody", html);
  }

  @Override
  protected boolean dependsOnSelection() {
    return dependsOnSelection;
  }

  /**
   * Called when a user action triggers selection.
   * 
   * @param event the event that triggered selection
   * @param value the value that was selected
   * @param row the row index of the value on the page
   * @param column the column index where the event occurred
   * @deprecated use
   *             {@link #addCellPreviewHandler(com.google.gwt.view.client.CellPreviewEvent.Handler)}
   *             instead
   */
  @Deprecated
  protected void doSelection(Event event, T value, int row, int column) {
  }

  /**
   * Set the width of a column.
   * 
   * @param column the column index
   * @param width the width, or null to clear the width
   */
  protected abstract void doSetColumnWidth(int column, String width);

  /**
   * Show or hide a header section.
   * 
   * @param isFooter true for the footer, false for the header
   * @param isVisible true to show, false to hide
   */
  protected abstract void doSetHeaderVisible(boolean isFooter, boolean isVisible);

  @Override
  protected Element getChildContainer() {
    return getTableBodyElement();
  }

  @Override
  protected Element getKeyboardSelectedElement() {
    // Do not use getRowElement() because that will flush the presenter.
    int rowIndex = getKeyboardSelectedRow();
    NodeList<TableRowElement> rows = getTableBodyElement().getRows();
    if (rowIndex >= 0 && rowIndex < rows.getLength() && columns.size() > 0) {
      TableRowElement tr = rows.getItem(rowIndex);
      TableCellElement td = tr.getCells().getItem(keyboardSelectedColumn);
      return getCellParent(td);
    }
    return null;
  }

  /**
   * Get the tbody element that contains the render row values.
   */
  protected abstract TableSectionElement getTableBodyElement();

  /**
   * Get the tfoot element that contains the footers.
   */
  protected abstract TableSectionElement getTableFootElement();

  /**
   * Get the thead element that contains theh eaders.
   */
  protected abstract TableSectionElement getTableHeadElement();

  @Override
  protected boolean isKeyboardNavigationSuppressed() {
    return cellIsEditing;
  }

  @Override
  protected void onBlur() {
    Element elem = getKeyboardSelectedElement();
    if (elem != null) {
      TableCellElement td = elem.getParentElement().cast();
      TableRowElement tr = td.getParentElement().cast();
      td.removeClassName(style.keyboardSelectedCell());
      setRowStyleName(tr, style.keyboardSelectedRow(), style.keyboardSelectedRowCell(), false);
    }
  }

  @SuppressWarnings("deprecation")
  @Override
  protected void onBrowserEvent2(Event event) {
    // Get the event target.
    EventTarget eventTarget = event.getEventTarget();
    if (!Element.is(eventTarget)) {
      return;
    }
    final Element target = event.getEventTarget().cast();

    // Ignore keydown events unless the cell is in edit mode
    String eventType = event.getType();
    if ("keydown".equals(eventType) && !isKeyboardNavigationSuppressed()
        && KeyboardSelectionPolicy.DISABLED != getKeyboardSelectionPolicy()) {
      if (handleKey(event)) {
        return;
      }
    }

    // Find the cell where the event occurred.
    TableCellElement tableCell = findNearestParentCell(target);
    if (tableCell == null) {
      return;
    }

    // Determine if we are in the header, footer, or body. Its possible that
    // the table has been refreshed before the current event fired (ex. change
    // event refreshes before mouseup fires), so we need to check each parent
    // element.
    Element trElem = tableCell.getParentElement();
    if (trElem == null) {
      return;
    }
    TableRowElement tr = TableRowElement.as(trElem);
    Element sectionElem = tr.getParentElement();
    if (sectionElem == null) {
      return;
    }
    TableSectionElement section = TableSectionElement.as(sectionElem);

    // Forward the event to the associated header, footer, or column.
    boolean isClick = "click".equals(eventType);
    int col = tableCell.getCellIndex();
    if (section == getTableHeadElement()) {
      Header<?> header = headers.get(col);
      if (header != null) {
        // Fire the event to the header.
        if (cellConsumesEventType(header.getCell(), eventType)) {
          Context context = new Context(0, col, header.getKey());
          header.onBrowserEvent(context, tableCell, event);
        }

        // Sort the header.
        Column<T, ?> column = columns.get(col);
        if (isClick && column.isSortable()) {
          updatingSortList = true;
          sortList.push(column);
          updatingSortList = false;
          ColumnSortEvent.fire(this, sortList);
        }
      }
    } else if (section == getTableFootElement()) {
      Header<?> footer = footers.get(col);
      if (footer != null && cellConsumesEventType(footer.getCell(), eventType)) {
        Context context = new Context(0, col, footer.getKey());
        footer.onBrowserEvent(context, tableCell, event);
      }
    } else if (section == getTableBodyElement()) {
      // Update the hover state.
      int row = tr.getSectionRowIndex();
      if ("mouseover".equals(eventType)) {
        // Unstyle the old row if it is still part of the table.
        if (hoveringRow != null && getTableBodyElement().isOrHasChild(hoveringRow)) {
          setRowStyleName(hoveringRow, style.hoveredRow(), style.hoveredRowCell(), false);
        }
        hoveringRow = tr;
        setRowStyleName(hoveringRow, style.hoveredRow(), style.hoveredRowCell(), true);
      } else if ("mouseout".equals(eventType) && hoveringRow != null) {
        setRowStyleName(hoveringRow, style.hoveredRow(), style.hoveredRowCell(), false);
        hoveringRow = null;
      } else if (isClick
          && ((getPresenter().getKeyboardSelectedRowInView() != row) || (keyboardSelectedColumn != col))) {
        // Move keyboard focus. Since the user clicked, allow focus to go to a
        // non-interactive column.
        boolean isFocusable = CellBasedWidgetImpl.get().isFocusable(target);
        isFocused = isFocused || isFocusable;
        keyboardSelectedColumn = col;
        getPresenter().setKeyboardSelectedRow(row, !isFocusable, true);
      }

      // Update selection. Selection occurs before firing the event to the cell
      // in case the cell operates on the currently selected item.
      if (!isRowWithinBounds(row)) {
        // If the event causes us to page, then the physical index will be out
        // of bounds of the underlying data.
        return;
      }
      boolean isSelectionHandled =
          handlesSelection
              || KeyboardSelectionPolicy.BOUND_TO_SELECTION == getKeyboardSelectionPolicy();
      T value = getVisibleItem(row);
      Context context = new Context(row + getPageStart(), col, getValueKey(value));
      CellPreviewEvent<T> previewEvent =
          CellPreviewEvent.fire(this, event, this, context, value, cellIsEditing,
              isSelectionHandled);
      if (isClick && !cellIsEditing && !isSelectionHandled) {
        doSelection(event, value, row, col);
      }

      // Pass the event to the cell.
      if (!previewEvent.isCanceled()) {
        fireEventToCell(event, eventType, tableCell, value, context, columns.get(col));
      }
    }
  }

  @Override
  protected void onFocus() {
    Element elem = getKeyboardSelectedElement();
    if (elem != null) {
      TableCellElement td = elem.getParentElement().cast();
      TableRowElement tr = td.getParentElement().cast();
      td.addClassName(style.keyboardSelectedCell());
      setRowStyleName(tr, style.keyboardSelectedRow(), style.keyboardSelectedRowCell(), true);
    }
  }

  protected void refreshColumnWidths() {
    int columnCount = getColumnCount();
    for (int i = 0; i < columnCount; i++) {
      Column<T, ?> column = columns.get(i);
      String width = columnWidths.get(column);
      doSetColumnWidth(i, width);
    }
  }

  @Override
  protected void renderRowValues(SafeHtmlBuilder sb, List<T> values, int start,
      SelectionModel<? super T> selectionModel) {
    createHeadersAndFooters();

    int keyboardSelectedRow = getKeyboardSelectedRow() + getPageStart();
    String evenRowStyle = style.evenRow();
    String oddRowStyle = style.oddRow();
    String cellStyle = style.cell();
    String evenCellStyle = " " + style.evenRowCell();
    String oddCellStyle = " " + style.oddRowCell();
    String firstColumnStyle = " " + style.firstColumn();
    String lastColumnStyle = " " + style.lastColumn();
    String selectedRowStyle = " " + style.selectedRow();
    String selectedCellStyle = " " + style.selectedRowCell();
    String keyboardRowStyle = " " + style.keyboardSelectedRow();
    String keyboardRowCellStyle = " " + style.keyboardSelectedRowCell();
    String keyboardCellStyle = " " + style.keyboardSelectedCell();
    int columnCount = columns.size();
    int length = values.size();
    int end = start + length;
    for (int i = start; i < end; i++) {
      T value = values.get(i - start);
      boolean isSelected =
          (selectionModel == null || value == null) ? false : selectionModel.isSelected(value);
      boolean isEven = i % 2 == 0;
      boolean isKeyboardSelected = i == keyboardSelectedRow && isFocused;
      String trClasses = isEven ? evenRowStyle : oddRowStyle;
      if (isSelected) {
        trClasses += selectedRowStyle;
      }
      if (isKeyboardSelected) {
        trClasses += keyboardRowStyle;
      }

      if (rowStyles != null) {
        String extraRowStyles = rowStyles.getStyleNames(value, i);
        if (extraRowStyles != null) {
          trClasses += " ";
          trClasses += extraRowStyles;
        }
      }

      SafeHtmlBuilder trBuilder = new SafeHtmlBuilder();
      int curColumn = 0;
      for (Column<T, ?> column : columns) {
        String tdClasses = cellStyle;
        tdClasses += isEven ? evenCellStyle : oddCellStyle;
        if (curColumn == 0) {
          tdClasses += firstColumnStyle;
        }
        if (isSelected) {
          tdClasses += selectedCellStyle;
        }
        if (isKeyboardSelected) {
          tdClasses += keyboardRowCellStyle;
        }
        // The first and last column could be the same column.
        if (curColumn == columnCount - 1) {
          tdClasses += lastColumnStyle;
        }

        // Add class names specific to the cell.
        Context context = new Context(i, curColumn, getValueKey(value));
        String cellStyles = column.getCellStyleNames(context, value);
        if (cellStyles != null) {
          tdClasses += " " + cellStyles;
        }

        SafeHtmlBuilder cellBuilder = new SafeHtmlBuilder();
        if (value != null) {
          column.render(context, value, cellBuilder);
        }

        // Build the contents.
        SafeHtml contents = SafeHtmlUtils.EMPTY_SAFE_HTML;
        if (i == keyboardSelectedRow && curColumn == keyboardSelectedColumn) {
          // This is the focused cell.
          if (isFocused) {
            tdClasses += keyboardCellStyle;
          }
          char accessKey = getAccessKey();
          if (accessKey != 0) {
            contents =
                template.divFocusableWithKey(getTabIndex(), accessKey, cellBuilder.toSafeHtml());
          } else {
            contents = template.divFocusable(getTabIndex(), cellBuilder.toSafeHtml());
          }
        } else {
          contents = template.div(cellBuilder.toSafeHtml());
        }

        // Build the cell.
        HorizontalAlignmentConstant hAlign = column.getHorizontalAlignment();
        VerticalAlignmentConstant vAlign = column.getVerticalAlignment();
        if (hAlign != null && vAlign != null) {
          trBuilder.append(template.tdBothAlign(tdClasses, hAlign.getTextAlignString(), vAlign
              .getVerticalAlignString(), contents));
        } else if (hAlign != null) {
          trBuilder.append(template.tdHorizontalAlign(tdClasses, hAlign.getTextAlignString(),
              contents));
        } else if (vAlign != null) {
          trBuilder.append(template.tdVerticalAlign(tdClasses, vAlign.getVerticalAlignString(),
              contents));
        } else {
          trBuilder.append(template.td(tdClasses, contents));
        }

        curColumn++;
      }

      sb.append(template.tr(trClasses, trBuilder.toSafeHtml()));
    }
  }

  @Override
  protected void replaceAllChildren(List<T> values, SafeHtml html) {
    TABLE_IMPL.replaceAllRows(AbstractCellTable.this, getTableBodyElement(), CellBasedWidgetImpl
        .get().processHtml(html));
  }

  @Override
  protected boolean resetFocusOnCell() {
    int row = getKeyboardSelectedRow();
    if (isRowWithinBounds(row) && columns.size() > 0) {
      Column<T, ?> column = columns.get(keyboardSelectedColumn);
      return resetFocusOnCellImpl(row, keyboardSelectedColumn, column);
    }
    return false;
  }

  @Override
  protected void setKeyboardSelected(int index, boolean selected, boolean stealFocus) {
    if (KeyboardSelectionPolicy.DISABLED == getKeyboardSelectionPolicy()
        || !isRowWithinBounds(index) || columns.size() == 0) {
      return;
    }

    TableRowElement tr = getRowElement(index);
    String cellStyle = style.keyboardSelectedCell();
    boolean updatedSelection = !selected || isFocused || stealFocus;
    setRowStyleName(tr, style.keyboardSelectedRow(), style.keyboardSelectedRowCell(), selected);
    NodeList<TableCellElement> cells = tr.getCells();
    for (int i = 0; i < cells.getLength(); i++) {
      TableCellElement td = cells.getItem(i);

      // Update the selected style.
      setStyleName(td, cellStyle, updatedSelection && selected && i == keyboardSelectedColumn);

      // Mark as focusable.
      final com.google.gwt.user.client.Element cellParent = getCellParent(td).cast();
      setFocusable(cellParent, selected && i == keyboardSelectedColumn);
    }

    // Move focus to the cell.
    if (selected && stealFocus && !cellIsEditing) {
      TableCellElement td = tr.getCells().getItem(keyboardSelectedColumn);
      final com.google.gwt.user.client.Element cellParent = getCellParent(td).cast();
      CellBasedWidgetImpl.get().resetFocus(new Scheduler.ScheduledCommand() {
        @Override
        public void execute() {
          cellParent.focus();
        }
      });
    }
  }

  /**
   * @deprecated this method is never called by AbstractHasData, render the
   *             selected styles in
   *             {@link #renderRowValues(SafeHtmlBuilder, List, int, SelectionModel)}
   */
  @Override
  @Deprecated
  protected void setSelected(Element elem, boolean selected) {
    TableRowElement tr = elem.cast();
    setRowStyleName(tr, style.selectedRow(), style.selectedRowCell(), selected);
  }

  /**
   * Check that the specified column is within bounds.
   * 
   * @param col the column index
   * @throws IndexOutOfBoundsException if the column is out of bounds
   */
  private void checkColumnBounds(int col) {
    if (col < 0 || col >= getColumnCount()) {
      throw new IndexOutOfBoundsException("Column index is out of bounds: " + col);
    }
  }

  /**
   * Coalesce the various cell properties (dependsOnSelection, handlesSelection,
   * isInteractive) into a table policy.
   */
  private void coalesceCellProperties() {
    dependsOnSelection = false;
    handlesSelection = false;
    isInteractive = false;
    for (Column<T, ?> column : columns) {
      Cell<?> cell = column.getCell();
      if (cell.dependsOnSelection()) {
        dependsOnSelection = true;
      }
      if (cell.handlesSelection()) {
        handlesSelection = true;
      }
      if (isColumnInteractive(column)) {
        isInteractive = true;
      }
    }
  }

  /**
   * Render the header or footer.
   * 
   * @param isFooter true if this is the footer table, false if the header table
   */
  private void createHeaders(boolean isFooter) {
    List<Header<?>> theHeaders = isFooter ? footers : headers;
    TableSectionElement section = isFooter ? getTableFootElement() : getTableHeadElement();
    String className = isFooter ? style.footer() : style.header();
    String firstColumnStyle =
        " " + (isFooter ? style.firstColumnFooter() : style.firstColumnHeader());
    String lastColumnStyle = " " + (isFooter ? style.lastColumnFooter() : style.lastColumnHeader());
    String sortableStyle = " " + style.sortableHeader();
    String sortedAscStyle = " " + style.sortedHeaderAscending();
    String sortedDescStyle = " " + style.sortedHeaderDescending();

    boolean hasHeader = false;
    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    sb.appendHtmlConstant("<tr>");
    int columnCount = columns.size();
    if (columnCount > 0) {
      // Get information about the sorted column.
      ColumnSortInfo sortedInfo = (sortList.size() == 0) ? null : sortList.get(0);
      Column<?, ?> sortedColumn = (sortedInfo == null) ? null : sortedInfo.getColumn();
      boolean isSortAscending = (sortedInfo == null) ? false : sortedInfo.isAscending();

      // Setup the first column.
      Header<?> prevHeader = theHeaders.get(0);
      Column<T, ?> column = columns.get(0);
      int prevColspan = 1;
      boolean isSortable = false;
      boolean isSorted = false;
      StringBuilder classesBuilder = new StringBuilder(className);
      classesBuilder.append(firstColumnStyle);
      if (!isFooter && column.isSortable()) {
        isSortable = true;
        isSorted = (column == sortedColumn);
      }

      // Loop through all column headers.
      int curColumn;
      for (curColumn = 1; curColumn < columnCount; curColumn++) {
        Header<?> header = theHeaders.get(curColumn);

        if (header != prevHeader) {
          // The header has changed, so append the previous one.
          SafeHtml headerHtml = SafeHtmlUtils.EMPTY_SAFE_HTML;
          if (prevHeader != null) {
            hasHeader = true;

            // Build the header.
            SafeHtmlBuilder headerBuilder = new SafeHtmlBuilder();
            Context context = new Context(0, curColumn - prevColspan, prevHeader.getKey());
            prevHeader.render(context, headerBuilder);

            // Wrap the header with a sort icon.
            if (isSorted) {
              SafeHtml unwrappedHeader = headerBuilder.toSafeHtml();
              headerBuilder = new SafeHtmlBuilder();
              getSortDecorator(isSortAscending).render(null, unwrappedHeader, headerBuilder);
            }
            headerHtml = headerBuilder.toSafeHtml();
          }
          if (isSortable) {
            classesBuilder.append(sortableStyle);
          }
          if (isSorted) {
            classesBuilder.append(isSortAscending ? sortedAscStyle : sortedDescStyle);
          }
          sb.append(template.th(prevColspan, classesBuilder.toString(), headerHtml));

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
        column = columns.get(curColumn);
        if (!isFooter && column.isSortable()) {
          isSortable = true;
          isSorted = (column == sortedColumn);
        }
      }

      // Append the last header.
      SafeHtml headerHtml = SafeHtmlUtils.EMPTY_SAFE_HTML;
      if (prevHeader != null) {
        hasHeader = true;

        // Build the header.
        SafeHtmlBuilder headerBuilder = new SafeHtmlBuilder();
        Context context = new Context(0, curColumn - prevColspan, prevHeader.getKey());
        prevHeader.render(context, headerBuilder);

        // Wrap the header with a sort icon.
        if (isSorted) {
          SafeHtml unwrappedHeader = headerBuilder.toSafeHtml();
          headerBuilder = new SafeHtmlBuilder();
          getSortDecorator(isSortAscending).render(null, unwrappedHeader, headerBuilder);
        }
        headerHtml = headerBuilder.toSafeHtml();
      }
      if (isSortable) {
        classesBuilder.append(sortableStyle);
      }
      if (isSorted) {
        classesBuilder.append(isSortAscending ? sortedAscStyle : sortedDescStyle);
      }

      // The first and last columns could be the same column.
      classesBuilder.append(" ");
      classesBuilder.append(lastColumnStyle);
      sb.append(template.th(prevColspan, classesBuilder.toString(), headerHtml));
    }
    sb.appendHtmlConstant("</tr>");

    // Render the section contents.
    TABLE_IMPL.replaceAllRows(this, section, sb.toSafeHtml());

    // If the section isn't used, hide it.
    doSetHeaderVisible(isFooter, hasHeader);
  }

  private void createHeadersAndFooters() {
    createHeaders(false);
    createHeaders(true);
  }

  /**
   * Find and return the index of the next interactive column. If no column is
   * interactive, 0 is returned. If the start index is the only interactive
   * column, it is returned.
   * 
   * @param start the start index, exclusive unless it is the only option
   * @param reverse true to do a reverse search
   * @return the interactive column index, or 0 if not interactive
   */
  private int findInteractiveColumn(int start, boolean reverse) {
    if (!isInteractive) {
      return 0;
    } else if (reverse) {
      for (int i = start - 1; i >= 0; i--) {
        if (isColumnInteractive(columns.get(i))) {
          return i;
        }
      }
      // Wrap to the end.
      for (int i = columns.size() - 1; i >= start; i--) {
        if (isColumnInteractive(columns.get(i))) {
          return i;
        }
      }
    } else {
      for (int i = start + 1; i < columns.size(); i++) {
        if (isColumnInteractive(columns.get(i))) {
          return i;
        }
      }
      // Wrap to the start.
      for (int i = 0; i <= start; i++) {
        if (isColumnInteractive(columns.get(i))) {
          return i;
        }
      }
    }
    return 0;
  }

  /**
   * Find the cell that contains the element. Note that the TD element is not
   * the parent. The parent is the div inside the TD cell.
   * 
   * @param elem the element
   * @return the parent cell
   */
  private TableCellElement findNearestParentCell(Element elem) {
    while ((elem != null) && (elem != getElement())) {
      // TODO: We need is() implementations in all Element subclasses.
      // This would allow us to use TableCellElement.is() -- much cleaner.
      String tagName = elem.getTagName();
      if ("td".equalsIgnoreCase(tagName) || "th".equalsIgnoreCase(tagName)) {
        return elem.cast();
      }
      elem = elem.getParentElement();
    }
    return null;
  }

  /**
   * Fire an event to the Cell within the specified {@link TableCellElement}.
   */
  private <C> void fireEventToCell(Event event, String eventType, TableCellElement tableCell,
      T value, Context context, Column<T, C> column) {
    Cell<C> cell = column.getCell();
    if (cellConsumesEventType(cell, eventType)) {
      C cellValue = column.getValue(value);
      Element parentElem = getCellParent(tableCell);
      boolean cellWasEditing = cell.isEditing(context, parentElem, cellValue);
      column.onBrowserEvent(context, parentElem, value, event);
      cellIsEditing = cell.isEditing(context, parentElem, cellValue);
      if (cellWasEditing && !cellIsEditing) {
        CellBasedWidgetImpl.get().resetFocus(new Scheduler.ScheduledCommand() {
          @Override
          public void execute() {
            setFocus(true);
          }
        });
      }
    }
  }

  /**
   * Get the parent element that is passed to the {@link Cell} from the table
   * cell element.
   * 
   * @param td the table cell
   * @return the parent of the {@link Cell}
   */
  private Element getCellParent(TableCellElement td) {
    return td.getFirstChildElement();
  }

  /**
   * Get the {@link IconCellDecorator} used to decorate sorted column headers.
   * 
   * @param ascending true if ascending, false if descending
   * @return the {@link IconCellDecorator}
   */
  private IconCellDecorator<SafeHtml> getSortDecorator(boolean ascending) {
    if (ascending) {
      if (sortAscDecorator == null) {
        sortAscDecorator =
            new IconCellDecorator<SafeHtml>(resources.sortAscending(), new SafeHtmlCell());
      }
      return sortAscDecorator;
    } else {
      if (sortDescDecorator == null) {
        sortDescDecorator =
            new IconCellDecorator<SafeHtml>(resources.sortDescending(), new SafeHtmlCell());
      }
      return sortDescDecorator;
    }
  }

  private boolean handleKey(Event event) {
    HasDataPresenter<T> presenter = getPresenter();
    int oldRow = getKeyboardSelectedRow();
    boolean isRtl = LocaleInfo.getCurrentLocale().isRTL();
    int keyCodeLineEnd = isRtl ? KeyCodes.KEY_LEFT : KeyCodes.KEY_RIGHT;
    int keyCodeLineStart = isRtl ? KeyCodes.KEY_RIGHT : KeyCodes.KEY_LEFT;
    int keyCode = event.getKeyCode();
    if (keyCode == keyCodeLineEnd) {
      int nextColumn = findInteractiveColumn(keyboardSelectedColumn, false);
      if (nextColumn <= keyboardSelectedColumn) {
        // Wrap to the next row.
        if (presenter.hasKeyboardNext()) {
          keyboardSelectedColumn = nextColumn;
          presenter.keyboardNext();
          event.preventDefault();
          return true;
        }
      } else {
        // Reselect the row to move the selected column.
        keyboardSelectedColumn = nextColumn;
        getPresenter().setKeyboardSelectedRow(oldRow, true, true);
        event.preventDefault();
        return true;
      }
    } else if (keyCode == keyCodeLineStart) {
      int prevColumn = findInteractiveColumn(keyboardSelectedColumn, true);
      if (prevColumn >= keyboardSelectedColumn) {
        // Wrap to the previous row.
        if (presenter.hasKeyboardPrev()) {
          keyboardSelectedColumn = prevColumn;
          presenter.keyboardPrev();
          event.preventDefault();
          return true;
        }
      } else {
        // Reselect the row to move the selected column.
        keyboardSelectedColumn = prevColumn;
        getPresenter().setKeyboardSelectedRow(oldRow, true, true);
        event.preventDefault();
        return true;
      }
    }

    return false;
  }

  /**
   * Initialize the widget.
   */
  private void init() {
    if (TABLE_IMPL == null) {
      TABLE_IMPL = GWT.create(Impl.class);
    }
    if (template == null) {
      template = GWT.create(Template.class);
    }

    // Sink events.
    Set<String> eventTypes = new HashSet<String>();
    eventTypes.add("mouseover");
    eventTypes.add("mouseout");
    CellBasedWidgetImpl.get().sinkEvents(this, eventTypes);
  }

  /**
   * Check if a column consumes events.
   */
  private boolean isColumnInteractive(Column<T, ?> column) {
    Set<String> consumedEvents = column.getCell().getConsumedEvents();
    return consumedEvents != null && consumedEvents.size() > 0;
  }

  private <C> boolean resetFocusOnCellImpl(int row, int col, Column<T, C> column) {
    Element parent = getKeyboardSelectedElement();
    T value = getVisibleItem(row);
    Object key = getValueKey(value);
    C cellValue = column.getValue(value);
    Cell<C> cell = column.getCell();
    Context context = new Context(row + getPageStart(), col, key);
    return cell.resetFocus(context, parent, cellValue);
  }

  /**
   * Apply a style to a row and all cells in the row.
   * 
   * @param tr the row element
   * @param rowStyle the style to apply to the row
   * @param cellStyle the style to apply to the cells
   * @param add true to add the style, false to remove
   */
  private void setRowStyleName(TableRowElement tr, String rowStyle, String cellStyle, boolean add) {
    setStyleName(tr, rowStyle, add);
    NodeList<TableCellElement> cells = tr.getCells();
    for (int i = 0; i < cells.getLength(); i++) {
      setStyleName(cells.getItem(i), cellStyle, add);
    }
  }
}
