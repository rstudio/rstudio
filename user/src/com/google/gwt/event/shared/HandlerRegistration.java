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

/**
 * Registration returned from a call to
 * {@link HandlerManager#addHandler(com.google.gwt.event.shared.GwtEvent.Type, EventHandler)}
 * . Use the handler registration to remove handlers when they are no longer
 * needed.
 * 
 * Note, this interface is under the control of the {@link HandlerManager} class
 * and may be expanded over time, so extend {@link DefaultHandlerRegistration}
 * if you do not wish to get compiler errors if we extend the handler registry
 * functionality.
 */
public interface HandlerRegistration {
  /**
   * Removes the given handler from its manager.
   */
  void removeHandler();
}
