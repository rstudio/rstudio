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

import com.google.gwt.bikeshed.list.client.CellTable;
import com.google.gwt.core.client.GWT;
import com.google.gwt.sample.bikeshed.stocks.shared.StockQuote;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.AbstractListViewAdapter;

/**
 * Widget for favorite stocks.
 */
public class FavoritesWidget extends Composite {

  interface Binder extends UiBinder<Widget, FavoritesWidget> { }
  private static final Binder binder = GWT.create(Binder.class);

  @UiField CellTable<StockQuote> table;

  private final AbstractListViewAdapter<StockQuote> adapter;

  public FavoritesWidget(AbstractListViewAdapter<StockQuote> adapter) {
    this.adapter = adapter;
    initWidget(binder.createAndBindUi(this));

    table.addColumn(Columns.tickerColumn, "ticker");
    table.addColumn(Columns.priceColumn, "price");
    table.addColumn(Columns.changeColumn, "change");
    table.addColumn(Columns.sharesColumn, "shares");
    table.addColumn(Columns.dollarsColumn, "value");
    table.addColumn(Columns.profitLossColumn, "profit/loss");
    table.addColumn(Columns.buyColumn);
    table.addColumn(Columns.sellColumn);
  }

  @UiFactory
  CellTable<StockQuote> createTable() {
    CellTable<StockQuote> view = new CellTable<StockQuote>(10);
    adapter.addView(view);
    return view;
  }
}
