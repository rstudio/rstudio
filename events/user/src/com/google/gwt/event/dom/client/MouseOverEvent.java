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
 * Represents a native mouse over event.
 */
public class MouseOverEvent extends MouseEvent {

  /**
    Event type for mouse over events. Represents the meta-data associated with this event.
  */
  public static final Type<MouseOverEvent, MouseOverHandler> TYPE = new Type<MouseOverEvent,MouseOverHandler>(
      Event.ONMOUSEOVER) {
     @Override
     public void fire(MouseOverHandler handler, MouseOverEvent event) {
       handler.onMouseOver(event);
     }

     @Override     
    MouseOverEvent wrap(Event nativeEvent) {
       return new MouseOverEvent(nativeEvent);
     }
   };

  /**
   * Constructor.
   * 
   * @param nativeEvent the native event object
   */
  public MouseOverEvent(Event nativeEvent) {
    super(nativeEvent);
  }
  
 @Override
  protected Type getType() {
    return TYPE;
  }

}
