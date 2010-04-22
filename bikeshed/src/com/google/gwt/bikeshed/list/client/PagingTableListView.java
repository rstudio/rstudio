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
package com.google.gwt.bikeshed.list.client;

import com.google.gwt.bikeshed.list.shared.ProvidesKey;
import com.google.gwt.bikeshed.list.shared.Range;
import com.google.gwt.bikeshed.list.shared.SelectionModel;
import com.google.gwt.bikeshed.list.shared.AbstractListViewAdapter.DefaultRange;
import com.google.gwt.bikeshed.list.shared.SelectionModel.SelectionChangeEvent;
import com.google.gwt.bikeshed.list.shared.SelectionModel.SelectionChangeHandler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.dom.client.TableSectionElement;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;
import java.util.List;

/**
 * A list view that supports paging and columns.
 *
 * @param <T> the data type of each row
 */
public class PagingTableListView<T> extends Widget implements ListView<T> {

  private class TableSelectionHandler implements SelectionChangeHandler {
    public void onSelectionChange(SelectionChangeEvent event) {
      refreshSelection();
    }
  }

  private static final int DEFAULT_SIZE = 10;

  private List<Column<T, ?, ?>> columns = new ArrayList<Column<T, ?, ?>>();
  private ArrayList<Boolean> dataSelected = new ArrayList<Boolean>();
  private ArrayList<T> dataValues = new ArrayList<T>();
  private Delegate<T> delegate;
  private List<Header<?>> footers = new ArrayList<Header<?>>();
  private List<Header<?>> headers = new ArrayList<Header<?>>();
  private TableRowElement hoveringRow;
  private int pageSize = -1;
  private int pageStart = 0;

  /**
   * If null, each T will be used as its own key.
   */
  private ProvidesKey<T> providesKey;

  private HandlerRegistration selectionHandler;
  private SelectionModel<T> selectionModel;
  private TableElement table;
  private TableSectionElement tbody;
  private TableSectionElement tfoot;
  private TableSectionElement thead;
  private int size = 0;

  /**
   * Constructs a table with a default page size of 10.
   */
  public PagingTableListView() {
    this(DEFAULT_SIZE);
  }

  /**
   * Constructs a table with the given page size.
   *
   * @param pageSize the page size
   */
  public PagingTableListView(final int pageSize) {
    setElement(table = Document.get().createTableElement());
    table.setCellSpacing(0);
    thead = table.createTHead();
    table.appendChild(tbody = Document.get().createTBodyElement());
    tfoot = table.createTFoot();

    setPageSize(pageSize);

    // TODO: Total hack. It would almost definitely be preferable to sink only
    // those events actually needed by cells.
    sinkEvents(Event.ONCLICK | Event.MOUSEEVENTS | Event.KEYEVENTS
        | Event.ONCHANGE | Event.FOCUSEVENTS);
  }

  /**
   * Adds a column to the table.
   */
  public void addColumn(Column<T, ?, ?> col) {
    addColumn(col, null, null);
  }

  /**
   * Adds a column to the table with an associated header.
   */
  public void addColumn(Column<T, ?, ?> col, Header<?> header) {
    addColumn(col, header, null);
  }

  /**
   * Adds a column to the table with an associated header and footer.
   */
  public void addColumn(Column<T, ?, ?> col, Header<?> header, Header<?> footer) {
    headers.add(header);
    footers.add(footer);
    createHeadersAndFooters(); // TODO: defer header recreation
    columns.add(col);
    createRows();

    refresh();
  }

  /**
   * Adds a column to the table with an associated String header.
   */
  public void addColumn(Column<T, ?, ?> col, String headerString) {
    addColumn(col, new TextHeader(headerString), null);
  }

  /**
   * Returns true if it there is enough data to allow a given number of
   * additional rows to be displayed.
   */
  public boolean canAddRows(int rows) {
    return size - pageSize >= rows;
  }

  /**
   * Returns true if the page size is sufficient to allow a given number of rows
   * to be removed.
   */
  public boolean canRemoveRows(int rows) {
    return pageSize > rows;
  }

  // TODO: remove(Column)

  public T getDisplayedItem(int indexOnPage) {
    if (indexOnPage < 0 || indexOnPage >= getNumDisplayedItems()) {
      throw new IndexOutOfBoundsException("indexOnPage = " + indexOnPage);
    }
    return dataValues.get(indexOnPage);
  }

  public List<T> getDisplayedItems() {
    return new ArrayList<T>(dataValues);
  }

  public int getNumDisplayedItems() {
    return Math.min(getPageSize(), size - pageStart);
  }

  public int getPageSize() {
    return pageSize;
  }

  public int getPageStart() {
    return pageStart;
  }

  public ProvidesKey<T> getProvidesKey() {
    return providesKey;
  }

  public Range getRange() {
    return new DefaultRange(pageStart, pageSize);
  }

  public int getSize() {
    return size;
  }

