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
package com.google.gwt.sample.mobilewebapp.client.mobile;

import com.google.gwt.core.client.GWT;
import com.google.gwt.sample.mobilewebapp.client.ProvidesPresenter;
import com.google.gwt.sample.mobilewebapp.client.activity.TaskListView;
import com.google.gwt.sample.mobilewebapp.shared.TaskProxy;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.NoSelectionModel;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionModel;

import java.util.List;

/**
 * View used to display the list of Tasks.
 */
public class MobileTaskListView extends Composite implements TaskListView {
  /**
   * Resources used by the mobile CellList.
   */
  static interface CellListResources extends CellList.Resources {
    @Source({CellList.Style.DEFAULT_CSS, "MobileCellList.css"})
    CellListStyle cellListStyle();
  }


  /**
   * Styles used by the mobile CellList.
   */
  static interface CellListStyle extends CellList.Style {
  }

  /**
   * The UiBinder interface.
   */
  interface MobileTaskListViewUiBinder extends
      UiBinder<Widget, MobileTaskListView> {
  }

  /**
   * Provides a new presenter for each session with this view.
   */
  private final ProvidesPresenter<TaskListView.Presenter, TaskListView> presenterFactory;

  /**
   * The UiBinder used to generate the view.
   */
  private static MobileTaskListViewUiBinder uiBinder = GWT
      .create(MobileTaskListViewUiBinder.class);

  /**
   * Displays the list of tasks.
   */
  @UiField(provided = true)
  CellList<TaskProxy> taskList;

  /**
   * The presenter for this view.
   */
  private Presenter presenter;

  /**
   * Construct a new {@link MobileTaskListView}.
   */
  public MobileTaskListView(ProvidesPresenter<TaskListView.Presenter, TaskListView> presenterFactory) {
    this.presenterFactory = presenterFactory;
    
    // Create the CellList.
    CellListResources cellListRes = GWT.create(CellListResources.class);
    taskList = new CellList<TaskProxy>(new TaskProxyCell(), cellListRes);
    taskList.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);

    /*
     * Inform the presenter when the user selects a task from the task list. We
     * use a NoSelectionModel because we don't want the task to remain selected,
     * we just want to be notified of the selection event.
     */
    final NoSelectionModel<TaskProxy> selectionModel = new NoSelectionModel<TaskProxy>();
    taskList.setSelectionModel(selectionModel);
    selectionModel
        .addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
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
    setPresenter(presenterFactory.getPresenter(this));
  }

  @Override
  public void stop() {
    presenter.onStop();
  }
}
