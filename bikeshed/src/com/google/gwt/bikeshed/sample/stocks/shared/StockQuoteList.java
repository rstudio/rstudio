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

import java.util.ArrayList;

/**
 * A list of {@link StockQuote} that knows its start index in the global list of
 * results.
 */
public class StockQuoteList extends ArrayList<StockQuote> {

  private int start;

  public StockQuoteList(int start) {
    this.start = start;
  }

  /**
   * Used by RPC.
   */
  StockQuoteList() {
  }

  public int getStartIndex() {
    return start;
  }
  
  /**
   * Returns the sum of stock prices times shares owned, in pennies.
   */
  public int getValue() {
    int value = 0;
    for (StockQuote q : this) {
      value += q.getPrice() * q.getSharesOwned();
    }
    return value;
  }
}

