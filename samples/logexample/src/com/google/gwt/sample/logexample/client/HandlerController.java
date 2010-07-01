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
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.logging.client.ConsoleLogHandler;
import com.google.gwt.logging.client.DevelopmentModeLogHandler;
import com.google.gwt.logging.client.FirebugLogHandler;
import com.google.gwt.logging.client.HasWidgetsLogHandler;
import com.google.gwt.logging.client.SimpleRemoteLogHandler;
import com.google.gwt.logging.client.SystemLogHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Panel;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Logger;

/**
 * A section allowing the user to enable and disable the different handlers
 * attached to the Root Logger.
 */
public class HandlerController {
  private class CheckboxHandler implements ValueChangeHandler<Boolean> {
    private CheckBox checkbox;
    private Handler handler;

    public CheckboxHandler(CheckBox checkbox, Handler handler) {
      this.checkbox = checkbox;
      this.handler = handler;
    }

    public void onValueChange(ValueChangeEvent<Boolean> event) {
      if (checkbox.getValue()) {
        logger.addHandler(handler);
      } else {
        logger.removeHandler(handler);
      }
    }
  }

  interface MyUiBinder extends UiBinder<HTMLPanel, HandlerController> { }

  private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);
  @UiField CheckBox consoleCheckbox;
  @UiField CheckBox devmodeCheckbox;
  @UiField CheckBox firebugCheckbox;
  @UiField CheckBox popupCheckbox;
  @UiField CheckBox systemCheckbox;
  @UiField CheckBox remoteCheckbox;
  private Map<String, Handler> handlers;
  private Logger logger;
  private Panel panel;

  public HandlerController(final Logger logger) {
    this.logger = logger;
    panel = uiBinder.createAndBindUi(this);
    Handler[] handlersArray = logger.getHandlers();
    handlers = new HashMap<String, Handler>();
    if (handlersArray != null) {
      for (Handler h : handlersArray) {
        handlers.put(h.getClass().getName(), h);
      }
    }
    setupHandler(SystemLogHandler.class, systemCheckbox);
    setupHandler(ConsoleLogHandler.class, consoleCheckbox);
    setupHandler(DevelopmentModeLogHandler.class, devmodeCheckbox);
    setupHandler(FirebugLogHandler.class, firebugCheckbox);
    setupHandler(HasWidgetsLogHandler.class, popupCheckbox);
    setupHandler(SimpleRemoteLogHandler.class, remoteCheckbox);
  }

  public Panel getPanel() {
    return panel;
  }

  void setupHandler(Class clazz, CheckBox checkbox) {
    Handler h = handlers.get(clazz.getName());
    if (h == null) {
      checkbox.setEnabled(false);
    } else {
      checkbox.setValue(true);
      checkbox.addValueChangeHandler(new CheckboxHandler(checkbox, h));
    }
  }
}
