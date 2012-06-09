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
import elemental.svg.SVGAnimatedString;
import elemental.svg.SVGFEConvolveMatrixElement;
import elemental.svg.SVGAnimatedBoolean;
import elemental.svg.SVGAnimatedEnumeration;
import elemental.svg.SVGAnimatedInteger;
import elemental.svg.SVGElement;
import elemental.svg.SVGAnimatedNumberList;
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

public class JsSVGFEConvolveMatrixElement extends JsSVGElement  implements SVGFEConvolveMatrixElement {
  protected JsSVGFEConvolveMatrixElement() {}

  public final native JsSVGAnimatedNumber getBias() /*-{
    return this.bias;
  }-*/;

  public final native JsSVGAnimatedNumber getDivisor() /*-{
    return this.divisor;
  }-*/;

  public final native JsSVGAnimatedEnumeration getEdgeMode() /*-{
    return this.edgeMode;
  }-*/;

  public final native JsSVGAnimatedString getIn1() /*-{
    return this.in1;
  }-*/;

  public final native JsSVGAnimatedNumberList getKernelMatrix() /*-{
    return this.kernelMatrix;
  }-*/;

  public final native JsSVGAnimatedNumber getKernelUnitLengthX() /*-{
    return this.kernelUnitLengthX;
  }-*/;

  public final native JsSVGAnimatedNumber getKernelUnitLengthY() /*-{
    return this.kernelUnitLengthY;
  }-*/;

  public final native JsSVGAnimatedInteger getOrderX() /*-{
    return this.orderX;
  }-*/;

  public final native JsSVGAnimatedInteger getOrderY() /*-{
    return this.orderY;
  }-*/;

  public final native JsSVGAnimatedBoolean getPreserveAlpha() /*-{
    return this.preserveAlpha;
  }-*/;

  public final native JsSVGAnimatedInteger getTargetX() /*-{
    return this.targetX;
  }-*/;

  public final native JsSVGAnimatedInteger getTargetY() /*-{
    return this.targetY;
  }-*/;
}
