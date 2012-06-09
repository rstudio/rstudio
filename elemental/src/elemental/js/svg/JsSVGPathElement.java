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
import elemental.svg.SVGPathSegArcRel;
import elemental.svg.SVGPathSegCurvetoQuadraticRel;
import elemental.svg.SVGElement;
import elemental.svg.SVGPathSegLinetoVerticalRel;
import elemental.svg.SVGPathSegMovetoRel;
import elemental.svg.SVGPathSegLinetoRel;
import elemental.svg.SVGPathSegMovetoAbs;
import elemental.svg.SVGPathElement;
import elemental.svg.SVGPoint;
import elemental.svg.SVGPathSegLinetoHorizontalRel;
import elemental.svg.SVGPathSegCurvetoCubicSmoothAbs;
import elemental.svg.SVGPathSegLinetoAbs;
import elemental.svg.SVGPathSegLinetoHorizontalAbs;
import elemental.svg.SVGPathSegCurvetoCubicSmoothRel;
import elemental.svg.SVGPathSegCurvetoCubicAbs;
import elemental.svg.SVGPathSegClosePath;
import elemental.svg.SVGPathSegCurvetoCubicRel;
import elemental.svg.SVGPathSegCurvetoQuadraticSmoothAbs;
import elemental.svg.SVGPathSegList;
import elemental.svg.SVGPathSegCurvetoQuadraticSmoothRel;
import elemental.svg.SVGPathSegArcAbs;
import elemental.svg.SVGPathSegCurvetoQuadraticAbs;
import elemental.svg.SVGPathSegLinetoVerticalAbs;
import elemental.svg.SVGAnimatedNumber;

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

public class JsSVGPathElement extends JsSVGElement  implements SVGPathElement {
  protected JsSVGPathElement() {}

  public final native JsSVGPathSegList getAnimatedNormalizedPathSegList() /*-{
    return this.animatedNormalizedPathSegList;
  }-*/;

  public final native JsSVGPathSegList getAnimatedPathSegList() /*-{
    return this.animatedPathSegList;
  }-*/;

  public final native JsSVGPathSegList getNormalizedPathSegList() /*-{
    return this.normalizedPathSegList;
  }-*/;

  public final native JsSVGAnimatedNumber getPathLength() /*-{
    return this.pathLength;
  }-*/;

  public final native JsSVGPathSegList getPathSegList() /*-{
    return this.pathSegList;
  }-*/;

  public final native JsSVGPathSegArcAbs createSVGPathSegArcAbs(float x, float y, float r1, float r2, float angle, boolean largeArcFlag, boolean sweepFlag) /*-{
    return this.createSVGPathSegArcAbs(x, y, r1, r2, angle, largeArcFlag, sweepFlag);
  }-*/;

  public final native JsSVGPathSegArcRel createSVGPathSegArcRel(float x, float y, float r1, float r2, float angle, boolean largeArcFlag, boolean sweepFlag) /*-{
    return this.createSVGPathSegArcRel(x, y, r1, r2, angle, largeArcFlag, sweepFlag);
  }-*/;

  public final native JsSVGPathSegClosePath createSVGPathSegClosePath() /*-{
    return this.createSVGPathSegClosePath();
  }-*/;

  public final native JsSVGPathSegCurvetoCubicAbs createSVGPathSegCurvetoCubicAbs(float x, float y, float x1, float y1, float x2, float y2) /*-{
    return this.createSVGPathSegCurvetoCubicAbs(x, y, x1, y1, x2, y2);
  }-*/;

  public final native JsSVGPathSegCurvetoCubicRel createSVGPathSegCurvetoCubicRel(float x, float y, float x1, float y1, float x2, float y2) /*-{
    return this.createSVGPathSegCurvetoCubicRel(x, y, x1, y1, x2, y2);
  }-*/;

  public final native JsSVGPathSegCurvetoCubicSmoothAbs createSVGPathSegCurvetoCubicSmoothAbs(float x, float y, float x2, float y2) /*-{
    return this.createSVGPathSegCurvetoCubicSmoothAbs(x, y, x2, y2);
  }-*/;

  public final native JsSVGPathSegCurvetoCubicSmoothRel createSVGPathSegCurvetoCubicSmoothRel(float x, float y, float x2, float y2) /*-{
    return this.createSVGPathSegCurvetoCubicSmoothRel(x, y, x2, y2);
  }-*/;

  public final native JsSVGPathSegCurvetoQuadraticAbs createSVGPathSegCurvetoQuadraticAbs(float x, float y, float x1, float y1) /*-{
    return this.createSVGPathSegCurvetoQuadraticAbs(x, y, x1, y1);
  }-*/;

  public final native JsSVGPathSegCurvetoQuadraticRel createSVGPathSegCurvetoQuadraticRel(float x, float y, float x1, float y1) /*-{
    return this.createSVGPathSegCurvetoQuadraticRel(x, y, x1, y1);
  }-*/;

  public final native JsSVGPathSegCurvetoQuadraticSmoothAbs createSVGPathSegCurvetoQuadraticSmoothAbs(float x, float y) /*-{
    return this.createSVGPathSegCurvetoQuadraticSmoothAbs(x, y);
  }-*/;

  public final native JsSVGPathSegCurvetoQuadraticSmoothRel createSVGPathSegCurvetoQuadraticSmoothRel(float x, float y) /*-{
    return this.createSVGPathSegCurvetoQuadraticSmoothRel(x, y);
  }-*/;

  public final native JsSVGPathSegLinetoAbs createSVGPathSegLinetoAbs(float x, float y) /*-{
    return this.createSVGPathSegLinetoAbs(x, y);
  }-*/;

  public final native JsSVGPathSegLinetoHorizontalAbs createSVGPathSegLinetoHorizontalAbs(float x) /*-{
    return this.createSVGPathSegLinetoHorizontalAbs(x);
  }-*/;

  public final native JsSVGPathSegLinetoHorizontalRel createSVGPathSegLinetoHorizontalRel(float x) /*-{
    return this.createSVGPathSegLinetoHorizontalRel(x);
  }-*/;

  public final native JsSVGPathSegLinetoRel createSVGPathSegLinetoRel(float x, float y) /*-{
    return this.createSVGPathSegLinetoRel(x, y);
  }-*/;

  public final native JsSVGPathSegLinetoVerticalAbs createSVGPathSegLinetoVerticalAbs(float y) /*-{
    return this.createSVGPathSegLinetoVerticalAbs(y);
  }-*/;

  public final native JsSVGPathSegLinetoVerticalRel createSVGPathSegLinetoVerticalRel(float y) /*-{
    return this.createSVGPathSegLinetoVerticalRel(y);
  }-*/;

  public final native JsSVGPathSegMovetoAbs createSVGPathSegMovetoAbs(float x, float y) /*-{
    return this.createSVGPathSegMovetoAbs(x, y);
  }-*/;

  public final native JsSVGPathSegMovetoRel createSVGPathSegMovetoRel(float x, float y) /*-{
    return this.createSVGPathSegMovetoRel(x, y);
  }-*/;

  public final native int getPathSegAtLength(float distance) /*-{
    return this.getPathSegAtLength(distance);
  }-*/;

  public final native JsSVGPoint getPointAtLength(float distance) /*-{
    return this.getPointAtLength(distance);
  }-*/;

  public final native float getTotalLength() /*-{
    return this.getTotalLength();
  }-*/;
}
