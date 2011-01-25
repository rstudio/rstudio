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
package com.google.gwt.canvas.dom.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.CanvasElement;
import com.google.gwt.dom.client.ImageElement;

/**
 * Rendering interface used to draw on a {@link CanvasElement}.
 *
 * @see <a href="http://www.w3.org/TR/2dcontext/#canvasrenderingcontext2d">W3C
 *      HTML 5 Specification</a>
 */
public class Context2d extends JavaScriptObject implements Context {
  /**
   * Enum for composite style.
   *
   * @see Context2d#setGlobalCompositeOperation(Composite)
   */
  public enum Composite {
    /**
     * A (B is ignored). Display the source image instead of the destination
     * image.
     */
    COPY("copy"),

    /**
     * B atop A. Same as source-atop but using the destination image instead of
     * the source image and vice versa.
     */
    DESTINATION_ATOP("destination-atop"),

    /**
     * B in A. Same as source-in but using the destination image instead of the
     * source image and vice versa.
     */
    DESTINATION_IN("destination-in"),

    /**
     * B out A. Same as source-out but using the destination image instead of the
     * source image and vice versa.
     */
    DESTINATION_OUT("destination-out"),

    /**
     * B over A. Same as source-over but using the destination image instead of
     * the source image and vice versa.
     */
    DESTINATION_OVER("destination-over"),

    /**
     * A plus B. Display the sum of the source image and destination image, with
     * color values approaching 1 as a limit.
     */
    LIGHTER("lighter"),

    /**
     * A atop B. Display the source image wherever both images are opaque. Display
     * the destination image wherever the destination image is opaque but the
     * source image is transparent. Display transparency elsewhere.
     */
    SOURCE_ATOP("source-atop"),

    /**
     * A in B. Display the source image wherever both the source image and
     * destination image are opaque. Display transparency elsewhere.
     */
    SOURCE_IN("source-in"),

    /**
     * A out B. Display the source image wherever the source image is opaque and
     * the destination image is transparent. Display transparency elsewhere.
     */
    SOURCE_OUT("source-out"),

    /**
     * A over B. Display the source image wherever the source image is opaque.
     * Display the destination image elsewhere.
     */
    SOURCE_OVER("source-over"),

    /**
     * A xor B. Exclusive OR of the source image and destination image.
     */
    XOR("xor");

    private final String value;

