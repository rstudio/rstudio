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
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.RecordListRequest;
import com.google.gwt.valuestore.shared.Record;
import com.google.gwt.view.client.ListView;
import com.google.gwt.view.client.Range;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    Activity, RecordListView.Delegate<R> {
  private final Map<String, Integer> recordToRow = new HashMap<String, Integer>();

  private RecordListView<R> view;
  private Display display;

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
    final Range range = listView.getRange();

    final Receiver<List<R>> callback = new Receiver<List<R>>() {
      public void onSuccess(List<R> values) {
        recordToRow.clear();
        for (int i = 0, r = range.getStart(); i < values.size(); i++, r++) {
          recordToRow.put(values.get(i).getId(), r);
        }
        getView().setData(range.getStart(), range.getLength(), values);
        if (display != null) {
          display.showActivityWidget(getView());
        }
      }
    };

    createRangeRequest(range).forProperties(getView().getProperties()).to(
        callback).fire();
  }

  public void onStop() {
    view.setDelegate((RecordListView.Delegate<R>) null);
  }

  public void start(Display display) {
    this.display = display;
    init();
  }

  public void update(R record) {
    // TODO Must handle delete, new
    Integer row = recordToRow.get(record.getId());
    getView().setData(row, 1, Collections.singletonList(record));
  }

  public boolean willStop() {
    return true;
  }

  protected abstract RecordListRequest<R> createRangeRequest(Range range);

  protected abstract void fireCountRequest(Receiver<Long> callback);

  private void init() {
    fireCountRequest(new Receiver<Long>() {
      public void onSuccess(Long response) {
        getView().setDataSize(response.intValue(), true);
        onRangeChanged(view);
      }
    });
  }
}
