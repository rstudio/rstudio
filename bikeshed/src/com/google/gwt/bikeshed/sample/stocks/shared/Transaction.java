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
package com.google.gwt.bikeshed.sample.stocks.shared;

import com.google.gwt.i18n.client.NumberFormat;

import java.io.Serializable;

/**
 * The buy or sell transaction.
 */
public class Transaction implements Serializable {

  private int actualPrice; // in pennies
  
  /**
   * True if a buy transaction, false if a sell transaction.
   */
  private boolean isBuy;

  private int quantity;
  private String ticker;

  public Transaction(boolean isBuy, String ticker, int quantity) {
    this(isBuy, ticker, quantity, -1);
  }

  public Transaction(boolean isBuy, String ticker, int quantity,
      int actualPrice) {
    super();
    this.isBuy = isBuy;
    this.ticker = ticker;
    this.quantity = quantity;
    this.actualPrice = actualPrice;
  }
  
  Transaction() {
  }

  public int getActualPrice() {
    return actualPrice;
  }

  public int getQuantity() {
    return quantity;
  }

  public String getTicker() {
    return ticker;
  }

  public boolean isBuy() {
    return isBuy;
  }

  @Override
  public String toString() {
    String op = isBuy ? "Bought" : "Sold";
    if (actualPrice == -1) {
      return op + " " + quantity + " shares of " + ticker;
    } else {
      return op + " " + quantity + " shares of " + ticker + " at " +
          NumberFormat.getCurrencyFormat("USD").format(actualPrice / 100.0);
    }
  }
}
