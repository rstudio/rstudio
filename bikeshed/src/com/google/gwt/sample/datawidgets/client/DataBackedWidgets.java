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

import com.google.gwt.cells.client.CheckboxCell;
import com.google.gwt.cells.client.CurrencyCell;
import com.google.gwt.cells.client.Mutator;
import com.google.gwt.cells.client.TextCell;
import com.google.gwt.cells.client.TextInputCell;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.list.client.Column;
import com.google.gwt.list.client.PagingTableListView;
import com.google.gwt.list.shared.AsyncListModel;
import com.google.gwt.list.shared.Range;
import com.google.gwt.list.shared.AsyncListModel.DataSource;
import com.google.gwt.sample.datawidgets.shared.StockQuote;
import com.google.gwt.sample.datawidgets.shared.StockQuoteList;
import com.google.gwt.sample.datawidgets.shared.StockRequest;
import com.google.gwt.sample.datawidgets.shared.StockResponse;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;

import java.util.HashMap;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class DataBackedWidgets implements EntryPoint {
  
  /**
   * The delay between updates in milliseconds.
   */
  private static final int UPDATE_DELAY = 5000;
  
  /**
   * The {@link StockService} used to retrieve data.
   */
  private final StockServiceAsync dataService = GWT.create(StockService.class);

  private final Label errorLabel = new Label();
  
  private Column<StockQuote, Boolean> favoriteColumn = new Column<StockQuote, Boolean>(
      new CheckboxCell()) {
    @Override
    protected Boolean getValue(StockQuote object) {
      return object.isFavorite();
    }
  };
  
  private Column<StockQuote, String> nameColumn = new Column<StockQuote, String>(
      new TextCell()) {
    @Override
    protected String getValue(StockQuote object) {
      return object.getName();
    }
  };

  private Column<StockQuote, Integer> priceColumn = new Column<StockQuote, Integer>(
      new CurrencyCell()) {
    @Override
    protected Integer getValue(StockQuote object) {
      return object.getPrice();
    }
  };

  private final TextBox queryField = new TextBox();

  private Column<StockQuote, String> tickerColumn = new Column<StockQuote, String>(
      new TextCell()) {
    @Override
    protected String getValue(StockQuote object) {
      return object.getTicker();
    }
  };

  private Column<StockQuote, String> sharesColumn = new Column<StockQuote, String>(
      new TextCell()) {
    @Override
    protected String getValue(StockQuote object) {
      return "" + object.getSharesOwned();
    }
  };

  private AsyncListModel<StockQuote> favoritesListModel;

  /**
   * User supplied notes, indexed by ticker symbol.
   */
  private HashMap<String,String> notesByTicker = new HashMap<String,String>();

  private PagingTableListView<StockQuote> resultsTable;

  private AsyncListModel<StockQuote> searchListModel;

  /**
   * The timer used to update the stock quotes.
   */
  private Timer updateTimer = new Timer() {
    @Override
    public void run() {
      update();
    }
  };

  private PagingTableListView<StockQuote> favoritesTable;

  /**
   * This is the entry point method.
   */
  public void onModuleLoad() {
    queryField.setText("G");

    // Add the nameField and sendButton to the RootPanel
    // Use RootPanel.get() to get the entire body element
    RootPanel.get("queryFieldContainer").add(queryField);
    RootPanel.get("errorLabelContainer").add(errorLabel);

    // Focus the cursor on the name field when the app loads
    queryField.setFocus(true);
    queryField.selectAll();
    
    // Create the list models
    searchListModel = new AsyncListModel<StockQuote>(new DataSource<StockQuote>() {
      public void requestData(AsyncListModel<StockQuote> listModel) {
        update();
      }
    });
    
    favoritesListModel = new AsyncListModel<StockQuote>(new DataSource<StockQuote>() {
      public void requestData(AsyncListModel<StockQuote> listModel) {
        update();
      }
    });

    // Create the results table.
    resultsTable = new PagingTableListView<StockQuote>(searchListModel, 10);
    resultsTable.addColumn(favoriteColumn);
    resultsTable.addColumn(tickerColumn);
    resultsTable.addColumn(nameColumn);
    resultsTable.addColumn(priceColumn);
    
    favoritesTable = new PagingTableListView<StockQuote>(favoritesListModel, 10);
    favoritesTable.addColumn(tickerColumn);
    favoritesTable.addColumn(priceColumn);
    favoritesTable.addColumn(sharesColumn);
    
    favoriteColumn.setMutator(new Mutator<StockQuote, Boolean>() {
      public void mutate(StockQuote object, Boolean after) {
        setFavorite(object.getTicker(), after);
      }
    });

    RootPanel.get().add(resultsTable);
    RootPanel.get().add(new HTML("<hr>"));
    RootPanel.get().add(favoritesTable);

    // Add a handler to send the name to the server
    queryField.addKeyUpHandler(new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        update();
      }
    });

    update();
  }
  
  /**
   * Set or unset a ticker symbol as a 'favorite.'
   *  
   * @param ticker the ticker symbol
   * @param favorite if true, make the stock a favorite
   */
  public void setFavorite(String ticker, boolean favorite) {
    if (favorite) {
      dataService.addFavorite(ticker, new AsyncCallback<Void>() {
        public void onFailure(Throwable caught) {
          Window.alert("Error adding favorite");
        }

        public void onSuccess(Void result) {
          // do nothing
        }
      });
    } else {
      dataService.removeFavorite(ticker, new AsyncCallback<Void>() {
        public void onFailure(Throwable caught) {
          Window.alert("Error removing favorite");
        }

        public void onSuccess(Void result) {
          // do nothing
        }
      });
    }
  }
  
  /**
   * Request data from the server using the last query string.
   */
  private void update() {
    updateTimer.cancel();
    
    Range[] searchRanges = searchListModel.getRanges();
    Range[] favoritesRanges = favoritesListModel.getRanges();
    
    if (searchRanges == null || searchRanges.length == 0 || favoritesRanges == null || favoritesRanges.length == 0) {
      return;
    }
    
    String searchQuery = queryField.getText();
    StockRequest request = new StockRequest(searchQuery, searchRanges[0], favoritesRanges[0]);
    dataService.getStockQuotes(request, new AsyncCallback<StockResponse>() {
      public void onFailure(Throwable caught) {
        Window.alert("ERROR: " + caught.getMessage());
        updateTimer.schedule(UPDATE_DELAY);
      }

      public void onSuccess(StockResponse result) {
        StockQuoteList searchResults = result.getSearchResults();
        
        searchListModel.updateDataSize(result.getNumSearchResults(), true);
        searchListModel.updateViewData(searchResults.getStartIndex(), searchResults.size(), searchResults);        

        StockQuoteList favorites = result.getFavorites();
        
        favoritesListModel.updateDataSize(result.getNumFavorites(), true);
        favoritesListModel.updateViewData(favorites.getStartIndex(), favorites.size(), favorites);
       
        updateTimer.schedule(UPDATE_DELAY);
      }
    });
  }
}
