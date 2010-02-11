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
package com.google.gwt.list.client;

import com.google.gwt.cells.client.ButtonCell;
import com.google.gwt.cells.client.Mutator;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.list.shared.ListEvent;
import com.google.gwt.list.shared.ListHandler;
import com.google.gwt.list.shared.ListModel;
import com.google.gwt.list.shared.ListRegistration;
import com.google.gwt.list.shared.SizeChangeEvent;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;
import java.util.List;

public class PagingTableListView<T> extends Widget {

  private int pageSize;
  private int numPages;
  private ListRegistration listReg;
  protected int curPage;
  private int totalSize;
  private List<Column<T, ?>> columns = new ArrayList<Column<T, ?>>();
  private ArrayList<T> data = new ArrayList<T>();
  private ButtonCell prevButton = new ButtonCell();
  private ButtonCell nextButton = new ButtonCell();

  public PagingTableListView(ListModel<T> listModel, final int pageSize) {
    this.pageSize = pageSize;
    setElement(Document.get().createTableElement());
    createRows();

    // TODO: total hack.
    sinkEvents(Event.MOUSEEVENTS | Event.KEYEVENTS);

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

  // TODO: remove(Column)
  public void addColumn(Column<T, ?> col) {
    columns.add(col);
    createRows();
    setPage(curPage); // TODO: better way to refresh?
  }

  @Override
  public void onBrowserEvent(Event event) {
    EventTarget target = event.getEventTarget();
    Node node = Node.as(target);
    while (node != null) {
      if (Element.is(node)) {
        Element elem = Element.as(node);

        // TODO: We need is() implementations in all Element subclasses.
        String tagName = elem.getTagName();
        if ("td".equalsIgnoreCase(tagName)) {
          TableCellElement td = TableCellElement.as(elem);
          TableRowElement tr = TableRowElement.as(td.getParentElement());

          // TODO: row/col assertions.
          int row = tr.getRowIndex(), col = td.getCellIndex();
          if (row < pageSize) {
            T value = data.get(row);
            Column<T, ?> column = columns.get(col);
            column.onBrowserEvent(elem, value, event);
          } else if (row == pageSize) {
            if (col == 0) {
              prevButton.onBrowserEvent(elem, null, event, new Mutator<String,String>() {
                public void mutate(String object, String after) {
                  previousPage();
                }
              });
            } else if (col == 2) {
              nextButton.onBrowserEvent(elem, null, event, new Mutator<String,String>() {
                public void mutate(String object, String after) {
                  nextPage();
                }
              });
            }
          }
          break;
        }
      }

      node = node.getParentNode();
    }
  }

  protected void render(int start, int length, List<T> values) {
    TableElement table = getElement().cast();
    int numCols = columns.size();
    int pageStart = curPage * pageSize;

    NodeList<TableRowElement> rows = table.getRows();
    for (int r = start; r < start + length; ++r) {
      TableRowElement row = rows.getItem(r - pageStart);
      T q = values.get(r - start);

      data.set(r - pageStart, q);
      for (int c = 0; c < numCols; ++c) {
        TableCellElement cell = row.getCells().getItem(c);
        StringBuilder sb = new StringBuilder();
        columns.get(c).render(q, sb);
        cell.setInnerHTML(sb.toString());

        // TODO: really total hack!
        Element child = cell.getFirstChildElement();
        if (child != null) {
          Event.sinkEvents(child, Event.ONCHANGE | Event.ONFOCUS | Event.ONBLUR);
        }
      }
    }
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

  private void updateRowVisibility() {
    int visible = Math.min(pageSize, totalSize - curPage * pageSize);

    TableElement table = getElement().cast();
    for (int r = 0; r < pageSize; ++r) {
      Style rowStyle = table.getRows().getItem(r).getStyle();
      if (r < visible) {
        rowStyle.clearDisplay();
      } else {
        rowStyle.setDisplay(Display.NONE);
      }
    }
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

  /**
   * Update the text that shows the current page.
   * 
   * @param page the current page
   */
  private void updatePageText(int page) {
    TableElement table = getElement().cast();
    NodeList<TableRowElement> rows = table.getRows();
    String text = "Page " + (page + 1) + " of " + numPages;
    rows.getItem(rows.getLength() - 1).getCells().getItem(1).setInnerText(text);
  }

  private void createRows() {
    TableElement table = getElement().cast();
    int numCols = columns.size();
    
    // TODO - only delete as needed
    int numRows = table.getRows().getLength();
    while (numRows-- > 0) {
      table.deleteRow(0);
    }
    
    for (int r = 0; r < pageSize; ++r) {
      TableRowElement row = table.insertRow(0);
      row.setClassName("pagingTableListView " + ((r & 0x1) == 0 ? "evenRow" : "oddRow"));

      // TODO: use cloneNode() to make this even faster.
      for (int c = 0; c < numCols; ++c) {
        row.insertCell(c);
      }
    }
    
    // Add the final row containing paging buttons
    TableRowElement pageRow = table.insertRow(pageSize);
    pageRow.insertCell(0);
    pageRow.insertCell(1);
    pageRow.insertCell(2);
    
    StringBuilder sb;
    
    sb = new StringBuilder();
    prevButton.render("Previous", sb);
    pageRow.getCells().getItem(0).setInnerHTML(sb.toString());
    
    pageRow.getCells().getItem(1).setAttribute("colspan", "" + (numCols - 2));
    pageRow.getCells().getItem(1).setAttribute("align", "center");
    
    sb = new StringBuilder();
    nextButton.render("Next", sb);
    pageRow.getCells().getItem(2).setInnerHTML(sb.toString());
    pageRow.getCells().getItem(2).setAttribute("align", "right");

    // Make room for the data cache
    data.ensureCapacity(pageSize);
    while (data.size() < pageSize) {
      data.add(null);
    }
  }
}
