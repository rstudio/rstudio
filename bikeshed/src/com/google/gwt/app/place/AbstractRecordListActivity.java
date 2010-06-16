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
package com.google.gwt.app.place;

import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.RecordListRequest;
import com.google.gwt.valuestore.shared.Record;
import com.google.gwt.valuestore.shared.WriteOperation;
import com.google.gwt.valuestore.ui.RecordListView;
import com.google.gwt.view.client.ListView;
import com.google.gwt.view.client.PagingListView;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.SingleSelectionModel;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * Abstract activity for requesting and displaying a list of {@Record}.
 * <p>
 * Subclasses must:
 * 
 * <ul>
 * <li>implement methods to provide a full count, and request a specific 
 * <li>provide a {@link RecordListView}
 * <li>respond to "show details" commands 
 * </ul>
 * 
 * Only the properties required by the view will be requested.
 * 
 * @param <R> the type of {@link Record} listed
 */
public abstract class AbstractRecordListActivity<R extends Record> implements
    Activity, RecordListView.Delegate<R>, ListView.Delegate<R> {
  private final Map<String, Integer> recordToRow = new HashMap<String, Integer>();
  private final Map<String, R> idToRecord = new HashMap<String, R>();
  private final SingleSelectionModel<R> selectionModel;

  private RecordListView<R> view;
  private Display display;

  public AbstractRecordListActivity(RecordListView<R> view) {
    this.view = view;
    view.setDelegate(this);
    view.asPagingListView().setDelegate(this);

    selectionModel = new SingleSelectionModel<R>() {
      @Override
      public void setSelected(R newSelection, boolean selected) {
        R wasSelected = this.getSelectedObject();
        super.setSelected(newSelection, selected);
        if (!newSelection.equals(wasSelected)) {
          showDetails(newSelection);
        }
      }
    };
    view.asPagingListView().setSelectionModel(selectionModel);
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
        if (view == null) {
          // This activity is dead
          return;
        }
        recordToRow.clear();
        idToRecord.clear();
        for (int i = 0, row = range.getStart(); i < values.size(); i++, row++) {
          R record = values.get(i);
          recordToRow.put(record.getId(), row);
          idToRecord.put(record.getId(), record);
        }
        getView().asPagingListView().setData(range.getStart(),
            range.getLength(), values);
        if (display != null) {
          display.showActivityWidget(getView());
        }
      }
    };

    createRangeRequest(range).forProperties(getView().getProperties()).to(
        callback).fire();
  }

  public void onStop() {
    view.setDelegate(null);
    view.asPagingListView().setDelegate(null);
    view = null;
  }

  /**
   * Select the record if it happens to be visible, or clear the selection if
   * called with null or "".
   */
  public void select(String id) {
    if (id == null || "".equals(id)) {
      R selected = selectionModel.getSelectedObject();
      if (selected != null) {
        selectionModel.setSelected(selected, false);
      }
    } else {
      R record = idToRecord.get(id);
      if (record != null) {
        selectionModel.setSelected(record, true);
      }
    }
  }

  public void start(Display display) {
    this.display = display;
    init();
  }

  public void update(WriteOperation writeOperation, R record) {
    switch (writeOperation) {
      case UPDATE:
        update(record);
        break;

      case DELETE:
        init();
        break;

      case CREATE:
        /*
         * On create, we presume the new record is at the end of the list, so
         * fetch the last page of items.
         */
        getLastPage();
        break;
    }
  }

  public boolean willStop() {
    return true;
  }

  protected abstract RecordListRequest<R> createRangeRequest(Range range);

  protected abstract void fireCountRequest(Receiver<Long> callback);

  protected abstract void showDetails(R record);

  private void getLastPage() {
    fireCountRequest(new Receiver<Long>() {
      public void onSuccess(Long response) {
        PagingListView<R> table = getView().asPagingListView();
        int rows = response.intValue();
        table.setDataSize(rows, true);
        int pageSize = table.getPageSize();
        int remnant = rows % pageSize;
        if (remnant == 0) {
          table.setPageStart(rows - pageSize);
        } else {
          table.setPageStart(rows - remnant);
        }
        onRangeChanged(table);
      }
    });
  }

  private void init() {
    fireCountRequest(new Receiver<Long>() {
      public void onSuccess(Long response) {
        getView().asPagingListView().setDataSize(response.intValue(), true);
        onRangeChanged(view.asPagingListView());
      }
    });
  }

  private void update(R record) {
    Integer row = recordToRow.get(record.getId());
    getView().asPagingListView().setData(row, 1,
        Collections.singletonList(record));
  }
}
