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
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.EntityProxyChange;
import com.google.gwt.requestfactory.shared.ProxyListRequest;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.requestfactory.shared.SyncResult;
import com.google.gwt.requestfactory.shared.WriteOperation;
import com.google.gwt.user.cellview.client.AbstractHasData;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.view.client.HasData;
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
 * Abstract activity for requesting and displaying a list of {@link EntityProxy}
 * .
 * <p>
 * Subclasses must:
 * 
 * <ul>
 * <li>implement methods to provide a full count, and request a specific
 * <li>provide a {@link ProxyListView}
 * <li>respond to "show details" commands
 * </ul>
 * 
 * Only the properties required by the view will be requested.
 * 
 * @param <P> the type of {@link EntityProxy} listed
 */
public abstract class AbstractProxyListActivity<P extends EntityProxy>
    implements Activity, ProxyListView.Delegate<P> {

  /**
   * This mapping allows us to update individual rows as records change.
   */
  private final Map<Long, Integer> recordToRow = new HashMap<Long, Integer>();

  private final RequestFactory requests;
  private final PlaceController placeController;
  private final SingleSelectionModel<P> selectionModel;
  private final Class<P> proxyType;

  /**
   * Used by the table and its selection model to rely on record id for
   * equality.
   */
  private final EntityProxyKeyProvider<P> keyProvider = new EntityProxyKeyProvider<P>();

  private HandlerRegistration rangeChangeHandler;
  private ProxyListView<P> view;
  private AcceptsOneWidget display;

  public AbstractProxyListActivity(RequestFactory requests,
      PlaceController placeController, ProxyListView<P> view, Class<P> proxyType) {
    this.view = view;
    this.requests = requests;
    this.placeController = placeController;
    this.proxyType = proxyType;
    view.setDelegate(this);

    final HasData<P> hasData = view.asHasData();
    rangeChangeHandler = hasData.addRangeChangeHandler(new RangeChangeEvent.Handler() {
      public void onRangeChange(RangeChangeEvent event) {
        AbstractProxyListActivity.this.onRangeChanged(hasData);
      }
    });

    selectionModel = new SingleSelectionModel<P>();
    selectionModel.setKeyProvider(keyProvider);
    hasData.setSelectionModel(selectionModel);
    ((AbstractHasData<P>) hasData).setKeyProvider(keyProvider);

    selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
      public void onSelectionChange(SelectionChangeEvent event) {
        P selectedObject = selectionModel.getSelectedObject();
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

  public ProxyListView<P> getView() {
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
  public void onRangeChanged(HasData<P> listView) {
    final Range range = listView.getVisibleRange();

    final Receiver<List<P>> callback = new Receiver<List<P>>() {
      public void onSuccess(List<P> values, Set<SyncResult> syncResults) {
        if (view == null) {
          // This activity is dead
          return;
        }
        recordToRow.clear();
        for (int i = 0, row = range.getStart(); i < values.size(); i++, row++) {
          P record = values.get(i);
          recordToRow.put(record.getId(), row);
        }
        getView().asHasData().setRowData(range.getStart(), values);
        if (display != null) {
          display.setWidget(getView());
        }
      }
    };

    fireRangeRequest(range, callback);
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
  public void select(P record) {
    if (record == null) {
      P selected = selectionModel.getSelectedObject();
      if (selected != null) {
        selectionModel.setSelected(selected, false);
      }
    } else {
      selectionModel.setSelected(record, true);
    }
  }

  public void start(AcceptsOneWidget display, EventBus eventBus) {
    EntityProxyChange.registerForProxyType(eventBus, proxyType,
        new EntityProxyChange.Handler<P>() {
          public void onProxyChange(EntityProxyChange<P> event) {
            update(event.getWriteOperation(), event.getProxy());
          }
        });
    eventBus.addHandler(PlaceChangeEvent.TYPE, new PlaceChangeEvent.Handler() {
      public void onPlaceChange(PlaceChangeEvent event) {
        updateSelection(event.getNewPlace());
      }
    });
    this.display = display;
    init();
    updateSelection(placeController.getWhere());
  }

  public void update(WriteOperation writeOperation, P record) {
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

  protected abstract ProxyListRequest<P> createRangeRequest(Range range);

  protected abstract void fireCountRequest(Receiver<Long> callback);

  /**
   * Called when the user chooses a record to view. This default implementation
   * sends the {@link PlaceController} to an appropriate {@link ProxyPlace}.
   * 
   * @param record the chosen record
   */
  protected void showDetails(P record) {
    placeController.goTo(new ProxyPlace(record, Operation.DETAILS));
  }

  private void fireRangeRequest(final Range range,
      final Receiver<List<P>> callback) {
    createRangeRequest(range).with(getView().getPaths()).fire(callback);
  }

  private void getLastPage() {
    fireCountRequest(new Receiver<Long>() {
      public void onSuccess(Long response, Set<SyncResult> syncResults) {
        HasData<P> table = getView().asHasData();
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
    select((P) ((ProxyPlace) newPlace).getProxy());
  }

  private void update(P record) {
    final Integer row = recordToRow.get(record.getId());
    if (row == null) {
      return;
    }
    fireRangeRequest(new Range(row, 1), new Receiver<List<P>>() {
      public void onSuccess(List<P> response, Set<SyncResult> syncResults) {
        getView().asHasData().setRowData(row,
            Collections.singletonList(response.get(0)));
      }
    });
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
