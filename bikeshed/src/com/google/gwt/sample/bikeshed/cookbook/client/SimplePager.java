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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.AbstractPager;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.view.client.PagingListView;

/**
 * A pager for controlling a PagingListView that uses a series of buttons for
 * demo purposes.
 * 
 * @param <T> the type of the PagingListView being controlled
 */
public class SimplePager<T> extends AbstractPager<T> implements ClickHandler {

  private Button nextPageButton;
  private Button prevPageButton;
  private Button remove1Button;
  private Button remove5Button;
  private Label infoLabel;
  private PagingListView<T> view;

  public SimplePager(PagingListView<T> view) {
    super(view);
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
  }

  /**
   * Returns true if it there is enough data to allow a given number of
   * additional rows to be displayed.
   */
  public boolean canAddRows(int rows) {
    return view.getDataSize() - view.getPageSize() >= rows;
  }

  /**
   * Returns true if the page size is sufficient to allow a given number of rows
   * to be removed.
   */
  public boolean canRemoveRows(int rows) {
    return view.getPageSize() > rows;
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

  @Override
  public void onRangeOrSizeChanged(PagingListView<T> listView) {
    super.onRangeOrSizeChanged(listView);
    updateButtons();
  }

  private void addRows(int rows) {
    view.setPageSize(view.getPageSize() + rows);
  }

  private Button makeButton(String label, String id) {
    Button button = new Button(label);
    button.getElement().setId(id);
    button.addClickHandler(this);
    return button;
  }

  private void removeRows(int rows) {
    view.setPageSize(view.getPageSize() - rows);
  }

  private void updateButtons() {
    remove1Button.setEnabled(canRemoveRows(1));
    remove5Button.setEnabled(canRemoveRows(5));
    prevPageButton.setEnabled(hasPreviousPage());
    nextPageButton.setEnabled(hasNextPage());

    int page = (view.getPageStart() / view.getPageSize()) + 1;
    int numPages = (view.getDataSize() + view.getPageSize() - 1)
        / view.getPageSize();
    infoLabel.setText("Page " + page + " of " + numPages + ": Page Start = "
        + view.getPageStart() + ", Page Size = " + view.getPageSize()
        + ", Data Size = " + view.getDataSize());
  }
}
