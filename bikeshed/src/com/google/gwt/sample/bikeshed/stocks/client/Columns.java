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

import com.google.gwt.bikeshed.list.client.Column;
import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.CurrencyCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.sample.bikeshed.stocks.shared.StockQuote;
import com.google.gwt.sample.bikeshed.stocks.shared.Transaction;

/**
 * Column definitions for the stock demo.
 */
public class Columns {

  static Column<StockQuote, String> buyColumn = new Column<StockQuote, String>(
      new ButtonCell()) {
    @Override
    public String getValue(StockQuote object) {
      return "Buy";
    }
  };

  static Column<StockQuote, String> changeColumn = new Column<StockQuote, String>(
      new ChangeCell()) {
    @Override
    public String getValue(StockQuote object) {
      return object.getChange();
    }
  };

  static Column<StockQuote, Integer> dollarsColumn = new Column<StockQuote, Integer>(
      new CurrencyCell()) {
    @Override
    public Integer getValue(StockQuote object) {
      return object.getPrice() * object.getSharesOwned();
    }
  };

  static Column<StockQuote, Boolean> favoriteColumn = new Column<StockQuote, Boolean>(
      new CheckboxCell()) {
    @Override
    public Boolean getValue(StockQuote object) {
      return object.isFavorite();
    }
  };

  // TODO - use an ellipsis cell
  static HighlightingTextCell nameCell = new HighlightingTextCell();

  static Column<StockQuote, String> nameColumn = new Column<StockQuote, String>(
      nameCell) {
    @Override
    public String getValue(StockQuote object) {
      return object.getName();
    }
  };

  static Column<StockQuote, Integer> priceColumn = new Column<StockQuote, Integer>(
      new CurrencyCell()) {
    @Override
    public Integer getValue(StockQuote object) {
      return object.getPrice();
    }
  };

  static Column<StockQuote, Integer> profitLossColumn = new Column<StockQuote, Integer>(
      new ProfitLossCell()) {
    @Override
    public Integer getValue(StockQuote object) {
      return object.getValue() - object.getTotalPaid();
    }
  };

  static Column<StockQuote, String> sellColumn = new Column<StockQuote, String>(
      new ButtonCell()) {
    @Override
    public String getValue(StockQuote object) {
      return "Sell";
    }
  };

  static TextCell textCell = new TextCell();

  static Column<StockQuote, String> sharesColumn =
    new Column<StockQuote, String>(textCell) {
    @Override
    public String getValue(StockQuote object) {
      return "" + object.getSharesOwned();
    }
  };

  static Column<Transaction, String> subtotalColumn =
    new Column<Transaction, String>(textCell) {
    @Override
    public String getValue(Transaction object) {
      int price = object.getActualPrice() * object.getQuantity();
      return (object.isBuy() ? " (" : " ")
          + StocksDesktop.getFormattedPrice(price) + (object.isBuy() ? ")" : "");
    }
  };

  static Column<StockQuote, String> tickerColumn =
    new Column<StockQuote, String>(textCell) {
    @Override
    public String getValue(StockQuote object) {
      return object.getTicker();
    }
  };

  static Column<Transaction, String> transactionColumn =
    new Column<Transaction, String>(textCell) {
    @Override
    public String getValue(Transaction object) {
      return object.toString();
    }
  };
}
