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
package com.google.gwt.sample.dynatablerf.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.TextArea;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * A simple glasspanel popup that terminates interaction with the application.
 */
class ErrorDialog {
  interface Binder extends UiBinder<DialogBox, ErrorDialog> {
  }

  @UiField
  DialogBox errorDialog;

  @UiField
  TextArea errorMessage;

  public ErrorDialog() {
    GWT.<Binder> create(Binder.class).createAndBindUi(this);
  }

  /**
   * @return
   */
  public Handler getHandler() {
    return new Handler() {
      {
        setLevel(Level.SEVERE);
      }

      @Override
      public void close() {
      }

      @Override
      public void flush() {
      }

      @Override
      public void publish(LogRecord record) {
        if (isLoggable(record)) {
          errorMessage.setText(record.getMessage());
          errorDialog.center();
        }
      }
    };
  }

  @UiHandler("dismiss")
  void onDismiss(ClickEvent event) {
    errorDialog.hide();
  }

  @UiHandler("reload")
  void onReload(ClickEvent event) {
    Window.Location.reload();
  }
}