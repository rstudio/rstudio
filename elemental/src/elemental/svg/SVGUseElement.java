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
  * The <code>SVGUseElement</code> interface provides access to the properties of <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/use">&lt;use&gt;</a></code>
 elements, as well as methods to manipulate them.
  */
public interface SVGUseElement extends SVGElement, SVGURIReference, SVGTests, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGTransformable {


  /**
    * If the 
<code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Attribute/xlink%3Ahref">xlink:href</a></code> attribute is being animated, contains the current animated root of the instance tree. If the 
<code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Attribute/xlink%3Ahref">xlink:href</a></code> attribute is not currently being animated, contains the same value as <code>instanceRoot</code>. See description of <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/SVGElementInstance" class="new">SVGElementInstance</a></code>
 to learn more about the instance tree.
    */
  SVGElementInstance getAnimatedInstanceRoot();


  /**
    * Corresponds to attribute 
<code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Attribute/height">height</a></code> on the given <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/use">&lt;use&gt;</a></code>
 element.
    */
  SVGAnimatedLength getAnimatedHeight();


  /**
    * The root of the instance tree. See description of <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/SVGElementInstance" class="new">SVGElementInstance</a></code>
 to learn more about the instance tree.
    */
  SVGElementInstance getInstanceRoot();


  /**
    * Corresponds to attribute 
<code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Attribute/width">width</a></code> on the given <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/use">&lt;use&gt;</a></code>
 element.
    */
  SVGAnimatedLength getAnimatedWidth();

  SVGAnimatedLength getX();

  SVGAnimatedLength getY();
}
