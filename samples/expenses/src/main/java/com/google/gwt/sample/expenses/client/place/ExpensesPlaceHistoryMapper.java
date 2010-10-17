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
package com.google.gwt.sample.expenses.client.place;

import com.google.gwt.place.shared.PlaceHistoryMapperWithFactory;
import com.google.gwt.sample.expenses.client.ioc.ExpensesFactory;

/**
 * This interface is the hub of your application's navigation system. It links
 * the {@link com.google.gwt.place.shared.Place Place}s your user navigates to
 * with the browser history system &mdash; that is, it makes the browser's back
 * and forth buttons work for you, and also makes each spot in your app
 * bookmarkable.
 * 
 * <p>
 * The simplest way to make new {@link com.google.gwt.place.shared.Place Place}
 * types available to your app is to uncomment the {@literal @}WithTokenizers
 * annotation below and list their corresponding
 * {@link com.google.gwt.place.shared.PlaceTokenizer PlaceTokenizer}s. Or if a
 * tokenizer needs more than a default constructor can provide, add a method to
 * the apps {@link ExpensesFactory}.
 * 
 * <p>
 * This code generated object looks to both the {@literal @}WithTokenizers
 * annotation and the factory to infer the types of
 * {@link com.google.gwt.place.Place Place}s your app can navigate to. In this
 * case it will find the {@link ExpensesFactory#getListTokenizer()} and
 * {@link ExpensesFactory#getReportTokenizer()} methods, and so be able to handle
 * {@link ReportListPlace}s and {@link ReportPlace}s.
 */
// @WithTokenizers({MyNewPlace.Tokenizer, MyOtherNewPlace.Tokenizer})
public interface ExpensesPlaceHistoryMapper extends
    PlaceHistoryMapperWithFactory<ExpensesFactory> {
}
