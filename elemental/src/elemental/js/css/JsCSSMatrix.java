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
package elemental.js.css;
import elemental.css.CSSMatrix;

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

public class JsCSSMatrix extends JsElementalMixinBase  implements CSSMatrix {
  protected JsCSSMatrix() {}

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

  public final native double getM11() /*-{
    return this.m11;
  }-*/;

  public final native void setM11(double param_m11) /*-{
    this.m11 = param_m11;
  }-*/;

  public final native double getM12() /*-{
    return this.m12;
  }-*/;

  public final native void setM12(double param_m12) /*-{
    this.m12 = param_m12;
  }-*/;

  public final native double getM13() /*-{
    return this.m13;
  }-*/;

  public final native void setM13(double param_m13) /*-{
    this.m13 = param_m13;
  }-*/;

  public final native double getM14() /*-{
    return this.m14;
  }-*/;

  public final native void setM14(double param_m14) /*-{
    this.m14 = param_m14;
  }-*/;

  public final native double getM21() /*-{
    return this.m21;
  }-*/;

  public final native void setM21(double param_m21) /*-{
    this.m21 = param_m21;
  }-*/;

  public final native double getM22() /*-{
    return this.m22;
  }-*/;

  public final native void setM22(double param_m22) /*-{
    this.m22 = param_m22;
  }-*/;

  public final native double getM23() /*-{
    return this.m23;
  }-*/;

  public final native void setM23(double param_m23) /*-{
    this.m23 = param_m23;
  }-*/;

  public final native double getM24() /*-{
    return this.m24;
  }-*/;

  public final native void setM24(double param_m24) /*-{
    this.m24 = param_m24;
  }-*/;

  public final native double getM31() /*-{
    return this.m31;
  }-*/;

  public final native void setM31(double param_m31) /*-{
    this.m31 = param_m31;
  }-*/;

  public final native double getM32() /*-{
    return this.m32;
  }-*/;

  public final native void setM32(double param_m32) /*-{
    this.m32 = param_m32;
  }-*/;

  public final native double getM33() /*-{
    return this.m33;
  }-*/;

  public final native void setM33(double param_m33) /*-{
    this.m33 = param_m33;
  }-*/;

  public final native double getM34() /*-{
    return this.m34;
  }-*/;

  public final native void setM34(double param_m34) /*-{
    this.m34 = param_m34;
  }-*/;

  public final native double getM41() /*-{
    return this.m41;
  }-*/;

  public final native void setM41(double param_m41) /*-{
    this.m41 = param_m41;
  }-*/;

  public final native double getM42() /*-{
    return this.m42;
  }-*/;

  public final native void setM42(double param_m42) /*-{
    this.m42 = param_m42;
  }-*/;

  public final native double getM43() /*-{
    return this.m43;
  }-*/;

  public final native void setM43(double param_m43) /*-{
    this.m43 = param_m43;
  }-*/;

  public final native double getM44() /*-{
    return this.m44;
  }-*/;

  public final native void setM44(double param_m44) /*-{
    this.m44 = param_m44;
  }-*/;

  public final native JsCSSMatrix inverse() /*-{
    return this.inverse();
  }-*/;

  public final native JsCSSMatrix multiply(CSSMatrix secondMatrix) /*-{
    return this.multiply(secondMatrix);
  }-*/;

  public final native JsCSSMatrix rotate(double rotX, double rotY, double rotZ) /*-{
    return this.rotate(rotX, rotY, rotZ);
  }-*/;

  public final native JsCSSMatrix rotateAxisAngle(double x, double y, double z, double angle) /*-{
    return this.rotateAxisAngle(x, y, z, angle);
  }-*/;

  public final native JsCSSMatrix scale(double scaleX, double scaleY, double scaleZ) /*-{
    return this.scale(scaleX, scaleY, scaleZ);
  }-*/;

  public final native void setMatrixValue(String string) /*-{
    this.setMatrixValue(string);
  }-*/;

  public final native JsCSSMatrix skewX(double angle) /*-{
    return this.skewX(angle);
  }-*/;

  public final native JsCSSMatrix skewY(double angle) /*-{
    return this.skewY(angle);
  }-*/;

  public final native JsCSSMatrix translate(double x, double y, double z) /*-{
    return this.translate(x, y, z);
  }-*/;
}
