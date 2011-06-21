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
import com.google.gwt.sample.mobilewebapp.client.event.TaskEditEvent;
import com.google.gwt.sample.mobilewebapp.shared.TaskProxy;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.requestfactory.shared.Receiver;

/**
 * Makes a TaskReadView display a task.
 */
public class TaskReadPresenter implements TaskReadView.Presenter {

  private final ClientFactory clientFactory;

  /**
   * A boolean indicating whether or not this activity is still active. The user
   * might move to another activity while this one is loading, in which case we
   * do not want to do any more work.
   */
  private boolean isDead = false;

  /**
   * The current task being displayed, might not be possible to edit it.
   */
  private TaskProxy task;

  /**
   * The ID of the current task being edited.
   */
  private final Long taskId;
  private EventBus eventBus;

  /**
   * Construct a new {@link TaskReadPresenter}.
   * 
   * @param clientFactory the {@link ClientFactory} of shared resources
   * @param place configuration for this activity
   */
  public TaskReadPresenter(ClientFactory clientFactory, TaskPlace place) {
    this.taskId = place.getTaskId();
    this.task = place.getTask();
    this.clientFactory = clientFactory;
    clientFactory.getTaskReadView().setPresenter(this);
  }

  @Override
  public Widget asWidget() {
    return getView().asWidget();
  }

  @Override
  public void editTask() {
    eventBus.fireEvent(new TaskEditEvent(task));
  }

  @Override
  public String mayStop() {
    return null;
  }

  public void start(EventBus newEventBus) {
    this.eventBus = newEventBus;

    // Hide the 'add' button in the shell.
    // TODO(rjrjr) Ick!
    clientFactory.getShell().setAddButtonVisible(false);

    // Try to load the task from local storage.
    if (task == null) {
      task = clientFactory.getTaskProxyLocalStorage().getTask(taskId);
    }

    if (task == null) {
      // Load the existing task.
      clientFactory.getRequestFactory().taskRequest().findTask(this.taskId).fire(
          new Receiver<TaskProxy>() {
            @Override
            public void onSuccess(TaskProxy response) {
              // Early exit if this activity has already been cancelled.
              if (isDead) {
                return;
              }

              // Task not found.
              if (response == null) {
                Window.alert("The task with id '" + taskId + "' could not be found."
                    + " Please select a different task from the task list.");
                ActionEvent.fire(eventBus, ActionNames.EDITING_CANCELED);
                return;
              }

              // Show the task.
              task = response;
              getView().getEditorDriver().edit(response);
            }
          });
    } else {
      // Use the task that was passed with the place.
      getView().getEditorDriver().edit(task);
    }
  }

  @Override
  public void stop() {
    eventBus = null;
    // Ignore all incoming responses to the requests from this activity.
    isDead = true;
  }

  private TaskReadView getView() {
    return clientFactory.getTaskReadView();
  }
}
