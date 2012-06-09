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
package elemental.js.svg;
import elemental.svg.SVGTransform;
import elemental.svg.SVGAngle;
import elemental.dom.Element;
import elemental.svg.SVGRect;
import elemental.svg.SVGNumber;
import elemental.js.dom.JsElement;
import elemental.svg.SVGViewSpec;
import elemental.svg.SVGSVGElement;
import elemental.svg.SVGPoint;
import elemental.svg.SVGAnimatedLength;
import elemental.js.dom.JsNodeList;
import elemental.svg.SVGMatrix;
import elemental.svg.SVGElement;
import elemental.svg.SVGLength;
import elemental.dom.NodeList;

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

public class JsSVGSVGElement extends JsSVGElement  implements SVGSVGElement {
  protected JsSVGSVGElement() {}

  public final native String getContentScriptType() /*-{
    return this.contentScriptType;
  }-*/;

  public final native void setContentScriptType(String param_contentScriptType) /*-{
    this.contentScriptType = param_contentScriptType;
  }-*/;

  public final native String getContentStyleType() /*-{
    return this.contentStyleType;
  }-*/;

  public final native void setContentStyleType(String param_contentStyleType) /*-{
    this.contentStyleType = param_contentStyleType;
  }-*/;

  public final native float getCurrentScale() /*-{
    return this.currentScale;
  }-*/;

  public final native void setCurrentScale(float param_currentScale) /*-{
    this.currentScale = param_currentScale;
  }-*/;

  public final native JsSVGPoint getCurrentTranslate() /*-{
    return this.currentTranslate;
  }-*/;

  public final native JsSVGViewSpec getCurrentView() /*-{
    return this.currentView;
  }-*/;

  public final native float getPixelUnitToMillimeterX() /*-{
    return this.pixelUnitToMillimeterX;
  }-*/;

  public final native float getPixelUnitToMillimeterY() /*-{
    return this.pixelUnitToMillimeterY;
  }-*/;

  public final native float getScreenPixelToMillimeterX() /*-{
    return this.screenPixelToMillimeterX;
  }-*/;

  public final native float getScreenPixelToMillimeterY() /*-{
    return this.screenPixelToMillimeterY;
  }-*/;

  public final native boolean isUseCurrentView() /*-{
    return this.useCurrentView;
  }-*/;

  public final native JsSVGRect getViewport() /*-{
    return this.viewport;
  }-*/;

  public final native JsSVGAnimatedLength getX() /*-{
    return this.x;
  }-*/;

  public final native JsSVGAnimatedLength getY() /*-{
    return this.y;
  }-*/;

  public final native boolean animationsPaused() /*-{
    return this.animationsPaused();
  }-*/;

  public final native boolean checkEnclosure(SVGElement element, SVGRect rect) /*-{
    return this.checkEnclosure(element, rect);
  }-*/;

  public final native boolean checkIntersection(SVGElement element, SVGRect rect) /*-{
    return this.checkIntersection(element, rect);
  }-*/;

  public final native JsSVGAngle createSVGAngle() /*-{
    return this.createSVGAngle();
  }-*/;

  public final native JsSVGLength createSVGLength() /*-{
    return this.createSVGLength();
  }-*/;

  public final native JsSVGMatrix createSVGMatrix() /*-{
    return this.createSVGMatrix();
  }-*/;

  public final native JsSVGNumber createSVGNumber() /*-{
    return this.createSVGNumber();
  }-*/;

  public final native JsSVGPoint createSVGPoint() /*-{
    return this.createSVGPoint();
  }-*/;

  public final native JsSVGRect createSVGRect() /*-{
    return this.createSVGRect();
  }-*/;

  public final native JsSVGTransform createSVGTransform() /*-{
    return this.createSVGTransform();
  }-*/;

  public final native JsSVGTransform createSVGTransformFromMatrix(SVGMatrix matrix) /*-{
    return this.createSVGTransformFromMatrix(matrix);
  }-*/;

  public final native void deselectAll() /*-{
    this.deselectAll();
  }-*/;

  public final native void forceRedraw() /*-{
    this.forceRedraw();
  }-*/;

  public final native float getCurrentTime() /*-{
    return this.getCurrentTime();
  }-*/;

  public final native JsElement getElementById(String elementId) /*-{
    return this.getElementById(elementId);
  }-*/;

  public final native JsNodeList getEnclosureList(SVGRect rect, SVGElement referenceElement) /*-{
    return this.getEnclosureList(rect, referenceElement);
  }-*/;

  public final native JsNodeList getIntersectionList(SVGRect rect, SVGElement referenceElement) /*-{
    return this.getIntersectionList(rect, referenceElement);
  }-*/;

  public final native void pauseAnimations() /*-{
    this.pauseAnimations();
  }-*/;

  public final native void setCurrentTime(float seconds) /*-{
    this.setCurrentTime(seconds);
  }-*/;

  public final native int suspendRedraw(int maxWaitMilliseconds) /*-{
    return this.suspendRedraw(maxWaitMilliseconds);
  }-*/;

  public final native void unpauseAnimations() /*-{
    this.unpauseAnimations();
  }-*/;

  public final native void unsuspendRedraw(int suspendHandleId) /*-{
    this.unsuspendRedraw(suspendHandleId);
  }-*/;

  public final native void unsuspendRedrawAll() /*-{
    this.unsuspendRedrawAll();
  }-*/;
}
