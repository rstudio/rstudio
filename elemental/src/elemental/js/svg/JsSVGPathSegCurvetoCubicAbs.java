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
import elemental.svg.SVGPathSeg;
import elemental.svg.SVGPathSegCurvetoCubicAbs;

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

public class JsSVGPathSegCurvetoCubicAbs extends JsSVGPathSeg  implements SVGPathSegCurvetoCubicAbs {
  protected JsSVGPathSegCurvetoCubicAbs() {}

  public final native float getX() /*-{
    return this.x;
  }-*/;

  public final native void setX(float param_x) /*-{
    this.x = param_x;
  }-*/;

  public final native float getX1() /*-{
    return this.x1;
  }-*/;

  public final native void setX1(float param_x1) /*-{
    this.x1 = param_x1;
  }-*/;

  public final native float getX2() /*-{
    return this.x2;
  }-*/;

  public final native void setX2(float param_x2) /*-{
    this.x2 = param_x2;
  }-*/;

  public final native float getY() /*-{
    return this.y;
  }-*/;

  public final native void setY(float param_y) /*-{
    this.y = param_y;
  }-*/;

  public final native float getY1() /*-{
    return this.y1;
  }-*/;

  public final native void setY1(float param_y1) /*-{
    this.y1 = param_y1;
  }-*/;

  public final native float getY2() /*-{
    return this.y2;
  }-*/;

  public final native void setY2(float param_y2) /*-{
    this.y2 = param_y2;
  }-*/;
}
