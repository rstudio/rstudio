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

import com.google.gwt.bikeshed.cells.client.Cell;
import com.google.gwt.bikeshed.cells.client.TextCell;
import com.google.gwt.bikeshed.list.shared.ListListModel;
import com.google.gwt.bikeshed.sample.stocks.shared.Transaction;
import com.google.gwt.bikeshed.tree.client.TreeNodeView;
import com.google.gwt.bikeshed.tree.client.TreeViewModel;

import java.util.Map;

/**
 * A TreeViewModel for a tree with a hidden root node of null,
 * a first level containing ticker symbol Strings, and a second
 * level containing Transactions.
 */
class TransactionTreeViewModel implements TreeViewModel {
  
  static class TransactionCell extends Cell<Transaction> {

    @Override
    public void render(Transaction value, StringBuilder sb) {
      sb.append(value.toString());
    }
  }
  
  private static final Cell<String> TEXT_CELL = new TextCell();
  
  private static final Cell<Transaction> TRANSACTION_CELL =
    new TransactionCell();
  
  private ListListModel<String> tickersListModel;
  private Map<String, ListListModel<Transaction>> transactionListListModelsByTicker;

  public TransactionTreeViewModel(ListListModel<String> tickersListModel,
      Map<String, ListListModel<Transaction>> transactionListListModelsByTicker) {
    this.tickersListModel = tickersListModel;
    this.transactionListListModelsByTicker = transactionListListModelsByTicker;
  }

  public <T> NodeInfo<?> getNodeInfo(T value, TreeNodeView<T> treeNodeView) {
    if (value == null) {
      return new TreeViewModel.DefaultNodeInfo<String>(tickersListModel,
          TEXT_CELL);
    } else if (value instanceof String) {
      return new TreeViewModel.DefaultNodeInfo<Transaction>(
          transactionListListModelsByTicker.get(value), TRANSACTION_CELL);
    } else if (value instanceof Transaction) {
      return null;
    }
    
    throw new IllegalArgumentException(value.toString());
  }

  public boolean isLeaf(Object value) {
    return value instanceof Transaction;
  }
}