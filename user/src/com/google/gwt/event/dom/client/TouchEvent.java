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
package com.google.gwt.event.dom.client;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Touch;
import com.google.gwt.event.shared.EventHandler;

/**
 * Abstract class representing touch events.
 *
 * @see http://developer.apple.com/library/safari/documentation/UserExperience/Reference/TouchEventClassReference/TouchEvent/TouchEvent.html
 *
 * @param <H> handler type
 */
public abstract class TouchEvent<H extends EventHandler>
    extends HumanInputEvent<H> {

  /**
   * Get an array of {@link Touch touches} which have changed since the last
   * touch event fired. Note, that for {@link TouchEndEvent touch end events},
   * the touch which has just ended will not be present in the array. Moreover,
   * if the touch which just ended was the last remaining touch, then a zero
   * length array will be returned.
   *
   * @return an array of touches
   */
  public JsArray<Touch> getChangedTouches() {
    return getNativeEvent().getChangedTouches();
  }

  /**
   * Get an array of {@link Touch touches} all touch which originated at the
   * same target as the current touch event. Note, that for {@link TouchEndEvent
   * touch end events}, the touch which has just ended will not be present in
   * the array. Moreover, if the touch which just ended was the last remaining
   * touch, then a zero length array will be returned.
   *
   * @return an array of touches
   */
  public JsArray<Touch> getTargetTouches() {
    return getNativeEvent().getTargetTouches();
  }

  /**
   * Get an array of all current {@link Touch touches}. Note, that for
   * {@link TouchEndEvent touch end events}, the touch which has just ended will
   * not be present in the array. Moreover, if the touch which just ended was
   * the last remaining touch, then a zero length array will be returned.
   *
   * @return an array of touches
   */
  public JsArray<Touch> getTouches() {
    return getNativeEvent().getTouches();
  }
}
