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

package com.google.gwt.sample.gaerequest.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.sample.gaerequest.shared.GaeUser;
import com.google.gwt.sample.gaerequest.shared.GaeUserServiceRequest;
import com.google.gwt.sample.gaerequest.shared.MakesGaeRequests;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window.Location;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.requestfactory.shared.Receiver;

/**
 * A simple widget which displays info about the user and a logout link. In real
 * life you'd probably blow this up into MVP parts.
 * <p>
 * On the other hand, it's pleasant that this widget is completely self
 * contained, taking care of its own RPC needs when awoken by being attached to
 * the dom. Hopefully that's not too magical.
 */
public class LoginWidget extends Composite {
  interface Binder extends UiBinder<Widget, LoginWidget> {
  }

  private static final Binder BINDER = GWT.create(Binder.class);

  private final MakesGaeRequests requests;

  @UiField
  SpanElement name;
  @UiField
  Anchor logoutLink;

  public LoginWidget(MakesGaeRequests requests) {
    this.requests = requests;
    initWidget(BINDER.createAndBindUi(this));
  }

  @Override
  protected void onLoad() {
    GaeUserServiceRequest request = requests.userServiceRequest();

    request.createLogoutURL(Location.getHref()).to(new Receiver<String>() {
      @Override
      public void onSuccess(String response) {
        setLogoutUrl(response);
      }
    });
    request.getCurrentUser().to(new Receiver<GaeUser>() {
      @Override
      public void onSuccess(GaeUser response) {
        setUserName(response.getNickname());
      }
    });
    request.fire();
  }

  public void setUserName(String userName) {
    name.setInnerText(userName);
  }

  public void setLogoutUrl(String url) {
    logoutLink.setHref(url);
  }

  /**
   * Squelch clicks of the logout link if no href has been set.
   */
  @UiHandler("logoutLink")
  void handleClick(ClickEvent e) {
    if ("".equals(logoutLink.getHref())) {
      e.stopPropagation();
    }
  }
}
