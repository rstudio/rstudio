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
package com.google.gwt.sample.bikeshed.cookbook.client;

import com.google.gwt.bikeshed.list.client.PagingListView;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

/**
 * A pager for controlling a PagingListView that uses a series of buttons
 * for demo purposes.
 *
 * @param <T> the type of the PagingListView being controlled
 */
public class SimplePager<T> extends Composite implements PagingListView.Pager<T>,
    ClickHandler {

  private int dataSize;
  private Button nextPageButton;
  private int pageSize;
  private int pageStart;
  private Button prevPageButton;
  private Button remove1Button;
  private Button remove5Button;
  private PagingListView<T> view;
  private Label infoLabel;

  public SimplePager(PagingListView<T> view) {
    FlowPanel p = new FlowPanel();
    p.add(prevPageButton = makeButton("Previous Page", "PREV"));
    p.add(nextPageButton = makeButton("Next Page", "NEXT"));
    p.add(remove5Button = makeButton("Remove 5 rows", "REM5"));
    p.add(remove1Button = makeButton("Remove row", "REM1"));
    p.add(makeButton("Add row", "ADD1"));
    p.add(makeButton("Add 5 rows", "ADD5"));
    p.add(infoLabel = new Label(""));
    initWidget(p);

    this.view = view;
    view.setPager(this);
  }

  /**
   * Returns true if it there is enough data to allow a given number of
   * additional rows to be displayed.
   */
  public boolean canAddRows(int rows) {
    return dataSize - pageSize >= rows;
  }

  /**
   * Returns true if the page size is sufficient to allow a given number of
   * rows to be removed.
   */
  public boolean canRemoveRows(int rows) {
    return pageSize > rows;
  }

  /**
   * Returns true if there is enough data such that a call to
   * {@link #nextPage()} will succeed in moving the starting point of the
   * table forward.
   */
  public boolean hasNextPage() {
    return pageStart + pageSize < dataSize;
  }

  /**
   * Returns true if there is enough data such that a call to
   * {@link #previousPage()} will succeed in moving the starting point of the
   * table backward.
   */
  public boolean hasPreviousPage() {
    return pageStart > 0 && dataSize > 0;
  }

  /**
   * Advance the starting row by 'pageSize' rows.
   */
  public void nextPage() {
    view.setPageStart(pageStart + pageSize);
  }

  public void onClick(ClickEvent event) {
    String id = ((Button) event.getSource()).getElement().getId();
    
    if ("NEXT".equals(id)) {
      nextPage();
    } else if ("PREV".equals(id)) {
      previousPage();
    } else if (id.startsWith("ADD")) {
      addRows(Integer.parseInt(id.substring(3)));
    } else if (id.startsWith("REM")) {
      removeRows(Integer.parseInt(id.substring(3)));
    }
    updateButtons();
  }

  public void onRangeOrSizeChanged(PagingListView<T> listView) {
    this.pageSize = listView.getPageSize();
    this.pageStart = listView.getPageStart();
    this.dataSize = listView.getDataSize();
    updateButtons();
  }

  /**
   * Move the starting row back by 'pageSize' rows.
   */
  public void previousPage() {
    view.setPageStart(pageStart - pageSize);
  }

  private void addRows(int rows) {
    view.setPageSize(pageSize + rows);
  }

  private Button makeButton(String label, String id) {
    Button button = new Button(label);
    button.getElement().setId(id);
    button.addClickHandler(this);
    return button;
  }

  private void removeRows(int rows) {
    view.setPageSize(pageSize - rows);
  }

  private void updateButtons() {
    remove1Button.setEnabled(canRemoveRows(1));
    remove5Button.setEnabled(canRemoveRows(5));
    prevPageButton.setEnabled(hasPreviousPage());
    nextPageButton.setEnabled(hasNextPage());

    int page = (pageStart / pageSize) + 1;
    int numPages = (dataSize + pageSize - 1) / pageSize;
    infoLabel.setText("Page " + page + " of " + numPages + ": Page Start = " + pageStart + ", Page Size = " +
        pageSize + ", Data Size = " + dataSize);
  }
}
