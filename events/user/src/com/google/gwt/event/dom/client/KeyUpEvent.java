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

import com.google.gwt.user.client.Event;

/**
 * Represents a native key up event.
 */
public class KeyUpEvent extends KeyCodeEvent {

  /**
    Event type for key up events. Represents the meta-data associated with this event.
  */
  public static final Type<KeyUpEvent, KeyUpHandler> TYPE = new Type<KeyUpEvent,KeyUpHandler>(
      Event.ONKEYUP) {
     @Override
     public void fire(KeyUpHandler handler, KeyUpEvent event) {
       handler.onKeyUp(event);
     }

     @Override     
    KeyUpEvent wrap(Event nativeEvent) {
       return new KeyUpEvent(nativeEvent);
     }
   };

  /**
   * Constructor.
   * 
   * @param nativeEvent the native event object
   */
  public KeyUpEvent(Event nativeEvent) {
    super(nativeEvent);
  }
  
 @Override
  protected Type getType() {
    return TYPE;
  }

}