    private Composite(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  /**
   * Enum for line-cap style.
   *
   * @see Context2d#setLineCap(LineCap)
   */
  public enum LineCap {
    BUTT("butt"), ROUND("round"), SQUARE("square");

    private final String value;

    private LineCap(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  /**
   * Enum for line-join style.
   *
   * @see Context2d#setLineJoin(LineJoin)
   */
  public enum LineJoin {
    BEVEL("bevel"), MITER("miter"), ROUND("round");

    private final String value;

    private LineJoin(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  /**
   * Enum for the repetition values.
   *
   * @see Context2d#createPattern(ImageElement, Repetition)
   * @see Context2d#createPattern(CanvasElement, Repetition)
   */
  public enum Repetition {
    NO_REPEAT("no-repeat"), REPEAT("repeat"), REPEAT_X("repeat-x"), REPEAT_Y("repeat-y");

    private final String value;

    private Repetition(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  /**
   * Enum for text align style.
   *
   * @see Context2d#setTextAlign(TextAlign)
   */
  public enum TextAlign {
    CENTER("center"), END("end"), LEFT("left"), RIGHT("right"), START("start");

    private final String value;

    private TextAlign(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  /**
   * Enum for text baseline style.
   *
   * @see Context2d#setTextBaseline(TextBaseline)
   */
  public enum TextBaseline {
    ALPHABETIC("alphabetic"), BOTTOM("bottom"), HANGING("hanging"), IDEOGRAPHIC("ideographic"),
    MIDDLE("middle"), TOP("top");

    private final String value;

    private TextBaseline(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  /**
   * Specifies the context id property used in creating a Context.
   */
  public static final String CONTEXT_ID = "2d";

  protected Context2d() {
  }

  /**
   * Draws an arc. If a current subpath exists, a line segment is added from the
   * current point to the starting point of the arc. If {@code endAngle -
   * startAngle} is equal to or greater than {@code 2 * Math.Pi}, the arc is the
   * whole circumference of the circle.
   *
   * @param x the x coordinate of the center of the arc
   * @param y the y coordinate of the center of the arc
   * @param radius the radius of the arc
   * @param startAngle the start angle, measured in radians clockwise from the
   *          positive x-axis
   * @param endAngle the end angle, measured in radians clockwise from the
   *          positive x-axis
   */
  public final native void arc(double x, double y, double radius, double startAngle,
      double endAngle) /*-{
    // We must explicitly use false for the anticlockwise parameter because firefox has a bug where 
    // the last parameter is not actually optional.
    this.arc(x, y, radius, startAngle, endAngle, false);
  }-*/;

  /**
   * Draws an arc. If a current subpath exists, a line segment is added from the
   * current point to the starting point of the arc. If {@code anticlockwise} is
   * false and {@code endAngle - startAngle} is equal to or greater than {@code
   * 2 * Math.PI}, or if {@code anticlockwise} is {@code true} and {@code
   * startAngle - endAngle} is equal to or greater than {@code 2 * Math.PI},
   * then the arc is the whole circumference of the circle.
   *
   * @param x the x coordinate of the center of the arc
   * @param y the y coordinate of the center of the arc
   * @param radius the radius of the arc
   * @param startAngle the start angle, measured in radians clockwise from the
   *          positive x-axis
   * @param endAngle the end angle, measured in radians clockwise from the
   *          positive x-axis
   * @param anticlockwise if {@code true}, the arc is drawn in an anticlockwise
   *       direction
   */
  public final native void arc(double x, double y, double radius, double startAngle, double endAngle,
      boolean anticlockwise) /*-{
    this.arc(x, y, radius, startAngle, endAngle, anticlockwise);
  }-*/;

  /**
   * Adds an arc to the current subpath, connecting it to the current point
   * with a line segment.
   *
   * @param x1 the x coordinate of the starting point of the arc
   * @param y1 the y coordinate of the starting point of the arc
   * @param x2 the x coordinate of the ending point of the arc
   * @param y2 the y coordinate of the ending point of the arc
   * @param radius the radius of a circle containing the arc
   */
  public final native void arcTo(double x1, double y1, double x2, double y2, double radius) /*-{
    this.arcTo(x1, y1, x2, y2, radius);
  }-*/;

  /**
   * Begins a new path.
   */
  public final native void beginPath() /*-{
    this.beginPath();
  }-*/;

  /**
   * Draws a cubic B\u00e9zier curve from the current point to the point
   * (x, y), with control points (cp1x, cp1y) and (cp2x, cp2y).
   *
   * @param cp1x the x coordinate of the first control point
   * @param cp1y the y coordinate of the first control point
   * @param cp2x the x coordinate of the second control point
   * @param cp2y the y coordinate of the second control point
   * @param x the x coordinate of the end point
   * @param y the y coordinate of the end point
   */
  public final native void bezierCurveTo(double cp1x, double cp1y,
      double cp2x, double cp2y, double x, double y) /*-{
    this.bezierCurveTo(cp1x, cp1y, cp2x, cp2y, x, y);
  }-*/;

  /**
   * Clears a rectangle.
   *
   * @param x the x coordinate of the rectangle's upper-left corner
   * @param y the y coordinate of the rectangle's upper-left corner
   * @param w the width of the rectangle
   * @param h the height of the rectangle
   */
  public final native void clearRect(double x, double y, double w, double h) /*-{
    this.clearRect(x, y, w, h);
  }-*/;

  /**
   * Creates a new clipping region from the current path.
   */
  public final native void clip() /*-{
    this.clip();
  }-*/;

  /**
   * Closes the current path.
   */
  public final native void closePath() /*-{
    this.closePath();
  }-*/;

  /**
   * Creates an image data object of the same size as the given object.
   *
   * @param imagedata an {@link ImageData} object
   * @return a new {@link ImageData} object
   */
  public final native ImageData createImageData(ImageData imagedata) /*-{
    return this.createImageData(imagedata);
  }-*/;

  /**
   * Creates an image data object of the given size.
   *
   * @param w the width of the image
   * @param h the height of the image
   * @return an {@link ImageData} object
   */
  public final native ImageData createImageData(int w, int h) /*-{
    return this.createImageData(w, h);
  }-*/;

  /**
   * Creates a linear gradient.
   *
   * @param x0 the x coordinate of the starting point of the gradient
   * @param y0 the y coordinate of the starting point of the gradient
   * @param x1 the x coordinate of the ending point of the gradient
   * @param y1 the y coordinate of the ending point of the gradient
   * @return a {@link CanvasGradient} object
   */
  public final native CanvasGradient createLinearGradient(double x0, double y0, double x1,
      double y1) /*-{
    return this.createLinearGradient(x0, y0, x1, y1);
  }-*/;

  /**
   * Creates a pattern from another canvas.
   *
   * @param image an {@link CanvasElement} object
   * @param repetition a {@link Repetition} object
   * @return a {@link CanvasPattern} object
   */
  public final CanvasPattern createPattern(CanvasElement image, Repetition repetition) {
    return createPattern(image, repetition.getValue());
  }

  /**
   * Creates a pattern from another canvas.
   *
   * @param image an {@link CanvasElement} object
   * @param repetition the repetition type as a String
   * @return a {@link CanvasPattern} object
   */
  public final native CanvasPattern createPattern(CanvasElement image, String repetition) /*-{
    return this.createPattern(image, repetition);
  }-*/;

  /**
   * Creates a pattern from an image.
   *
   * @param image an {@link ImageElement} object
   * @param repetition a {@link Repetition} object
   * @return a {@link CanvasPattern} object
   */
  public final CanvasPattern createPattern(ImageElement image, Repetition repetition) {
    return createPattern(image, repetition.getValue());
  }

  /**
   * Creates a pattern from an image.
   *
   * @param image an {@link ImageElement} object
   * @param repetition the repetition type as a String
   * @return a {@link CanvasPattern} object
   */
  public final native CanvasPattern createPattern(ImageElement image, String repetition) /*-{
    return this.createPattern(image, repetition);
  }-*/;

  /**
   * Creates a radial gradient.
   *
   * @param x0 the x coordinate of the center of the start circle of the gradient
   * @param y0 the y coordinate of the center of the start circle of the gradient
   * @param r0 the radius of the start circle of the gradient
   * @param x1 the x coordinate of the center of the end circle of the gradient
   * @param y1 the y coordinate of the center of the end circle of the gradient
   * @param r1 the radius of the end circle of the gradient
   * @return a {@link CanvasGradient} object
   */
  public final native CanvasGradient createRadialGradient(double x0, double y0, double r0, double x1,
      double y1, double r1) /*-{
    return this.createRadialGradient(x0, y0, r0, x1, y1, r1);
  }-*/;

  /**
   * Draws an image.
   *
   * @param image an {@link CanvasElement} object
   * @param dx the x coordinate of the upper-left corner of the destination rectangle
   * @param dy the y coordinate of the upper-left corner of the destination rectangle
   */
  public final native void drawImage(CanvasElement image, double dx, double dy) /*-{
    this.drawImage(image, dx, dy);
  }-*/;

  /**
   * Draws a scaled image.
   *
   * @param image an {@link CanvasElement} object
   * @param dx the x coordinate of the upper-left corner of the destination rectangle
   * @param dy the y coordinate of the upper-left corner of the destination rectangle
   * @param dw the width of the destination rectangle
   * @param dh the height of the destination rectangle
   */
  public final native void drawImage(CanvasElement image, double dx, double dy, double dw,
      double dh) /*-{
    this.drawImage(image, dx, dy, dw, dh);
  }-*/;

  /**
   * Draws a scaled subset of an image.
   *
   * @param image an {@link CanvasElement} object
   * @param sx the x coordinate of the upper-left corner of the source rectangle
   * @param sy the y coordinate of the upper-left corner of the source rectangle
   * @param sw the width of the source rectangle
   * @param sh the width of the source rectangle
   * @param dx the x coordinate of the upper-left corner of the destination rectangle
   * @param dy the y coordinate of the upper-left corner of the destination rectangle
   * @param dw the width of the destination rectangle
   * @param dh the height of the destination rectangle
   */
  public final native void drawImage(CanvasElement image, double sx, double sy, double sw, double sh,
      double dx, double dy, double dw, double dh) /*-{
    this.drawImage(image, sx, sy, sw, sh, dx, dy, dw, dh);
  }-*/;

  /**
   * Draws an image.
   *
   * @param image an {@link ImageElement} object
   * @param dx the x coordinate of the upper-left corner of the destination rectangle
   * @param dy the y coordinate of the upper-left corner of the destination rectangle
   */
  public final native void drawImage(ImageElement image, double dx, double dy) /*-{
    this.drawImage(image, dx, dy);
  }-*/;

  /**
   * Draws a scaled image.
   *
   * @param image an {@link ImageElement} object
   * @param dx the x coordinate of the upper-left corner of the destination rectangle
   * @param dy the y coordinate of the upper-left corner of the destination rectangle
   * @param dw the width of the destination rectangle
   * @param dh the height of the destination rectangle
   */
  public final native void drawImage(ImageElement image, double dx, double dy, double dw,
      double dh) /*-{
    this.drawImage(image, dx, dy, dw, dh);
  }-*/;

  /**
   * Draws a scaled subset of an image.
   *
   * @param image an {@link ImageElement} object
   * @param sx the x coordinate of the upper-left corner of the source rectangle
   * @param sy the y coordinate of the upper-left corner of the source rectangle
   * @param sw the width of the source rectangle
   * @param sh the width of the source rectangle
   * @param dx the x coordinate of the upper-left corner of the destination rectangle
   * @param dy the y coordinate of the upper-left corner of the destination rectangle
   * @param dw the width of the destination rectangle
   * @param dh the height of the destination rectangle
   */
  public final native void drawImage(ImageElement image, double sx, double sy, double sw, double sh,
      double dx, double dy, double dw, double dh) /*-{
    this.drawImage(image, sx, sy, sw, sh, dx, dy, dw, dh);
  }-*/;

  /**
   * Fills the current path.
   */
  public final native void fill() /*-{
    this.fill();
  }-*/;

  /**
   * Fills a rectangle.
   *
   * @param x the x coordinate of the rectangle's upper-left corner
   * @param y the y coordinate of the rectangle's upper-left corner
   * @param w the width of the rectangle
   * @param h the height of the rectangle
   */
  public final native void fillRect(double x, double y, double w, double h) /*-{
    this.fillRect(x, y, w, h);
  }-*/;

  /**
   * Draws filled text.
   *
   * @param text the text as a String
   * @param x the x coordinate of the text position
   * @param y the y coordinate of the text position
   */
  public final native void fillText(String text, double x, double y) /*-{
    // FF3.0 does not implement this method.
    if (this.fillText) {
      this.fillText(text, x, y);
    }
  }-*/;

  /**
   * Draws filled text squeezed into the given max width.
   *
   * @param text the text as a String
   * @param x the x coordinate of the text position
   * @param y the y coordinate of the text position
   * @param maxWidth the maximum width for the text
   */
  public final native void fillText(String text, double x, double y, double maxWidth) /*-{
    this.fillText(text, x, y, maxWidth);
  }-*/;

  /**
   * Gets this context's canvas.
   *
   * @return a {@link CanvasElement} object
   */
  public final native CanvasElement getCanvas() /*-{
    return this.canvas;
  }-*/;

  /**
   * Returns the context's fillStyle. In dev mode, the returned object will
   * be wrapped in a JavaScript array.
   *
   * @return a {@link FillStrokeStyle} object
   * @see #setFillStyle(FillStrokeStyle)
   * @see #setFillStyle(String)
   * @see CssColor
   */
  public final FillStrokeStyle getFillStyle() {
    if (GWT.isScript()) {
      return getFillStyleWeb();
    } else {
      return getFillStyleDev();
    }
  }

  /**
   * Gets this context's font.
   *
   * @return the font name as a String
   * @see #setFont(String)
   */
  public final native String getFont() /*-{
    return this.font;
  }-*/;

  /**
   * Gets the global alpha value.
   *
   * @return the global alpha value as a double
   * @see #setGlobalAlpha(double)
   */
  public final native double getGlobalAlpha() /*-{
    return this.globalAlpha;
  }-*/;

  /**
   * Gets the global composite operation.
   *
   * @return the global composite operation as a String
   * @see #setGlobalCompositeOperation(Composite)
   * @see #setGlobalCompositeOperation(String)
   */
  public final native String getGlobalCompositeOperation() /*-{
    return this.globalCompositeOperation;
  }-*/;

  /**
   * Returns an image data object for the screen area denoted by
   * sx, sy, sw and sh.
   *
   * @param sx the x coordinate of the upper-left corner of the desired area
   * @param sy the y coordinate of the upper-left corner of the desired area
   * @param sw the width of the desired area
   * @param sh the height of the desired area
   * @return an {@link ImageData} object containing screen pixel data
   */
  public final native ImageData getImageData(double sx, double sy, double sw, double sh) /*-{
    return this.getImageData(sx, sy, sw, sh);
  }-*/;

  /**
   * Gets the current line-cap style.
   *
   * @return the line cap style as a String
   * @see #setLineCap(LineCap)
   * @see #setLineCap(String)
   */
  public final native String getLineCap() /*-{
    return this.lineCap;
  }-*/;

  /**
   * Gets the current line-join style.
   *
   * @return the line join style as a String
   * @see #setLineJoin(LineJoin)
   * @see #setLineJoin(String)
   */
  public final native String getLineJoin() /*-{
    return this.lineJoin;
  }-*/;

  /**
   * Gets the current line-width.
   *
   * @return the line width as a double
   * @see #setLineWidth(double)
   */
  public final native double getLineWidth() /*-{
    return this.lineWidth;
  }-*/;

  /**
   * Gets the current miter-limit.
   *
   * @return the miter limit as a double
   * @see #setMiterLimit(double)
   */
  public final native double getMiterLimit() /*-{
    return this.miterLimit;
  }-*/;

  /**
   * Gets the current shadow-blur.
   *
   * @return the shadow blur amount as a double
   * @see #setShadowBlur(double)
   */
  public final native double getShadowBlur() /*-{
    return this.shadowBlur;
  }-*/;

  /**
   * Gets the current shadow color.
   *
   * @return the shadow color as a String
   * @see #setShadowColor(String)
   */
  public final native String getShadowColor() /*-{
    return this.shadowColor;
  }-*/;

  /**
   * Gets the current x-shadow-offset.
   *
   * @return the shadow x offset as a double
   * @see #setShadowOffsetX(double)
   * @see #getShadowOffsetY()
   */
  public final native double getShadowOffsetX() /*-{
    return this.shadowOffsetX;
  }-*/;

  /**
   * Gets the current y-shadow-offset.
   *
   * @return the shadow y offset as a double
   * @see #setShadowOffsetY(double)
   * @see #getShadowOffsetX()
   */
  public final native double getShadowOffsetY() /*-{
    return this.shadowOffsetY;
  }-*/;

  /**
   * Returns the context's strokeStyle. In dev mode, the returned object will
   * be wrapped in a JavaScript array.
   *
   * @return the stroke style as a {@link FillStrokeStyle} object
   * @see #setStrokeStyle(FillStrokeStyle)
   * @see #setStrokeStyle(String)
   * @see CssColor
   */
  public final FillStrokeStyle getStrokeStyle() {
    if (GWT.isScript()) {
      return getStrokeStyleWeb();
    } else {
      return getStrokeStyleDev();
    }
  }

  /**
   * Gets the current text align.
   *
   * @return the text align as a String
   * @see #setTextAlign(TextAlign)
   * @see #setTextAlign(String)
   */
  public final native String getTextAlign() /*-{
    return this.textAlign;
  }-*/;

  /**
   * Gets the current text baseline.
   *
   * @return the text baseline as a String
   * @see #setTextBaseline(TextBaseline)
   * @see #setTextBaseline(String)
   */
  public final native String getTextBaseline() /*-{
    return this.textBaseline;
  }-*/;

  /**
   * Returns true if the given point is in the current path.
   *
   * @param x the x coordinate of the point to test.
   * @param y the y coordinate of the point to test.
   * @return {@code true} if the given point is in the current path.
   */
  public final native boolean isPointInPath(double x, double y) /*-{
    return this.isPointInPath(x, y);
  }-*/;

  /**
   * Adds a line from the current point to the point (x, y) to the current
   * path.
   *
   * @param x the x coordinate of the line endpoint
   * @param y the y coordinate of the line endpoint
   */
  public final native void lineTo(double x, double y) /*-{
    this.lineTo(x, y);
  }-*/;

  /**
   * Returns the metrics for the given text.
   *
   * @param text the text to measure, as a String
   * @return a {@link TextMetrics} object
   */
  public final native TextMetrics measureText(String text) /*-{
    return this.measureText(text);
  }-*/;

  /**
   * Terminates the current path and sets the current path position to the point
   * (x, y).
   *
   * @param x the x coordinate of the new position
   * @param y the y coordinate of the new position
   */
  public final native void moveTo(double x, double y) /*-{
    this.moveTo(x, y);
  }-*/;

  /**
   * Draws the given image data at the given screen position.
   *
   * @param imagedata an {@link ImageData} instance to be written to the screen
   * @param x the x coordinate of the upper-left corner at which to draw
   * @param y the y coordinate of the upper-left corner at which to draw
   */
  public final native void putImageData(ImageData imagedata, double x, double y) /*-{
    return this.putImageData(imagedata, x, y);
  }-*/;

  /**
   * Draws a quadratic B\u00e9zier curve from the current point to the point
   * (x, y), with control point (cpx, cpy).
   *
   * @param cpx the x coordinate of the control point
   * @param cpy the y coordinate of the control point
   * @param x the x coordinate of the end point
   * @param y the y coordinate of the end point
   */
  public final native void quadraticCurveTo(double cpx, double cpy, double x, double y) /*-{
    this.quadraticCurveTo(cpx, cpy, x, y);
  }-*/;

  /**
   * Creates a new rectangular path.
   *
   * @param x the x coordinate of the rectangle's upper-left corner
   * @param y the y coordinate of the rectangle's upper-left corner
   * @param w the width of the rectangle
   * @param h the height of the rectangle
   */
  public final native void rect(double x, double y, double w, double h) /*-{
    this.rect(x, y, w, h);
  }-*/;

  /**
   * Restores the context's state.
   */
  public final native void restore() /*-{
    this.restore();
  }-*/;

  /**
   * Applies rotation to the current transform.
   *
   * @param angle the clockwise rotation angle, in radians
   */
  public final native void rotate(double angle) /*-{
    this.rotate(angle);
  }-*/;

  /**
   * Saves the context's state.
   */
  public final native void save() /*-{
    this.save();
  }-*/;

  /**
   * Applies scale to the current transform.
   *
   * @param x the scale factor along the x-axis
   * @param y the scale factor along the y-axis
   */
  public final native void scale(double x, double y) /*-{
    this.scale(x, y);
  }-*/;

  /**
   * Sets the context's fillStyle.
   *
   * @param fillStyle the fill style to set.
   * @see #getFillStyle()
   * @see CssColor
   */
  public final void setFillStyle(FillStrokeStyle fillStyle) {
    if (GWT.isScript()) {
      setFillStyleWeb(fillStyle);
    } else {
      setFillStyleDev(fillStyle);
    }
  }

  /**
   * Convenience method to set the context's fillStyle to a {@link CssColor},
   * specified in String form.
   *
   * @param fillStyleColor the color as a String
   * @see #getFillStyle()
   */
  public final void setFillStyle(String fillStyleColor) {
    setFillStyle(CssColor.make(fillStyleColor));
  }

  /**
   * Sets the font.
   *
   * @param f the font name as a String
   * @see #getFont()
   */
  public final native void setFont(String f) /*-{
    this.font = f;
  }-*/;

  /**
   * Sets the global alpha value.
   *
   * @param alpha the global alpha value as a double
   * @see #getGlobalAlpha()
   */
  public final native void setGlobalAlpha(double alpha) /*-{
    this.globalAlpha = alpha;
  }-*/;

  /**
   * Sets the global composite operation.
   *
   * @param composite a {@link Composite} value
   * @see #getGlobalCompositeOperation()
   */
  public final void setGlobalCompositeOperation(Composite composite) {
    setGlobalCompositeOperation(composite.getValue());
  }

  /**
   * Sets the global composite operation.
   *
   * @param globalCompositeOperation the operation as a String
   * @see #getGlobalCompositeOperation()
   */
  public final native void setGlobalCompositeOperation(String globalCompositeOperation) /*-{
    this.globalCompositeOperation = globalCompositeOperation;
  }-*/;

  /**
   * Sets the line-cap style.
   *
   * @param lineCap the line cap style as a {@link LineCap} value
   * @see #getLineCap()
   */
  public final void setLineCap(LineCap lineCap) {
    setLineCap(lineCap.getValue());
  }

  /**
   * Sets the line-cap style.
   *
   * @param lineCap the line cap style as a String
   * @see #getLineCap()
   */
  public final native void setLineCap(String lineCap) /*-{
    this.lineCap = lineCap;
  }-*/;

  /**
   * Sets the line-join style.
   *
   * @param lineJoin the line join style as a {@link LineJoin} value
   * @see #getLineJoin()
   */
  public final void setLineJoin(LineJoin lineJoin) {
    setLineJoin(lineJoin.getValue());
  }

  /**
   * Sets the line-join style.
   *
   * @param lineJoin the ling join style as a String
   * @see #getLineJoin()
   */
  public final native void setLineJoin(String lineJoin) /*-{
    this.lineJoin = lineJoin;
  }-*/;

  /**
   * Sets the line-width.
   *
   * @param lineWidth the line width as a double
   * @see #getMiterLimit()
   * @see #getLineWidth()
   */
  public final native void setLineWidth(double lineWidth) /*-{
    this.lineWidth = lineWidth;
  }-*/;

  /**
   * Sets the miter-limit.
   *
   * @param miterLimit the miter limit as a double
   * @see #getMiterLimit()
   */
  public final native void setMiterLimit(double miterLimit) /*-{
    this.miterLimit = miterLimit;
  }-*/;

  /**
   * Sets the shadow-blur.
   *
   * @param shadowBlur the amount of blur as a double
   * @see #getShadowBlur()
   */
  public final native void setShadowBlur(double shadowBlur) /*-{
    this.shadowBlur = shadowBlur;
  }-*/;

  /**
   * Sets the shadow-color.
   *
   * @param shadowColor the shadow color as a String
   * @see #getShadowColor()
   */
  public final native void setShadowColor(String shadowColor) /*-{
    this.shadowColor = shadowColor;
  }-*/;

  /**
   * Sets the x-shadow-offset.
   *
   * @param shadowOffsetX the x shadow offset
   * @see #getShadowOffsetX()
   * @see #getShadowOffsetY()
   */
  public final native void setShadowOffsetX(double shadowOffsetX) /*-{
    this.shadowOffsetX = shadowOffsetX;
  }-*/;

  /**
   * Sets the y-shadow-offset.
   *
   * @param shadowOffsetY the y shadow offset
   * @see #getShadowOffsetX()
   * @see #getShadowOffsetY()
   */
  public final native void setShadowOffsetY(double shadowOffsetY) /*-{
    this.shadowOffsetY = shadowOffsetY;
  }-*/;

  /**
   * Sets the context's stroke style.
   *
   * @param strokeStyle the stroke style to set
   * @see #getStrokeStyle()
   * @see CssColor
   */
  public final void setStrokeStyle(FillStrokeStyle strokeStyle) {
    if (GWT.isScript()) {
      setStrokeStyleWeb(strokeStyle);
    } else {
      setStrokeStyleDev(strokeStyle);
    }
  }

  /**
   * Convenience method to set the context's strokeStyle to a {@link CssColor}.
   *
   * @param strokeStyleColor the stroke color as a String
   * @see #getStrokeStyle()
   */
  public final void setStrokeStyle(String strokeStyleColor) {
    setStrokeStyle(CssColor.make(strokeStyleColor));
  }

  /**
   * Sets the text alignment.
   *
   * @param align the alignment setting as a String
   * @see #getTextAlign()
   */
  public final native void setTextAlign(String align) /*-{
    this.textAlign = align
  }-*/;

  /**
   * Sets the text alignment.
   *
   * @param align the alignment setting as a {@link TextAlign} value
   * @see #getTextAlign()
   */
  public final void setTextAlign(TextAlign align) {
    setTextAlign(align.getValue());
  }

  /**
   * Sets the text baseline.
   *
   * @param baseline the baseline setting as a String
   * @see #getTextBaseline()
   */
  public final native void setTextBaseline(String baseline) /*-{
    this.textBaseline = baseline
  }-*/;

  /**
   * Sets the text baseline.
   *
   * @param baseline a the baseline setting as a {@link TextBaseline} value
   * @see #getTextBaseline()
   */
  public final void setTextBaseline(TextBaseline baseline) {
    setTextBaseline(baseline.getValue());
  }

  /**
   * Sets the 2D transformation matrix.
   *
   * @param m11 the value at position (1, 1) of the matrix
   * @param m12 the value at position (1, 2) of the matrix
   * @param m21 the value at position (2, 1) of the matrix
   * @param m22 the value at position (2, 2) of the matrix
   * @param dx the x translation value
   * @param dy the y translation value
   */
  public final native void setTransform(double m11, double m12, double m21,
      double m22, double dx, double dy) /*-{
    this.setTransform(m11, m12, m21, m22, dx, dy);
  }-*/;

  /**
   * Draws the current path with the current stroke style.
   */
  public final native void stroke() /*-{
    this.stroke();
  }-*/;

  /**
   * Draws the outline of a rectangle with the current stroke style.
   *
   * @param x the x coordinate of the rectangle's upper-left corner
   * @param y the y coordinate of the rectangle's upper-left corner
   * @param w the width of the rectangle
   * @param h the height of the rectangle
   */
  public final native void strokeRect(double x, double y, double w, double h) /*-{
    this.strokeRect(x, y, w, h);
  }-*/;

  /**
   * Draws the text outline.
   *
   * @param text the text as a String
   * @param x the x coordinate of the text position
   * @param y the y coordinate of the text position
   */
  public final native void strokeText(String text, double x, double y) /*-{
    this.strokeText(text, x, y);
  }-*/;

  /**
   * Draws the text outline, squeezing the text into the given max width by
   * compressing the font.
   *
   * @param text the text as a String
   * @param x the x coordinate of the text position
   * @param y the y coordinate of the text position
   * @param maxWidth the maximum width for the text
   */
  public final native void strokeText(String text, double x, double y, double maxWidth) /*-{
    this.strokeText(text, x, y, maxWidth);
  }-*/;

  /**
   * Multiplies the current transform by the given matrix.
   *
   * @param m11 the value at position (1, 1) of the matrix
   * @param m12 the value at position (1, 2) of the matrix
   * @param m21 the value at position (2, 1) of the matrix
   * @param m22 the value at position (2, 2) of the matrix
   * @param dx the x translation value
   * @param dy the y translation value
   */
  public final native void transform(double m11, double m12, double m21, double m22, double dx,
      double dy) /*-{
    this.transform(m11, m12, m21, m22, dx, dy);
  }-*/;

  /**
   * Applies a translation to the current transform.
   *
   * @param x the amount of translation along the x-axis
   * @param y the amount of translation along the y-axis
   */
  public final native void translate(double x, double y) /*-{
    this.translate(x, y);
  }-*/;

  /**
   * Returns the fill style when in dev mode. The JSO is wrapped in
   * an array before being returned.
   *
   * @return the fill style.
   * @see CssColor
   */
  private native FillStrokeStyle getFillStyleDev() /*-{
    if (typeof(this.fillStyle) == 'string') { // it's a color
      return [this.fillStyle];
    } else {
      return this.fillStyle;
    }
  }-*/;

  /**
   * Returns the fill style when in Production Mode.
   *
   * @return the fill style
   */
  private native FillStrokeStyle getFillStyleWeb() /*-{
    return this.fillStyle;
  }-*/;

  /**
   * Returns the stroke style when in dev mode. The JSO is wrapped in
   * an array before being returned.
   *
   * @return the stroke style
   * @see CssColor
   */
  private native FillStrokeStyle getStrokeStyleDev() /*-{
    if (typeof(this.strokeStyle) == 'string') { // if it's a color
      return [this.strokeStyle];
    } else {
      return this.strokeStyle;
    }
  }-*/;

  /**
   * Returns the stroke style when in Production Mode.
   *
   * @return the stroke style
   */
  private native FillStrokeStyle getStrokeStyleWeb() /*-{
    return this.strokeStyle;
  }-*/;

  /**
   * Sets the fill style when in dev mode. The incoming JSO is wrapped in
   * an array.
   *
   * @param fillStyle the fill style to set
   */
  private native void setFillStyleDev(FillStrokeStyle fillStyle) /*-{
    if (fillStyle[0] && typeof(fillStyle[0]) == 'string') {
      this.fillStyle = fillStyle[0];
    } else {
      this.fillStyle = fillStyle;
    }
  }-*/;

  /**
   * Sets the fill style when in Production Mode.
   *
   * @param fillStyle the fill style to set
   */
  private native void setFillStyleWeb(FillStrokeStyle fillStyle) /*-{
    this.fillStyle = fillStyle;
  }-*/;

  /**
   * Sets the stroke style when in dev mode. The incoming JSO is wrapped in
   * an array.
   *
   * @param strokeStyle the stroke style to set
   */
  private native void setStrokeStyleDev(FillStrokeStyle strokeStyle) /*-{
    if (strokeStyle[0] && typeof(strokeStyle[0]) == 'string') {
      this.strokeStyle = strokeStyle[0];
    } else {
      this.strokeStyle = strokeStyle;
    }
  }-*/;

  /**
   * Sets the stroke style when in Production Mode.
   *
   * @param strokeStyle the strokeStyle to set
   */
  private native void setStrokeStyleWeb(FillStrokeStyle strokeStyle) /*-{
    this.strokeStyle = strokeStyle;
  }-*/;
}
