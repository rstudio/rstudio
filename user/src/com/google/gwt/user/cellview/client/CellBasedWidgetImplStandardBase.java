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
package com.google.gwt.user.cellview.client;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

/**
 * StandardBase implementation of {@link CellBasedWidgetImpl}.
 */
public class CellBasedWidgetImplStandardBase extends CellBasedWidgetImplStandard {

  @Override
  public void resetFocus(ScheduledCommand command) {
    // Some browsers will not focus an element that was created in this event loop.
    Scheduler.get().scheduleDeferred(command);
  }
}
