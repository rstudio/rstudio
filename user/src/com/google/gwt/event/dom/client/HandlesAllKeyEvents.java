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

package com.google.gwt.event.dom.client;

/**
 * Receiver used to handle all key events at once.
 * 
 * WARNING, PLEASE READ: As this class is intended for developers who wish to
 * handle all key events in GWT, new key handler interfaces will be added to it.
 * Therefore, updates to GWT could cause breaking API changes.
 * 
 */
public abstract class HandlesAllKeyEvents implements KeyDownHandler,
    KeyUpHandler, KeyPressHandler {

  /**
   * Convenience method used to handle all key events from an event source.
   * 
   * @param <H> receiver type, must implement all key handlers
   * @param eventSource the event source
   * @param reciever the receiver implementing all key handlers
   */
  public static <H extends KeyDownHandler & KeyUpHandler & KeyPressHandler> void addHandlers(
      HasAllKeyHandlers eventSource, H reciever) {
    eventSource.addKeyDownHandler(reciever);
    eventSource.addKeyPressHandler(reciever);
    eventSource.addKeyUpHandler(reciever);
  }

  /**
   * Constructor.
   */
  public HandlesAllKeyEvents() {
  }

  /**
   * Convenience method to handle all key events from an event source.
   * 
   * @param source the event source
   */
  public final void addKeyHandlersTo(HasAllKeyHandlers source) {
    addHandlers(source, this);
  }
}
