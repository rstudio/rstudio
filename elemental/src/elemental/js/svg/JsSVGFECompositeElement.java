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
import elemental.svg.SVGAnimatedEnumeration;
import elemental.svg.SVGAnimatedString;
import elemental.svg.SVGElement;
import elemental.svg.SVGAnimatedNumber;
import elemental.svg.SVGFECompositeElement;

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

public class JsSVGFECompositeElement extends JsSVGElement  implements SVGFECompositeElement {
  protected JsSVGFECompositeElement() {}

  public final native JsSVGAnimatedString getIn1() /*-{
    return this.in1;
  }-*/;

  public final native JsSVGAnimatedString getIn2() /*-{
    return this.in2;
  }-*/;

  public final native JsSVGAnimatedNumber getK1() /*-{
    return this.k1;
  }-*/;

  public final native JsSVGAnimatedNumber getK2() /*-{
    return this.k2;
  }-*/;

  public final native JsSVGAnimatedNumber getK3() /*-{
    return this.k3;
  }-*/;

  public final native JsSVGAnimatedNumber getK4() /*-{
    return this.k4;
  }-*/;

  public final native JsSVGAnimatedEnumeration getOperator() /*-{
    return this.operator;
  }-*/;
}
