/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.sample.mobilewebapp.client.desktop;

import com.google.gwt.cell.client.DateCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.sample.mobilewebapp.client.activity.TaskListView;
import com.google.gwt.sample.mobilewebapp.shared.TaskProxy;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.NoSelectionModel;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionModel;

import java.util.Date;
import java.util.List;

/**
 * View used to display the list of Tasks.
 */
public class DesktopTaskListView extends Composite implements TaskListView {

  /**
   * The UiBinder interface.
   */
  interface DesktopTaskListViewUiBinder extends UiBinder<Widget, DesktopTaskListView> {
  }

  /**
   * The UiBinder used to generate the view.
   */
  private static DesktopTaskListViewUiBinder uiBinder =
      GWT.create(DesktopTaskListViewUiBinder.class);

  /**
   * Displays the list of tasks.
   */
  @UiField(provided = true)
  CellTable<TaskProxy> taskList;

  /**
   * The presenter for this view.
   */
  private Presenter presenter;

  /**
   * Construct a new {@link DesktopTaskListView}.
   */
  public DesktopTaskListView() {

    // Create the CellTable.
    taskList = new CellTable<TaskProxy>();
    taskList.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
    taskList.setWidth("100%");

    // Add the task name column.
    Column<TaskProxy, String> nameColumn = new TextColumn<TaskProxy>() {
      @Override
      public String getValue(TaskProxy object) {
        return (object == null) ? null : object.getName();
      }
    };
    taskList.addColumn(nameColumn, "Task");

    // Add the task notes column.
    Column<TaskProxy, String> notesColumn = new TextColumn<TaskProxy>() {
      @Override
      public String getValue(TaskProxy object) {
        return (object == null) ? "" : object.getNotes();
      }
    };
    taskList.addColumn(notesColumn, "Description");

    // Add the task due date column.
    Column<TaskProxy, Date> dateColumn = new Column<TaskProxy, Date>(new DateCell()) {
      @Override
      public Date getValue(TaskProxy object) {
        return (object == null) ? null : object.getDueDate();
      }
    };
    taskList.addColumn(dateColumn, "Due Date");

    /*
     * Inform the presenter when the user selects a task from the task list.
     */
    final NoSelectionModel<TaskProxy> selectionModel = new NoSelectionModel<TaskProxy>();
    taskList.setSelectionModel(selectionModel);
    selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
      public void onSelectionChange(SelectionChangeEvent event) {
        // Edit the task.
        if (presenter != null) {
          presenter.selectTask(selectionModel.getLastSelectedObject());
        }
      }
    });

    // Initialize the widget.
    initWidget(uiBinder.createAndBindUi(this));
  }

  public void clearList() {
    taskList.setVisibleRangeAndClearData(taskList.getVisibleRange(), true);
  }

  public void setPresenter(Presenter presenter) {
    this.presenter = presenter;
  }

  public void setSelectionModel(SelectionModel<TaskProxy> selectionModel) {
    taskList.setSelectionModel(selectionModel);
  }

  public void setTasks(List<TaskProxy> tasks) {
    taskList.setRowData(tasks);
  }

  @Override
  public void start() {
    //TODO
    throw new UnsupportedOperationException("Auto-generated method stub");
  }

  @Override
  public void stop() {
    //TODO
    throw new UnsupportedOperationException("Auto-generated method stub");
  }
}
