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
package com.google.gwt.place.shared;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Window.ClosingEvent;
import com.google.gwt.user.client.Window.ClosingHandler;

class MockPlaceControllerDelegate implements PlaceController.Delegate {
  String message = null;
  boolean confirm = false;
  ClosingHandler handler = null;

  public HandlerRegistration addWindowClosingHandler(ClosingHandler handler) {
    this.handler = handler;
    return new HandlerRegistration() {
      public void removeHandler() {
        throw new UnsupportedOperationException("Auto-generated method stub");
      }
    };
  }

  public void close() {
    ClosingEvent event = new ClosingEvent();
    handler.onWindowClosing(event);
    message = event.getMessage();
  }
  
  public boolean confirm(String message) {
    this.message = message;
    return confirm;
  }
}