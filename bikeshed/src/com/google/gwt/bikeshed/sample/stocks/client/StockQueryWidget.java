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
package com.google.gwt.bikeshed.sample.stocks.client;

import com.google.gwt.bikeshed.list.client.PagingTableListView;
import com.google.gwt.bikeshed.list.shared.ListModel;
import com.google.gwt.bikeshed.sample.stocks.shared.StockQuote;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextBox;

/**
 * A widget containing a search box and a results table.
 */
public class StockQueryWidget extends Composite {

  private final TextBox queryField = new TextBox();
  private PagingTableListView<StockQuote> resultsTable;

  public StockQueryWidget(ListModel<StockQuote> searchListModel, final Updater updater) { 
    // Create the results table.
    resultsTable = new PagingTableListView<StockQuote>(searchListModel, 10);
    resultsTable.addColumn(Columns.favoriteColumn);
    resultsTable.addColumn(Columns.tickerColumn);
    resultsTable.addColumn(Columns.nameColumn);
    resultsTable.addColumn(Columns.priceColumn);
    resultsTable.addColumn(Columns.buyColumn);
   
    // Focus the cursor on the name field when the app loads
    queryField.setFocus(true);
    queryField.selectAll();
    queryField.setText("G");
    
    // Add a handler to send the name to the server
    queryField.addKeyUpHandler(new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        updater.update();
      }
    });
    
    DockLayoutPanel layoutPanel = new DockLayoutPanel(Unit.EM);
    
    HorizontalPanel panel = new HorizontalPanel();
    panel.add(new Label("Enter query: "));
    panel.add(queryField);
    layoutPanel.addNorth(panel, 2.0);
    layoutPanel.add(new ScrollPanel(resultsTable));
    
    initWidget(layoutPanel);
  }
  
  public String getSearchQuery() {
    return queryField.getText();
  }
}
