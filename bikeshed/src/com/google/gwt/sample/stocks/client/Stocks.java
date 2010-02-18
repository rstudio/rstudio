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
package com.google.gwt.sample.stocks.client;

import com.google.gwt.cells.client.ButtonCell;
import com.google.gwt.cells.client.CheckboxCell;
import com.google.gwt.cells.client.CurrencyCell;
import com.google.gwt.cells.client.FieldUpdater;
import com.google.gwt.cells.client.TextCell;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.list.client.Column;
import com.google.gwt.list.client.PagingTableListView;
import com.google.gwt.list.shared.AsyncListModel;
import com.google.gwt.list.shared.ListListModel;
import com.google.gwt.list.shared.Range;
import com.google.gwt.list.shared.AsyncListModel.DataSource;
import com.google.gwt.sample.stocks.shared.StockQuote;
import com.google.gwt.sample.stocks.shared.StockQuoteList;
import com.google.gwt.sample.stocks.shared.StockRequest;
import com.google.gwt.sample.stocks.shared.StockResponse;
import com.google.gwt.sample.stocks.shared.Transaction;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;

import java.util.List;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Stocks implements EntryPoint {

  /**
   * The delay between updates in milliseconds.
   */
  private static final int UPDATE_DELAY = 5000;

  private Column<StockQuote, String> buyColumn = new Column<StockQuote, String>(
      new ButtonCell()) {
    @Override
    protected String getValue(StockQuote object) {
      return "Buy";
    }
  };

  /**
   * The popup used to purchase stock.
   */
  private BuySellPopup buySellPopup = new BuySellPopup();

  private final Label cashLabel = new Label();

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

  private AsyncListModel<StockQuote> favoritesListModel;

  private PagingTableListView<StockQuote> favoritesTable;
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
  
  private PagingTableListView<StockQuote> resultsTable;

  private AsyncListModel<StockQuote> searchListModel;
  
  private Column<StockQuote, String> sellColumn = new Column<StockQuote, String>(
      new ButtonCell()) {
    @Override
    protected String getValue(StockQuote object) {
      return "Sell";
    }
  };

  private Column<StockQuote, String> sharesColumn = new Column<StockQuote, String>(
      new TextCell()) {
    @Override
    protected String getValue(StockQuote object) {
      return "" + object.getSharesOwned();
    }
  };

  private Column<Transaction, String> subtotalColumn = new Column<Transaction, String>(
      new TextCell()) {
    @Override
    protected String getValue(Transaction object) {
      int price = object.getActualPrice() * object.getQuantity();
      return (object.isBuy() ? " (" : " ") + getFormattedPrice(price) + 
          (object.isBuy() ? ")" : "");
    }
  };

  private Column<StockQuote, String> tickerColumn = new Column<StockQuote, String>(
      new TextCell()) {
    @Override
    protected String getValue(StockQuote object) {
      return object.getTicker();
    }
  };

  private Column<Transaction, String> transactionColumn = new Column<Transaction, String>(
      new TextCell()) {
    @Override
    protected String getValue(Transaction object) {
      return object.toString();
    }
  };

  private ListListModel<Transaction> transactionListModel;
  
  private List<Transaction> transactions;

  private PagingTableListView<Transaction> transactionTable;

  /**
   * The timer used to update the stock quotes.
   */
  private Timer updateTimer = new Timer() {
    @Override
    public void run() {
      update();
    }
  };

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
    searchListModel = new AsyncListModel<StockQuote>(
        new DataSource<StockQuote>() {
          public void requestData(AsyncListModel<StockQuote> listModel) {
            update();
          }
        });

    favoritesListModel = new AsyncListModel<StockQuote>(
        new DataSource<StockQuote>() {
          public void requestData(AsyncListModel<StockQuote> listModel) {
            update();
          }
        });
    
    transactionListModel = new ListListModel<Transaction>();
    transactions = transactionListModel.getList();

    // Create the results table.
    resultsTable = new PagingTableListView<StockQuote>(searchListModel, 10);
    resultsTable.addColumn(favoriteColumn);
    resultsTable.addColumn(tickerColumn);
    resultsTable.addColumn(nameColumn);
    resultsTable.addColumn(priceColumn);
    resultsTable.addColumn(buyColumn);

    favoritesTable = new PagingTableListView<StockQuote>(favoritesListModel, 10);
    favoritesTable.addColumn(tickerColumn);
    favoritesTable.addColumn(priceColumn);
    favoritesTable.addColumn(sharesColumn);
    favoritesTable.addColumn(buyColumn);
    favoritesTable.addColumn(sellColumn);
    
    transactionTable = new PagingTableListView<Transaction>(transactionListModel, 10);
    transactionTable.addColumn(transactionColumn);
    transactionTable.addColumn(subtotalColumn);

    favoriteColumn.setFieldUpdater(new FieldUpdater<StockQuote, Boolean>() {
      public void update(StockQuote object, Boolean value) {
        setFavorite(object.getTicker(), value);
      }
    });

    buyColumn.setFieldUpdater(new FieldUpdater<StockQuote, String>() {
      public void update(StockQuote object, String value) {
        buySellPopup.setStockQuote(object, true);
        buySellPopup.center();
      }
    });

    sellColumn.setFieldUpdater(new FieldUpdater<StockQuote, String>() {
      public void update(StockQuote object, String value) {
        buySellPopup.setStockQuote(object, false);
        buySellPopup.center();
      }
    });

    buySellPopup.addCloseHandler(new CloseHandler<PopupPanel>() {
      public void onClose(CloseEvent<PopupPanel> event) {
        Transaction t = buySellPopup.getTransaction();
        if (t != null) {
          dataService.transact(t, new AsyncCallback<Transaction>() {
            public void onFailure(Throwable caught) {
              Window.alert("Error: " + caught.getMessage());
            }

            public void onSuccess(Transaction result) {
              transactions.add(0, result);
              update();
            }
          });
        }
      }
    });

    // Add components to the page.
    HorizontalPanel hPanel = new HorizontalPanel();
    hPanel.add(new HTML("<b>Available cash:</b>"));
    hPanel.add(cashLabel);
    RootPanel.get().add(hPanel);

    RootPanel.get().add(resultsTable);
    RootPanel.get().add(new HTML("<hr>"));
    RootPanel.get().add(favoritesTable);
    RootPanel.get().add(new HTML("<hr>"));
    RootPanel.get().add(transactionTable);

    // Add a handler to send the name to the server
    queryField.addKeyUpHandler(new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        update();
      }
    });

    update();
  }

  /**
   * Set or unset a ticker symbol as a 'favorite'.
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

  private String getFormattedPrice(int price) {
    return NumberFormat.getCurrencyFormat("USD").format((double) price / 100.0);
  }
  
  /**
   * Process the {@link StockResponse} from the server.
   *
   * @param response the stock response
   */
  private void processStockResponse(StockResponse response) {
    // Update the search list.
    StockQuoteList searchResults = response.getSearchResults();
    searchListModel.updateDataSize(response.getNumSearchResults(), true);
    searchListModel.updateViewData(searchResults.getStartIndex(),
        searchResults.size(), searchResults);

    // Update the favorites list.
    StockQuoteList favorites = response.getFavorites();
    favoritesListModel.updateDataSize(response.getNumFavorites(), true);
    favoritesListModel.updateViewData(favorites.getStartIndex(),
        favorites.size(), favorites);

    // Update available cash.
    int cash = response.getCash();
    cashLabel.setText(getFormattedPrice(cash));
    buySellPopup.setAvailableCash(cash);

    // Restart the update timer.
    updateTimer.schedule(UPDATE_DELAY);
  }

  /**
   * Request data from the server using the last query string.
   */
  private void update() {
    updateTimer.cancel();

    Range[] searchRanges = searchListModel.getRanges();
    Range[] favoritesRanges = favoritesListModel.getRanges();

    if (searchRanges == null || searchRanges.length == 0
        || favoritesRanges == null || favoritesRanges.length == 0) {
      return;
    }

    String searchQuery = queryField.getText();
    StockRequest request = new StockRequest(searchQuery, searchRanges[0],
        favoritesRanges[0]);
    dataService.getStockQuotes(request, new AsyncCallback<StockResponse>() {
      public void onFailure(Throwable caught) {
        String message = caught.getMessage();
        if (message.contains("Not logged in")) {
          // Force the user to login.
          Window.Location.reload();
        } else {
          Window.alert("ERROR: " + caught.getMessage());
          updateTimer.schedule(UPDATE_DELAY);
        }
      }

      public void onSuccess(StockResponse result) {
        processStockResponse(result);
      }
    });
  }
}
