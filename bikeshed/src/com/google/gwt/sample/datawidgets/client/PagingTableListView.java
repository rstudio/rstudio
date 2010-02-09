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
package com.google.gwt.sample.datawidgets.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.list.shared.ListEvent;
import com.google.gwt.list.shared.ListHandler;
import com.google.gwt.list.shared.ListModel;
import com.google.gwt.list.shared.ListRegistration;
import com.google.gwt.list.shared.SizeChangeEvent;
import com.google.gwt.sample.datawidgets.shared.StockQuote;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;

import com.google.gwt.user.client.ui.Composite;

import java.util.List;

/**
 * A view of a list model that can be paged.
 */
public class PagingTableListView extends Composite {

  /**
   * The main widget.
   */
  private FlexTable table = new FlexTable();

  /**
   * The current page.
   */
  private int curPage = 0;

  /**
   * The total number of pages.
   */
  private int numPages = 0;

  /**
   * The page size.
   */
  private int pageSize;

  private ListRegistration listReg;

  /**
   * Construct a new {@link PagingTableListView}.
   * 
   * @param listModel the listModel that backs the table
   * @param pageSize the page size
   */
  public PagingTableListView(ListModel<StockQuote> listModel, final int pageSize) {
    this.pageSize = pageSize;
    initWidget(table);
    table.setBorderWidth(3);

    // Create the next and previous buttons.
    final Button nextButton = new Button("Next", new ClickHandler() {
      public void onClick(ClickEvent event) {
        setPage(curPage + 1);
      }
    });
    final Button prevButton = new Button("Prev", new ClickHandler() {
      public void onClick(ClickEvent event) {
        setPage(curPage - 1);
      }
    });

    // Attach to the list model.
    listReg = listModel.addListHandler(new ListHandler<StockQuote>() {
      public void onDataChanged(ListEvent<StockQuote> event) {
        // Clear existing data.
        table.removeAllRows();

        // Add the headers.
        table.setHTML(0, 0, "<b>Ticker</b>");
        table.setHTML(0, 1, "<b>Company</b>");
        table.setHTML(0, 2, "<b>Price</b>");

        // Add the new data.
        int row = table.getRowCount();
        List<StockQuote> values = event.getValues();
        for (StockQuote value : values) {
          table.setText(row, 0, value.getTicker());
          table.setText(row, 1, value.getName());
          table.setText(row, 2, "" + value.getDisplayPrice());
          row++;
        }

        // Add next/prev buttons.
        table.setWidget(row, 0, prevButton);
        table.setText(row, 1, "Page " + (curPage + 1) + " of " + numPages);
        table.setWidget(row, 2, nextButton);
      }

      public void onSizeChanged(SizeChangeEvent event) {
        int size = event.getSize();
        if (size <= 0) {
          numPages = 0;
        } else {
          numPages = 1 + (size - 1) / pageSize;
        }
        setPage(curPage);
      }
    });
    listReg.setRangeOfInterest(0, pageSize);
  }

  /**
   * Get the current page.
   * 
   * @return the current page
   */
  public int getPage() {
    return curPage;
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
    if (table.getRowCount() > 0) {
      int row = table.getRowCount() - 1;
      table.setText(row, 1, "Page " + (page + 1) + " of " + numPages);
    }
  }
}
