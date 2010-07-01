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
import com.google.gwt.logging.client.HasWidgetsLogHandler;
import com.google.gwt.logging.client.LogConfiguration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;

import java.util.logging.Logger;

/**
 * An example of a custom logging area using the HasWidgetsLogHandler.
 */
public class CustomLogArea {
  interface MyUiBinder extends UiBinder<HTMLPanel, CustomLogArea> { }
  private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);
  @UiField VerticalPanel customLogArea;
  private Panel panel;

  public CustomLogArea(Logger logger) {
    panel = uiBinder.createAndBindUi(this);

    // An example of adding our own custom logging area.  Since VerticalPanel
    // extends HasWidgets, and handles multiple calls to add(widget) gracefully
    // we simply create a new HasWidgetsLogHandler with it, and add that
    // handler to a logger. In this case, we add it to a particular logger in
    // order to demonstrate how the logger hierarchy works, but adding it to the
    // root logger would be fine. Note that we guard this code with a call to
    // LogConfiguration.loggingIsEnabled(). Although this code will compile out
    // without this check in web mode, the guard will ensure that the handler
    // does not show up in development mode.
    if (LogConfiguration.loggingIsEnabled()) {
      logger.addHandler(new HasWidgetsLogHandler(customLogArea));
    }
  }

  public Panel getPanel() {
    return panel;
  }
}
