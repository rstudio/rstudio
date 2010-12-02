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
package com.google.gwt.canvas.client;

import com.google.gwt.canvas.dom.client.Context;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.dom.client.CanvasElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.IsSupported;
import com.google.gwt.user.client.ui.FocusWidget;

/**
 * A widget representing a &lt;canvas&gt; element.
 * 
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * 
 * This widget may not be supported on all browsers, see {@link IsSupported}
 */
public class Canvas extends FocusWidget implements IsSupported {

  /**
   * Runtime check for whether the canvas element is supported in this browser.
   * See {@link IsSupported}
   * 
   * @return whether the canvas element is supported
   */
  public static final native boolean isSupported() /*-{
    return !!$doc.createElement('canvas').getContext;
  }-*/;

  /**
   * Creates a Canvas.
   */
  public Canvas() {
    setElement(Document.get().createCanvasElement());
  }

  /**
   * Returns the attached Canvas Element.
   * 
   * @return the Canvas Element
   */
  public CanvasElement getCanvasElement() {
    return this.getElement().cast();
  }

  /**
   * Gets the rendering context that may be used to draw on this canvas.
   * 
   * @param contextId the context id as a String
   * @return the canvas rendering context
   */
  public Context getContext(String contextId) {
    return getCanvasElement().getContext(contextId);
  }

  /**
   * Returns a 2D rendering context.
   * 
   * This is a convenience method, see {@link #getContext(String)}.
   * 
   * @return a 2D canvas rendering context
   */
  public Context2d getContext2d() {
    return getCanvasElement().getContext2d();
  }

  /**
   * Gets the height of the internal canvas coordinate space.
   * 
   * @return the height, in pixels
   * @see #setCoordinateSpaceHeight(int)
   */
  public int getCoordinateSpaceHeight() {
    return getCanvasElement().getHeight();
  }

  /**
   * Gets the width of the internal canvas coordinate space.
   * 
   * @return the width, in pixels
   * @see #setCoordinateSpaceWidth(int)
   */
  public int getCoordinateSpaceWidth() {
    return getCanvasElement().getWidth();
  }

  /**
   * Sets the height of the internal canvas coordinate space.
   * 
   * @param height the height, in pixels
   * @see #getCoordinateSpaceHeight()
   */
  public void setCoordinateSpaceHeight(int height) {
    getCanvasElement().setHeight(height);
  }

  /**
   * Sets the width of the internal canvas coordinate space.
   * 
   * @param width the width, in pixels
   * @see #getCoordinateSpaceWidth()
   */
  public void setCoordinateSpaceWidth(int width) {
    getCanvasElement().setWidth(width);
  }

  /**
   * Returns a data URL for the current content of the canvas element.
   * 
   * @return a data URL for the current content of this element.
   */
  public String toDataUrl() {
    return getCanvasElement().toDataUrl();
  }

  /**
   * Returns a data URL for the current content of the canvas element, with a
   * specified type.
   * 
   * @param type the type of the data url, e.g., image/jpeg or image/png.
   * @return a data URL for the current content of this element with the
   *         specified type.
   */
  public String toDataUrl(String type) {
    return getCanvasElement().toDataUrl(type);
  }
}
