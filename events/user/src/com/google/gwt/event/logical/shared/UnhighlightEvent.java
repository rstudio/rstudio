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
 * Represents an unhighlight event.
 * 
 * @param <Value> the value of the element unhighlighted
 */
public class UnhighlightEvent<Value> extends ValueEvent<Value> {
  /**
   * Event type for {@link UnhighlightEvent}.
   */
  public static final Type<UnhighlightEvent, UnhighlightHandler> TYPE = new Type<UnhighlightEvent, UnhighlightHandler>() {
    @Override
    protected void fire(UnhighlightHandler handler, UnhighlightEvent event) {
      handler.onUnhighlight(event);
    }
  };

  /**
   * Constructs a {@link UnhighlightEvent}.
   * 
   * @param value value
   */
  public UnhighlightEvent(Value value) {
    super(value);
  }

  @Override
  protected Type getType() {
    return TYPE;
  }
}
