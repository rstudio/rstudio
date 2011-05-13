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
package com.google.gwt.sample.mobilewebapp.client;

import com.google.gwt.sample.mobilewebapp.client.activity.TaskEditView;
import com.google.gwt.sample.mobilewebapp.client.activity.TaskListActivity;
import com.google.gwt.sample.mobilewebapp.client.activity.TaskListView;
import com.google.gwt.sample.mobilewebapp.client.activity.TaskListView.Presenter;
import com.google.gwt.sample.mobilewebapp.client.activity.TaskReadView;
import com.google.gwt.sample.mobilewebapp.client.mobile.MobileTaskEditView;
import com.google.gwt.sample.mobilewebapp.client.mobile.MobileTaskListView;
import com.google.gwt.sample.mobilewebapp.client.mobile.MobileTaskReadView;
import com.google.gwt.sample.mobilewebapp.client.mobile.MobileWebAppShellMobile;
import com.google.gwt.sample.mobilewebapp.client.ui.OrientationHelper;
import com.google.gwt.sample.mobilewebapp.client.ui.WindowBasedOrientationHelper;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;

/**
 * Mobile version of {@link ClientFactory}.
 */
public class ClientFactoryImplMobile extends ClientFactoryImpl {
  private final OrientationHelper orientationHelper = new WindowBasedOrientationHelper();
  
  @Override
  protected MobileWebAppShell createShell() {
    return new MobileWebAppShellMobile(orientationHelper, getTaskListView(),
        getTaskEditView(), getTaskReadView(), getEventBus());
  }

  @Override
  protected TaskEditView createTaskEditView() {
    return new MobileTaskEditView();
  }

  @Override
  protected TaskListView createTaskListView() {
    ProvidesPresenter<TaskListView.Presenter, TaskListView> factory =
      new ProvidesPresenter<TaskListView.Presenter, TaskListView>() {
        @Override
        public Presenter getPresenter(TaskListView view) {
          TaskListActivity taskListActivity = new TaskListActivity(ClientFactoryImplMobile.this, false);
          taskListActivity.start(new AcceptsOneWidget() {
            @Override
            public void setWidget(IsWidget w) {
              // No op until we separate presenter and activity
            }
          }, getEventBus());
          return taskListActivity;
        }
      };
    return new MobileTaskListView(factory);
  }

  @Override
  protected TaskReadView createTaskReadView() {
    return new MobileTaskReadView();
  }
}
