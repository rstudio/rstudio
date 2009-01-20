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

import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HasHandlers;

/**
 * Fired when the event source is resized.
 */
public class ResizeEvent extends GwtEvent<ResizeHandler> {

  /**
   * The event type.
   */
  private static Type<ResizeHandler> TYPE;

  /**
   * Fires a resize event on all registered handlers in the handler source.
   * 
   * @param <S> The handler source
   * @param source the source of the handlers
   * @param width the new width
   * @param height the new height
   */
  public static <S extends HasResizeHandlers & HasHandlers> void fire(S source,
      int width, int height) {
    if (TYPE != null) {
      ResizeEvent event = new ResizeEvent(width, height);
      source.fireEvent(event);
    }
  }

  /**
   * Ensures the existence of the handler hook and then returns it.
   * 
   * @return returns a handler hook
   */
  public static Type<ResizeHandler> getType() {
    if (TYPE == null) {
      TYPE = new Type<ResizeHandler>();
    }
    return TYPE;
  }

  private final int width;
  private final int height;

  /**
   * Construct a new {@link ResizeEvent}.
   * 
   * @param width the new width
   * @param height the new height
   */
  protected ResizeEvent(int width, int height) {
    this.width = width;
    this.height = height;
  }

  @Override
  public final Type<ResizeHandler> getAssociatedType() {
    return TYPE;
  }

  /**
   * Returns the new height.
   * 
   * @return the new height
   */
  public int getHeight() {
    return height;
  }

  /**
   * Returns the new width.
   * 
   * @return the new width
   */
  public int getWidth() {
    return width;
  }

  @Override
  public String toDebugString() {
    assertLive();
    return super.toDebugString() + " width = " + width + " height =" + height;
  }

  @Override
  protected void dispatch(ResizeHandler handler) {
    handler.onResize(this);
  }
}
