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
import java.util.List;

/**
 * A response to a request for stock data.
 */
public class StockResponse implements Serializable {

  private int numRows;
  private List<StockQuoteList> lists;

  public StockResponse(int numRows, List<StockQuoteList> lists) {
    this.numRows = numRows;
    this.lists = lists;
  }

  /**
   * Used for RPC.
   */
  StockResponse() {
  }

  /**
   * Get the data for specific ranges.
   * 
   * @return the data
   */
  public List<StockQuoteList> getLists() {
    return lists;
  }

  /**
   * @return the total number of rows available
   */
  public int getNumRows() {
    return numRows;
  }
}

