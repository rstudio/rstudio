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

import com.google.gwt.bikeshed.list.shared.Range;
import com.google.gwt.sample.bikeshed.stocks.shared.StockRequest;
import com.google.gwt.sample.bikeshed.stocks.shared.StockResponse;
import com.google.gwt.sample.bikeshed.stocks.shared.Transaction;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The async counterpart of <code>DataService</code>.
 */
public interface StockServiceAsync {
  void getStockQuotes(StockRequest request,
      AsyncCallback<StockResponse> callback);

  void addFavorite(String ticker, Range favoritesRange, AsyncCallback<StockResponse> callback);

  void removeFavorite(String ticker, Range favoritesRange, AsyncCallback<StockResponse> callback);

  void transact(Transaction transaction, AsyncCallback<Transaction> callback);
}
