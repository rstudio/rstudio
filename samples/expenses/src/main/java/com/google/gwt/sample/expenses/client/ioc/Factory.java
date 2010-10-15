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
package com.google.gwt.sample.expenses.client.ioc;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.place.shared.PlaceHistoryHandler;
import com.google.gwt.sample.expenses.client.ExpenseDetails;
import com.google.gwt.sample.expenses.client.ExpenseTree;
import com.google.gwt.sample.expenses.client.ExpensesApp;
import com.google.gwt.sample.expenses.client.ExpensesShell;
import com.google.gwt.sample.expenses.client.place.ExpensesPlaceHistoryMapper;
import com.google.gwt.sample.expenses.client.place.ReportListPlace;
import com.google.gwt.sample.expenses.client.place.ReportPlace;
import com.google.gwt.sample.expenses.shared.ExpensesRequestFactory;

/**
 * In charge of instantiation.
 * <p>
 * TODO: Use {@link http ://code.google.com/p/google-gin/} to generate this
 */
public class Factory {

  private final EventBus eventBus = new SimpleEventBus();
  private final ExpensesRequestFactory requestFactory = GWT.create(ExpensesRequestFactory.class);
  private final ExpensesPlaceHistoryMapper historyMapper = GWT.create(ExpensesPlaceHistoryMapper.class);
  private final PlaceHistoryHandler placeHistoryHandler;
  private final PlaceController placeController = new PlaceController(eventBus);

  public Factory() {
    requestFactory.initialize(eventBus);
    historyMapper.setFactory(this);
    placeHistoryHandler = new PlaceHistoryHandler(historyMapper);
  }

  public ExpensesApp getExpensesApp() {
    return new ExpensesApp(requestFactory, eventBus, new ExpensesShell(
        new ExpenseTree(requestFactory), new ExpenseDetails(requestFactory)),
        placeHistoryHandler, placeController);
  }

  public ReportListPlace.Tokenizer getListTokenizer() {
    return new ReportListPlace.Tokenizer(requestFactory);
  }

  public ReportPlace.Tokenizer getReportTokenizer() {
    return new ReportPlace.Tokenizer(getListTokenizer(), requestFactory);
  }
}
