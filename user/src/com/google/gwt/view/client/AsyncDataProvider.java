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
package com.google.gwt.view.client;

import java.util.List;

/**
 * An implementation of {@link AbstractDataProvider} that allows the data to be
 * modified.
 *
 * <p>
 * <h3>Example</h3>
 * {@example com.google.gwt.examples.view.AsyncDataProviderExample}
 * </p>
 *
 * @param <T> the data type of records in the list
 */
public abstract class AsyncDataProvider<T> extends AbstractDataProvider<T> {

  /**
   * Constructs an AsyncDataProvider without a key provider.
   */
  protected AsyncDataProvider() {
    super(null);
  }

  /**
   * Constructs an AsyncDataProvider with the given key provider.
   *
   * @param keyProvider an instance of ProvidesKey<T>, or null if the record
   *        object should act as its own key
   */
  protected AsyncDataProvider(ProvidesKey<T> keyProvider) {
    super(keyProvider);
  }

  @Override
  public void updateRowCount(int size, boolean exact) {
    super.updateRowCount(size, exact);
  }

  @Override
  public void updateRowData(int start, List<T> values) {
    super.updateRowData(start, values);
  }
}
