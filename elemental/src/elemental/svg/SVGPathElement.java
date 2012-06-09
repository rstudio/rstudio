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
  * The <code>SVGPathElement</code> interface corresponds to the <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/path">&lt;path&gt;</a></code>
 element.
  */
public interface SVGPathElement extends SVGElement, SVGTests, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGTransformable {

  SVGPathSegList getAnimatedNormalizedPathSegList();

  SVGPathSegList getAnimatedPathSegList();

  SVGPathSegList getNormalizedPathSegList();


  /**
    * Corresponds to attribute 
<code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Attribute/pathLength">pathLength</a></code> on the given <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/path">&lt;path&gt;</a></code>
 element.
    */
  SVGAnimatedNumber getPathLength();

  SVGPathSegList getPathSegList();


  /**
    * Returns a stand-alone, parentless <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/SVGPathSegArcAbs" class="new">SVGPathSegArcAbs</a></code>
 object.<br> <br> <strong>Parameters:</strong> <ul> <li><code>float <em>x</em></code><br> The absolute X coordinate for the end point of this path segment.</li> <li><code>float <em>y</em> </code><br> The absolute Y coordinate for the end point of this path segment.</li> <li><code>float <em>r1</em></code><br> The x-axis radius for the ellipse.</li> <li><code>float <em>r2 </em></code><br> The y-axis radius for the ellipse.</li> <li><code>float <em>angle </em></code><br> The rotation angle in degrees for the ellipse's x-axis relative to the x-axis of the user coordinate system.</li> <li><code>boolean <em>largeArcFlag </em></code><br> The value of the large-arc-flag parameter.</li> <li><code>boolean <em>sweepFlag </em></code><br> The value of the large-arc-flag parameter.</li> </ul>
    */
  SVGPathSegArcAbs createSVGPathSegArcAbs(float x, float y, float r1, float r2, float angle, boolean largeArcFlag, boolean sweepFlag);


  /**
    * Returns a stand-alone, parentless <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/SVGPathSegArcRel" class="new">SVGPathSegArcRel</a></code>
 object.<br> <br> <strong>Parameters:</strong> <ul> <li><code>float <em>x</em></code><br> The relative X coordinate for the end point of this path segment.</li> <li><code>float <em>y</em> </code><br> The relative Y coordinate for the end point of this path segment.</li> <li><code>float <em>r1</em></code><br> The x-axis radius for the ellipse.</li> <li><code>float <em>r2 </em></code><br> The y-axis radius for the ellipse.</li> <li><code>float <em>angle </em></code><br> The rotation angle in degrees for the ellipse's x-axis relative to the x-axis of the user coordinate system.</li> <li><code>boolean <em>largeArcFlag </em></code><br> The value of the large-arc-flag parameter.</li> <li><code>boolean <em>sweepFlag </em></code><br> The value of the large-arc-flag parameter.</li> </ul>
    */
  SVGPathSegArcRel createSVGPathSegArcRel(float x, float y, float r1, float r2, float angle, boolean largeArcFlag, boolean sweepFlag);


  /**
    * Returns a stand-alone, parentless <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/SVGPathSegClosePath" class="new">SVGPathSegClosePath</a></code>
 object.
    */
  SVGPathSegClosePath createSVGPathSegClosePath();


  /**
    * Returns a stand-alone, parentless <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/SVGPathSegCurvetoCubicAbs" class="new">SVGPathSegCurvetoCubicAbs</a></code>
 object.<br> <br> Parameters: <ul> <li><code>float <em>x</em></code><br> The absolute X coordinate for the end point of this path segment.</li> <li><code>float <em>y</em> </code><br> The absolute Y coordinate for the end point of this path segment.</li> <li><code>float <em>x1</em></code><br> The absolute X coordinate for the first control point.</li> <li><code>float <em>y1</em></code><br> The absolute Y coordinate for the first control point.</li> <li><code>float <em>x2</em></code><br> The absolute X coordinate for the second control point.</li> <li><code>float <em>y2</em></code><br> The absolute Y coordinate for the second control point.</li> </ul>
    */
  SVGPathSegCurvetoCubicAbs createSVGPathSegCurvetoCubicAbs(float x, float y, float x1, float y1, float x2, float y2);


  /**
    * Returns a stand-alone, parentless <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/SVGPathSegCurvetoCubicRel" class="new">SVGPathSegCurvetoCubicRel</a></code>
 object.<br> <br> Parameters: <ul> <li><code>float <em>x</em></code><br> The relative X coordinate for the end point of this path segment.</li> <li><code>float <em>y</em> </code><br> The relative Y coordinate for the end point of this path segment.</li> <li><code>float <em>x1</em></code><br> The relative X coordinate for the first control point.</li> <li><code>float <em>y1</em></code><br> The relative Y coordinate for the first control point.</li> <li><code>float <em>x2</em></code><br> The relative X coordinate for the second control point.</li> <li><code>float <em>y2</em></code><br> The relative Y coordinate for the second control point.</li> </ul>
    */
  SVGPathSegCurvetoCubicRel createSVGPathSegCurvetoCubicRel(float x, float y, float x1, float y1, float x2, float y2);


