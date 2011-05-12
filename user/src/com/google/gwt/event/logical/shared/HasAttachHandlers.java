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
package com.google.gwt.event.logical.shared;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;

/**
 * A widget that implements this interface is a public source of
 * {@link AttachEvent} events.
 */
public interface HasAttachHandlers extends HasHandlers {
  /**
   * Adds an {@link AttachEvent} handler.
   * 
   * @param handler the handler
   * @return the handler registration
   */
  HandlerRegistration addAttachHandler(AttachEvent.Handler handler);

  /**
   * Returns whether or not the receiver is attached to the
   * {@link com.google.gwt.dom.client.Document Document}'s
   * {@link com.google.gwt.dom.client.BodyElement BodyElement}.
   * 
   * @return true if attached, false otherwise
   */
  boolean isAttached();
}
