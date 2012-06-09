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
import elemental.svg.SVGComponentTransferFunctionElement;
import elemental.svg.SVGAnimatedEnumeration;
import elemental.svg.SVGAnimatedNumber;
import elemental.svg.SVGElement;
import elemental.svg.SVGAnimatedNumberList;

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

public class JsSVGComponentTransferFunctionElement extends JsSVGElement  implements SVGComponentTransferFunctionElement {
  protected JsSVGComponentTransferFunctionElement() {}

  public final native JsSVGAnimatedNumber getAmplitude() /*-{
    return this.amplitude;
  }-*/;

  public final native JsSVGAnimatedNumber getExponent() /*-{
    return this.exponent;
  }-*/;

  public final native JsSVGAnimatedNumber getIntercept() /*-{
    return this.intercept;
  }-*/;

  public final native JsSVGAnimatedNumber getOffset() /*-{
    return this.offset;
  }-*/;

  public final native JsSVGAnimatedNumber getSlope() /*-{
    return this.slope;
  }-*/;

  public final native JsSVGAnimatedNumberList getTableValues() /*-{
    return this.tableValues;
  }-*/;

  public final native JsSVGAnimatedEnumeration getType() /*-{
    return this.type;
  }-*/;
}
