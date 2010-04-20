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

import com.google.gwt.bikeshed.list.client.PagingTableListView;
import com.google.gwt.bikeshed.list.shared.AbstractListViewAdapter;
import com.google.gwt.core.client.GWT;
import com.google.gwt.sample.bikeshed.stocks.shared.StockQuote;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

/**
 * Widget for favorite stocks.
 */
public class FavoritesWidget extends Composite {

  interface Binder extends UiBinder<Widget, FavoritesWidget> { }
  private static final Binder binder = GWT.create(Binder.class);

  @UiField PagingTableListView<StockQuote> listView;

  private final AbstractListViewAdapter<StockQuote> adapter;

  public FavoritesWidget(AbstractListViewAdapter<StockQuote> adapter) {
    this.adapter = adapter;
    initWidget(binder.createAndBindUi(this));

    listView.addColumn(Columns.tickerColumn, "ticker");
    listView.addColumn(Columns.priceColumn, "price");
    listView.addColumn(Columns.changeColumn, "change");
    listView.addColumn(Columns.sharesColumn, "shares");
    listView.addColumn(Columns.dollarsColumn, "value");
    listView.addColumn(Columns.profitLossColumn, "profit/loss");
    listView.addColumn(Columns.buyColumn);
    listView.addColumn(Columns.sellColumn);
  }

  @UiFactory
  PagingTableListView<StockQuote> createListView() {
    PagingTableListView<StockQuote> view = new PagingTableListView<StockQuote>(10);
    adapter.addView(view);
    return view;
  }
}
