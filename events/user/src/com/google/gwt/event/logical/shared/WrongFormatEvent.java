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

/**
 * Fired when an event source receives a value in the wrong format.
 * 
 * @param <Value> the type of value with the wrong format
 */
public class WrongFormatEvent<Value> extends ValueEvent<Value> {

  /**
   * Event type for {@link WrongFormatEvent}.
   */
  public static final Type<WrongFormatEvent, WrongFormatHandler> TYPE = new Type<WrongFormatEvent, WrongFormatHandler>() {
    @Override
    protected void fire(WrongFormatHandler handler, WrongFormatEvent event) {
      handler.onWrongFormat(event);
    }
  };

  /**
   * Constructs a {@link WrongFormatEvent} event.
   * 
   * @param value the value with the wrong format
   */
  public WrongFormatEvent(Value value) {
    super(value);
  }

  @Override
  protected Type getType() {
    return TYPE;
  }
}
