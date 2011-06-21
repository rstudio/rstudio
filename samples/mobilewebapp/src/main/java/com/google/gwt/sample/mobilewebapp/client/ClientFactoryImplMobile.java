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

import com.google.gwt.sample.mobilewebapp.client.mobile.MobileTaskEditView;
import com.google.gwt.sample.mobilewebapp.client.mobile.MobileTaskListView;
import com.google.gwt.sample.mobilewebapp.client.mobile.MobileTaskReadView;
import com.google.gwt.sample.mobilewebapp.client.mobile.MobileWebAppShellMobile;
import com.google.gwt.sample.mobilewebapp.presenter.task.TaskEditView;
import com.google.gwt.sample.mobilewebapp.presenter.task.TaskReadView;
import com.google.gwt.sample.mobilewebapp.presenter.tasklist.TaskListView;
import com.google.gwt.sample.ui.client.OrientationHelper;
import com.google.gwt.sample.ui.client.WindowBasedOrientationHelper;

/**
 * Mobile version of {@link ClientFactory}.
 */
public class ClientFactoryImplMobile extends ClientFactoryImpl {
  private final OrientationHelper orientationHelper = new WindowBasedOrientationHelper();

  @Override
  protected MobileWebAppShell createShell() {
    return new MobileWebAppShellMobile(getEventBus(), orientationHelper, getTaskListView(),
        getTaskEditView(), getTaskReadView());
  }

  @Override
  protected TaskEditView createTaskEditView() {
    return new MobileTaskEditView();
  }

  @Override
  protected TaskListView createTaskListView() {
    return new MobileTaskListView();
  }

  @Override
  protected TaskReadView createTaskReadView() {
    return new MobileTaskReadView();
  }
}
