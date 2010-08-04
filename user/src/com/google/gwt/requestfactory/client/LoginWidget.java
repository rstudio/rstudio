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

package com.google.gwt.requestfactory.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.requestfactory.shared.UserInformationRecord;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window.Location;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

/**
 * A simple widget which displays info about the user and a logout link.
 */
public class LoginWidget extends Composite {
  interface Binder extends UiBinder<Widget, LoginWidget> { }
  private static final Binder BINDER = GWT.create(Binder.class);

  @UiField SpanElement name;
  String logoutUrl = "";
  
  public LoginWidget() {
    initWidget(BINDER.createAndBindUi(this));
  }
  
  public void setUserInformation(UserInformationRecord info) {
    name.setInnerText(info.getName());
    logoutUrl = info.getLogoutUrl();
  }
  
  @UiHandler("logoutLink")
  void handleClick(ClickEvent e) {
    if (logoutUrl != "") {
      Location.replace(logoutUrl);
    }
  }
  
}
