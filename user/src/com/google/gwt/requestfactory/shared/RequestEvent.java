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
package com.google.gwt.requestfactory.shared;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.http.client.Response;

/**
 * An event posted whenever an RPC request is sent or its response is received.
 */
public class RequestEvent extends GwtEvent<RequestEvent.Handler> {
  
  /**
   * Implemented by handlers of this type of event.
   */
  public interface Handler extends EventHandler {
    /**
     * Called when a {@link RequestEvent} is fired.
     *
     * @param requestEvent a {@link RequestEvent} instance
     */
    void onRequestEvent(RequestEvent requestEvent);
  }

  /**
   * The request state.
   */
  public enum State {
    SENT, RECEIVED
  }

  private static final Type<Handler> TYPE = new Type<Handler>();

  /**
   * Register a {@link RequestEvent.Handler} on an {@link EventBus}.
   *
   * @param eventBus the {@link EventBus}
   * @param handler a {@link RequestEvent.Handler}
   * @return a {@link HandlerRegistration} instance
   */
  public static HandlerRegistration register(EventBus eventBus,
      RequestEvent.Handler handler) {
    return eventBus.addHandler(TYPE, handler);
  }

  private final State state;

  /**
   * Will only be non-null if this is an event of type {@link State#RECEIVED},
   * and the RPC was successful.
   */
  private final Response response;

  /**
   * Constructs a new @{link RequestEvent}.
   *
   * @param state a {@link State} instance
   * @param response a {@link Response} instance
   */
  public RequestEvent(State state, Response response) {
    this.state = state;
    this.response = response;
  }

  @Override
  public GwtEvent.Type<Handler> getAssociatedType() {
    return TYPE;
  }

  /**
   * Returns the {@link Response} associated with this event.
   *
   * @return a {@link Response} instance
   */
  public Response getResponse() {
    return response;
  }

  /**
   * Returns the {@link State} associated with this event.
   *
   * @return a {@link State} instance
   */
  public State getState() {
    return state;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onRequestEvent(this);
  }
}
