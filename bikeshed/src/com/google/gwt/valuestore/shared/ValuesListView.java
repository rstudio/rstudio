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
package com.google.gwt.valuestore.shared;

import com.google.gwt.user.client.ui.TakesValueList;
import com.google.gwt.user.client.ui.Widget;

import java.util.Collection;
import java.util.List;

/**
 * Implemented by widgets that display lists of records.
 *
 * @param <K> the type of the key of the records
 */
public interface ValuesListView<K extends ValuesKey<K>> extends
    TakesValueList<Values<K>> {

  /**
   * Implemented by ValuesListView delegates, to keep informed when the visible
   * range changes.
   */
  public interface Delegate {
    void onRangeChanged(int start, int length);
  }

  Widget asWidget();

  Collection<Property<K, ?>> getProperties();

  /**
   * @param delegate the new delegate for this view, may be null
   */
  void setDelegate(ValuesListView.Delegate delegate);

  void setValueList(List<Values<K>> newValues);
}
