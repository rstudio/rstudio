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

import java.util.List;

/**
 * Event fired when the task list is updated.
 */
public class TaskListUpdateEvent extends GwtEvent<TaskListUpdateEvent.Handler> {

  /**
   * Handler for {@link TaskListUpdateEvent}.
   */
  public interface Handler extends EventHandler {
  
    /**
     * Called when the task list is updated.
     */
    void onTaskListUpdated(TaskListUpdateEvent event);
  }

  public static final Type<TaskListUpdateEvent.Handler> TYPE = new Type<TaskListUpdateEvent.Handler>();

  private final List<TaskProxy> tasks;

  public TaskListUpdateEvent(List<TaskProxy> tasks) {
    this.tasks = tasks;
  }

  @Override
  public Type<TaskListUpdateEvent.Handler> getAssociatedType() {
    return TYPE;
  }

  public List<TaskProxy> getTasks() {
    return tasks;
  }

  @Override
  protected void dispatch(TaskListUpdateEvent.Handler handler) {
    handler.onTaskListUpdated(this);
  }
}