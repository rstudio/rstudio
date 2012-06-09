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
  * This filter changes colors based on a transformation matrix. Every pixel's color value (represented by an [R,G,B,A] vector) is <a title="http://en.wikipedia.org/wiki/Matrix_multiplication" class=" external" rel="external" href="http://en.wikipedia.org/wiki/Matrix_multiplication" target="_blank">matrix multiplated</a> to create a new color.
  */
public interface SVGFEColorMatrixElement extends SVGElement, SVGFilterPrimitiveStandardAttributes {

    static final int SVG_FECOLORMATRIX_TYPE_HUEROTATE = 3;

    static final int SVG_FECOLORMATRIX_TYPE_LUMINANCETOALPHA = 4;

    static final int SVG_FECOLORMATRIX_TYPE_MATRIX = 1;

    static final int SVG_FECOLORMATRIX_TYPE_SATURATE = 2;

    static final int SVG_FECOLORMATRIX_TYPE_UNKNOWN = 0;

  SVGAnimatedString getIn1();

  SVGAnimatedEnumeration getType();

  SVGAnimatedNumberList getValues();
}
