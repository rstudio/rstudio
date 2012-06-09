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
  * The <code>SVGMaskElement</code> interface provides access to the properties of <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/mask">&lt;mask&gt;</a></code>
 elements, as well as methods to manipulate them.
  */
public interface SVGMaskElement extends SVGElement, SVGTests, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable {


  /**
    * Corresponds to attribute 
<code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Attribute/height">height</a></code> on the given <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/mask">&lt;mask&gt;</a></code>
 element.
    */
  SVGAnimatedLength getAnimatedHeight();


  /**
    * Corresponds to attribute 
<code><a rel="internal" href="https://developer.mozilla.org/en/SVG/Attribute/maskContentUnits" class="new">maskContentUnits</a></code> on the given <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/mask">&lt;mask&gt;</a></code>
 element. Takes one of the constants defined in <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/SVGUnitTypes" class="new">SVGUnitTypes</a></code>
    */
  SVGAnimatedEnumeration getMaskContentUnits();


  /**
    * Corresponds to attribute 
<code><a rel="internal" href="https://developer.mozilla.org/en/SVG/Attribute/maskUnits" class="new">maskUnits</a></code> on the given <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/mask">&lt;mask&gt;</a></code>
 element. Takes one of the constants defined in <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/SVGUnitTypes" class="new">SVGUnitTypes</a></code>
    */
  SVGAnimatedEnumeration getMaskUnits();


  /**
    * Corresponds to attribute 
<code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Attribute/width">width</a></code> on the given <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/mask">&lt;mask&gt;</a></code>
 element.
    */
  SVGAnimatedLength getAnimatedWidth();

  SVGAnimatedLength getX();

  SVGAnimatedLength getY();
}
