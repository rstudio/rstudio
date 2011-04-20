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
package com.google.gwt.sample.mobilewebapp.client.activity;

import com.google.gwt.user.client.ui.IsWidget;

import java.util.Date;

/**
 * A view of {@link TaskEditActivity}.
 */
public interface TaskEditView extends IsWidget {

  /**
   * The presenter for this view.
   */
  public static interface Presenter {
    /**
     * Delete the current task or cancel the creation of a task.
     */
    void deleteTask();

    /**
     * Create a new task or save the current task based on the values in the
     * inputs.
     * 
     * @param addToCalendar true to add the task to the calendar
     */
    void saveTask(boolean addToCalendar);
  }

  Date getDueDate();

  String getName();

  String getNotes();

  /**
   * Set the due date of the task.
   */
  void setDueDate(Date date);

  /**
   * Specify whether the view is editing an existing task or creating a new
   * task.
   * 
   * @param isEditing true if editing, false if creating
   */
  void setEditing(boolean isEditing);

  /**
   * Lock or unlock the UI so the user cannot enter data. The UI is locked until
   * the task is loaded.
   * 
   * @param locked true to lock, false to unlock
   */
  void setLocked(boolean locked);

  /**
   * Set the task name.
   */
  void setName(String name);

  /**
   * Set the notes associated with the task.
   */
  void setNotes(String notes);

  /**
   * Set the {@link Presenter} for this view.
   * 
   * @param presenter the presenter
   */
  void setPresenter(Presenter presenter);
}
