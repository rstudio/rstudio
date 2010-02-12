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
package com.google.gwt.sample.datawidgets.shared;

import java.io.Serializable;

/**
 * The buy or sell transaction.
 */
public class Transaction implements Serializable {

  /**
   * True if a buy transaction, false if a sell transaction.
   */
  private boolean isBuy;

  private String ticker;
  private int quantity;

  public Transaction(boolean isBuy, String ticker, int quantity) {
    super();
    this.isBuy = isBuy;
    this.ticker = ticker;
    this.quantity = quantity;
  }

  Transaction() {
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
    return op + " " + quantity + " shares of " + ticker;
  }
}
