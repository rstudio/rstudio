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

import com.google.gwt.app.place.ProxyPlace.Operation;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.RecordListRequest;
import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.user.cellview.client.AbstractHasData;
import com.google.gwt.valuestore.shared.Record;
import com.google.gwt.valuestore.shared.SyncResult;
import com.google.gwt.valuestore.shared.WriteOperation;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.RangeChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * Abstract activity for requesting and displaying a list of {@link Record}.
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
public abstract class AbstractRecordListActivity<R extends Record>
    implements Activity, RecordListView.Delegate<R> {
  /**
   * Used by the table and its selection model, to define record equality via
   * id.
   */
  private static class RecordKeyProvider<R extends Record> implements
      ProvidesKey<R> {
    public Object getKey(R item) {
      return item == null ? null : item.getId();
    }
  }

  /**
   * This mapping allows us to update individual rows as records change.
   */
  private final Map<Long, Integer> recordToRow = new HashMap<Long, Integer>();

  private final RequestFactory requests;
  private final PlaceController placeController;
  private final SingleSelectionModel<R> selectionModel;
  private final Class<R> proxyType;

  /**
   * Used by the table and its selection model to rely on record id for
   * equality.
   */
  private final RecordKeyProvider<R> keyProvider = new RecordKeyProvider<R>();

  private HandlerRegistration rangeChangeHandler;
  private RecordListView<R> view;
  private Display display;

  public AbstractRecordListActivity(RequestFactory requests,
      PlaceController placeController, RecordListView<R> view,
      Class<R> proxyType) {
    this.view = view;
    this.requests = requests;
    this.placeController = placeController;
    this.proxyType = proxyType;
    view.setDelegate(this);

    final HasData<R> hasData = view.asHasData();
    rangeChangeHandler = hasData.addRangeChangeHandler(
        new RangeChangeEvent.Handler() {
          public void onRangeChange(RangeChangeEvent event) {
            AbstractRecordListActivity.this.onRangeChanged(hasData);
          }
        });

    selectionModel = new SingleSelectionModel<R>();
    selectionModel.setKeyProvider(keyProvider);
    hasData.setSelectionModel(selectionModel);
    ((AbstractHasData<R>) hasData).setKeyProvider(keyProvider);

    selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
      public void onSelectionChange(SelectionChangeEvent event) {
        R selectedObject = selectionModel.getSelectedObject();
        if (selectedObject != null) {
          showDetails(selectedObject);
        }
      }
    });
  }

  public void createClicked() {
    placeController.goTo(new ProxyPlace(requests.create(proxyType),
        Operation.CREATE));
  }

  public RecordListView<R> getView() {
    return view;
  }

  public String mayStop() {
    return null;
  }

  public void onCancel() {
    onStop();
  }

  /**
   * Called by the table as it needs data.
   */
  public void onRangeChanged(HasData<R> listView) {
    final Range range = listView.getVisibleRange();

    final Receiver<List<R>> callback = new Receiver<List<R>>() {
      public void onSuccess(List<R> values, Set<SyncResult> syncResults) {
        if (view == null) {
          // This activity is dead
          return;
        }
        recordToRow.clear();
        for (int i = 0, row = range.getStart(); i < values.size(); i++, row++) {
          R record = values.get(i);
          recordToRow.put(record.getId(), row);
        }
        getView().asHasData().setRowValues(range.getStart(), values);
        if (display != null) {
          display.showActivityWidget(getView());
        }
      }
    };

    createRangeRequest(range).forProperties(getView().getProperties()).fire(
        callback);
  }

  public void onStop() {
    view.setDelegate(null);
    view = null;
    rangeChangeHandler.removeHandler();
    rangeChangeHandler = null;
  }

  /**
   * Select the given record, or clear the selection if called with null.
   */
  public void select(R record) {
    if (record == null) {
      R selected = selectionModel.getSelectedObject();
      if (selected != null) {
        selectionModel.setSelected(selected, false);
      }
    } else {
      selectionModel.setSelected(record, true);
    }
  }

  public void start(Display display, EventBus eventBus) {
    eventBus.addHandler(PlaceChangeEvent.TYPE, new PlaceChangeEvent.Handler() {
      public void onPlaceChange(PlaceChangeEvent event) {
        updateSelection(event.getNewPlace());
      }
    });
    this.display = display;
    init();
    updateSelection(placeController.getWhere());
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

  protected abstract RecordListRequest<R> createRangeRequest(Range range);

  protected abstract void fireCountRequest(Receiver<Long> callback);

  private void getLastPage() {
    fireCountRequest(new Receiver<Long>() {
      public void onSuccess(Long response, Set<SyncResult> syncResults) {
        HasData<R> table = getView().asHasData();
        int rows = response.intValue();
        table.setRowCount(rows, true);
        int pageSize = table.getVisibleRange().getLength();
        int remnant = rows % pageSize;
        if (remnant == 0) {
          table.setVisibleRange(rows - pageSize, pageSize);
        } else {
          table.setVisibleRange(rows - remnant, pageSize);
        }
        onRangeChanged(table);
      }
    });
  }

  private void init() {
    fireCountRequest(new Receiver<Long>() {
      public void onSuccess(Long response, Set<SyncResult> syncResults) {
        getView().asHasData().setRowCount(response.intValue(), true);
        onRangeChanged(view.asHasData());
      }
    });
  }

  @SuppressWarnings("unchecked")
  private void selectCoerced(Place newPlace) {
    select((R) ((ProxyPlace) newPlace).getProxy());
  }

  private void showDetails(R record) {
    placeController.goTo(new ProxyPlace(record, Operation.DETAILS));
  }

  private void update(R record) {
    Integer row = recordToRow.get(record.getId());
    getView().asHasData().setRowValues(row, Collections.singletonList(record));
  }

  private void updateSelection(Place newPlace) {
    if (newPlace instanceof ProxyPlace) {
      ProxyPlace proxyPlace = (ProxyPlace) newPlace;
      if (proxyPlace.getOperation() != Operation.CREATE
          && requests.getClass(proxyPlace.getProxy()).equals(proxyType)) {
        selectCoerced(newPlace);
        return;
      }
    }

    select(null);
  }
}
