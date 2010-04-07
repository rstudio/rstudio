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
import com.google.gwt.bikeshed.list.shared.AsyncListModel;
import com.google.gwt.bikeshed.list.shared.ListRegistration;
import com.google.gwt.bikeshed.list.shared.Range;
import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.sample.bikeshed.stocks.shared.StockQuote;
import com.google.gwt.sample.bikeshed.stocks.shared.StockQuoteList;
import com.google.gwt.sample.bikeshed.stocks.shared.StockRequest;
import com.google.gwt.sample.bikeshed.stocks.shared.StockResponse;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Mobile client for the stocks demo.
 */
public class StocksMobile {

  interface Binder extends UiBinder<Widget, StocksMobile> {  
  }
  
  private static final Binder binder = GWT.create(Binder.class);

  /**
   * The delay between updates in milliseconds.
   */
  private static final int UPDATE_DELAY = 5000;

  static String getFormattedPrice(int price) {
    return NumberFormat.getCurrencyFormat("USD").format(price / 100.0);
  }

  @UiField PagingTableListView<StockQuote> listView;
  private final StockServiceAsync dataService = GWT.create(StockService.class);
  private AsyncListModel<StockQuote> favoritesListModel;

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
    // Create the various models. Do this before binding the UI, because some
    // of the UiFactories need the models to instantiate their widgets.
    favoritesListModel = new AsyncListModel<StockQuote>() {
      @Override
      protected void onRangeChanged(ListRegistration reg, int start, int length) {
        update();
      }
    };
    favoritesListModel.setKeyProvider(StockQuote.KEY_PROVIDER);

    // Now create the UI.
    RootPanel.get().add(binder.createAndBindUi(this));
    update();
  }

  /**
   * Process the {@link StockResponse} from the server.
   * 
   * @param response the stock response
   */
  public void processStockResponse(StockResponse response) {
    // Update the favorites list.
    updateFavorites(response);

    // Restart the update timer.
    updateTimer.schedule(UPDATE_DELAY);
  }

  public void update() {
    Range[] favoritesRanges = favoritesListModel.getRanges();

    StockRequest request = new StockRequest("TODO", null, null,
        favoritesRanges[0], null);

    dataService.getStockQuotes(request, new AsyncCallback<StockResponse>() {
      public void onFailure(Throwable caught) {
        if (handleRpcError(caught, null)) {
          updateTimer.schedule(UPDATE_DELAY);
        }
      }

      public void onSuccess(StockResponse result) {
        processStockResponse(result);
      }
    });
  }

  @UiFactory
  PagingTableListView<StockQuote> createFavoritesWidget() {
    PagingTableListView<StockQuote> favorite = new PagingTableListView<StockQuote>(
        favoritesListModel, 10);

    favorite.addColumn(Columns.tickerColumn, "ticker");
    favorite.addColumn(Columns.priceColumn, "price");
    favorite.addColumn(Columns.changeColumn, "change");
    favorite.addColumn(Columns.sharesColumn, "shares");
    favorite.addColumn(Columns.dollarsColumn, "value");
    favorite.addColumn(Columns.profitLossColumn, "profit/loss");

    return favorite;
  }

  /**
   * Display a message to the user when an RPC call fails.
   * 
   * @param caught the exception
   * @param displayMessage the message to display to the user, or null to
   *          display a default message
   * @return true if recoverable, false if not
   */
  private boolean handleRpcError(Throwable caught, String displayMessage) {
    String message = caught.getMessage();
    if (message.contains("Not logged in")) {
      // Force the user to login.
      Window.Location.reload();
      return false;
    }

    if (displayMessage == null) {
      displayMessage = "ERROR: " + caught.getMessage();
    }
    Window.alert(displayMessage);
    return true;
  }

  private void updateFavorites(StockResponse response) {
    // Update the favorites list.
    StockQuoteList favorites = response.getFavorites();
    favoritesListModel.updateDataSize(response.getNumFavorites(), true);
    favoritesListModel.updateViewData(favorites.getStartIndex(),
        favorites.size(), favorites);
  }
}
