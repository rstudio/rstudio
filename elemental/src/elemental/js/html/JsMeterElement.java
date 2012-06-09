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
import elemental.js.dom.JsElement;
import elemental.dom.Element;
import elemental.js.dom.JsNodeList;
import elemental.dom.NodeList;
import elemental.html.MeterElement;

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

public class JsMeterElement extends JsElement  implements MeterElement {
  protected JsMeterElement() {}

  public final native double getHigh() /*-{
    return this.high;
  }-*/;

  public final native void setHigh(double param_high) /*-{
    this.high = param_high;
  }-*/;

  public final native JsNodeList getLabels() /*-{
    return this.labels;
  }-*/;

  public final native double getLow() /*-{
    return this.low;
  }-*/;

  public final native void setLow(double param_low) /*-{
    this.low = param_low;
  }-*/;

  public final native double getMax() /*-{
    return this.max;
  }-*/;

  public final native void setMax(double param_max) /*-{
    this.max = param_max;
  }-*/;

  public final native double getMin() /*-{
    return this.min;
  }-*/;

  public final native void setMin(double param_min) /*-{
    this.min = param_min;
  }-*/;

  public final native double getOptimum() /*-{
    return this.optimum;
  }-*/;

  public final native void setOptimum(double param_optimum) /*-{
    this.optimum = param_optimum;
  }-*/;

  public final native double getValue() /*-{
    return this.value;
  }-*/;

  public final native void setValue(double param_value) /*-{
    this.value = param_value;
  }-*/;
}
