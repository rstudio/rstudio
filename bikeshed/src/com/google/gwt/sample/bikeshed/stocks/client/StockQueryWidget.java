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

import com.google.gwt.bikeshed.list.client.CellTable;
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
import com.google.gwt.view.client.AbstractListViewAdapter;

/**
 * A widget containing a search box and a results table.
 */
public class StockQueryWidget extends Composite {

  interface Binder extends UiBinder<Widget, StockQueryWidget> { }
  private static final Binder binder = GWT.create(Binder.class);

  @UiField CellTable<StockQuote> table;
  @UiField TextBox queryField = new TextBox();

  private final AbstractListViewAdapter<StockQuote> adapter;

  public StockQueryWidget(AbstractListViewAdapter<StockQuote> adapter, final Updater updater) {
    this.adapter = adapter;
    initWidget(binder.createAndBindUi(this));

    table.addColumn(Columns.favoriteColumn);
    table.addColumn(Columns.tickerColumn, "ticker");
    table.addColumn(Columns.nameColumn, "name");
    table.addColumn(Columns.changeColumn, "change");
    table.addColumn(Columns.priceColumn, "price");
    table.addColumn(Columns.buyColumn);

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
  CellTable<StockQuote> createTable() {
    CellTable<StockQuote> view = new CellTable<StockQuote>(10);
    adapter.addView(view);
    return view;
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
