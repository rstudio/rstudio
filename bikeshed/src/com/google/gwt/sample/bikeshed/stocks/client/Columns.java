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
import com.google.gwt.bikeshed.cells.client.CheckboxCell;
import com.google.gwt.bikeshed.cells.client.CurrencyCell;
import com.google.gwt.bikeshed.cells.client.ProfitLossCell;
import com.google.gwt.bikeshed.cells.client.TextCell;
import com.google.gwt.bikeshed.list.client.SimpleColumn;
import com.google.gwt.sample.bikeshed.stocks.shared.StockQuote;
import com.google.gwt.sample.bikeshed.stocks.shared.Transaction;

/**
 * Column definitions for the stock demo.
 */
public class Columns {

  static SimpleColumn<StockQuote, String> buyColumn = new SimpleColumn<StockQuote, String>(
      ButtonCell.getInstance()) {
    @Override
    public String getValue(StockQuote object) {
      return "Buy";
    }
  };

  static SimpleColumn<StockQuote, String> changeColumn = new SimpleColumn<StockQuote, String>(
      new ChangeCell()) {
    @Override
    public String getValue(StockQuote object) {
      return object.getChange();
    }
  };

  static SimpleColumn<StockQuote, Integer> dollarsColumn = new SimpleColumn<StockQuote, Integer>(
      new CurrencyCell()) {
    @Override
    public Integer getValue(StockQuote object) {
      return object.getPrice() * object.getSharesOwned();
    }
  };

  static SimpleColumn<StockQuote, Boolean> favoriteColumn = new SimpleColumn<StockQuote, Boolean>(
      new CheckboxCell()) {
    @Override
    public Boolean getValue(StockQuote object) {
      return object.isFavorite();
    }
  };

  // TODO - use an ellipsis cell
  static HighlightingTextCell nameCell = new HighlightingTextCell();

  static SimpleColumn<StockQuote, String> nameColumn = new SimpleColumn<StockQuote, String>(
      nameCell) {
    @Override
    public String getValue(StockQuote object) {
      return object.getName();
    }
  };

  static SimpleColumn<StockQuote, Integer> priceColumn = new SimpleColumn<StockQuote, Integer>(
      new CurrencyCell()) {
    @Override
    public Integer getValue(StockQuote object) {
      return object.getPrice();
    }
  };

  static SimpleColumn<StockQuote, Integer> profitLossColumn = new SimpleColumn<StockQuote, Integer>(
      new ProfitLossCell()) {
    @Override
    public Integer getValue(StockQuote object) {
      return object.getValue() - object.getTotalPaid();
    }
  };

  static SimpleColumn<StockQuote, String> sellColumn = new SimpleColumn<StockQuote, String>(
      ButtonCell.getInstance()) {
    @Override
    public String getValue(StockQuote object) {
      return "Sell";
    }
  };

  static TextCell textCell = TextCell.getInstance();

  static SimpleColumn<StockQuote, String> sharesColumn =
    new SimpleColumn<StockQuote, String>(textCell) {
    @Override
    public String getValue(StockQuote object) {
      return "" + object.getSharesOwned();
    }
  };

  static SimpleColumn<Transaction, String> subtotalColumn =
    new SimpleColumn<Transaction, String>(textCell) {
    @Override
    public String getValue(Transaction object) {
      int price = object.getActualPrice() * object.getQuantity();
      return (object.isBuy() ? " (" : " ")
          + StocksDesktop.getFormattedPrice(price) + (object.isBuy() ? ")" : "");
    }
  };

  static SimpleColumn<StockQuote, String> tickerColumn =
    new SimpleColumn<StockQuote, String>(textCell) {
    @Override
    public String getValue(StockQuote object) {
      return object.getTicker();
    }
  };

  static SimpleColumn<Transaction, String> transactionColumn =
    new SimpleColumn<Transaction, String>(textCell) {
    @Override
    public String getValue(Transaction object) {
      return object.toString();
    }
  };
}