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
import elemental.svg.SVGAngle;
import elemental.svg.SVGMarkerElement;
import elemental.svg.SVGAnimatedEnumeration;
import elemental.svg.SVGAnimatedLength;
import elemental.svg.SVGElement;
import elemental.svg.SVGAnimatedAngle;

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

public class JsSVGMarkerElement extends JsSVGElement  implements SVGMarkerElement {
  protected JsSVGMarkerElement() {}

  public final native JsSVGAnimatedLength getMarkerHeight() /*-{
    return this.markerHeight;
  }-*/;

  public final native JsSVGAnimatedEnumeration getMarkerUnits() /*-{
    return this.markerUnits;
  }-*/;

  public final native JsSVGAnimatedLength getMarkerWidth() /*-{
    return this.markerWidth;
  }-*/;

  public final native JsSVGAnimatedAngle getOrientAngle() /*-{
    return this.orientAngle;
  }-*/;

  public final native JsSVGAnimatedEnumeration getOrientType() /*-{
    return this.orientType;
  }-*/;

  public final native JsSVGAnimatedLength getRefX() /*-{
    return this.refX;
  }-*/;

  public final native JsSVGAnimatedLength getRefY() /*-{
    return this.refY;
  }-*/;

  public final native void setOrientToAngle(SVGAngle angle) /*-{
    this.setOrientToAngle(angle);
  }-*/;

  public final native void setOrientToAuto() /*-{
    this.setOrientToAuto();
  }-*/;
}
