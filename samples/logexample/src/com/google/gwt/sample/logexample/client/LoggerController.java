/*
 * Copyright 2010 Google Inc.
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

package com.google.gwt.sample.logexample.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;

import java.util.logging.Logger;

/**
 * A section explaining how to set logger levels and containing controllers
 * for 3 specific loggers.
 */
public class LoggerController {
  interface MyUiBinder extends UiBinder<HTMLPanel, LoggerController> { }
  private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);
  @UiField SimplePanel childControls;
  @UiField SimplePanel parentControls;
  @UiField SimplePanel rootControls;
  private Panel panel;

  public LoggerController(Logger rootLogger, Logger parentLogger,
      Logger childLogger) {
    panel = uiBinder.createAndBindUi(this);
    rootControls.setWidget(
        new OneLoggerController(rootLogger, "Root Logger").getPanel());
    parentControls.setWidget(
        new OneLoggerController(parentLogger, "ParentLogger").getPanel());
    childControls.setWidget(
        new OneLoggerController(childLogger, "ParentLogger.Child").getPanel());
  }

  public Panel getPanel() {
    return panel;
  }
}