  /**
    * Returns a stand-alone, parentless <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/SVGPathSegCurvetoCubicSmoothAbs" class="new">SVGPathSegCurvetoCubicSmoothAbs</a></code>
 object.<br> <br> Parameters <ul> <li><code>float <em>x </em></code><br> The absolute X coordinate for the end point of this path segment.</li> <li><code>float <em>y </em></code><br> The absolute Y coordinate for the end point of this path segment.</li> <li><code>float <em>x2 </em></code><br> The absolute X coordinate for the second control point.</li> <li><code>float <em>y2 </em></code><br> The absolute Y coordinate for the second control point.</li> </ul>
    */
  SVGPathSegCurvetoCubicSmoothAbs createSVGPathSegCurvetoCubicSmoothAbs(float x, float y, float x2, float y2);


  /**
    * Returns a stand-alone, parentless <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/SVGPathSegCurvetoCubicSmoothRel" class="new">SVGPathSegCurvetoCubicSmoothRel</a></code>
 object.<br> <br> Parameters <ul> <li><code>float <em>x </em></code><br> The absolute X coordinate for the end point of this path segment.</li> <li><code>float <em>y </em></code><br> The absolute Y coordinate for the end point of this path segment.</li> <li><code>float <em>x2 </em></code><br> The absolute X coordinate for the second control point.</li> <li><code>float <em>y2 </em></code><br> The absolute Y coordinate for the second control point.</li> </ul>
    */
  SVGPathSegCurvetoCubicSmoothRel createSVGPathSegCurvetoCubicSmoothRel(float x, float y, float x2, float y2);


  /**
    * Returns a stand-alone, parentless <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/SVGPathSegCurvetoQuadraticAbs" class="new">SVGPathSegCurvetoQuadraticAbs</a></code>
 object.<br> <br> Parameters: <ul> <li><code>float <em>x</em></code><br> The absolute X coordinate for the end point of this path segment.</li> <li><code>float <em>y</em> </code><br> The absolute Y coordinate for the end point of this path segment.</li> <li><code>float <em>x1</em></code><br> The absolute X coordinate for the first control point.</li> <li><code>float <em>y1</em></code><br> The absolute Y coordinate for the first control point.</li> </ul>
    */
  SVGPathSegCurvetoQuadraticAbs createSVGPathSegCurvetoQuadraticAbs(float x, float y, float x1, float y1);


  /**
    * Returns a stand-alone, parentless <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/SVGPathSegCurvetoQuadraticRel" class="new">SVGPathSegCurvetoQuadraticRel</a></code>
 object.<br> <br> Parameters: <ul> <li><code>float <em>x</em></code><br> The relative X coordinate for the end point of this path segment.</li> <li><code>float <em>y</em> </code><br> The relative Y coordinate for the end point of this path segment.</li> <li><code>float <em>x1</em></code><br> The relative X coordinate for the first control point.</li> <li><code>float <em>y1</em></code><br> The relative Y coordinate for the first control point.</li> </ul>
    */
  SVGPathSegCurvetoQuadraticRel createSVGPathSegCurvetoQuadraticRel(float x, float y, float x1, float y1);


  /**
    * Returns a stand-alone, parentless <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/SVGPathSegCurvetoQuadraticSmoothAbs" class="new">SVGPathSegCurvetoQuadraticSmoothAbs</a></code>
 object.<br> <br> <strong>Parameters:</strong> <ul> <li><code>float <em>x</em></code><br> The absolute X coordinate for the end point of this path segment.</li> <li><code>float <em>y</em></code><br> The absolute Y coordinate for the end point of this path segment.</li> </ul>
    */
  SVGPathSegCurvetoQuadraticSmoothAbs createSVGPathSegCurvetoQuadraticSmoothAbs(float x, float y);


  /**
    * Returns a stand-alone, parentless <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/SVGPathSegCurvetoQuadraticSmoothRel" class="new">SVGPathSegCurvetoQuadraticSmoothRel</a></code>
 object.<br> <br> <strong>Parameters:</strong> <ul> <li><code>float <em>x</em></code><br> The absolute X coordinate for the end point of this path segment.</li> <li><code>float <em>y</em></code><br> The absolute Y coordinate for the end point of this path segment.</li> </ul>
    */
  SVGPathSegCurvetoQuadraticSmoothRel createSVGPathSegCurvetoQuadraticSmoothRel(float x, float y);


