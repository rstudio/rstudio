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
package com.google.gwt.sample.expenses.client;

import com.google.gwt.bikeshed.list.shared.AsyncListModel;
import com.google.gwt.user.client.ui.TakesValueList;
import com.google.gwt.valuestore.shared.Values;
import com.google.gwt.valuestore.shared.ValuesKey;

import java.util.List;

/**
 * Simple adapter from a {@link com.google.gwt.valuestore.shared.ValueStore
 * ValueStore} to a {@link com.google.gwt.bikeshed.list.shared.ListModel
 * ListModel}
 * <p>
 * TODO: pay attention to the visible range info that subclasses receive via
 * {@link #onRangeChanged}
 * 
 * @param <K> the ValuesKey of the records to display
 */
public abstract class ListModelAdapter<K extends ValuesKey<K>> extends
    AsyncListModel<Values<K>> implements TakesValueList<Values<K>> {

  public void setValueList(List<Values<K>> newValues) {
    updateDataSize(newValues.size(), true);
    updateViewData(0, newValues.size() - 1, newValues);
  }
}