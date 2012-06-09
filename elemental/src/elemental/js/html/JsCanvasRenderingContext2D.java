/*
 * Copyright 2012 Google Inc.
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
package elemental.js.html;
import elemental.util.Indexable;
import elemental.html.TextMetrics;
import elemental.html.CanvasRenderingContext;
import elemental.html.CanvasGradient;
import elemental.js.util.JsIndexable;
import elemental.html.CanvasRenderingContext2D;
import elemental.html.ImageData;
import elemental.html.VideoElement;
import elemental.html.CanvasPattern;
import elemental.html.ImageElement;
import elemental.html.CanvasElement;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.js.stylesheets.*;
import elemental.js.events.*;
import elemental.js.util.*;
import elemental.js.dom.*;
import elemental.js.html.*;
import elemental.js.css.*;
import elemental.js.stylesheets.*;

import java.util.Date;

public class JsCanvasRenderingContext2D extends JsCanvasRenderingContext  implements CanvasRenderingContext2D {
  protected JsCanvasRenderingContext2D() {}

  public final native Object getFillStyle() /*-{
    return this.fillStyle;
  }-*/;

  public final native void setFillStyle(Object param_fillStyle) /*-{
    this.fillStyle = param_fillStyle;
  }-*/;

  public final native String getFont() /*-{
    return this.font;
  }-*/;

  public final native void setFont(String param_font) /*-{
    this.font = param_font;
  }-*/;

  public final native float getGlobalAlpha() /*-{
    return this.globalAlpha;
  }-*/;

  public final native void setGlobalAlpha(float param_globalAlpha) /*-{
    this.globalAlpha = param_globalAlpha;
  }-*/;

  public final native String getGlobalCompositeOperation() /*-{
    return this.globalCompositeOperation;
  }-*/;

  public final native void setGlobalCompositeOperation(String param_globalCompositeOperation) /*-{
    this.globalCompositeOperation = param_globalCompositeOperation;
  }-*/;

  public final native String getLineCap() /*-{
    return this.lineCap;
  }-*/;

  public final native void setLineCap(String param_lineCap) /*-{
    this.lineCap = param_lineCap;
  }-*/;

  public final native String getLineJoin() /*-{
    return this.lineJoin;
  }-*/;

  public final native void setLineJoin(String param_lineJoin) /*-{
    this.lineJoin = param_lineJoin;
  }-*/;

  public final native float getLineWidth() /*-{
    return this.lineWidth;
  }-*/;

  public final native void setLineWidth(float param_lineWidth) /*-{
    this.lineWidth = param_lineWidth;
  }-*/;

  public final native float getMiterLimit() /*-{
    return this.miterLimit;
  }-*/;

  public final native void setMiterLimit(float param_miterLimit) /*-{
    this.miterLimit = param_miterLimit;
  }-*/;

  public final native float getShadowBlur() /*-{
    return this.shadowBlur;
  }-*/;

  public final native void setShadowBlur(float param_shadowBlur) /*-{
    this.shadowBlur = param_shadowBlur;
  }-*/;

  public final native String getShadowColor() /*-{
    return this.shadowColor;
  }-*/;

  public final native void setShadowColor(String param_shadowColor) /*-{
    this.shadowColor = param_shadowColor;
  }-*/;

  public final native float getShadowOffsetX() /*-{
    return this.shadowOffsetX;
  }-*/;

  public final native void setShadowOffsetX(float param_shadowOffsetX) /*-{
    this.shadowOffsetX = param_shadowOffsetX;
  }-*/;

  public final native float getShadowOffsetY() /*-{
    return this.shadowOffsetY;
  }-*/;

  public final native void setShadowOffsetY(float param_shadowOffsetY) /*-{
    this.shadowOffsetY = param_shadowOffsetY;
  }-*/;

  public final native Object getStrokeStyle() /*-{
    return this.strokeStyle;
  }-*/;

  public final native void setStrokeStyle(Object param_strokeStyle) /*-{
    this.strokeStyle = param_strokeStyle;
  }-*/;

  public final native String getTextAlign() /*-{
    return this.textAlign;
  }-*/;

  public final native void setTextAlign(String param_textAlign) /*-{
    this.textAlign = param_textAlign;
  }-*/;

  public final native String getTextBaseline() /*-{
    return this.textBaseline;
  }-*/;

  public final native void setTextBaseline(String param_textBaseline) /*-{
    this.textBaseline = param_textBaseline;
  }-*/;

  public final native float getWebkitBackingStorePixelRatio() /*-{
    return this.webkitBackingStorePixelRatio;
  }-*/;

  public final native boolean isWebkitImageSmoothingEnabled() /*-{
    return this.webkitImageSmoothingEnabled;
  }-*/;

  public final native void setWebkitImageSmoothingEnabled(boolean param_webkitImageSmoothingEnabled) /*-{
    this.webkitImageSmoothingEnabled = param_webkitImageSmoothingEnabled;
  }-*/;

  public final native JsIndexable getWebkitLineDash() /*-{
    return this.webkitLineDash;
  }-*/;

  public final native void setWebkitLineDash(Indexable param_webkitLineDash) /*-{
    this.webkitLineDash = param_webkitLineDash;
  }-*/;

  public final native float getWebkitLineDashOffset() /*-{
    return this.webkitLineDashOffset;
  }-*/;

  public final native void setWebkitLineDashOffset(float param_webkitLineDashOffset) /*-{
    this.webkitLineDashOffset = param_webkitLineDashOffset;
  }-*/;

  public final native void arc(float x, float y, float radius, float startAngle, float endAngle, boolean anticlockwise) /*-{
    this.arc(x, y, radius, startAngle, endAngle, anticlockwise);
  }-*/;

  public final native void arcTo(float x1, float y1, float x2, float y2, float radius) /*-{
    this.arcTo(x1, y1, x2, y2, radius);
  }-*/;

  public final native void beginPath() /*-{
    this.beginPath();
  }-*/;

  public final native void bezierCurveTo(float cp1x, float cp1y, float cp2x, float cp2y, float x, float y) /*-{
    this.bezierCurveTo(cp1x, cp1y, cp2x, cp2y, x, y);
  }-*/;

  public final native void clearRect(float x, float y, float width, float height) /*-{
    this.clearRect(x, y, width, height);
  }-*/;

  public final native void clearShadow() /*-{
    this.clearShadow();
  }-*/;

  public final native void clip() /*-{
    this.clip();
  }-*/;

  public final native void closePath() /*-{
    this.closePath();
  }-*/;

  public final native JsImageData createImageData(ImageData imagedata) /*-{
    return this.createImageData(imagedata);
  }-*/;

  public final native JsImageData createImageData(float sw, float sh) /*-{
    return this.createImageData(sw, sh);
  }-*/;

  public final native JsCanvasGradient createLinearGradient(float x0, float y0, float x1, float y1) /*-{
    return this.createLinearGradient(x0, y0, x1, y1);
  }-*/;

  public final native JsCanvasPattern createPattern(CanvasElement canvas, String repetitionType) /*-{
    return this.createPattern(canvas, repetitionType);
  }-*/;

  public final native JsCanvasPattern createPattern(ImageElement image, String repetitionType) /*-{
    return this.createPattern(image, repetitionType);
  }-*/;

  public final native JsCanvasGradient createRadialGradient(float x0, float y0, float r0, float x1, float y1, float r1) /*-{
    return this.createRadialGradient(x0, y0, r0, x1, y1, r1);
  }-*/;

  public final native void drawImage(ImageElement image, float x, float y) /*-{
    this.drawImage(image, x, y);
  }-*/;

  public final native void drawImage(ImageElement image, float x, float y, float width, float height) /*-{
    this.drawImage(image, x, y, width, height);
  }-*/;

  public final native void drawImage(ImageElement image, float sx, float sy, float sw, float sh, float dx, float dy, float dw, float dh) /*-{
    this.drawImage(image, sx, sy, sw, sh, dx, dy, dw, dh);
  }-*/;

  public final native void drawImage(CanvasElement canvas, float x, float y) /*-{
    this.drawImage(canvas, x, y);
  }-*/;

  public final native void drawImage(CanvasElement canvas, float x, float y, float width, float height) /*-{
    this.drawImage(canvas, x, y, width, height);
  }-*/;

  public final native void drawImage(CanvasElement canvas, float sx, float sy, float sw, float sh, float dx, float dy, float dw, float dh) /*-{
    this.drawImage(canvas, sx, sy, sw, sh, dx, dy, dw, dh);
  }-*/;

  public final native void drawImage(VideoElement video, float x, float y) /*-{
    this.drawImage(video, x, y);
  }-*/;

  public final native void drawImage(VideoElement video, float x, float y, float width, float height) /*-{
    this.drawImage(video, x, y, width, height);
  }-*/;

  public final native void drawImage(VideoElement video, float sx, float sy, float sw, float sh, float dx, float dy, float dw, float dh) /*-{
    this.drawImage(video, sx, sy, sw, sh, dx, dy, dw, dh);
  }-*/;

  public final native void drawImageFromRect(ImageElement image) /*-{
    this.drawImageFromRect(image);
  }-*/;

  public final native void drawImageFromRect(ImageElement image, float sx) /*-{
    this.drawImageFromRect(image, sx);
  }-*/;

  public final native void drawImageFromRect(ImageElement image, float sx, float sy) /*-{
    this.drawImageFromRect(image, sx, sy);
  }-*/;

  public final native void drawImageFromRect(ImageElement image, float sx, float sy, float sw) /*-{
    this.drawImageFromRect(image, sx, sy, sw);
  }-*/;

  public final native void drawImageFromRect(ImageElement image, float sx, float sy, float sw, float sh) /*-{
    this.drawImageFromRect(image, sx, sy, sw, sh);
  }-*/;

  public final native void drawImageFromRect(ImageElement image, float sx, float sy, float sw, float sh, float dx) /*-{
    this.drawImageFromRect(image, sx, sy, sw, sh, dx);
  }-*/;

  public final native void drawImageFromRect(ImageElement image, float sx, float sy, float sw, float sh, float dx, float dy) /*-{
    this.drawImageFromRect(image, sx, sy, sw, sh, dx, dy);
  }-*/;

  public final native void drawImageFromRect(ImageElement image, float sx, float sy, float sw, float sh, float dx, float dy, float dw) /*-{
    this.drawImageFromRect(image, sx, sy, sw, sh, dx, dy, dw);
  }-*/;

  public final native void drawImageFromRect(ImageElement image, float sx, float sy, float sw, float sh, float dx, float dy, float dw, float dh) /*-{
    this.drawImageFromRect(image, sx, sy, sw, sh, dx, dy, dw, dh);
  }-*/;

  public final native void drawImageFromRect(ImageElement image, float sx, float sy, float sw, float sh, float dx, float dy, float dw, float dh, String compositeOperation) /*-{
    this.drawImageFromRect(image, sx, sy, sw, sh, dx, dy, dw, dh, compositeOperation);
  }-*/;

  public final native void fill() /*-{
    this.fill();
  }-*/;

  public final native void fillRect(float x, float y, float width, float height) /*-{
    this.fillRect(x, y, width, height);
  }-*/;

  public final native void fillText(String text, float x, float y) /*-{
    this.fillText(text, x, y);
  }-*/;

  public final native void fillText(String text, float x, float y, float maxWidth) /*-{
    this.fillText(text, x, y, maxWidth);
  }-*/;

  public final native JsImageData getImageData(float sx, float sy, float sw, float sh) /*-{
    return this.getImageData(sx, sy, sw, sh);
  }-*/;

  public final native boolean isPointInPath(float x, float y) /*-{
    return this.isPointInPath(x, y);
  }-*/;

  public final native void lineTo(float x, float y) /*-{
    this.lineTo(x, y);
  }-*/;

  public final native JsTextMetrics measureText(String text) /*-{
    return this.measureText(text);
  }-*/;

  public final native void moveTo(float x, float y) /*-{
    this.moveTo(x, y);
  }-*/;

  public final native void putImageData(ImageData imagedata, float dx, float dy) /*-{
    this.putImageData(imagedata, dx, dy);
  }-*/;

  public final native void putImageData(ImageData imagedata, float dx, float dy, float dirtyX, float dirtyY, float dirtyWidth, float dirtyHeight) /*-{
    this.putImageData(imagedata, dx, dy, dirtyX, dirtyY, dirtyWidth, dirtyHeight);
  }-*/;

  public final native void quadraticCurveTo(float cpx, float cpy, float x, float y) /*-{
    this.quadraticCurveTo(cpx, cpy, x, y);
  }-*/;

  public final native void rect(float x, float y, float width, float height) /*-{
    this.rect(x, y, width, height);
  }-*/;

  public final native void restore() /*-{
    this.restore();
  }-*/;

  public final native void rotate(float angle) /*-{
    this.rotate(angle);
  }-*/;

  public final native void save() /*-{
    this.save();
  }-*/;

  public final native void scale(float sx, float sy) /*-{
    this.scale(sx, sy);
  }-*/;

  public final native void setAlpha(float alpha) /*-{
    this.setAlpha(alpha);
  }-*/;

  public final native void setCompositeOperation(String compositeOperation) /*-{
    this.setCompositeOperation(compositeOperation);
  }-*/;

  public final native void setFillColor(String color) /*-{
    this.setFillColor(color);
  }-*/;

  public final native void setFillColor(String color, float alpha) /*-{
    this.setFillColor(color, alpha);
  }-*/;

  public final native void setFillColor(float grayLevel) /*-{
    this.setFillColor(grayLevel);
  }-*/;

  public final native void setFillColor(float grayLevel, float alpha) /*-{
    this.setFillColor(grayLevel, alpha);
  }-*/;

  public final native void setFillColor(float r, float g, float b, float a) /*-{
    this.setFillColor(r, g, b, a);
  }-*/;

  public final native void setFillColor(float c, float m, float y, float k, float a) /*-{
    this.setFillColor(c, m, y, k, a);
  }-*/;

  public final native void setShadow(float width, float height, float blur) /*-{
    this.setShadow(width, height, blur);
  }-*/;

  public final native void setShadow(float width, float height, float blur, String color) /*-{
    this.setShadow(width, height, blur, color);
  }-*/;

  public final native void setShadow(float width, float height, float blur, String color, float alpha) /*-{
    this.setShadow(width, height, blur, color, alpha);
  }-*/;

  public final native void setShadow(float width, float height, float blur, float grayLevel) /*-{
    this.setShadow(width, height, blur, grayLevel);
  }-*/;

  public final native void setShadow(float width, float height, float blur, float grayLevel, float alpha) /*-{
    this.setShadow(width, height, blur, grayLevel, alpha);
  }-*/;

  public final native void setShadow(float width, float height, float blur, float r, float g, float b, float a) /*-{
    this.setShadow(width, height, blur, r, g, b, a);
  }-*/;

  public final native void setShadow(float width, float height, float blur, float c, float m, float y, float k, float a) /*-{
    this.setShadow(width, height, blur, c, m, y, k, a);
  }-*/;

  public final native void setStrokeColor(String color) /*-{
    this.setStrokeColor(color);
  }-*/;

  public final native void setStrokeColor(String color, float alpha) /*-{
    this.setStrokeColor(color, alpha);
  }-*/;

  public final native void setStrokeColor(float grayLevel) /*-{
    this.setStrokeColor(grayLevel);
  }-*/;

  public final native void setStrokeColor(float grayLevel, float alpha) /*-{
    this.setStrokeColor(grayLevel, alpha);
  }-*/;

  public final native void setStrokeColor(float r, float g, float b, float a) /*-{
    this.setStrokeColor(r, g, b, a);
  }-*/;

  public final native void setStrokeColor(float c, float m, float y, float k, float a) /*-{
    this.setStrokeColor(c, m, y, k, a);
  }-*/;

  public final native void setTransform(float m11, float m12, float m21, float m22, float dx, float dy) /*-{
    this.setTransform(m11, m12, m21, m22, dx, dy);
  }-*/;

  public final native void stroke() /*-{
    this.stroke();
  }-*/;

  public final native void strokeRect(float x, float y, float width, float height) /*-{
    this.strokeRect(x, y, width, height);
  }-*/;

  public final native void strokeRect(float x, float y, float width, float height, float lineWidth) /*-{
    this.strokeRect(x, y, width, height, lineWidth);
  }-*/;

  public final native void strokeText(String text, float x, float y) /*-{
    this.strokeText(text, x, y);
  }-*/;

  public final native void strokeText(String text, float x, float y, float maxWidth) /*-{
    this.strokeText(text, x, y, maxWidth);
  }-*/;

  public final native void transform(float m11, float m12, float m21, float m22, float dx, float dy) /*-{
    this.transform(m11, m12, m21, m22, dx, dy);
  }-*/;

  public final native void translate(float tx, float ty) /*-{
    this.translate(tx, ty);
  }-*/;

  public final native JsImageData webkitGetImageDataHD(float sx, float sy, float sw, float sh) /*-{
    return this.webkitGetImageDataHD(sx, sy, sw, sh);
  }-*/;

  public final native void webkitPutImageDataHD(ImageData imagedata, float dx, float dy) /*-{
    this.webkitPutImageDataHD(imagedata, dx, dy);
  }-*/;

  public final native void webkitPutImageDataHD(ImageData imagedata, float dx, float dy, float dirtyX, float dirtyY, float dirtyWidth, float dirtyHeight) /*-{
    this.webkitPutImageDataHD(imagedata, dx, dy, dirtyX, dirtyY, dirtyWidth, dirtyHeight);
  }-*/;
}
