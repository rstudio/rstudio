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
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.CanvasElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.PartialSupport;
import com.google.gwt.user.client.ui.FocusWidget;

/**
 * A widget representing a &lt;canvas&gt; element.
 * 
 * This widget may not be supported on all browsers.
 */
@PartialSupport
public class Canvas extends FocusWidget {
  private static CanvasElementSupportDetector detector;

  /**
   * Return a new {@link Canvas} if supported,  and null otherwise.
   * 
   * @return a new {@link Canvas} if supported, and null otherwise
   */
  public static Canvas createIfSupported() {
    if (detector == null) {
      detector = GWT.create(CanvasElementSupportDetector.class);
    }
    if (!detector.isSupportedCompileTime()) {
      return null;
    }
    CanvasElement element = Document.get().createCanvasElement();
    if (!detector.isSupportedRunTime(element)) {
      return null;
    }
    return new Canvas(element);
  }

  /**
   * Runtime check for whether the canvas element is supported in this browser.
   * 
   * @return whether the canvas element is supported
   */
  public static boolean isSupported() {
    if (detector == null) {
      detector = GWT.create(CanvasElementSupportDetector.class);
    }
    if (!detector.isSupportedCompileTime()) {
      return false;
    }
    CanvasElement element = Document.get().createCanvasElement();
    if (!detector.isSupportedRunTime(element)) {
      return false;
    }
    return true;
  }

  /**
   * Protected constructor. Use {@link #createIfSupported()} to create a Canvas.
   */
  private Canvas(CanvasElement element) {
    setElement(element);
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

  /**
   * Detector for browser support of {@link CanvasElement}.
   */
  private static class CanvasElementSupportDetector {
    /**
     * Using a run-time check, return true if the {@link CanvasElement} is 
     * supported.
     * 
     * @return true if supported, false otherwise.
     */
    static native boolean isSupportedRunTime(CanvasElement element) /*-{
      return !!element.getContext;
    }-*/;

    /**
     * Using a compile-time check, return true if {@link CanvasElement} might 
     * be supported.
     * 
     * @return true if might be supported, false otherwise.
     */
    boolean isSupportedCompileTime() {
      // will be true in CanvasElementSupportDetectedMaybe
      // will be false in CanvasElementSupportDetectedNo
      return false;
    }
  }

  /**
   * Detector for permutations that might support {@link CanvasElement}.
   */
  @SuppressWarnings("unused")
  private static class CanvasElementSupportDetectedMaybe
      extends CanvasElementSupportDetector {
    /**
     * Using a compile-time check, return true if {@link CanvasElement} might be
     * supported.
     *
     * @return true if might be supported, false otherwise.
     */
    @Override
    boolean isSupportedCompileTime() {
      return true;
    }
  }

  /**
   * Detector for permutations that do not support {@link CanvasElement}.
   */
  @SuppressWarnings("unused")
  private static class CanvasElementSupportDetectedNo
      extends CanvasElementSupportDetector {
    /**
     * Using a compile-time check, return true if {@link CanvasElement} might be
     * supported.
     *
     * @return true if might be supported, false otherwise.
     */
    @Override
    boolean isSupportedCompileTime() {
      return false;
    } 
  }
}
