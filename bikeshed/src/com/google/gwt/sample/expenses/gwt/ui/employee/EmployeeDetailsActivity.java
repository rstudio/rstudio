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
package com.google.gwt.sample.expenses.gwt.ui.employee;

import com.google.gwt.app.place.AbstractActivity;
import com.google.gwt.app.place.PlaceController;
import com.google.gwt.app.place.ProxyPlace;
import com.google.gwt.app.place.RecordDetailsView;
import com.google.gwt.app.place.ProxyPlace.Operation;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.RequestObject;
import com.google.gwt.requestfactory.shared.SyncResult;
import com.google.gwt.requestfactory.shared.Value;
import com.google.gwt.sample.expenses.gwt.request.EmployeeRecord;
import com.google.gwt.sample.expenses.gwt.request.ExpensesRequestFactory;

import java.util.Set;

/**
 * An {@link com.google.gwt.app.place.Activity Activity} that requests and
 * displays detailed information on a given employee.
 */
public class EmployeeDetailsActivity extends AbstractActivity implements
    RecordDetailsView.Delegate {
  private static RecordDetailsView<EmployeeRecord> defaultView;

  private static RecordDetailsView<EmployeeRecord> getDefaultView() {
    if (defaultView == null) {
      defaultView = new EmployeeDetailsView();
    }
    return defaultView;
  }

  private final ExpensesRequestFactory requests;
  private final PlaceController placeController;
  private final RecordDetailsView<EmployeeRecord> view;
  private EmployeeRecord record;
  private Display display;

  /**
   * Creates an activity that uses the default singleton view instance.
   */
  public EmployeeDetailsActivity(EmployeeRecord proxy,
      ExpensesRequestFactory requests, PlaceController placeController) {
    this(proxy, requests, placeController, getDefaultView());
  }

  /**
   * Creates an activity that uses its own view instance.
   */
  public EmployeeDetailsActivity(EmployeeRecord proxy,
      ExpensesRequestFactory requests, PlaceController placeController,
      RecordDetailsView<EmployeeRecord> view) {
    this.placeController = placeController;
    this.record = proxy;
    this.requests = requests;
    view.setDelegate(this);
    this.view = view;
  }

  public void deleteClicked() {
    if (!view.confirm("Really delete this record? You cannot undo this change.")) {
      return;
    }

    RequestObject<Void> deleteRequest = requests.employeeRequest().remove(view.getValue());
    deleteRequest.fire(new Receiver<Void>() {

      public void onSuccess(Void ignore, Set<SyncResult> response) {
        if (display == null) {
          // This activity is dead
          return;
        }

        display.showActivityWidget(null);
      }
    });
  }

  public void editClicked() {
    placeController.goTo(new ProxyPlace(view.getValue(), Operation.EDIT));
  }

  @Override
  public void onCancel() {
    onStop();
  }
  
  @Override
  public void onStop() {
    display = null;
  }

  public void start(Display displayIn, EventBus eventBus) {
    this.display = displayIn;
    Receiver<EmployeeRecord> callback = new Receiver<EmployeeRecord>() {
      public void onSuccess(EmployeeRecord record, Set<SyncResult> syncResults) {
        if (display == null) {
          return;
        }
        view.setValue(record);
        display.showActivityWidget(view);
      }
    };

    requests.employeeRequest().findEmployee(Value.of(record.getId())).with("supervisor").fire(callback);
  }
}
