/*
 * Copyright 2006 Google Inc.
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
 * Event listener interface for mouse events.
 * 
 * @deprecated use {@link com.google.gwt.event.dom.client.MouseDownHandler},
 *             {@link com.google.gwt.event.dom.client.MouseUpHandler},
 *             {@link com.google.gwt.event.dom.client.MouseOverHandler},
 *             {@link com.google.gwt.event.dom.client.MouseMoveHandler}, and
 *             {@link com.google.gwt.event.dom.client.MouseOutHandler} instead
 */
@Deprecated
public interface MouseListener extends EventListener {

  /**
   * Fired when the user depresses the mouse button over a widget.
   * 
   * @param sender the widget sending the event
   * @param x the x coordinate of the mouse
   * @param y the y coordinate of the mouse
   * @deprecated use
   *             {@link com.google.gwt.event.dom.client.MouseDownHandler#onMouseDown(com.google.gwt.event.dom.client.MouseDownEvent)}
   *             instead
   */
  @Deprecated
  void onMouseDown(Widget sender, int x, int y);

  /**
   * Fired when the mouse enters a widget's area.
   * 
   * @param sender the widget sending the event
   * @deprecated use
   *             {@link com.google.gwt.event.dom.client.MouseOverHandler#onMouseOver(com.google.gwt.event.dom.client.MouseOverEvent)}
   *             instead
   */
  @Deprecated
  void onMouseEnter(Widget sender);

  /**
   * Fired when the mouse leaves a widget's area.
   * 
   * @param sender the widget sending the event
   * @deprecated use
   *             {@link com.google.gwt.event.dom.client.MouseOutHandler#onMouseOut(com.google.gwt.event.dom.client.MouseOutEvent)}
   *             instead
   */
  @Deprecated
  void onMouseLeave(Widget sender);

  /**
   * Fired when the user moves the mouse over a widget.
   * 
   * @param sender the widget sending the event
   * @param x the x coordinate of the mouse
   * @param y the y coordinate of the mouse
   * @deprecated use
   *             {@link com.google.gwt.event.dom.client.MouseMoveHandler#onMouseMove(com.google.gwt.event.dom.client.MouseMoveEvent)}
   *             instead
   */
  @Deprecated
  void onMouseMove(Widget sender, int x, int y);

  /**
   * Fired when the user releases the mouse button over a widget.
   * 
   * @param sender the widget sending the event
   * @param x the x coordinate of the mouse
   * @param y the y coordinate of the mouse
   * @deprecated use
   *             {@link com.google.gwt.event.dom.client.MouseUpHandler#onMouseUp(com.google.gwt.event.dom.client.MouseUpEvent)}
   *             instead
   */
  @Deprecated
  void onMouseUp(Widget sender, int x, int y);
}
