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

package com.google.gwt.sample.gaerequest.client;

import com.google.gwt.user.client.Window.Location;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;

/**
 * A minimal auth failure handler which takes the user a login page.
 */
public class ReloadOnAuthenticationFailure implements GaeAuthenticationFailureEvent.Handler {

  public void onAuthFailure(GaeAuthenticationFailureEvent requestEvent) {
    Location.replace(requestEvent.getLoginUrl());
  }

  public HandlerRegistration register(EventBus eventBus) {
    return GaeAuthenticationFailureEvent.register(eventBus, this);
  }
}
