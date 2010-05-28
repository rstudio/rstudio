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

import com.google.gwt.app.place.Activity;
import com.google.gwt.bikeshed.list.client.ListView;
import com.google.gwt.requestfactory.shared.EntityListRequest;
import com.google.gwt.user.client.ui.TakesValueList;
import com.google.gwt.valuestore.shared.Record;

import java.util.List;

/**
 * Abstract activity for requesting and displaying a list of {@Record}.
 * <p>
 * Subclasses must:
 * 
 * <ul>
 * <li>implement a method for creating request objects
 * <li>provide a {@link RecordListView}
 * <li>respond to "show" and "edit" requests
 * </ul>
 * 
 * Only the properties required by the view will be requested.
 * 
 * @param <R> the type of {@link Record} listed
 */
public abstract class AbstractRecordListActivity<R extends Record> implements
    Activity, RecordListView.Delegate<R>, TakesValueList<R> {
  private RecordListView<R> view;
  private Callback callback;

  public AbstractRecordListActivity(RecordListView<R> view) {
    this.view = view;
    view.setDelegate(this);
  }

  public RecordListView<R> getView() {
    return view;
  }

  public void onCancel() {
    onStop();
  }

  /**
   * Called by the table as it needs data.
   */
  public void onRangeChanged(ListView<R> listView) {
    // TODO use listview.getRange()
    getData();
  }

  public void onStop() {
    view.setDelegate((RecordListView.Delegate<R>) null);
  }

  /**
   * When the request returns it calls this method.
   * <p>
   * TODO this was supposed to be a convenience but it's just confusing. Get
   * real callbacks into request factory
   */
  public void setValueList(List<R> values) {
    getView().setDataSize(values.size(), true);
    getView().setData(0, values.size(), values);
    if (callback != null) {
      callback.onStarted(getView().asWidget());
    }
  }

  public void start(Callback callback) {
    this.callback = callback;
    getData();
  }

  public boolean willStop() {
    return true;
  }

  protected abstract EntityListRequest<R> createRequest();

  private void getData() {
    createRequest().forProperties(getView().getProperties()).to(this).fire();
  }

}
