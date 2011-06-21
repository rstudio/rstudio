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
package com.google.gwt.sample.mobilewebapp.presenter.task;

import com.google.gwt.sample.mobilewebapp.client.ClientFactory;
import com.google.gwt.sample.mobilewebapp.client.event.ActionEvent;
import com.google.gwt.sample.mobilewebapp.client.event.ActionNames;
import com.google.gwt.sample.mobilewebapp.client.ui.SoundEffects;
import com.google.gwt.sample.mobilewebapp.shared.TaskProxy;
import com.google.gwt.sample.mobilewebapp.shared.TaskRequest;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.ServerFailure;

import java.util.Set;
import java.util.logging.Logger;

import javax.validation.ConstraintViolation;

/**
 * Drives a {@link TaskEditView} to fetch and edit a given task, or to create a
 * new one.
 */
public class TaskEditPresenter implements TaskEditView.Presenter {

  private static final Logger log = Logger.getLogger(TaskEditPresenter.class.getName());
  private final ClientFactory clientFactory;

  /**
   * Indicates whether the activity is editing an existing task or creating a
   * new task.
   */
  private boolean isEditing;

  /**
   * The current task being edited, provided by RequestFactory.
   */
  private TaskProxy editTask;

  /**
   * The ID of the current task being edited.
   */
  private final Long taskId;

  /**
   * The request used to persist the modified task.
   */
  private Request<Void> taskPersistRequest;
  private EventBus eventBus;

  /**
   * For creating a new task.
   */
  public TaskEditPresenter(ClientFactory clientFactory) {
    this.taskId = null;
    this.clientFactory = clientFactory;
    clientFactory.getTaskEditView().setPresenter(this);
  }

  /**
   * For editing an existing task.
   */
  public TaskEditPresenter(ClientFactory clientFactory, TaskProxy readOnlyTask) {
    /*
     * TODO surely we can find a way to show the read-only values while waiting
     * for the async fetch
     */
    this.taskId = readOnlyTask.getId();
    this.clientFactory = clientFactory;
    clientFactory.getTaskEditView().setPresenter(this);
  }

  @Override
  public Widget asWidget() {
    return getView().asWidget();
  }

  public void deleteTask() {
    if (isEditing) {
      doDeleteTask();
    } else {
      doCancelTask();
    }
  }

  @Override
  public String mayStop() {
    if ((eventBus != null && editTask != null) && getView().getEditorDriver().isDirty()) {
      return "Are you sure you want to discard these changes?";
    }
    return null;
  }

  public void saveTask() {
    // Flush the changes into the editable task.
    TaskRequest context = (TaskRequest) clientFactory.getTaskEditView().getEditorDriver().flush();

    /*
     * Create a persist request the first time we try to save this task. If a
     * request already exists, reuse it.
     */
    if (taskPersistRequest == null) {
      taskPersistRequest = context.persist().using(editTask);
    }

    // Fire the request.
    taskPersistRequest.fire(new Receiver<Void>() {
      @Override
      public void onConstraintViolation(Set<ConstraintViolation<?>> violations) {
        handleConstraintViolations(violations);
      }

      @Override
      public void onSuccess(Void response) {
        editTask = null;

        // Notify the user that the task was updated.
        TaskEditPresenter.this.notify("Task Saved");
        
        // Return to the task list.
        ActionEvent.fire(eventBus, ActionNames.TASK_SAVED);
      }
    });
  }

  public void start(EventBus eventBus) {
    this.eventBus = eventBus;
    getView().setNameViolation(null);

    // Prefetch the sounds used in this activity.
    SoundEffects.get().prefetchError();

    // Hide the 'add' button in the shell.
    // TODO(rjrjr) Ick!
    clientFactory.getShell().setAddButtonVisible(false);

    if (taskId == null) {
      startCreate();
    } else {
      startEdit();
    }
  }

  @Override
  public void stop() {
    eventBus = null;
    clientFactory.getTaskEditView().setLocked(false);
  }

  /**
   * Cancel the current task.
   */
  private void doCancelTask() {
    ActionEvent.fire(eventBus, ActionNames.EDITING_CANCELED);
  }

  /**
   * Delete the current task.
   */
  private void doDeleteTask() {
    if (editTask == null) {
      return;
    }

    // Delete the task in the data store.
    final TaskProxy toDelete = this.editTask;
    clientFactory.getRequestFactory().taskRequest().remove().using(toDelete).fire(
        new Receiver<Void>() {
          @Override
          public void onFailure(ServerFailure error) {
            Window.alert("An error occurred on the server while deleting this task: \"."
                + error.getMessage() + "\".");
          }

          @Override
          public void onSuccess(Void response) {
            onTaskDeleted();
          }
        });
  }

  private TaskEditView getView() {
    return clientFactory.getTaskEditView();
  }

  /**
   * Handle constraint violations.
   */
  private void handleConstraintViolations(Set<ConstraintViolation<?>> violations) {
    // Display the violations.
    getView().getEditorDriver().setConstraintViolations(violations);

    // Play a sound.
    SoundEffects.get().playError();
  }

  /**
   * Notify the user of a message.
   * 
   * @param message the message to display
   */
  private void notify(String message) {
    // TODO Add notification pop-up
    log.fine("Tell the user: " + message);
  }

  /**
   * Called when a task has been successfully deleted.
   */
  private void onTaskDeleted() {
    // Notify the user that the task was deleted.
    notify("Task Deleted");

    // Return to the task list.
    ActionEvent.fire(eventBus, ActionNames.TASK_SAVED);
  }

  private void startCreate() {
    isEditing = false;
    getView().setEditing(false);
    TaskRequest request = clientFactory.getRequestFactory().taskRequest();
    editTask = request.create(TaskProxy.class);
    getView().getEditorDriver().edit(editTask, request);
  }

  private void startEdit() {
    isEditing = true;
    getView().setEditing(true);
    // Lock the display until the task is loaded.
    getView().setLocked(true);
    clientFactory.getRequestFactory().taskRequest().findTask(this.taskId).fire(
        new Receiver<TaskProxy>() {
          @Override
          public void onConstraintViolation(Set<ConstraintViolation<?>> violations) {
            getView().setLocked(false);
            getView().getEditorDriver().setConstraintViolations(violations);
          }

          @Override
          public void onFailure(ServerFailure error) {
            getView().setLocked(false);
            doCancelTask();
            super.onFailure(error);
          }

          @Override
          public void onSuccess(TaskProxy response) {
            // Early exit if we have already stopped.
            if (eventBus == null) {
              return;
            }

            // Task not found.
            if (response == null) {
              Window.alert("The task with id '" + taskId + "' could not be found."
                  + " Please select a different task from the task list.");
              doCancelTask();
              return;
            }

            // Show the task.
            editTask = response;
            getView().getEditorDriver().edit(response,
                clientFactory.getRequestFactory().taskRequest());
            getView().setLocked(false);
          }
        });
  }
}
