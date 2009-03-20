/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.user.client.ui;

import java.util.EventListener;

/**
 * Event listener interface for mouse wheel events.
 * 
 * @deprecated use {@link com.google.gwt.event.dom.client.MouseWheelHandler}
 *             instead
 */
@Deprecated
public interface MouseWheelListener extends EventListener {

  /**
   * Fired when the user scrolls the mouse wheel over a widget.
   * 
   * @param sender the widget sending the event
   * @param velocity the velocity information for the wheel event
   * @deprecated use {@link com.google.gwt.event.dom.client.MouseWheelHandler#onMouseWheel}
   *             instead
   */
  @Deprecated
  void onMouseWheel(Widget sender, MouseWheelVelocity velocity);
}
