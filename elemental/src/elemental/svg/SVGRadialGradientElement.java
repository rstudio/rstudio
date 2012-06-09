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
  * The <code>SVGRadialGradientElement</code> interface corresponds to the <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/radialGradient">&lt;radialGradient&gt;</a></code>
 element.
  */
public interface SVGRadialGradientElement extends SVGGradientElement {


  /**
    * Corresponds to attribute 
<code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Attribute/cx">cx</a></code> on the given <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/radialGradient">&lt;radialGradient&gt;</a></code>
 element.
    */
  SVGAnimatedLength getCx();


  /**
    * Corresponds to attribute 
<code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Attribute/cy">cy</a></code> on the given <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/radialGradient">&lt;radialGradient&gt;</a></code>
 element.
    */
  SVGAnimatedLength getCy();


  /**
    * Corresponds to attribute 
<code><a rel="internal" href="https://developer.mozilla.org/en/SVG/Attribute/fx" class="new">fx</a></code> on the given <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/radialGradient">&lt;radialGradient&gt;</a></code>
 element.
    */
  SVGAnimatedLength getFx();


  /**
    * Corresponds to attribute 
<code><a rel="internal" href="https://developer.mozilla.org/en/SVG/Attribute/fy" class="new">fy</a></code> on the given <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/radialGradient">&lt;radialGradient&gt;</a></code>
 element.
    */
  SVGAnimatedLength getFy();

  SVGAnimatedLength getR();
}
