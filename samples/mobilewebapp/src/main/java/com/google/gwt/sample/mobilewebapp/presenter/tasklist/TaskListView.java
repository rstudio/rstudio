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
package com.google.gwt.sample.mobilewebapp.presenter.tasklist;

import com.google.gwt.sample.mobilewebapp.shared.TaskProxy;
import com.google.gwt.sample.ui.client.PresentsWidgets;
import com.google.gwt.user.client.ui.IsWidget;

import java.util.List;

/**
 * Implemented by views that display a list of tasks.
 */
public interface TaskListView extends IsWidget {

  /**
   * The presenter for this view.
   */
  public interface Presenter extends PresentsWidgets {
    /**
     * Select a task.
     * 
     * @param selected the select task
     */
    void selectTask(TaskProxy selected);
  }

  /**
   * Clear the list of tasks.
   */
  void clearList();

  /**
   * Sets the new presenter, and calls {@link Presenter#stop()} on the previous
   * one.
   */
  void setPresenter(Presenter presenter);

  /**
   * Set the list of tasks to display.
   * 
   * @param tasks the list of tasks
   */
  void setTasks(List<TaskProxy> tasks);
}