  /**
    * Returns a stand-alone, parentless <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/SVGPathSegLinetoAbs" class="new">SVGPathSegLinetoAbs</a></code>
 object.<br> <br> <strong>Parameters:</strong> <ul> <li><code>float <em>x</em></code><br> The absolute X coordinate for the end point of this path segment.</li> <li><code>float <em>y</em></code><br> The absolute Y coordinate for the end point of this path segment.</li> </ul>
    */
  SVGPathSegLinetoAbs createSVGPathSegLinetoAbs(float x, float y);


  /**
    * Returns a stand-alone, parentless <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/SVGPathSegLinetoHorizontalAbs" class="new">SVGPathSegLinetoHorizontalAbs</a></code>
 object.<br> <br> <strong>Parameters:</strong> <ul> <li><code>float <em>x</em></code><br> The absolute X coordinate for the end point of this path segment.</li> </ul>
    */
  SVGPathSegLinetoHorizontalAbs createSVGPathSegLinetoHorizontalAbs(float x);


  /**
    * Returns a stand-alone, parentless <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/SVGPathSegLinetoHorizontalRel" class="new">SVGPathSegLinetoHorizontalRel</a></code>
 object.<br> <br> <strong>Parameters:</strong> <ul> <li><code>float <em>x</em></code><br> The relative X coordinate for the end point of this path segment.</li> </ul>
    */
  SVGPathSegLinetoHorizontalRel createSVGPathSegLinetoHorizontalRel(float x);


  /**
    * Returns a stand-alone, parentless <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/SVGPathSegLinetoRel" class="new">SVGPathSegLinetoRel</a></code>
 object.<br> <br> <strong>Parameters:</strong> <ul> <li><code>float <em>x</em></code><br> The relative X coordinate for the end point of this path segment.</li> <li><code>float <em>y</em></code><br> The relative Y coordinate for the end point of this path segment.</li> </ul>
    */
  SVGPathSegLinetoRel createSVGPathSegLinetoRel(float x, float y);


  /**
    * Returns a stand-alone, parentless <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/SVGPathSegLinetoVerticalAbs" class="new">SVGPathSegLinetoVerticalAbs</a></code>
 object.<br> <br> <strong>Parameters:</strong> <ul> <li><code>float <em>y</em></code><br> The absolute Y coordinate for the end point of this path segment.</li> </ul>
    */
  SVGPathSegLinetoVerticalAbs createSVGPathSegLinetoVerticalAbs(float y);


  /**
    * Returns a stand-alone, parentless <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/SVGPathSegLinetoVerticalRel" class="new">SVGPathSegLinetoVerticalRel</a></code>
 object.<br> <br> <strong>Parameters:</strong> <ul> <li><code>float <em>y</em></code><br> The relative Y coordinate for the end point of this path segment.</li> </ul>
    */
  SVGPathSegLinetoVerticalRel createSVGPathSegLinetoVerticalRel(float y);


  /**
    * Returns a stand-alone, parentless <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/SVGPathSegMovetoAbs" class="new">SVGPathSegMovetoAbs</a></code>
 object.<br> <br> <strong>Parameters:</strong> <ul> <li><code>float <em>x</em></code><br> The absolute X coordinate for the end point of this path segment.</li> <li><code>float <em>y</em></code><br> The absolute Y coordinate for the end point of this path segment.</li> </ul>
    */
  SVGPathSegMovetoAbs createSVGPathSegMovetoAbs(float x, float y);


  /**
    * Returns a stand-alone, parentless <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/SVGPathSegMovetoRel" class="new">SVGPathSegMovetoRel</a></code>
 object.<br> <br> <strong>Parameters:</strong> <ul> <li><code>float <em>x</em></code><br> The relative X coordinate for the end point of this path segment.</li> <li><code>float <em>y</em></code><br> The relative Y coordinate for the end point of this path segment.</li> </ul>
    */
  SVGPathSegMovetoRel createSVGPathSegMovetoRel(float x, float y);


  /**
    * Returns the index into <code>pathSegList</code> which is <code>distance</code> units along the path, utilizing the user agent's distance-along-a-path algorithm.
    */
  int getPathSegAtLength(float distance);


  /**
    * Returns the (x,y) coordinate in user space which is distance units along the path, utilizing the browser's distance-along-a-path algorithm.
    */
  SVGPoint getPointAtLength(float distance);


  /**
    * Returns the computed value for the total length of the path using the browser's distance-along-a-path algorithm, as a distance in the current user coordinate system.
    */
  float getTotalLength();
}
