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

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.sample.expenses.gwt.request.EmployeeRecord;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.valuestore.shared.DeltaValueStore;
import com.google.gwt.valuestore.shared.Property;
import com.google.gwt.valuestore.ui.RecordEditView;

import java.util.HashSet;
import java.util.Set;

/**
 * Edit view for employee records.
 */
public class EmployeeEditView extends Composite implements
    RecordEditView<EmployeeRecord> {
  interface Binder extends UiBinder<HTMLPanel, EmployeeEditView> {
  }

  private static final Binder BINDER = GWT.create(Binder.class);

  @UiField
  TextBox displayName;
  @UiField
  TextBox userName;
  @UiField
  Button save;
  @UiField
  SpanElement idSpan;

  private Delegate delegate;
  private DeltaValueStore deltas;
  
  private EmployeeRecord record;

  public EmployeeEditView() {
    initWidget(BINDER.createAndBindUi(this));
  }

  public EmployeeEditView asWidget() {
    return this;
  }

  public Set<Property<?>> getProperties() {
    Set<Property<?>> rtn = new HashSet<Property<?>>();
    rtn.add(EmployeeRecord.userName);
    rtn.add(EmployeeRecord.displayName);
    return rtn;
  }

  public void setDelegate(Delegate delegate) {
    this.delegate = delegate;
  }

  public void setDeltaValueStore(DeltaValueStore deltas) {
    this.deltas = deltas;
  }

  public void setEnabled(boolean enabled) {
    displayName.setEnabled(enabled);
    userName.setEnabled(enabled);
    save.setEnabled(enabled);
  }

  public void setValue(EmployeeRecord value) {
    this.record = value;
    displayName.setValue(record.getDisplayName());
    userName.setValue(record.getUserName());
    idSpan.setInnerText(record.getId());
  }

  @UiHandler("displayName")
  void onDisplayNameChange(ValueChangeEvent<String> event) {
    deltas.set(EmployeeRecord.displayName, record, event.getValue());
  }

  @UiHandler("save")
  void onSave(@SuppressWarnings("unused") ClickEvent event) {
    delegate.saveClicked();
  }

  @UiHandler("userName")
  void onUserNameChange(ValueChangeEvent<String> event) {
    deltas.set(EmployeeRecord.userName, record, event.getValue());
  }
}
