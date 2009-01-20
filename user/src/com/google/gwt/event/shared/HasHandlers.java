/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.event.shared;

import com.google.gwt.event.shared.GwtEvent.Type;

/**
 * An object that implements this interface has a collection of event handlers
 * associated with it.
 */
public interface HasHandlers {

  /**
   * Determines whether the there are any handlers of the given type. This may
   * be used to avoid creating events for which there are no handlers.
   * 
   * @param type the type of event to be queried
   * @return <code>true</code> if there are any handlers for this event type
   */
  boolean isEventHandled(Type<?> type);

  /**
   * Fires the given event to all the appropriate handlers.
   * 
   * @param event the event to be fired
   */
  void fireEvent(GwtEvent<?> event);
}
