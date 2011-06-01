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
import com.google.gwt.sample.mobilewebapp.shared.TaskProxy;

/**
 * Fired when the user wants to see a task.
 */
public class ShowTaskEvent extends GwtEvent<ShowTaskEvent.Handler> {

  /**
   * Implemented by objects that handle {@link ShowTaskEvent}.
   */
  public interface Handler extends EventHandler {
    void onShowTask(ShowTaskEvent event);
  }

  private final TaskProxy task;
  
  /**
   * The event type.
   */
  public static final Type<ShowTaskEvent.Handler> TYPE = new Type<ShowTaskEvent.Handler>();

  public ShowTaskEvent(TaskProxy task) {
    this.task = task;
  }
 
  @Override
  public final Type<ShowTaskEvent.Handler> getAssociatedType() {
    return TYPE;
  }

  /**
   * Get the task to edit.
   * 
   * @return the task to edit, or null if not available
   */
  public TaskProxy getTask() {
    return task;
  }
  
  @Override
  protected void dispatch(ShowTaskEvent.Handler handler) {
    handler.onShowTask(this);
  }
}
