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
package com.google.gwt.sample.dynatablerf.client.widgets;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.SyncResult;
import com.google.gwt.requestfactory.shared.WriteOperation;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.sample.dynatablerf.client.events.EditPersonEvent;
import com.google.gwt.sample.dynatablerf.shared.DynaTableRequestFactory;
import com.google.gwt.sample.dynatablerf.shared.PersonProxy;
import com.google.gwt.sample.dynatablerf.shared.PersonProxyChanged;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.CellTable.CleanStyle;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.RangeChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * A paging table with summaries of all known people.
 */
public class SummaryWidget extends Composite {

  interface Binder extends UiBinder<Widget, SummaryWidget> {
  }

  interface Style extends CssResource {
    String thirty();
  }

  interface TableResources extends CellTable.CleanResources {
    @Source(value = {
        "com/google/gwt/user/cellview/client/CellTableClean.css",
        "CellTablePatch.css"})
    CleanStyle cellTableStyle();
  }

  private class DescriptionColumn extends Column<PersonProxy, String> {
    public DescriptionColumn() {
      super(new TextCell());
    }

    @Override
    public String getValue(PersonProxy object) {
      return object.getDescription();
    }
  }

  private class NameColumn extends Column<PersonProxy, String> {
    public NameColumn() {
      super(new TextCell());
    }

    @Override
    public String getValue(PersonProxy object) {
      return object.getName();
    }
  }

  private class ScheduleColumn extends Column<PersonProxy, String> {
    public ScheduleColumn() {
      super(new TextCell());
    }

    @Override
    public String getValue(PersonProxy object) {
      return object.getSchedule();
    }
  }

  @UiField
  DockLayoutPanel dock;

  @UiField
  Style style;

  @UiField(provided = true)
  SimplePager pager = new SimplePager();

  @UiField(provided = true)
  CellTable<PersonProxy> table;

  private final EventBus eventBus;
  private int numRows;
  private final DynaTableRequestFactory requestFactory;
  private final SingleSelectionModel<PersonProxy> selectionModel = new SingleSelectionModel<PersonProxy>();

  public SummaryWidget(EventBus eventBus,
      DynaTableRequestFactory requestFactory, int numRows) {
    this.eventBus = eventBus;
    this.requestFactory = requestFactory;
    this.numRows = numRows;

    table = new CellTable<PersonProxy>(numRows,
        GWT.<TableResources> create(TableResources.class));
    initWidget(GWT.<Binder> create(Binder.class).createAndBindUi(this));
    dock.getWidgetContainerElement(table).getStyle().setProperty("overflowY",
        "visible");

    table.addColumn(new NameColumn(), "Name");
    table.addColumn(new DescriptionColumn(), "Description");
    table.addColumn(new ScheduleColumn(), "Schedule");
    table.addColumnStyleName(0, style.thirty());
    table.addColumnStyleName(1, style.thirty());
    table.setRowCount(numRows, false);
    table.setSelectionModel(selectionModel);

    eventBus.addHandler(PersonProxyChanged.TYPE,
        new PersonProxyChanged.Handler() {
          public void onPersonChanged(PersonProxyChanged event) {
            SummaryWidget.this.onPersonChanged(event);
          }
        });

    selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
      public void onSelectionChange(SelectionChangeEvent event) {
        SummaryWidget.this.onSelectionChange(event);
      }
    });

    fetch(0);
  }

  void onPersonChanged(PersonProxyChanged event) {
    if (WriteOperation.UPDATE.equals(event.getWriteOperation())) {
      PersonProxy record = event.getProxy();

      // Is the changing record onscreen?
      int displayOffset = offsetOf(record);
      if (displayOffset != -1) {
        // Record is onscreen and may differ from our data
        requestFactory.personRequest().findPerson(record.getId()).fire(
            new Receiver<PersonProxy>() {
              public void onSuccess(PersonProxy response,
                  Set<SyncResult> syncResults) {
                // Re-check offset in case of changes while waiting for data
                int offset = offsetOf(response);
                if (offset != -1) {
                  table.setRowData(table.getPageStart() + offset,
                      Collections.singletonList(response));
                }
              }
            });
      }
    }
  }

  @UiHandler("table")
  void onRangeChange(RangeChangeEvent event) {
    Range r = event.getNewRange();
    int start = r.getStart();

    fetch(start);
  }

  void onSelectionChange(SelectionChangeEvent event) {
    PersonProxy person = selectionModel.getSelectedObject();
    if (person == null) {
      return;
    }
    SummaryWidget.this.eventBus.fireEvent(new EditPersonEvent(person));
    selectionModel.setSelected(person, false);
  }

  private void fetch(final int start) {
    requestFactory.schoolCalendarRequest().getPeople(start, numRows).fire(
        new Receiver<List<PersonProxy>>() {
          public void onSuccess(List<PersonProxy> response,
              Set<SyncResult> syncResults) {
            int responses = response.size();
            table.setRowData(start, response);
            if (!table.isRowCountExact()) {
              table.setRowCount(start + responses, responses < numRows);
            }
          }
        });
  }

  private int offsetOf(PersonProxy person) {
    long lookFor = person.getId();
    List<PersonProxy> displayedItems = table.getDisplayedItems();
    for (int offset = 0, j = displayedItems.size(); offset < j; offset++) {
      if (lookFor == displayedItems.get(offset).getId()) {
        return offset;
      }
    }
    return -1;
  }
}