  /**
   * Returns true if there is enough data such that a call to
   * {@link #nextPage()} will succeed in moving the starting point of the table
   * forward.
   */
  public boolean hasNextPage() {
    return pageStart + pageSize < size;
  }

  /**
   * Returns true if there is enough data such that a call to
   * {@link #previousPage()} will succeed in moving the starting point of the
   * table backward.
   */
  public boolean hasPreviousPage() {
    return pageStart > 0 && size > 0;
  }

  /**
   * Advance the starting row by {@link #getPageSize()} rows.
   */
  public void nextPage() {
    setPageStart(Math.min(getSize() - 1, getPageStart() + getPageSize()));
  }

  @Override
  public void onBrowserEvent(Event event) {
    EventTarget target = event.getEventTarget();
    Node node = Node.as(target);
    TableCellElement cell = findNearestParentCell(node);
    if (cell == null) {
      return;
    }

    TableRowElement tr = TableRowElement.as(cell.getParentElement());
    TableSectionElement section = TableSectionElement.as(tr.getParentElement());
    int col = cell.getCellIndex();
    if (section == thead) {
      Header<?> header = headers.get(col);
      if (header != null) {
        header.onBrowserEvent(cell, event);
      }
    } else if (section == tfoot) {
      Header<?> footer = footers.get(col);
      if (footer != null) {
        footer.onBrowserEvent(cell, event);
      }
    } else if (section == tbody) {
      int row = tr.getSectionRowIndex();

      if (event.getType().equals("mouseover")) {
        if (hoveringRow != null) {
          hoveringRow.removeClassName("hover");
        }
        hoveringRow = tr;
        tr.addClassName("hover");
      } else if (event.getType().equals("mouseout")) {
        hoveringRow = null;
        tr.removeClassName("hover");
      }

      T value = dataValues.get(row);
      Column<T, ?, ?> column = columns.get(col);

      column.onBrowserEvent(cell, pageStart + row, value, event, providesKey);
    }
  }

  /**
   * Move the starting row back by {@link #getPageSize()} rows.
   */
  public void previousPage() {
    setPageStart(Math.max(0, getPageStart() - getPageSize()));
  }

  /**
   * Redraw the table, requesting data from the delegate.
   */
  public void refresh() {
    if (delegate != null) {
      delegate.onRangeChanged(this);
    }
    updateRowVisibility();
  }

  /**
   * Refresh those portions of the table that depend on the state of the
   * {@link SelectionModel}.
   */
  public void refreshSelection() {
    // Refresh headers
    Element th = thead.getFirstChild().getFirstChild().cast();
    for (Header<?> header : headers) {
      if (header.dependsOnSelection()) {
        StringBuilder sb = new StringBuilder();
        header.render(sb);
        th.setInnerHTML(sb.toString());
      }
      th = th.getNextSibling().cast();
    }

    int numCols = columns.size();

    // Refresh body
    NodeList<TableRowElement> rows = tbody.getRows();
    for (int indexOnPage = 0; indexOnPage < pageSize; indexOnPage++) {
      TableRowElement row = rows.getItem(indexOnPage);

      T q = dataValues.get(indexOnPage);
      boolean qSelected = dataSelected.get(indexOnPage);
      boolean selected = q != null && selectionModel.isSelected(q);

      // Process the row only if the selection has changed
      if (qSelected != selected) {
        dataSelected.set(indexOnPage, selected);

        if (selected) {
          // item became selected
          row.setClassName("pagingTableListView selected");
        } else {
          // item became unselected
          row.setClassName("pagingTableListView "
              + ((indexOnPage & 0x1) == 0 ? "evenRow" : "oddRow"));
        }

        for (int c = 0; c < numCols; ++c) {
          Column<T, ?, ?> column = columns.get(c);
          if (column.dependsOnSelection()) {
            TableCellElement cell = row.getCells().getItem(c);
            StringBuilder sb = new StringBuilder();
            columns.get(c).render(q, sb);
            cell.setInnerHTML(sb.toString());
          }
        }
      }
    }
  }

  public void setData(int start, int length, List<T> values) {
    int numCols = columns.size();

    NodeList<TableRowElement> rows = tbody.getRows();
    for (int r = start; r < start + length; ++r) {
      int indexOnPage = r - pageStart;
      TableRowElement row = rows.getItem(indexOnPage);
      T q = values.get(r - start);
      if (selectionModel != null && selectionModel.isSelected(q)) {
        row.setClassName("pagingTableListView selected");
      } else {
        row.setClassName("pagingTableListView "
            + ((indexOnPage & 0x1) == 0 ? "evenRow" : "oddRow"));
      }

      dataValues.set(indexOnPage, q);
      for (int c = 0; c < numCols; ++c) {
        TableCellElement cell = row.getCells().getItem(c);
        StringBuilder sb = new StringBuilder();
        columns.get(c).render(q, sb);
        cell.setInnerHTML(sb.toString());

        // TODO: Really total hack! There's gotta be a better way...
        Element child = cell.getFirstChildElement();
        if (child != null) {
          Event.sinkEvents(child, Event.ONFOCUS | Event.ONBLUR);
        }
      }
    }
  }

