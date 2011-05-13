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
 * Fired when the user wants a new task.
 */
public class AddTaskEvent extends GwtEvent<AddTaskEvent.Handler> {

  /**
   * Implemented by objects that handle {@link AddTaskEvent}.
   */
  public interface Handler extends EventHandler {
    void onAddTask(AddTaskEvent event);
  }

  /**
   * The event type.
   */
  public static final Type<AddTaskEvent.Handler> TYPE = new Type<AddTaskEvent.Handler>();

  @Override
  public final Type<AddTaskEvent.Handler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(AddTaskEvent.Handler handler) {
    handler.onAddTask(this);
  }
}
