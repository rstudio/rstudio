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

import com.google.gwt.cells.client.Mutator;
import com.google.gwt.cells.client.TextCell;
import com.google.gwt.cells.client.TextInputCell;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.list.client.PagingTableListView2;
import com.google.gwt.list.client.PagingTableListView2.Column;
import com.google.gwt.list.shared.AsyncListModel;
import com.google.gwt.sample.datawidgets.shared.StockQuote;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class DataBackedWidgets implements EntryPoint {

  final TextBox queryField = new TextBox();
  final Label errorLabel = new Label();
  private PagingTableListView2<StockQuote> resultsTable0;

  private ApplicationCache appCache = new ApplicationCache();
  private AsyncListModel<StockQuote> listModel1 = new AsyncListModel<StockQuote>(
      appCache);

  Column<StockQuote, String> tickerColumn = new Column<StockQuote, String>(
      new TextCell()) {
    @Override
    protected String getValue(StockQuote object) {
      return object.getTicker();
    }
  };

  Column<StockQuote, String> nameColumn = new Column<StockQuote, String>(
      new TextCell()) {
    @Override
    protected String getValue(StockQuote object) {
      return object.getName();
    }
  };

  Column<StockQuote, Integer> priceColumn = new Column<StockQuote, Integer>(
      new CurrencyCell()) {
    @Override
    protected Integer getValue(StockQuote object) {
      return object.getPrice();
    }
  };

  Column<StockQuote, String> notesColumn = new Column<StockQuote, String>(new TextInputCell()) {
    @Override
    protected String getValue(StockQuote object) {
      return object.getNotes();
    }
  };

  /**
   * This is the entry point method.
   */
  public void onModuleLoad() {
    appCache.addAsyncListModel(listModel1);

    queryField.setText("G");

    // Add the nameField and sendButton to the RootPanel
    // Use RootPanel.get() to get the entire body element
    RootPanel.get("queryFieldContainer").add(queryField);
    RootPanel.get("errorLabelContainer").add(errorLabel);

    // Focus the cursor on the name field when the app loads
    queryField.setFocus(true);
    queryField.selectAll();

    // Create the results table.
    resultsTable0 = new PagingTableListView2<StockQuote>(listModel1, 10);
    resultsTable0.addColumn(tickerColumn);
    resultsTable0.addColumn(nameColumn);
    resultsTable0.addColumn(priceColumn);
    resultsTable0.addColumn(notesColumn);

    notesColumn.setMutator(new Mutator<StockQuote, String>() {
      public void mutate(StockQuote object, String after) {
        Window.alert("mutating " + object.getTicker() + " (" + after + ")");
      }
    });

    RootPanel.get().add(resultsTable0);

    // Add a handler to send the name to the server
    queryField.addKeyUpHandler(new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        sendQueryToServer();
      }
    });

    sendQueryToServer();
  }

  /**
   * Send the query to the server.
   */
  private void sendQueryToServer() {
    String ticker = queryField.getText();
    if (ticker.length() > 0) {
      errorLabel.setText("");
      appCache.query(ticker);
    }
  }
}
