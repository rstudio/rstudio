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
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.sample.logexample.shared.MyService;
import com.google.gwt.sample.logexample.shared.MyServiceAsync;
import com.google.gwt.sample.logexample.shared.SharedClass;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Panel;

/**
 * A section allowing the user to experiment with server-side logging and
 * shared library logging when it is called from server-side vs. client-side code.
 */
public class ServerLoggingArea {
  interface MyUiBinder extends UiBinder<HTMLPanel, ServerLoggingArea> { }
  private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);
  private final MyServiceAsync myService =
    GWT.create(MyService.class);

  private Panel panel;

  public ServerLoggingArea() {
    panel = uiBinder.createAndBindUi(this);
  }

  public Panel getPanel() {
    return panel;
  }

  @UiHandler("clientSharedMethodButton")
  void handleClientSharedMethodButton(ClickEvent e) {
    SharedClass.doSomething("client");
  }

  @UiHandler("serverSharedMethodButton")
  void handleServerSharedMethodButton(ClickEvent e) {
    myService.doSomethingUsingSharedLibrary(new AsyncCallback<Void>() {

      public void onFailure(Throwable caught) {
        Window.alert("Call to my service failed");
      }

      public void onSuccess(Void result) {
      }
    });
  }

  @UiHandler("serverMethodButton")
  void handleServerMethodButton(ClickEvent e) {
    myService.doSomething(new AsyncCallback<Void>() {

      public void onFailure(Throwable caught) {
        Window.alert("Call to my service failed");
      }

      public void onSuccess(Void result) {
      }
    });
  }
}
