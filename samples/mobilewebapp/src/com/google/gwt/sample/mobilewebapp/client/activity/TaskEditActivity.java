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

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.sample.mobilewebapp.client.ClientFactory;
import com.google.gwt.sample.mobilewebapp.client.TaskRequest;
import com.google.gwt.sample.mobilewebapp.client.place.TaskEditPlace;
import com.google.gwt.sample.mobilewebapp.client.place.TaskListPlace;
import com.google.gwt.sample.mobilewebapp.shared.TaskProxy;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.web.bindery.requestfactory.shared.Receiver;

/**
 * Activity that presents a task to be edited.
 */
public class TaskEditActivity extends AbstractActivity implements TaskEditView.Presenter {

  private static final int ONE_HOUR = 3600000;

  private final ClientFactory clientFactory;

  /**
   * A boolean indicating whether or not this activity is still active. The user
   * might move to another activity while this one is loading, in which case we
   * do not want to do any more work.
   */
  private boolean isDead = false;

  /**
   * The current task being edited.
   */
  private TaskProxy task;

  /**
   * The ID of the current task being edited.
   */
  private final Long taskId;

  /**
   * Construct a new {@link TaskEditActivity}.
   * 
   * @param place the task being edited
   * @param clientFactory the {@link ClientFactory} of shared resources
   */
  public TaskEditActivity(TaskEditPlace place, ClientFactory clientFactory) {
    this.taskId = place.getTaskId();
    this.clientFactory = clientFactory;
  }

  public void deleteTask() {
    if (task == null) {
      doCancelTask();
    } else {
      doDeleteTask();
    }
  }

  @Override
  public void onCancel() {
    // Ignore all incoming responses to the requests from this activity.
    isDead = true;
    clientFactory.getTaskEditView().setLocked(false);
  }

  @Override
  public void onStop() {
    // Ignore all incoming responses to the requests from this activity.
    isDead = true;
    clientFactory.getTaskEditView().setLocked(false);
  }

  public void saveTask(boolean addToCalendar) {
    if (task == null) {
      doCreateTask(addToCalendar);
    } else {
      doUpdateTask();
    }
  }

  public void start(AcceptsOneWidget container, EventBus eventBus) {
    // Hide the 'add' button in the shell.
    clientFactory.getShell().setAddButtonHandler(null);

    // Set the presenter on the view.
    final TaskEditView view = clientFactory.getTaskEditView();
    view.setPresenter(this);

    // Clear the display until the task is loaded.
    showTask(null);

    if (taskId != null) {
      // Lock the display until the task is loaded.
      view.setLocked(true);

      // Load the existing task.
      clientFactory.getRequestFactory().taskRequest().findTask(this.taskId).fire(
          new Receiver<TaskProxy>() {
            @Override
            public void onSuccess(TaskProxy response) {
              // Early exit if this activity has already been cancelled.
              if (isDead) {
                return;
              }

              // Show the task.
              showTask(response);
              view.setLocked(false);
            }
          });
    }

    // Display the view.
    container.setWidget(view.asWidget());
  }

  /**
   * Cancel the current task.
   */
  private void doCancelTask() {
    clientFactory.getPlaceController().goTo(new TaskListPlace(false));
  }

  /**
   * Create a new task.
   * 
   * @param addToCalendar true to add the task to the calendar
   */
  private void doCreateTask(final boolean addToCalendar) {
    TaskRequest request = clientFactory.getRequestFactory().taskRequest();
    final TaskProxy toCreate = request.create(TaskProxy.class);
    populateTaskFromView(toCreate);
    request.persist().using(toCreate).fire(new Receiver<Void>() {
      @Override
      public void onSuccess(Void response) {
        onTaskCreated(toCreate, addToCalendar);
      }
    });
  }

  /**
   * Delete the current task.
   */
  private void doDeleteTask() {
    if (task == null) {
      return;
    }

    // Delete the task in the data store.
    final TaskProxy toDelete = this.task;
    clientFactory.getRequestFactory().taskRequest().remove().using(toDelete).fire(
        new Receiver<Void>() {
          @Override
          public void onSuccess(Void response) {
            onTaskDeleted(toDelete);
          }
        });
  }

  /**
   * Update the current task.
   */
  private void doUpdateTask() {
    if (task == null) {
      return;
    }

    // Create a mutable version of the current task.
    TaskRequest request = clientFactory.getRequestFactory().taskRequest();
    final TaskProxy toEdit = request.edit(task);
    populateTaskFromView(toEdit);

    // Persist the changes.
    request.persist().using(toEdit).fire(new Receiver<Void>() {
      @Override
      public void onSuccess(Void response) {
        onTaskUpdated(toEdit);
      }
    });
  }

  /**
   * Notify the user of a message.
   * 
   * @param message the message to display
   */
  private void notify(String message) {
    // TODO Add notification pop-up
  }

  /**
   * Called when a task has been successfully created.
   * 
   * @param task the task that was created
   * @param addToCalendar true to add the task to the calendar
   */
  private void onTaskCreated(TaskProxy task, boolean addToCalendar) {
    // Notify the user that the task was created.
    notify("Created task '" + task.getName() + "'");

    // Return to the task list.
    clientFactory.getPlaceController().goTo(new TaskListPlace(true));
  }

  /**
   * Called when a task has been successfully deleted.
   * 
   * @param task the task that was deleted
   */
  private void onTaskDeleted(TaskProxy task) {
    // Notify the user that the task was deleted.
    notify("Task Deleted");

    // Return to the task list.
    clientFactory.getPlaceController().goTo(new TaskListPlace(true));
  }

  /**
   * Called when a task has been successfully updated.
   * 
   * @param task the task that was updated
   */
  private void onTaskUpdated(TaskProxy task) {
    // Notify the user that the task was updated.
    notify("Task Updated");

    // Return to the task list.
    clientFactory.getPlaceController().goTo(new TaskListPlace(true));
  }

  /**
   * Populate the specified task using the values the user specified in the
   * view.
   * 
   * @param task the task to populate
   */
  private void populateTaskFromView(TaskProxy task) {
    TaskEditView view = clientFactory.getTaskEditView();
    task.setName(view.getName());
    task.setNotes(view.getNotes());
    task.setDueDate(view.getDueDate());
  }

  /**
   * Show the specified task in the view.
   * 
   * @param task the task to show
   */
  private void showTask(TaskProxy task) {
    this.task = task;
    TaskEditView view = clientFactory.getTaskEditView();
    if (task == null) {
      // Create a new task.
      view.setEditing(false);
      view.setDueDate(null);
      view.setName("");
      view.setNotes("");
    } else {
      // Edit an existing task.
      view.setEditing(true);
      view.setDueDate(task.getDueDate());
      view.setName(task.getName());
      view.setNotes(task.getNotes());
    }
  }
}
