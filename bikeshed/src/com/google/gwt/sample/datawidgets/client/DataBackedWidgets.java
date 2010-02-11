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
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

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

  private Column<StockQuote, String> notesColumn = new Column<StockQuote, String>(new TextInputCell()) {
    @Override
    protected String getValue(StockQuote object) {
      return object.getNotes();
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

  /**
   * A set of user-marked 'favorite' ticker symbols.
   */
  private HashSet<String> favoritesByTicker = new HashSet<String>();

  private AsyncListModel<StockQuote> favoritesListModel;

  /**
   * User supplied notes, indexed by ticker symbol.
   */
  private HashMap<String,String> notesByTicker = new HashMap<String,String>();

  private PagingTableListView<StockQuote> resultsTable0;

  private AsyncListModel<StockQuote> searchListModel;

  private String searchQuery;

  /**
   * The timer used to update the stock quotes.
   */
  private Timer updateTimer = new Timer() {
    @Override
    public void run() {
      update();
    }
  };

  private Range[] searchRanges;

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
        sendSearchRequest(searchListModel.getRanges());
      }
    });
    
    favoritesListModel = new AsyncListModel<StockQuote>(new DataSource<StockQuote>() {
      public void requestData(AsyncListModel<StockQuote> listModel) {
        sendFavoritesRequest(favoritesListModel.getRanges());
      }
    });

    // Create the results table.
    resultsTable0 = new PagingTableListView<StockQuote>(searchListModel, 10);
    resultsTable0.addColumn(favoriteColumn);
    resultsTable0.addColumn(tickerColumn);
    resultsTable0.addColumn(nameColumn);
    resultsTable0.addColumn(priceColumn);
    resultsTable0.addColumn(notesColumn);
    
    favoriteColumn.setMutator(new Mutator<StockQuote, Boolean>() {
      public void mutate(StockQuote object, Boolean after) {
        setFavorite(object.getTicker(), after);
      }
    });

    notesColumn.setMutator(new Mutator<StockQuote, String>() {
      public void mutate(StockQuote object, String after) {
        setNotes(object.getTicker(), after);
      }
    });

    RootPanel.get().add(resultsTable0);

    // Add a handler to send the name to the server
    queryField.addKeyUpHandler(new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        setSearchQuery();
      }
    });

    setSearchQuery();
  }
  
  /**
   * Set or unset a ticker symbol as a 'favorite.'
   *  
   * @param ticker the ticker symbol
   * @param favorite if true, make the stock a favorite
   */
  public void setFavorite(String ticker, boolean favorite) {
    if (favorite) {
      favoritesByTicker.add(ticker);
    } else {
      favoritesByTicker.remove(ticker);
    }
  }

  /**
   * Set or unset a note on a ticker symbol.
   * 
   * @param ticker the ticker symbol
   * @param note a note to associate with the stock, or null
   */
  public void setNotes(String ticker, String note) {
    if (note == null || note.length() == 0) {
      notesByTicker.remove(ticker);
    } else {
      notesByTicker.put(ticker, note);
    }
  }
  
  /**
   * Request data from the server using the last query string.
   */
  public void update() {
    if (searchQuery == null || searchQuery.length() < 1) {
      return;
    }

    sendSearchRequest(searchListModel.getRanges());
  }
  
  @SuppressWarnings("unused")
  private void sendFavoritesRequest(Range[] ranges) {
    sendRequest();
  }
  
  private void sendSearchRequest(Range[] ranges) {
    if (searchQuery == null) {
      return;
    }
    searchRanges = ranges;
    sendRequest();
  }
  
  private void sendRequest() {
    for (Range range : searchRanges) {
      List<StockRequest> requests = new ArrayList<StockRequest>();
      requests.add(new StockRequest(searchQuery, range));
      
      dataService.getStockQuotes(requests, new AsyncCallback<List<StockResponse>>() {
        public void onFailure(Throwable caught) {
          Window.alert("ERROR: " + caught.getMessage());
          updateTimer.schedule(UPDATE_DELAY);
        }

        public void onSuccess(List<StockResponse> responses) {
          
          for (StockResponse response : responses) {
            StockQuoteList stocks = response.getStocks();
            // Refresh 'notes' and 'favorite' fields
            // TODO (rice) keep this info on the server
            setTransientData(stocks);
            
            if (response instanceof StockResponse.Search) {
              searchListModel.updateDataSize(((StockResponse.Search) response).getNumRows(), true);
              searchListModel.updateViewData(stocks.getStartIndex(), stocks.size(), stocks);
            } else if (response instanceof StockResponse.Favorites) {
              // TODO (rice) implement
            } else {
              throw new RuntimeException("Unknown response type: " + response.getClass().getName());
            }
          }
          updateTimer.schedule(UPDATE_DELAY);
        }
      });
    }
  }

  /**
   * Send the query to the server.
   */
  private void setSearchQuery() {
    String query = queryField.getText();
    if (query.length() > 0) {
      errorLabel.setText("");
      this.searchQuery = query;
      update();
    }
  }

  private void setTransientData(StockQuoteList stocks) {
    for (StockQuote quote : stocks) {
      String ticker = quote.getTicker();

      // Set notes
      String notes = notesByTicker.get(ticker);
      if (notes != null) {
        quote.setNotes(notes);
      }

      // Set 'favorite' status
      quote.setFavorite(favoritesByTicker.contains(ticker));
    }
  }
}
