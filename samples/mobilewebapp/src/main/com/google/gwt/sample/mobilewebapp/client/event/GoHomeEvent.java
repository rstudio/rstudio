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

package com.google.gwt.sample.mobilewebapp.client.event;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Fired when the user wants to return to the main screen of the app.
 */
public class GoHomeEvent extends GwtEvent<GoHomeEvent.Handler> {

  /**
   * Implemented by objects that handle {@link GoHomeEvent}.
   */
  public interface Handler extends EventHandler {
    void onGoHome(GoHomeEvent event);
  }

  /**
   * The event type.
   */
  public static final Type<GoHomeEvent.Handler> TYPE = new Type<GoHomeEvent.Handler>();

  @Override
  public final Type<GoHomeEvent.Handler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(GoHomeEvent.Handler handler) {
    handler.onGoHome(this);
  }
}
