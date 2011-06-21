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

import com.google.web.bindery.event.shared.Event;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;


/**
 * An event posted when an authentication failure is detected.
 */
public class GaeAuthenticationFailureEvent extends Event<GaeAuthenticationFailureEvent.Handler> {

  /**
   * Implemented by handlers of this type of event.
   */
  public interface Handler {
    /**
     * Called when a {@link GaeAuthenticationFailureEvent} is fired.
     * 
     * @param requestEvent a {@link GaeAuthenticationFailureEvent} instance
     */
    void onAuthFailure(GaeAuthenticationFailureEvent requestEvent);
  }

  private static final Type<Handler> TYPE = new Type<Handler>();

  /**
   * Register a {@link GaeAuthenticationFailureEvent.Handler} on an
   * {@link EventBus}.
   * 
   * @param eventBus the {@link EventBus}
   * @param handler a {@link GaeAuthenticationFailureEvent.Handler}
   * @return a {@link HandlerRegistration} instance
   */
  public static HandlerRegistration register(EventBus eventBus,
      GaeAuthenticationFailureEvent.Handler handler) {
    return eventBus.addHandler(TYPE, handler);
  }

  private final String loginUrl;

  /**
   * Constructs a new @{link RequestEvent}.
   * 
   * @param loginUrl the url used to login
   */
  public GaeAuthenticationFailureEvent(String loginUrl) {
    this.loginUrl = loginUrl;
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  /**
   * Returns the URL the user can visit to reauthenticate.
   * 
   * @return a {@link com.google.gwt.http.client.Response} instance
   */
  public String getLoginUrl() {
    return loginUrl;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onAuthFailure(this);
  }
}
