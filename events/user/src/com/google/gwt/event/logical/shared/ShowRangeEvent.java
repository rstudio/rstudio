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

import com.google.gwt.event.shared.AbstractEvent;

/**
 * Fired after an event source shows a range of values.
 * 
 * @param <Value> the type of value shown in the range
 */
public class ShowRangeEvent<Value> extends AbstractEvent {

  /**
   * Event type for {@link ShowRangeEvent}.
   */
  public static final Type<ShowRangeEvent, ShowRangeHandler> TYPE = new Type<ShowRangeEvent, ShowRangeHandler>() {
    @Override
    protected void fire(ShowRangeHandler handler, ShowRangeEvent event) {
      handler.onShowRange(event);
    }
  };
  private Value start;
  private Value end;

  /**
   * Constructs a ShowRangeEvent event.
   * 
   * @param start start of range
   * @param end end of range
   */
  public ShowRangeEvent(Value start, Value end) {
    this.start = start;
    this.end = end;
  }

  /**
   * Gets the end of the range.
   * 
   * @return range end
   */
  public Value getEnd() {
    assertLive();
    return end;
  }

  /**
   * Gets the start of the range.
   * 
   * @return range start
   */
  public Value getStart() {
    assertLive();
    return start;
  }

  @Override
  protected Type getType() {
    return TYPE;
  }
}
