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
import com.google.gwt.sample.mobilewebapp.presenter.tasklist.TaskListView;
import com.google.gwt.sample.mobilewebapp.shared.TaskProxy;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.view.client.NoSelectionModel;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionModel;

import java.util.Date;
import java.util.List;

/**
 * View used to display the list of Tasks.
 */
public class DesktopTaskListView extends ResizeComposite implements TaskListView {

  /**
   * Displays the list of tasks.
   */
  DataGrid<TaskProxy> taskList;

  /**
   * The presenter for this view.
   */
  private Presenter presenter;

  /**
   * Construct a new {@link DesktopTaskListView}.
   */
  public DesktopTaskListView() {

    // Create the CellTable.
    taskList = new DataGrid<TaskProxy>();
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
      @Override
      public void onSelectionChange(SelectionChangeEvent event) {
        // Edit the task.
        if (presenter != null) {
          presenter.selectTask(selectionModel.getLastSelectedObject());
        }
      }
    });

    // Initialize the widget.
    initWidget(taskList);
  }

  @Override
  public void clearList() {
    taskList.setVisibleRangeAndClearData(taskList.getVisibleRange(), true);
  }

  public void setPresenter(Presenter presenter) {
    if (this.presenter != null) {
      this.presenter.stop();
    }
    this.presenter = presenter;
  }

  public void setSelectionModel(SelectionModel<TaskProxy> selectionModel) {
    taskList.setSelectionModel(selectionModel);
  }

  @Override
  public void setTasks(List<TaskProxy> tasks) {
    taskList.setRowData(tasks);
  }
}
