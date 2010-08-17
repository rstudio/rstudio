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
package com.google.gwt.app.place;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.HandlerRegistration;

/**
 * Implemented by objects that monitor {@link PlaceChangeEvent}s and
 * {@link com.google.gwt.user.client.History} events and keep them in sync.
 */
public interface PlaceHistoryHandler {
  /**
   * Sets the current place from current history token, e.g. in case of being
   * launched from a bookmark.
   */
  void handleCurrentHistory();

  /**
   * Registers this {@link PlaceHistoryHandler} with the event bus, and sets its
   * default place (where to go when there is no history token).
   * 
   * @return registration object to deregister and reset the default place to
   *         {@link Place#NOWHERE}.
   */
  HandlerRegistration register(PlaceController placeController, EventBus eventBus,
      Place defaultPlace);
}
