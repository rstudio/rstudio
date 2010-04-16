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
package com.google.gwt.valuestore.client;

import com.google.gwt.bikeshed.list.shared.AsyncListViewAdapter;
import com.google.gwt.user.client.ui.TakesValueList;
import com.google.gwt.valuestore.shared.Record;

import java.util.List;

/**
 * Simple adapter from a {@link com.google.gwt.valuestore.shared.ValueStore
 * ValueStore} to an {@link AsyncListViewAdapter}.
 * <p>
 * TODO: pay attention to the visible range info that subclasses receive via
 * {@link #onRangeChanged}
 *
 * @param <K> the ValuesKey of the records to display
 */
public abstract class ValueStoreListViewAdapter<K extends Record> extends
    AsyncListViewAdapter<K> implements TakesValueList<K> {

  public void setValueList(List<K> newValues) {
    updateDataSize(newValues.size(), true);
    updateViewData(0, newValues.size(), newValues);
  }
}
