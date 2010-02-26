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

import com.google.gwt.bikeshed.list.shared.Range;

import java.io.Serializable;

/**
 * A request for new stock data.
 */
public class StockRequest implements Serializable {

  String searchQuery;
  Range searchRange;
  Range favoritesRange;

  public StockRequest(String searchQuery, Range searchRange,
      Range favoritesRange) {
    this.searchQuery = searchQuery;
    this.searchRange = searchRange;
    this.favoritesRange = favoritesRange;
  }

  /**
   * Used by RPC.
   */
  StockRequest() {
  }

  public Range getFavoritesRange() {
    return favoritesRange;
  }

  public String getSearchQuery() {
    return searchQuery;
  }

  public Range getSearchRange() {
    return searchRange;
  }
}
