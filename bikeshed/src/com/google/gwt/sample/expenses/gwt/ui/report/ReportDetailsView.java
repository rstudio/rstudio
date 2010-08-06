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
package com.google.gwt.sample.expenses.gwt.ui.report;

import com.google.gwt.app.place.RecordDetailsView;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.sample.expenses.gwt.request.ReportRecord;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Details view for employee records.
 */
public class ReportDetailsView extends Composite implements RecordDetailsView<ReportRecord> {
  interface Binder extends UiBinder<HTMLPanel, ReportDetailsView> {
  }

  private static final Binder BINDER = GWT.create(Binder.class);

  ReportRecord record;

  @UiField
  SpanElement idSpan;
  @UiField
  SpanElement versionSpan;
  @UiField
  SpanElement notes;
  @UiField
  SpanElement purpose;
  @UiField
  SpanElement created;
  @UiField
  SpanElement reporterKey;
  @UiField
  SpanElement approvedSupervisorKey;
  @UiField
  HasClickHandlers edit;
  @UiField
  HasClickHandlers delete;

  private Delegate delegate;

  public ReportDetailsView() {
    initWidget(BINDER.createAndBindUi(this));
  }

  public Widget asWidget() {
    return this;
  }

  public boolean confirm(String msg) {
    return Window.confirm(msg);
  }

  public ReportRecord getValue() {
    return record;
  }

  @UiHandler("delete")
  public void onDeleteClicked(@SuppressWarnings("unused") ClickEvent e) {
    delegate.deleteClicked();
  }

  @UiHandler("edit")
  public void onEditClicked(@SuppressWarnings("unused") ClickEvent e) {
    delegate.editClicked();
  }

  public void setDelegate(Delegate delegate) {
    this.delegate = delegate;
  }

  public void setValue(ReportRecord record) {
    this.record = record;
    purpose.setInnerText(record.getPurpose());
    notes.setInnerText(record.getNotes());
    created.setInnerText(DateTimeFormat.getShortDateFormat().format(record.getCreated()));
    idSpan.setInnerText(record.getId().toString());
    versionSpan.setInnerText(record.getVersion().toString());
    reporterKey.setInnerText(String.valueOf(record.getReporterKey()));
    approvedSupervisorKey.setInnerText(String.valueOf(record.getApprovedSupervisorKey()));
  }
}
