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
  * The <code>filter</code> element serves as container for atomic filter operations. It is never rendered directly. A filter is referenced by using the 
<code><a rel="internal" href="https://developer.mozilla.org/en/SVG/Attribute/filter" class="new">filter</a></code> attribute on the target SVG element.
  */
public interface SVGFilterElement extends SVGElement, SVGURIReference, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable {

  SVGAnimatedInteger getFilterResX();

  SVGAnimatedInteger getFilterResY();

  SVGAnimatedEnumeration getFilterUnits();

  SVGAnimatedLength getAnimatedHeight();

  SVGAnimatedEnumeration getPrimitiveUnits();

  SVGAnimatedLength getAnimatedWidth();

  SVGAnimatedLength getX();

  SVGAnimatedLength getY();

  void setFilterRes(int filterResX, int filterResY);
}
