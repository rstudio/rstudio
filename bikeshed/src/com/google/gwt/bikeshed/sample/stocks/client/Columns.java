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
import com.google.gwt.bikeshed.cells.client.CheckboxCell;
import com.google.gwt.bikeshed.cells.client.CurrencyCell;
import com.google.gwt.bikeshed.cells.client.TextCell;
import com.google.gwt.bikeshed.list.client.Column;
import com.google.gwt.bikeshed.sample.stocks.shared.StockQuote;
import com.google.gwt.bikeshed.sample.stocks.shared.Transaction;

/**
 * Column definitions for the stock demo.
 */
public class Columns {

  static Column<StockQuote, String> buyColumn = new Column<StockQuote, String>(
      new ButtonCell()) {
    @Override
    protected String getValue(StockQuote object) {
      return "Buy";
    }
  };

  static Column<StockQuote, Integer> dollarsColumn =
    new Column<StockQuote, Integer>(new CurrencyCell()) {
    @Override
    protected Integer getValue(StockQuote object) {
      return object.getPrice() * object.getSharesOwned();
    }
  };
  
  static Column<StockQuote, Boolean> favoriteColumn =
    new Column<StockQuote, Boolean>(new CheckboxCell()) {
    @Override
    protected Boolean getValue(StockQuote object) {
      return object.isFavorite();
    }
  };

  static Column<StockQuote, String> nameColumn =
    new Column<StockQuote, String>(new TextCell()) {
    @Override
    protected String getValue(StockQuote object) {
      return object.getName();
    }
  };

  static Column<StockQuote, Integer> priceColumn =
    new Column<StockQuote, Integer>(new CurrencyCell()) {
    @Override
    protected Integer getValue(StockQuote object) {
      return object.getPrice();
    }
  };

  static Column<StockQuote, String> sellColumn =
    new Column<StockQuote, String>(new ButtonCell()) {
    @Override
    protected String getValue(StockQuote object) {
      return "Sell";
    }
  };

  static Column<StockQuote, String> sharesColumn =
    new Column<StockQuote, String>(new TextCell()) {
    @Override
    protected String getValue(StockQuote object) {
      return "" + object.getSharesOwned();
    }
  };

  static Column<Transaction, String> subtotalColumn =
    new Column<Transaction, String>(new TextCell()) {
    @Override
    protected String getValue(Transaction object) {
      int price = object.getActualPrice() * object.getQuantity();
      return (object.isBuy() ? " (" : " ") + StockSample.getFormattedPrice(price) + 
          (object.isBuy() ? ")" : "");
    }
  };
  
  static Column<StockQuote, String> tickerColumn =
    new Column<StockQuote, String>(new TextCell()) {
    @Override
    protected String getValue(StockQuote object) {
      return object.getTicker();
    }
  };
  
  static Column<Transaction, String> transactionColumn =
    new Column<Transaction, String>(new TextCell()) {
    @Override
    protected String getValue(Transaction object) {
      return object.toString();
    }
  };
}