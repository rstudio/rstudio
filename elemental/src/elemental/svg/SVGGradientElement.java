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
  * The <code>SVGGradient</code> interface is a base interface used by <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/SVGLinearGradientElement">SVGLinearGradientElement</a></code>
 and <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/SVGRadialGradientElement">SVGRadialGradientElement</a></code>
.
  */
public interface SVGGradientElement extends SVGElement, SVGURIReference, SVGExternalResourcesRequired, SVGStylable {

  /**
    * Corresponds to value <em>pad</em>.
    */

    static final int SVG_SPREADMETHOD_PAD = 1;

  /**
    * Corresponds to value <em>reflect</em>.
    */

    static final int SVG_SPREADMETHOD_REFLECT = 2;

  /**
    * Corresponds to value <em>repeat</em>.
    */

    static final int SVG_SPREADMETHOD_REPEAT = 3;

  /**
    * The type is not one of predefined types. It is invalid to attempt to define a new value of this type or to attempt to switch an existing value to this type.
    */

    static final int SVG_SPREADMETHOD_UNKNOWN = 0;


  /**
    * Corresponds to attribute 
<code><a rel="internal" href="https://developer.mozilla.org/en/SVG/Attribute/gradientTransform" class="new">gradientTransform</a></code> on the given element.
    */
  SVGAnimatedTransformList getGradientTransform();


  /**
    * Corresponds to attribute 
<code><a rel="internal" href="https://developer.mozilla.org/en/SVG/Attribute/gradientUnits" class="new">gradientUnits</a></code> on the given element. Takes one of the constants defined in <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/SVGUnitTypes" class="new">SVGUnitTypes</a></code>
.
    */
  SVGAnimatedEnumeration getGradientUnits();


  /**
    * Corresponds to attribute 
<code><a rel="internal" href="https://developer.mozilla.org/en/SVG/Attribute/spreadMethod" class="new">spreadMethod</a></code> on the given element. One of the Spread Method Types defined on this interface.
    */
  SVGAnimatedEnumeration getSpreadMethod();
}
