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
package com.google.gwt.valuestore.ui;

import com.google.gwt.app.util.IsWidget;
import com.google.gwt.bikeshed.list.client.ListView;
import com.google.gwt.valuestore.shared.Record;

/**
 * A view of a list of {@link Records}, which declares which properties it is
 * able to display.
 * <p>
 * It is expected that such views will typically (eventually) be defined largely
 * in ui.xml files which declare the properties of interest, which is why the
 * view is a source of a property set rather than a receiver of one.
 * 
 * @param <R> the type of the records to display
 */
public interface RecordListView<R extends Record> extends ListView<R>,
    IsWidget, PropertyView<R> {
  /**
   * Implemented by the owner of a RecordTableView.
   * 
   * @param<R> the type of the records to display
   */
  interface Delegate<R extends Record> extends ListView.Delegate<R> {
    /**
     * @param record the record the user wants to edit
     */
    void edit(R record);

    /**
     * @param record the record whose details the user wants to see
     */
    void showDetails(R record);
  }

  /**
   * A RecordListView requires a RecordListView.Delegate.
   */
  void setDelegate(
      com.google.gwt.bikeshed.list.client.ListView.Delegate<R> delegate)
      throws UnsupportedOperationException;

  /**
   * Sets the delegate.
   */
  void setDelegate(Delegate<R> delegate);
}
