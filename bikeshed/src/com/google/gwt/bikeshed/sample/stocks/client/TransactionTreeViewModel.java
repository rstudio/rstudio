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

import com.google.gwt.bikeshed.cells.client.ButtonCell;
import com.google.gwt.bikeshed.cells.client.Cell;
import com.google.gwt.bikeshed.cells.client.TextCell;
import com.google.gwt.bikeshed.cells.client.ValueUpdater;
import com.google.gwt.bikeshed.list.shared.AsyncListModel;
import com.google.gwt.bikeshed.list.shared.ListListModel;
import com.google.gwt.bikeshed.list.shared.ListModel;
import com.google.gwt.bikeshed.sample.stocks.shared.StockQuote;
import com.google.gwt.bikeshed.sample.stocks.shared.Transaction;
import com.google.gwt.bikeshed.tree.client.TreeNode;
import com.google.gwt.bikeshed.tree.client.TreeViewModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A TreeViewModel for a tree with a hidden root node of null,
 * a first level containing ticker symbol Strings, and a second
 * level containing Transactions.
 */
class TransactionTreeViewModel implements TreeViewModel {

  class SectorListModel extends AsyncListModel<StockQuote> {

    String sector;

    public SectorListModel(final Updater updater, String sector) {
      super(new DataSource<StockQuote>() {
        public void requestData(AsyncListModel<StockQuote> listModel) {
          updater.update();
        }
      });
      this.sector = sector;
    }

    public String getSector() {
      return sector;
    }
  }

  static class TransactionCell extends Cell<Transaction> {
    @Override
    public void render(Transaction value, StringBuilder sb) {
      sb.append(value.toString());
    }
  }

  private static final Cell<StockQuote> STOCK_QUOTE_CELL = new Cell<StockQuote>() {
    @Override
    public void render(StockQuote value, StringBuilder sb) {
      sb.append(value.getTicker() + " - " + value.getDisplayPrice());
    }
  };

  private static final Cell<Transaction> TRANSACTION_CELL =
    new TransactionCell();

  private Map<String, SectorListModel> sectorListModels = new HashMap<String, SectorListModel>();
  private ListModel<StockQuote> stockQuoteListModel;
  private ListListModel<String> topLevelListListModel =
    new ListListModel<String>();

  private Map<String, ListListModel<Transaction>> transactionListListModelsByTicker;

  private Updater updater;

  public TransactionTreeViewModel(Updater updater, ListModel<StockQuote> stockQuoteListModel,
      Map<String, ListListModel<Transaction>> transactionListListModelsByTicker) {
    this.updater = updater;
    this.stockQuoteListModel = stockQuoteListModel;
    List<String> topLevelList = topLevelListListModel.getList();
    topLevelList.add("Favorites");
    topLevelList.add("Dow Jones Industrials");
    topLevelList.add("S&P 500");
    this.transactionListListModelsByTicker = transactionListListModelsByTicker;
  }
  
  public <T> NodeInfo<?> getNodeInfo(T value, final TreeNode<T> treeNode) {
    if (value == null) {
      return new TreeViewModel.DefaultNodeInfo<String>(topLevelListListModel,
          new TextCell());
    } else if ("Favorites".equals(value)) {
      return new TreeViewModel.DefaultNodeInfo<StockQuote>(stockQuoteListModel,
          STOCK_QUOTE_CELL) {
        @Override
        public Object getKey(StockQuote value) {
          return value.getTicker();
        }
      };
    } else if ("History".equals(value)) {
      String ticker = ((StockQuote) treeNode.getParentNode().getValue()).getTicker();
      ListListModel<Transaction> listModel = transactionListListModelsByTicker.get(ticker);
      if (listModel == null) {
        listModel = new ListListModel<Transaction>();
        transactionListListModelsByTicker.put(ticker, listModel);
      }
      return new TreeViewModel.DefaultNodeInfo<Transaction>(listModel,
          TRANSACTION_CELL);
    } else if ("Actions".equals(value)) {
      ListListModel<String> listModel = new ListListModel<String>();
      List<String> list = listModel.getList();
      list.add("Buy");
      list.add("Sell");
      return new TreeViewModel.DefaultNodeInfo<String>(listModel, new ButtonCell(),
          new ValueUpdater<String>() {
            public void update(String value) {
              StockQuote stockQuote = (StockQuote) treeNode.getParentNode().getValue();
              if ("Buy".equals(value)) {
                updater.buy(stockQuote);
              } else {
                updater.sell(stockQuote);
              }
            }
          });
    } else if (value instanceof String) {
      SectorListModel listModel = new SectorListModel(updater, (String) value);
      sectorListModels.put((String) value, listModel);
      return new TreeViewModel.DefaultNodeInfo<StockQuote>(listModel, STOCK_QUOTE_CELL) {
        @Override
        public Object getKey(StockQuote value) {
          return value.getTicker();
        }
      };
    } else if (value instanceof StockQuote) {
      ListListModel<String> listModel = new ListListModel<String>();
      List<String> list = listModel.getList();
      list.add("Actions");
      list.add("History");
      return new TreeViewModel.DefaultNodeInfo<String>(listModel, new TextCell());
    }

    throw new IllegalArgumentException(value.toString());
  }

  public SectorListModel getSectorListModel(String value) {
    return sectorListModels.get(value);
  }

  public boolean isLeaf(Object value, final TreeNode<?> parentNode) {
    if (value instanceof Transaction ||
      "Buy".equals(value) || "Sell".equals(value)) {
      return true;
    }
    
    return false;
  }
}
