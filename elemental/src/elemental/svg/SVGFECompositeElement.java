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
package elemental.svg;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * <p>Two input images are joined by means of an 
<code><a rel="internal" href="https://developer.mozilla.org/en/SVG/Attribute/operator" class="new">operator</a></code> applied to each input pixel together with an arithmetic operation</p>
<pre>result = k1*in1*in2 + k2*in1 + k3*in2 + k4</pre>
  */
public interface SVGFECompositeElement extends SVGElement, SVGFilterPrimitiveStandardAttributes {

    static final int SVG_FECOMPOSITE_OPERATOR_ARITHMETIC = 6;

    static final int SVG_FECOMPOSITE_OPERATOR_ATOP = 4;

    static final int SVG_FECOMPOSITE_OPERATOR_IN = 2;

    static final int SVG_FECOMPOSITE_OPERATOR_OUT = 3;

    static final int SVG_FECOMPOSITE_OPERATOR_OVER = 1;

    static final int SVG_FECOMPOSITE_OPERATOR_UNKNOWN = 0;

    static final int SVG_FECOMPOSITE_OPERATOR_XOR = 5;

  SVGAnimatedString getIn1();

  SVGAnimatedString getIn2();

  SVGAnimatedNumber getK1();

  SVGAnimatedNumber getK2();

  SVGAnimatedNumber getK3();

  SVGAnimatedNumber getK4();

  SVGAnimatedEnumeration getOperator();
}
