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
package com.google.gwt.sample.bikeshed.stocks.client;

import com.google.gwt.bikeshed.list.client.PagingTableListView;
import com.google.gwt.bikeshed.list.client.TextHeader;
import com.google.gwt.bikeshed.list.shared.ListModel;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.sample.bikeshed.stocks.shared.StockQuote;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * A widget containing a search box and a results table.
 */
public class StockQueryWidget extends Composite {

  interface Binder extends UiBinder<Widget, StockQueryWidget> { }
  private static final Binder binder = GWT.create(Binder.class);

  @UiField PagingTableListView<StockQuote> listView;
  @UiField TextBox queryField = new TextBox();

  private final ListModel<StockQuote> model;

  public StockQueryWidget(ListModel<StockQuote> model, final Updater updater) {
    this.model = model;
    initWidget(binder.createAndBindUi(this));

    listView.addColumn(Columns.favoriteColumn);
    listView.addColumn(Columns.tickerColumn, new TextHeader("ticker"));
    listView.addColumn(Columns.nameColumn, new TextHeader("name"));
    listView.addColumn(Columns.changeColumn, new TextHeader("change"));
    listView.addColumn(Columns.priceColumn, new TextHeader("price"));
    listView.addColumn(Columns.buyColumn);

    // Focus the cursor on the name field when the app loads
    queryField.setFocus(true);
    queryField.selectAll();
    queryField.setText("G");

    // Add a handler to send the name to the server
    final Timer requestTimer = new Timer() {
      @Override
      public void run() {
        updater.update();
      }
    };
    queryField.addKeyUpHandler(new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        Columns.nameCell.setHighlightRegex(getSearchQuery());
        // Delay the request until the user stops typing.
        requestTimer.schedule(250);
      }
    });
  }

  public String getSearchQuery() {
    return normalize(queryField.getText());
  }
  
  @UiFactory
  PagingTableListView<StockQuote> createListView() {
    return new PagingTableListView<StockQuote>(model, 10);
  }

  private String normalize(String input) {
    String output = input;
    output = output.replaceAll("\\|+", " ");
    output = output.replaceAll("^[\\| ]+", "");
    output = output.replaceAll("[\\| ]+$", "");
    output = output.replaceAll("[ ]+", "|");
    return output;
  }
}
