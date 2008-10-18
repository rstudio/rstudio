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
 * Fired when an event source changes its highlighted value.
 * 
 * @param <Value> the value highlighted
 */
public class HighlightEvent<Value> extends ValueEvent<Value> {
  /**
   * Event type.
   */
  public static final Type<HighlightEvent, HighlightHandler> TYPE = new Type<HighlightEvent, HighlightHandler>() {
    @Override
    protected void fire(HighlightHandler handler, HighlightEvent event) {
      handler.onHighlight(event);
    }
  };

  /**
   * Constructor.
   * 
   * @param value value highlighted
   */
  public HighlightEvent(Value value) {
    super(value);
  }

  @Override
  protected Type getType() {
    return TYPE;
  }
}
