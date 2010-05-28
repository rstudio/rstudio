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
import com.google.gwt.sample.expenses.gwt.request.EmployeeRecord;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.valuestore.ui.RecordDetailsView;

/**
 * Details view for employee records.
 */
public class EmployeeDetailsView extends Composite implements
    RecordDetailsView<EmployeeRecord> {
  interface Binder extends UiBinder<HTMLPanel, EmployeeDetailsView> {
  }

  private static final Binder BINDER = GWT.create(Binder.class);

  EmployeeRecord record;

  @UiField
  SpanElement idSpan;
  @UiField
  SpanElement versionSpan;
  @UiField
  SpanElement displayName;
  @UiField
  SpanElement userName;
  @UiField
  SpanElement password;
  @UiField
  SpanElement supervisorKey;
  @UiField
  Button edit;

  private Delegate delegate;

  public EmployeeDetailsView() {
    initWidget(BINDER.createAndBindUi(this));
  }

  public Widget asWidget() {
    return this;
  }

  public EmployeeRecord getValue() {
    return record;
  }

  @UiHandler("edit")
  public void onEditClicked(@SuppressWarnings("unused") ClickEvent e) {
    delegate.editClicked();
  }

  public void setDelegate(Delegate delegate) {
    this.delegate = delegate;
  }

  public void setValue(EmployeeRecord record) {
    this.record = record;
    displayName.setInnerText(record.getDisplayName());
    userName.setInnerText(record.getUserName());
    password.setInnerText(record.getPassword());
    supervisorKey.setInnerText(record.getSupervisorKey());
    idSpan.setInnerText(record.getId());
    versionSpan.setInnerText(record.getVersion().toString());
  }
}
