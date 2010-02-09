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
package com.google.gwt.list.shared;

import java.util.List;

/**
 * Asynchronous version of a {@link ListModel}.
 * 
 * @param <T> the data type
 */
public class AsyncListModel<T> extends AbstractListModel<T> {

  /**
   * The source of data for the list model.
   * 
   * @param <T> the data type
   */
  public static interface DataSource<T> {
    /**
     * Request that the data source pushes new data to the client. The data
     * source should call {@link #updateViewData} and/or {@link #updateDataSize}
     * when the data is available.
     * 
     * @param listModel the model requesting the data
     */
    void requestData(AsyncListModel<T> listModel);
  }

  /**
   * The data source.
   */
  private DataSource<T> dataSource;

  /**
   * Construct a new {@link AsyncListModel}.
   * 
   * @param dataSource the data source
   */
  public AsyncListModel(DataSource<T> dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public void updateDataSize(int size, boolean exact) {
    super.updateDataSize(size, exact);
  }

  @Override
  public void updateViewData(int start, int length, List<T> values) {
    super.updateViewData(start, length, values);
  }

  @Override
  protected void onRangeChanged() {
    dataSource.requestData(this);
  }
}
