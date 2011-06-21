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
import com.google.gwt.sample.mobilewebapp.client.ClientFactory;
import com.google.gwt.sample.mobilewebapp.client.event.TaskEditEvent;
import com.google.gwt.sample.mobilewebapp.presenter.task.TaskEditPresenter;
import com.google.gwt.sample.mobilewebapp.presenter.task.TaskPlace;
import com.google.gwt.sample.mobilewebapp.presenter.task.TaskReadPresenter;
import com.google.gwt.sample.mobilewebapp.shared.TaskProxy;
import com.google.gwt.sample.ui.client.PresentsWidgets;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.web.bindery.event.shared.ResettableEventBus;

/**
 * An activity that shows details on a particular task, and allows the user to
 * edit it.
 */
public class TaskActivity extends AbstractActivity {
  private PresentsWidgets presenter;

  private final TaskPlace place;

  private final ClientFactory clientFactory;

  private ResettableEventBus childEventBus;

  /**
   * Construct a new {@link TaskActivity}.
   * 
   * @param clientFactory the {@link ClientFactory} of shared resources
   * @param place configuration for this activity
   */
  public TaskActivity(ClientFactory clientFactory, TaskPlace place) {
    this.place = place;
    this.clientFactory = clientFactory;
  }

  @Override
  public String mayStop() {
    return presenter.mayStop();
  }

  @Override
  public void onCancel() {
    presenter.stop();
  }

  @Override
  public void onStop() {
    childEventBus.removeHandlers();
    presenter.stop();
  }

  public void start(final AcceptsOneWidget container, com.google.gwt.event.shared.EventBus eventBus) {
    this.childEventBus = new ResettableEventBus(eventBus);
    eventBus.addHandler(TaskEditEvent.TYPE, new TaskEditEvent.Handler() {
      @Override
      public void onTaskEdit(TaskEditEvent event) {
        // Stop the read presenter
        onStop();
        presenter = startEdit(event.getReadOnlyTask());
        container.setWidget(presenter);
      }
    });

    if (place.getTaskId() == null) {
      presenter = startCreate();
    } else {
      presenter = startDisplay(place);
    }
    container.setWidget(presenter);
  }

  private PresentsWidgets startCreate() {
    PresentsWidgets rtn = new TaskEditPresenter(clientFactory);
    rtn.start(childEventBus);
    return rtn;
  }

  private PresentsWidgets startDisplay(TaskPlace place) {
    PresentsWidgets rtn = new TaskReadPresenter(clientFactory, place);
    rtn.start(childEventBus);
    return rtn;
  }

  private PresentsWidgets startEdit(TaskProxy readOnlyTask) {
    PresentsWidgets rtn = new TaskEditPresenter(clientFactory, readOnlyTask);
    rtn.start(childEventBus);
    return rtn;
  }
}
