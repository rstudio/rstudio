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
 * A response to a request for stock data.
 */
public class StockResponse implements Serializable {

  private StockQuoteList searchResults;
  private StockQuoteList favorites;
  private int numSearchResults;
  private int numFavorites;

  /**
   * Used for RPC.
   */
  StockResponse() {
  }

  public StockResponse(StockQuoteList searchResults, StockQuoteList favorites,
      int numSearchResults, int numFavorites) {
    this.searchResults = searchResults;
    this.favorites = favorites;
    this.numSearchResults = numSearchResults;
    this.numFavorites = numFavorites;
  }

  public StockQuoteList getSearchResults() {
    return searchResults;
  }

  public StockQuoteList getFavorites() {
    return favorites;
  }

  public int getNumSearchResults() {
    return numSearchResults;
  }

  public int getNumFavorites() {
    return numFavorites;
  }
}
