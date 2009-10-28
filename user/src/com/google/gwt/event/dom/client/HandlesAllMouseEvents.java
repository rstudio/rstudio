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
 * Receiver used to handle all mouse events at once.
 * 
 * WARNING, PLEASE READ: As this class is intended for developers who wish to
 * handle all mouse events in GWT, new mouse handler interfaces will be added to
 * it. Therefore, updates to GWT could cause breaking API changes.
 * 
 */
public abstract class HandlesAllMouseEvents implements MouseDownHandler,
    MouseUpHandler, MouseMoveHandler, MouseOutHandler, MouseOverHandler,
    MouseWheelHandler {

  /**
   * Convenience method used to handle all mouse events from an event source.
   * 
   * @param <H> receiver type, must implement all mouse handlers
   * @param source the event source
   * @param reciever the receiver implementing all mouse handlers
   */
  public static <H extends MouseDownHandler & MouseUpHandler & MouseOutHandler & MouseOverHandler & MouseMoveHandler & MouseWheelHandler> void handle(
      HasAllMouseHandlers source, H reciever) {
    source.addMouseDownHandler(reciever);
    source.addMouseUpHandler(reciever);
    source.addMouseOutHandler(reciever);
    source.addMouseOverHandler(reciever);
    source.addMouseMoveHandler(reciever);
    source.addMouseWheelHandler(reciever);
  }

  /**
   * Constructor.
   */
  public HandlesAllMouseEvents() {
  }

  /**
   * Convenience method to handle all mouse events from an event source.
   * 
   * @param eventSource the event source
   */
  public void handle(HasAllMouseHandlers eventSource) {
    handle(eventSource, this);
  }
}