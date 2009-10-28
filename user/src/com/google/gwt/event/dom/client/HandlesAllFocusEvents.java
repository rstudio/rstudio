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
 * Receiver used to handle all focus events at once.
 */
public abstract class HandlesAllFocusEvents implements FocusHandler,
    BlurHandler {

  /**
   * Convenience method used to handle both focus and blur events from an event
   * source.
   * 
   * @param <H> receiver type, must implement both {@link FocusHandler} and
   *          {@link BlurHandler} handlers
   * @param eventSource the event source
   * @param reciever the receiver implementing both focus and blur handlers
   */
  public static <H extends BlurHandler & FocusHandler> void handle(
      HasAllFocusHandlers eventSource, H reciever) {
    eventSource.addBlurHandler(reciever);
    eventSource.addFocusHandler(reciever);
  }

  /**
   * Constructor.
   */
  public HandlesAllFocusEvents() {
  }

  /**
   * Convenience method to handle both focus and blur events from an event
   * source.
   * 
   * @param source the event source
   */
  public void handle(HasAllFocusHandlers source) {
    handle(source, this);
  }
}