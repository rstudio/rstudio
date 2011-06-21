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
import com.google.gwt.activity.shared.Activity;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.sample.mobilewebapp.client.ClientFactory;
import com.google.gwt.sample.mobilewebapp.presenter.task.TaskPlace;
import com.google.gwt.sample.mobilewebapp.presenter.tasklist.TaskListPlace;
import com.google.gwt.sample.mobilewebapp.presenter.tasklist.TaskListPresenter;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

/**
 * A mapping of places to activities used by this application.
 */
public class AppActivityMapper implements ActivityMapper {

  private final ClientFactory clientFactory;

  public AppActivityMapper(ClientFactory clientFactory) {
    this.clientFactory = clientFactory;
  }

  public Activity getActivity(final Place place) {
    if (place instanceof TaskListPlace) {
      // The list of tasks.
      return new AbstractActivity() {
        @Override
        public void start(AcceptsOneWidget panel, EventBus eventBus) {
          TaskListPresenter presenter = new TaskListPresenter(clientFactory, (TaskListPlace) place);
          presenter.start(eventBus);
          panel.setWidget(presenter);
        }

        /*
         * Note no call to presenter.stop(). The TaskListViews do that
         * themselves as a side effect of setPresenter.
         */
      };
    }

    if (place instanceof TaskPlace) {
      // Editable view of a task.
      return new TaskActivity(clientFactory, (TaskPlace) place);
    }

    return null;
  }
}
