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
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.sample.mobilewebapp.client.ClientFactory;
import com.google.gwt.sample.mobilewebapp.client.place.TaskEditPlace;
import com.google.gwt.sample.mobilewebapp.client.place.TaskListPlace;
import com.google.gwt.sample.mobilewebapp.shared.TaskProxy;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.ServerFailure;

import java.util.Collections;
import java.util.List;

/**
 * Activity that presents a list of tasks.
 */
public class TaskListActivity extends AbstractActivity implements TaskListView.Presenter {

  /**
   * Event fired when the task list is updated.
   */
  public static class TaskListUpdateEvent extends GwtEvent<TaskListActivity.TaskListUpdateHandler> {

    /**
     * Handler type.
     */
    private static Type<TaskListUpdateHandler> TYPE;

    /**
     * Gets the type associated with this event.
     * 
     * @return returns the handler type
     */
    public static Type<TaskListUpdateHandler> getType() {
      if (TYPE == null) {
        TYPE = new Type<TaskListUpdateHandler>();
      }
      return TYPE;
    }

    private final List<TaskProxy> tasks;

    public TaskListUpdateEvent(List<TaskProxy> tasks) {
      this.tasks = tasks;
    }

    @Override
    public Type<TaskListUpdateHandler> getAssociatedType() {
      return TYPE;
    }

    public List<TaskProxy> getTasks() {
      return tasks;
    }

    @Override
    protected void dispatch(TaskListUpdateHandler handler) {
      handler.onTaskListUpdated(this);
    }
  }

  /**
   * Handler for {@link TaskListUpdateEvent}.
   */
  public static interface TaskListUpdateHandler extends EventHandler {

    /**
     * Called when the task list is updated.
     */
    void onTaskListUpdated(TaskListUpdateEvent event);
  }

  /**
   * The delay in milliseconds between calls to refresh the task list.
   */
  private static final int REFRESH_DELAY = 5000;

  /**
   * The handler that handlers add button clicks.
   */
  private final ClickHandler addButtonHandler = new ClickHandler() {
    public void onClick(ClickEvent event) {
      clientFactory.getPlaceController().goTo(TaskEditPlace.getTaskCreatePlace());
    }
  };

  /**
   * A boolean indicating that we should clear the task list when started.
   */
  private final boolean clearTaskList;

  private final ClientFactory clientFactory;

  /**
   * A boolean indicating whether or not this activity is still active. The user
   * might move to another activity while this one is loading, in which case we
   * do not want to do any more work.
   */
  private boolean isDead = false;

  /**
   * The refresh timer used to periodically refresh the task list.
   */
  private Timer refreshTimer;

  /**
   * Construct a new {@link TaskListActivity}.
   * 
   * @param clientFactory the {@link ClientFactory} of shared resources
   * @param place configuration for this activity
   */
  public TaskListActivity(ClientFactory clientFactory, TaskListPlace place) {
    this(clientFactory, place.isTaskListStale());
  }

  public TaskListActivity(ClientFactory clientFactory, boolean clearTaskList) {
    this.clientFactory = clientFactory;
    this.clearTaskList = clearTaskList;
  }

  public ClickHandler getAddButtonHandler() {
    return addButtonHandler;
  }

  @Override
  public void onCancel() {
    killActivity();
  }

  @Override
  public void onStop() {
    killActivity();
  }

  public void selectTask(TaskProxy selected) {
    // Go into edit mode when a task is selected.
    clientFactory.getPlaceController().goTo(
        TaskEditPlace.createTaskEditPlace(selected.getId(), selected));
  }

  public void start(AcceptsOneWidget container, EventBus eventBus) {
    // Add a handler to the 'add' button in the shell.
    clientFactory.getShell().setAddButtonHandler(addButtonHandler);

    // Set the presenter on the view.
    final TaskListView view = clientFactory.getTaskListView();
    view.setPresenter(this);

    // Clear the task list and display it.
    if (clearTaskList) {
      view.clearList();
    }
    container.setWidget(view);

    // Create a timer to periodically refresh the task list.
    refreshTimer = new Timer() {
      @Override
      public void run() {
        refreshTaskList();
      }
    };

    // Load the saved task list from storage
    List<TaskProxy> list = clientFactory.getTaskProxyLocalStorage().getTasks();
    setTasks(list);

    // Request the task list now.
    refreshTaskList();
  }

  /**
   * Kill this activity.
   */
  private void killActivity() {
    // Ignore all incoming responses to the requests from this activity.
    isDead = true;

    // Kill the refresh timer.
    if (refreshTimer != null) {
      refreshTimer.cancel();
    }
  }

  /**
   * Refresh the task list.
   */
  private void refreshTaskList() {
    clientFactory.getRequestFactory().taskRequest().findAllTasks().fire(
        new Receiver<List<TaskProxy>>() {
          @Override
          public void onFailure(ServerFailure error) {
            // ignore
          }

          @Override
          public void onSuccess(List<TaskProxy> response) {
            // Early exit if this activity has already been canceled.
            if (isDead) {
              return;
            }

            // Display the tasks in the view.
            if (response == null) {
              response = Collections.<TaskProxy> emptyList();
            }
            setTasks(response);

            // save the response to storage
            clientFactory.getTaskProxyLocalStorage().setTasks(response);

            // Restart the timer.
            refreshTimer.schedule(REFRESH_DELAY);
          }
        });
  }

  /**
   * Set the list of tasks.
   */
  private void setTasks(List<TaskProxy> tasks) {
    clientFactory.getTaskListView().setTasks(tasks);
    clientFactory.getEventBus().fireEventFromSource(new TaskListUpdateEvent(tasks), this);
  }
}
