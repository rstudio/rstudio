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

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.http.client.Response;

/**
 * An event posted when an authentication failure is detected.
 */
public class GaeAuthenticationFailureEvent extends GwtEvent<GaeAuthenticationFailureEvent.Handler> {
  
  /**
   * Implemented by handlers of this type of event.
   */
  public interface Handler extends EventHandler {
    /**
     * Called when a {@link GaeAuthenticationFailureEvent} is fired.
     *
     * @param requestEvent a {@link GaeAuthenticationFailureEvent} instance
     */
    void onAuthFailure(GaeAuthenticationFailureEvent requestEvent);
  }

  private static final Type<Handler> TYPE = new Type<Handler>();

  /**
   * Register a {@link GaeAuthenticationFailureEvent.Handler} on an {@link EventBus}.
   *
   * @param eventBus the {@link EventBus}
   * @param handler a {@link GaeAuthenticationFailureEvent.Handler}
   * @return a {@link HandlerRegistration} instance
   */
  public static HandlerRegistration register(EventBus eventBus,
      GaeAuthenticationFailureEvent.Handler handler) {
    return eventBus.addHandler(TYPE, handler);
  }

  /**
   * Will only be non-null if this is an event of type {@link State#RECEIVED},
   * and the RPC was successful.
   */
  private final String loginUrl;

  /**
   * Constructs a new @{link RequestEvent}.
   *
   * @param state a {@link State} instance
   * @param response a {@link Response} instance
   */
  public GaeAuthenticationFailureEvent(String loginUrl) {
    this.loginUrl = loginUrl;
  }

  @Override
  public GwtEvent.Type<Handler> getAssociatedType() {
    return TYPE;
  }

  /**
   * Returns the URL the user can visit to reauthenticate.
   * 
   * @return a {@link Response} instance
   */
  public String getLoginUrl() {
    return loginUrl;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onAuthFailure(this);
  }
}