  public void setDataSize(int size, boolean isExact) {
    this.size = size;
    refresh();
  }

  public void setDelegate(Delegate<T> delegate) {
    this.delegate = delegate;
  }

  /**
   * Set the number of rows per page and refresh the table.
   *
   * @param pageSize the page size
   *
   * @throw {@link IllegalArgumentException} if pageSize is negative or 0
   */
  public void setPageSize(int pageSize) {
    if (pageSize <= 0) {
      throw new IllegalArgumentException("pageSize = " + pageSize);
    }
    if (this.pageSize == pageSize) {
      return;
    }
    this.pageSize = pageSize;

    // If on last page and page size increases, move the page start upwards
    if (pageStart + pageSize > size) {
      pageStart = Math.max(0, size - pageSize);
    }

    // TODO - avoid requesting data if the page size has decreased
    createRows();
    refresh();
  }

  /**
   * Set the starting index of the current visible page.  The actual page
   * start will be clamped in the range [0, getSize() - 1].
   *
   * @param pageStart the index of the row that should appear at the start of
   *          the page
   */
  public void setPageStart(int pageStart) {
    this.pageStart = Math.max(Math.min(pageStart, size - 1), 0);
    refresh();
  }

  /**
   * Sets the {@link ProvidesKey} instance that will be used to generate keys
   * for each record object as needed.
   *
   * @param providesKey an instance of {@link ProvidesKey<T>} used to generate
   *          keys for record objects.
   */
  // TODO - when is this valid?  Do we rehash column view data if it changes?
  public void setProvidesKey(ProvidesKey<T> providesKey) {
    this.providesKey = providesKey;
  }

  /**
   * Sets the selection model.
   */
  public void setSelectionModel(SelectionModel<T> selectionModel) {
    if (selectionHandler != null) {
      selectionHandler.removeHandler();
      selectionHandler = null;
    }
    this.selectionModel = selectionModel;
    if (selectionModel != null && isAttached()) {
      selectionHandler = selectionModel.addSelectionChangeHandler(new TableSelectionHandler());
    }

    refreshSelection();
  }

  @Override
  protected void onLoad() {
    // Attach a selection handler.
    if (selectionModel != null) {
      selectionHandler = selectionModel.addSelectionChangeHandler(new TableSelectionHandler());
    }
  }

  @Override
  protected void onUnload() {
    // Detach the selection handler.
    if (selectionHandler != null) {
      selectionHandler.removeHandler();
      selectionHandler = null;
    }
  }

  private void createHeaders(List<Header<?>> headers,
      TableSectionElement section) {
    StringBuilder sb = new StringBuilder();
    sb.append("<tr>");
    for (Header<?> header : headers) {
      sb.append("<th>");
      if (header != null) {
        header.render(sb);
      }
      sb.append("</th>");
    }
    sb.append("</tr>");

    section.setInnerHTML(sb.toString());
  }

  private void createHeadersAndFooters() {
    createHeaders(headers, thead);
    createHeaders(footers, tfoot);
  }

  private void createRows() {
    int numCols = columns.size();

    // TODO - only delete as needed
    int numRows = tbody.getRows().getLength();
    while (numRows-- > 0) {
      tbody.deleteRow(0);
    }

    for (int r = 0; r < pageSize; ++r) {
      TableRowElement row = tbody.insertRow(0);
      row.setClassName("pagingTableListView "
          + ((r & 0x1) == 0 ? "evenRow" : "oddRow"));

      // TODO: use cloneNode() to make this even faster.
      for (int c = 0; c < numCols; ++c) {
        row.insertCell(c);
      }
    }

    // Make room for the data cache
    dataValues.ensureCapacity(pageSize);
    dataSelected.ensureCapacity(pageSize);
    while (dataValues.size() < pageSize) {
      dataValues.add(null);
      dataSelected.add(Boolean.FALSE);
    }
  }

  private TableCellElement findNearestParentCell(Node node) {
    while ((node != null) && (node != table)) {
      if (Element.is(node)) {
        Element elem = Element.as(node);

        // TODO: We need is() implementations in all Element subclasses.
        // This would allow us to use TableCellElement.is() -- much cleaner.
        String tagName = elem.getTagName();
        if ("td".equalsIgnoreCase(tagName) || "th".equalsIgnoreCase(tagName)) {
          return elem.cast();
        }
      }
      node = node.getParentNode();
    }
    return null;
  }

  private void updateRowVisibility() {
    int visible = Math.min(pageSize, size - pageStart);

    for (int r = 0; r < pageSize; ++r) {
      Style rowStyle = tbody.getRows().getItem(r).getStyle();
      if (r < visible) {
        rowStyle.clearDisplay();
      } else {
        rowStyle.setDisplay(Display.NONE);
      }
    }
  }
}
