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

import com.google.gwt.bikeshed.list.shared.ListEvent;
import com.google.gwt.bikeshed.list.shared.ListHandler;
import com.google.gwt.bikeshed.list.shared.ListModel;
import com.google.gwt.bikeshed.list.shared.ListRegistration;
import com.google.gwt.bikeshed.list.shared.SizeChangeEvent;
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
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;
import java.util.List;

/**
 * A list view that supports paging and columns.
 * 
 * @param <T> the data type of each row.
 */
public class PagingTableListView<T> extends Widget {

  protected int curPage;
  private int pageSize;
  private int numPages;
  private ListRegistration listReg;
  private int totalSize;
  private List<Column<T, ?, ?>> columns = new ArrayList<Column<T, ?, ?>>();
  private ArrayList<T> data = new ArrayList<T>();

  private List<Header<?>> headers = new ArrayList<Header<?>>();
  private List<Header<?>> footers = new ArrayList<Header<?>>();

  private TableElement table;
  private TableSectionElement thead;
  private TableSectionElement tfoot;
  private TableSectionElement tbody;

  public PagingTableListView(ListModel<T> listModel, final int pageSize) {
    this.pageSize = pageSize;
    setElement(table = Document.get().createTableElement());
    thead = table.createTHead();
    table.appendChild(tbody = Document.get().createTBodyElement());
    tfoot = table.createTFoot();
    createRows();

    // TODO: Total hack. It would almost definitely be preferable to sink only
    // those events actually needed by cells.
    sinkEvents(Event.ONCLICK | Event.MOUSEEVENTS | Event.KEYEVENTS
        | Event.ONCHANGE);

    // Attach to the list model.
    listReg = listModel.addListHandler(new ListHandler<T>() {
      public void onDataChanged(ListEvent<T> event) {
        render(event.getStart(), event.getLength(), event.getValues());
      }

      public void onSizeChanged(SizeChangeEvent event) {
        totalSize = event.getSize();
        if (totalSize <= 0) {
          numPages = 0;
        } else {
          numPages = 1 + (totalSize - 1) / pageSize;
        }
        setPage(curPage);
      }
    });
    listReg.setRangeOfInterest(0, pageSize);
  }

  public void addColumn(Column<T, ?, ?> col) {
    addColumn(col, null, null);
  }

  public void addColumn(Column<T, ?, ?> col, Header<?> header) {
    addColumn(col, header, null);
  }

  // TODO: remove(Column)
  public void addColumn(Column<T, ?, ?> col, Header<?> header, Header<?> footer) {
    headers.add(header);
    footers.add(footer);
    createHeadersAndFooters();  // TODO: defer header recreation
    columns.add(col);
    createRows();
    setPage(curPage); // TODO: better way to refresh?
  }

  /**
   * Get the current page.
   * 
   * @return the current page
   */
  public int getPage() {
    return curPage;
  }

  public void nextPage() {
    setPage(curPage + 1);
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
      T value = data.get(row);
      Column<T, ?, ?> column = columns.get(col);
      column.onBrowserEvent(cell, curPage * pageSize + row, value, event);
    }
  }

  public void previousPage() {
    setPage(curPage - 1);
  }

  /**
   * Set the current visible page.
   * 
   * @param page the page index
   */
  public void setPage(int page) {
    int newPage = Math.min(page, numPages - 1);
    newPage = Math.max(0, newPage);

    // Update the text showing the page number.
    updatePageText(newPage);

    // Early exit if we are already on the right page.
    if (curPage != newPage) {
      curPage = newPage;
      listReg.setRangeOfInterest(curPage * pageSize, pageSize);
    }

    updateRowVisibility();
  }

  /**
   * Set the number of rows per page.
   * 
   * @param pageSize the page size
   */
  public void setPageSize(int pageSize) {
    if (this.pageSize == pageSize) {
      return;
    }
    this.pageSize = pageSize;
    curPage = -1;
    setPage(curPage);
  }

  protected void render(int start, int length, List<T> values) {
    int numCols = columns.size();
    int pageStart = curPage * pageSize;

    NodeList<TableRowElement> rows = tbody.getRows();
    for (int r = start; r < start + length; ++r) {
      TableRowElement row = rows.getItem(r - pageStart);
      T q = values.get(r - start);

      data.set(r - pageStart, q);
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

  private void createHeaders(List<Header<?>> headers, TableSectionElement section) {
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
      row.setClassName("pagingTableListView " + ((r & 0x1) == 0 ? "evenRow" : "oddRow"));

      // TODO: use cloneNode() to make this even faster.
      for (int c = 0; c < numCols; ++c) {
        row.insertCell(c);
      }
    }

    // Make room for the data cache
    data.ensureCapacity(pageSize);
    while (data.size() < pageSize) {
      data.add(null);
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

  /**
   * Update the text that shows the current page.
   * 
   * @param page the current page
   */
  private void updatePageText(int page) {
    // TODO: Update external paging widget.
  }

  private void updateRowVisibility() {
    int visible = Math.min(pageSize, totalSize - curPage * pageSize);

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
