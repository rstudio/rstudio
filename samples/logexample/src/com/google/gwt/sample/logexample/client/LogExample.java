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

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.logging.client.HasWidgetsLogHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * A page to help users understand logging in GWT.
 */
public class LogExample implements EntryPoint {
  interface MyUiBinder extends UiBinder<HTMLPanel, LogExample> { }
  private static Logger childLogger = Logger.getLogger("ParentLogger.Child");
  private static Logger parentLogger = Logger.getLogger("ParentLogger");
  private static Logger rootLogger = Logger.getLogger("");
  private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);
  @UiField SimplePanel customLogArea;
  @UiField SimplePanel handlerControls;
  @UiField SimplePanel loggerControls;
  @UiField SimplePanel logOnServerButton;

  public void onModuleLoad() {
    HTMLPanel p = uiBinder.createAndBindUi(this);
    RootPanel.get().add(p);

    loggerControls.setWidget(new LoggerController(
        rootLogger, parentLogger, childLogger).getPanel());
    handlerControls.setWidget(new HandlerController(rootLogger).getPanel());
    logOnServerButton.setWidget(new ServerLoggingArea().getPanel());
    customLogArea.setWidget(new CustomLogArea(childLogger).getPanel());
    
    // This is kind of hacky, but we want the user to see this explanation
    // in the popup when the page starts up, so we pull out the popup handler
    // and publish a message directly to it. Most applications should not
    // do this.
    Handler[] handlers = Logger.getLogger("").getHandlers();
    if (handlers != null) {
      for (Handler h : handlers) {
        if (h instanceof HasWidgetsLogHandler) {
          String msg = "This popup can be resized, moved and minimized";
          h.publish(new LogRecord(Level.SEVERE, msg));
        }
      }
    }
  }
}
