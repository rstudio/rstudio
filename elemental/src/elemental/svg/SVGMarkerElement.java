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
  * The <code>marker</code> element defines the graphics that is to be used for drawing arrowheads or polymarkers on a given <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/path">&lt;path&gt;</a></code>
, <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/line">&lt;line&gt;</a></code>
, <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/polyline">&lt;polyline&gt;</a></code>
 or <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/polygon">&lt;polygon&gt;</a></code>
 element.
  */
public interface SVGMarkerElement extends SVGElement, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGFitToViewBox {

    static final int SVG_MARKERUNITS_STROKEWIDTH = 2;

    static final int SVG_MARKERUNITS_UNKNOWN = 0;

    static final int SVG_MARKERUNITS_USERSPACEONUSE = 1;

    static final int SVG_MARKER_ORIENT_ANGLE = 2;

    static final int SVG_MARKER_ORIENT_AUTO = 1;

    static final int SVG_MARKER_ORIENT_UNKNOWN = 0;

  SVGAnimatedLength getMarkerHeight();

  SVGAnimatedEnumeration getMarkerUnits();

  SVGAnimatedLength getMarkerWidth();

  SVGAnimatedAngle getOrientAngle();

  SVGAnimatedEnumeration getOrientType();

  SVGAnimatedLength getRefX();

  SVGAnimatedLength getRefY();

  void setOrientToAngle(SVGAngle angle);

  void setOrientToAuto();
}
