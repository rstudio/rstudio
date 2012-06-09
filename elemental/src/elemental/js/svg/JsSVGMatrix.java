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
import elemental.svg.SVGMatrix;

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

public class JsSVGMatrix extends JsElementalMixinBase  implements SVGMatrix {
  protected JsSVGMatrix() {}

  public final native double getA() /*-{
    return this.a;
  }-*/;

  public final native void setA(double param_a) /*-{
    this.a = param_a;
  }-*/;

  public final native double getB() /*-{
    return this.b;
  }-*/;

  public final native void setB(double param_b) /*-{
    this.b = param_b;
  }-*/;

  public final native double getC() /*-{
    return this.c;
  }-*/;

  public final native void setC(double param_c) /*-{
    this.c = param_c;
  }-*/;

  public final native double getD() /*-{
    return this.d;
  }-*/;

  public final native void setD(double param_d) /*-{
    this.d = param_d;
  }-*/;

  public final native double getE() /*-{
    return this.e;
  }-*/;

  public final native void setE(double param_e) /*-{
    this.e = param_e;
  }-*/;

  public final native double getF() /*-{
    return this.f;
  }-*/;

  public final native void setF(double param_f) /*-{
    this.f = param_f;
  }-*/;

  public final native JsSVGMatrix flipX() /*-{
    return this.flipX();
  }-*/;

  public final native JsSVGMatrix flipY() /*-{
    return this.flipY();
  }-*/;

  public final native JsSVGMatrix inverse() /*-{
    return this.inverse();
  }-*/;

  public final native JsSVGMatrix multiply(SVGMatrix secondMatrix) /*-{
    return this.multiply(secondMatrix);
  }-*/;

  public final native JsSVGMatrix rotate(float angle) /*-{
    return this.rotate(angle);
  }-*/;

  public final native JsSVGMatrix rotateFromVector(float x, float y) /*-{
    return this.rotateFromVector(x, y);
  }-*/;

  public final native JsSVGMatrix scale(float scaleFactor) /*-{
    return this.scale(scaleFactor);
  }-*/;

  public final native JsSVGMatrix scaleNonUniform(float scaleFactorX, float scaleFactorY) /*-{
    return this.scaleNonUniform(scaleFactorX, scaleFactorY);
  }-*/;

  public final native JsSVGMatrix skewX(float angle) /*-{
    return this.skewX(angle);
  }-*/;

  public final native JsSVGMatrix skewY(float angle) /*-{
    return this.skewY(angle);
  }-*/;

  public final native JsSVGMatrix translate(float x, float y) /*-{
    return this.translate(x, y);
  }-*/;
}
