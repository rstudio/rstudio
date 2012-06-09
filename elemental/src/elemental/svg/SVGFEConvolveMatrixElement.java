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
  * The filter modifies a pixel by means of a convolution matrix, that also takes neighboring pixels into account.
  */
public interface SVGFEConvolveMatrixElement extends SVGElement, SVGFilterPrimitiveStandardAttributes {

    static final int SVG_EDGEMODE_DUPLICATE = 1;

    static final int SVG_EDGEMODE_NONE = 3;

    static final int SVG_EDGEMODE_UNKNOWN = 0;

    static final int SVG_EDGEMODE_WRAP = 2;

  SVGAnimatedNumber getBias();

  SVGAnimatedNumber getDivisor();

  SVGAnimatedEnumeration getEdgeMode();

  SVGAnimatedString getIn1();

  SVGAnimatedNumberList getKernelMatrix();

  SVGAnimatedNumber getKernelUnitLengthX();

  SVGAnimatedNumber getKernelUnitLengthY();

  SVGAnimatedInteger getOrderX();

  SVGAnimatedInteger getOrderY();

  SVGAnimatedBoolean getPreserveAlpha();

  SVGAnimatedInteger getTargetX();

  SVGAnimatedInteger getTargetY();
}
