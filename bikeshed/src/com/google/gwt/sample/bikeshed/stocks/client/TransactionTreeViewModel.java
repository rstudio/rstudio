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

import com.google.gwt.bikeshed.cells.client.ButtonCell;
import com.google.gwt.bikeshed.cells.client.Cell;
import com.google.gwt.bikeshed.cells.client.TextCell;
import com.google.gwt.bikeshed.cells.client.ValueUpdater;
import com.google.gwt.bikeshed.list.client.ListView;
import com.google.gwt.bikeshed.list.shared.AbstractListViewAdapter;
import com.google.gwt.bikeshed.list.shared.AsyncListViewAdapter;
import com.google.gwt.bikeshed.list.shared.ListViewAdapter;
import com.google.gwt.bikeshed.list.shared.ProvidesKey;
import com.google.gwt.bikeshed.list.shared.SingleSelectionModel;
import com.google.gwt.bikeshed.tree.client.TreeViewModel;
import com.google.gwt.sample.bikeshed.stocks.shared.StockQuote;
import com.google.gwt.sample.bikeshed.stocks.shared.Transaction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A TreeViewModel for a tree with a hidden root node of null, a first level
 * containing ticker symbol Strings, and a second level containing Transactions.
 */
class TransactionTreeViewModel implements TreeViewModel {

  class SectorListViewAdapter extends AsyncListViewAdapter<StockQuote> {

    String sector;

    public SectorListViewAdapter(String sector) {
      this.sector = sector;
      setKeyProvider(StockQuote.KEY_PROVIDER);
    }

    public String getSector() {
      return sector;
    }

    @Override
    protected void onRangeChanged(ListView<StockQuote> view) {
      updater.update();
    }
  }

  /**
   * A {@link Cell} used to render a {@link StockQuote}.
   */
  private static final Cell<StockQuote, Void> STOCK_QUOTE_CELL = new Cell<StockQuote, Void>() {
    @Override
    public void render(StockQuote value, Void viewData, StringBuilder sb) {
      sb.append(value.getTicker() + " - " + value.getDisplayPrice());
    }
  };

  /**
   * A {@link Cell} used to render a {@link Transaction}.
   */
  private static final Cell<Transaction, Void> TRANSACTION_CELL = new Cell<Transaction, Void>() {
    @Override
    public void render(Transaction value, Void viewData, StringBuilder sb) {
      sb.append(value.toString());
    }
  };

  /**
   * The last {@link StockQuote} that was opened.
   */
  private StockQuote lastStockQuote;

  /**
   * The last Sector that was opened.
   */
  private String lastSector;

  private Map<String, SectorListViewAdapter> sectorListViewAdapters = new HashMap<String, SectorListViewAdapter>();
  private AbstractListViewAdapter<StockQuote> stockQuoteListViewAdapter;
  private ListViewAdapter<String> topLevelListViewAdapter = new ListViewAdapter<String>();
  private SingleSelectionModel<Object> selectionModel = new SingleSelectionModel<Object>();

  private Map<String, ListViewAdapter<Transaction>> transactionListViewAdaptersByTicker;

  private Updater updater;

  public TransactionTreeViewModel(
      Updater updater,
      AbstractListViewAdapter<StockQuote> stockQuoteListViewAdapter,
      Map<String, ListViewAdapter<Transaction>> transactionListViewAdaptersByTicker) {
    this.selectionModel.setKeyProvider(new ProvidesKey<Object>() {
      public Object getKey(Object item) {
        if (item instanceof StockQuote) {
          return StockQuote.KEY_PROVIDER.getKey((StockQuote) item);
        } else {
          return item;
        }
      }
    });
    this.updater = updater;
    this.stockQuoteListViewAdapter = stockQuoteListViewAdapter;
    
    // Setup the sector list.
    List<String> topLevelList = topLevelListViewAdapter.getList();
    topLevelList.add("Favorites");
    topLevelList.add("Dow Jones Industrials");
    topLevelList.add("S&P 500");
    this.transactionListViewAdaptersByTicker = transactionListViewAdaptersByTicker;
  }

  public <T> NodeInfo<?> getNodeInfo(T value) {
    if (value == null) {
      // Return list of sectors.
      return new TreeViewModel.DefaultNodeInfo<String>(topLevelListViewAdapter,
          TextCell.getInstance(), selectionModel, null);
    } else if ("Favorites".equals(value)) {
      // Return favorites. 
      return new TreeViewModel.DefaultNodeInfo<StockQuote>(
          stockQuoteListViewAdapter, STOCK_QUOTE_CELL, selectionModel, null);
    } else if ("History".equals(value)) {
      // Return history of the current stock quote.
      String ticker = lastStockQuote.getTicker();
      ListViewAdapter<Transaction> adapter = transactionListViewAdaptersByTicker.get(ticker);
      if (adapter == null) {
        adapter = new ListViewAdapter<Transaction>();
        transactionListViewAdaptersByTicker.put(ticker, adapter);
      }
      return new TreeViewModel.DefaultNodeInfo<Transaction>(adapter,
          TRANSACTION_CELL, selectionModel, null);
    } else if ("Actions".equals(value)) {
      // Return the actions for the current stock quote.
      ListViewAdapter<String> adapter = new ListViewAdapter<String>();
      List<String> list = adapter.getList();
      list.add("Buy");
      list.add("Sell");
      return new TreeViewModel.DefaultNodeInfo<String>(adapter,
          ButtonCell.getInstance(), selectionModel,
          new ValueUpdater<String, Void>() {
            public void update(String value, Void viewData) {
              if ("Buy".equals(value)) {
                updater.buy(lastStockQuote);
              } else {
                updater.sell(lastStockQuote);
              }
            }
          });
    } else if (value instanceof String) {
      // Return the stocks for a given sector.
      lastSector = (String) value;
      SectorListViewAdapter adapter = new SectorListViewAdapter(lastSector);
      sectorListViewAdapters.put(lastSector, adapter);
      return new TreeViewModel.DefaultNodeInfo<StockQuote>(adapter,
          STOCK_QUOTE_CELL, selectionModel, null);
    } else if (value instanceof StockQuote) {
      // Return the submenu for a stock quote.
      lastStockQuote = (StockQuote) value;
      ListViewAdapter<String> adapter = new ListViewAdapter<String>();
      List<String> list = adapter.getList();
      list.add("Actions");
      list.add("History");
      return new TreeViewModel.DefaultNodeInfo<String>(adapter,
          TextCell.getInstance(), selectionModel, null);
    }

    throw new IllegalArgumentException(value.toString());
  }

  /**
   * Get the {@link SectorListViewAdapter} for the last selected sector.
   * 
   * @return the {@link SectorListViewAdapter}
   */
  public SectorListViewAdapter getSectorListViewAdapter() {
    return lastSector == null ? null : sectorListViewAdapters.get(lastSector);
  }

  public boolean isLeaf(Object value) {
    if (value instanceof Transaction || "Buy".equals(value)
        || "Sell".equals(value)) {
      return true;
    }

    return false;
  }
}
