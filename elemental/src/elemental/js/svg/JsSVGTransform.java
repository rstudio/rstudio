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
import elemental.svg.SVGTransform;

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

public class JsSVGTransform extends JsElementalMixinBase  implements SVGTransform {
  protected JsSVGTransform() {}

  public final native float getAngle() /*-{
    return this.angle;
  }-*/;

  public final native JsSVGMatrix getMatrix() /*-{
    return this.matrix;
  }-*/;

  public final native int getType() /*-{
    return this.type;
  }-*/;

  public final native void setRotate(float angle, float cx, float cy) /*-{
    this.setRotate(angle, cx, cy);
  }-*/;

  public final native void setScale(float sx, float sy) /*-{
    this.setScale(sx, sy);
  }-*/;

  public final native void setSkewX(float angle) /*-{
    this.setSkewX(angle);
  }-*/;

  public final native void setSkewY(float angle) /*-{
    this.setSkewY(angle);
  }-*/;

  public final native void setTranslate(float tx, float ty) /*-{
    this.setTranslate(tx, ty);
  }-*/;
}
