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

import com.google.gwt.bikeshed.list.client.PagingTableListView;
import com.google.gwt.bikeshed.list.client.TextHeader;
import com.google.gwt.bikeshed.list.shared.ListModel;
import com.google.gwt.bikeshed.sample.stocks.shared.StockQuote;
import com.google.gwt.core.client.GWT;
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

  private final ListModel<StockQuote> model;

  public FavoritesWidget(ListModel<StockQuote> model) {
    this.model = model;
    initWidget(binder.createAndBindUi(this));

    listView.addColumn(Columns.tickerColumn, new TextHeader("ticker"));
    listView.addColumn(Columns.priceColumn, new TextHeader("price"));
    listView.addColumn(Columns.changeColumn, new TextHeader("change"));
    listView.addColumn(Columns.sharesColumn, new TextHeader("shares"));
    listView.addColumn(Columns.dollarsColumn, new TextHeader("value"));
    listView.addColumn(Columns.buyColumn);
    listView.addColumn(Columns.sellColumn);
  }

  @UiFactory
  PagingTableListView<StockQuote> createListView() {
    return new PagingTableListView<StockQuote>(model, 10);
  }
}
