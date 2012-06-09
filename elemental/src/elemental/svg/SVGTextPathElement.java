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
  * In addition to text drawn in a straight line, SVG also includes the ability to place text along the shape of a <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/path">&lt;path&gt;</a></code>
 element. To specify that a block of text is to be rendered along the shape of a <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/path">&lt;path&gt;</a></code>
, include the given text within a <code>textPath</code> element which includes an <code>xlink:href</code> attribute with a reference to a <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/path">&lt;path&gt;</a></code>
 element.
  */
public interface SVGTextPathElement extends SVGTextContentElement, SVGURIReference {

    static final int TEXTPATH_METHODTYPE_ALIGN = 1;

    static final int TEXTPATH_METHODTYPE_STRETCH = 2;

    static final int TEXTPATH_METHODTYPE_UNKNOWN = 0;

    static final int TEXTPATH_SPACINGTYPE_AUTO = 1;

    static final int TEXTPATH_SPACINGTYPE_EXACT = 2;

    static final int TEXTPATH_SPACINGTYPE_UNKNOWN = 0;

  SVGAnimatedEnumeration getMethod();

  SVGAnimatedEnumeration getSpacing();

  SVGAnimatedLength getStartOffset();
}
