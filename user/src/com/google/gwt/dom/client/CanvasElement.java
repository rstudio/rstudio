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
package com.google.gwt.dom.client;

import com.google.gwt.canvas.dom.client.Context;
import com.google.gwt.canvas.dom.client.Context2d;

/**
 * Canvas element.
 * 
 * @see <a href="http://www.w3.org/TR/html5/#canvas">W3C HTML 5 Specification</a>
 */
@TagName(CanvasElement.TAG)
public class CanvasElement extends Element {

  /**
   * The tag for this element.
   */
  public static final String TAG = "canvas";

  protected CanvasElement() {
  }

  /**
   * Gets the rendering context that may be used to draw on this canvas.
   *
   * @param contextId the context id as a String
   * @return the canvas rendering context
   */
  public final native Context getContext(String contextId) /*-{
    return this.getContext(contextId);
  }-*/;

  /**
   * Returns a 2D rendering context.
   * 
   * This is a convenience method, see {@link #getContext(String)}.
   * 
   * @return a 2D canvas rendering context
   */
  public final native Context2d getContext2d() /*-{
    return this.getContext(@com.google.gwt.canvas.dom.client.Context2d::CONTEXT_ID);
  }-*/;

  /**
   * Gets the height of the canvas.
   * 
   * @return the height, in pixels
   * @see #setHeight(int)
   */
  public final native int getHeight() /*-{
    return this.height;
  }-*/;

  /**
   * Gets the width of the canvas.
   * 
   * @return the width, in pixels
   * @see #setWidth(int)
   */
  public final native int getWidth() /*-{
    return this.width;
  }-*/;

  /**
   * Sets the height of the canvas.
   * 
   * @param height the height, in pixels
   * @see #getHeight()
   */
  public final native void setHeight(int height) /*-{
    this.height = height;
  }-*/;

  /**
   * Sets the width of the canvas.
   * 
   * @param width the width, in pixels
   * @see #getWidth()
   */
  public final native void setWidth(int width) /*-{
    this.width = width;
  }-*/;

  /**
   * Returns a data URL for the current content of the canvas element.
   * 
   * @return a data URL for the current content of this element.
   */
  public final native String toDataUrl() /*-{
    return this.toDataURL();
  }-*/;

  /**
   * Returns a data URL for the current content of the canvas element, with a specified type.
   * 
   * @param type the type of the data url, e.g., image/jpeg or image/png.
   * @return a data URL for the current content of this element with the specified type.
   */
  public final native String toDataUrl(String type) /*-{
    return this.toDataURL(type);
  }-*/;
}
