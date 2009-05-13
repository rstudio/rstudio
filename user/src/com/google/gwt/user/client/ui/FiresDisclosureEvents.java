/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.user.client.ui;

/**
 * A widget that implements this interface fires the events defined by the
 * {@link DisclosureHandler} interface.
 * 
 * @deprecated Use {@link com.google.gwt.event.logical.shared.HasOpenHandlers}
 *             and {@link com.google.gwt.event.logical.shared.HasCloseHandlers}
 *             instead
 */
@Deprecated
public interface FiresDisclosureEvents {

  /**
   * Adds a handler interface to receive open events.
   * 
   * @param handler the handler to add
   * @deprecated Add an open or close handler to the event source instead
   */
  @Deprecated
  void addEventHandler(DisclosureHandler handler);

  /**
   * Removes a previously added handler interface.
   * 
   * @param handler the handler to remove
   * @deprecated Use the
   * {@link com.google.gwt.event.shared.HandlerRegistration#removeHandler}
   * method on the object returned by an add*Handler method instead
   */
  @Deprecated
  void removeEventHandler(DisclosureHandler handler);
}
