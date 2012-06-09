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
import elemental.css.CSSStyleDeclaration;
import elemental.css.CSSValue;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * The <code>SVGStylable</code> interface is implemented on all objects corresponding to SVG elements that can have 
<code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Attribute/style">style</a></code>, {{SVGAttr("class") and presentation attributes specified on them.
  */
public interface SVGStylable {


  /**
    * Corresponds to attribute 
<code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Attribute/class">class</a></code> on the given element.
    */
  SVGAnimatedString getAnimatedClassName();


  /**
    * Corresponds to attribute 
<code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Attribute/style">style</a></code> on the given element.
    */
  CSSStyleDeclaration getSvgStyle();


  /**
    * Returns the base (i.e., static) value of a given presentation attribute as an object of type <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/CSSValue" class="new">CSSValue</a></code>
. The returned object is live; changes to the objects represent immediate changes to the objects to which the <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/CSSValue" class="new">CSSValue</a></code>
 is attached.
    */
  CSSValue getPresentationAttribute(String name);
}
