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
package com.google.gwt.bikeshed.list.client;

import com.google.gwt.bikeshed.list.shared.ListEvent;
import com.google.gwt.bikeshed.list.shared.Range;
import com.google.gwt.bikeshed.list.shared.SizeChangeEvent;

/**
 * A list view.
 *
 * @param <T> the data type of each row
 */
public interface ListView<T> {

  /**
   * A list view delegate, implemented by classes that supply data to a view.
   *
   * @param <T> the data type of each row
   */
  public interface Delegate<T> {
    void onRangeChanged(ListView<T> listView);
  }

  void setDelegate(Delegate<T> delegate);

  Range getRange();

  // TODO - rename to setData, don't use event?
  void onDataChanged(ListEvent<T> event);

  // TODO - rename to setDataSize, don't use event?
  void onSizeChanged(SizeChangeEvent event);
}
