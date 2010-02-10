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

import com.google.gwt.core.client.GWT;
import com.google.gwt.list.shared.AsyncListModel;
import com.google.gwt.list.shared.Range;
import com.google.gwt.list.shared.AsyncListModel.DataSource;
import com.google.gwt.sample.datawidgets.shared.StockQuote;
import com.google.gwt.sample.datawidgets.shared.StockQuoteList;
import com.google.gwt.sample.datawidgets.shared.StockResponse;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * The application level cache used by this app.
 */
public class ApplicationCache implements DataSource<StockQuote> {

  /**
   * The delay between updates in milliseconds.
   */
  private static final int UPDATE_DELAY = 5000;

  /**
   * The list models used in this application.
   */
  private List<AsyncListModel<StockQuote>> asyncListModels = new ArrayList<AsyncListModel<StockQuote>>();

  /**
   * The {@link StockService} used to retrieve data.
   */
  private final StockServiceAsync dataService = GWT.create(StockService.class);

  /**
   * User supplied notes, indexed by ticker symbol.
   */
  private HashMap<String,String> notesByTicker = new HashMap<String,String>();

  /**
   * The current query string.
   */
  private String query;

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
   * A set of user-marked 'favorite' ticker symbols.
   */
  private HashSet<String> favoritesByTicker = new HashSet<String>();

  /**
   * Subscribe a list model to this cache.
   * 
   * @param listModel the list model to subscribe
   */
  public void addAsyncListModel(AsyncListModel<StockQuote> listModel) {
    asyncListModels.add(listModel);
    // (TODO): Need to update the new listModel
  }

  /**
   * Request data from the server.
   * 
   * @param query the query string
   */
  public void query(String query) {
    this.query = query;
    update();
  }

  public void requestData(AsyncListModel<StockQuote> listModel) {
    sendRequest(listModel.getRanges());
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
    if (query == null || query.length() < 1) {
      return;
    }

    List<Range> ranges = new ArrayList<Range>();
    for (AsyncListModel<StockQuote> listModel : asyncListModels) {
      Range[] curRanges = listModel.getRanges();
      for (Range range : curRanges) {
        ranges.add(range);
      }
    }
    sendRequest(ranges.toArray(new Range[ranges.size()]));
  }

  private void sendRequest(Range[] ranges) {
    if (query == null) {
      return;
    }
    dataService.getStockQuotes(query, ranges,
        new AsyncCallback<StockResponse>() {
      public void onFailure(Throwable caught) {
        Window.alert("ERROR: " + caught.getMessage());
        updateTimer.schedule(UPDATE_DELAY);
      }

      public void onSuccess(StockResponse result) {
        setTransientData(result);

        for (AsyncListModel<StockQuote> listModel : asyncListModels) {
          listModel.updateDataSize(result.getNumRows(), true);
          for (StockQuoteList list : result.getLists()) {
            listModel.updateViewData(list.getStartIndex(), list.size(),
                list);
          }
        }
        updateTimer.schedule(UPDATE_DELAY);
      }
    });
  }

  private void setTransientData(StockResponse result) {
    for (StockQuoteList list : result.getLists()) {
      for (StockQuote quote : list) {
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
}
