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
  * <p><code>SVGTransform</code> is the interface for one of the component transformations within an <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/SVGTransformList">SVGTransformList</a></code>
; thus, an <code>SVGTransform</code> object corresponds to a single component (e.g., <code>scale(…)</code> or <code>matrix(…)</code>) within a 
<code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Attribute/transform">transform</a></code> attribute.</p>
<p>An <code>SVGTransform</code> object can be designated as read only, which means that attempts to modify the object will result in an exception being thrown.</p>
  */
public interface SVGTransform {

  /**
    * A <code>matrix(…)</code> transformation
    */

    static final int SVG_TRANSFORM_MATRIX = 1;

    static final int SVG_TRANSFORM_ROTATE = 4;

  /**
    * A <code>scale(…)</code> transformation
    */

    static final int SVG_TRANSFORM_SCALE = 3;

    static final int SVG_TRANSFORM_SKEWX = 5;

    static final int SVG_TRANSFORM_SKEWY = 6;

  /**
    * A <code>translate(…)</code> transformation
    */

    static final int SVG_TRANSFORM_TRANSLATE = 2;

  /**
    * The unit type is not one of predefined unit types. It is invalid to attempt to define a new value of this type or to attempt to switch an existing value to this type.
    */

    static final int SVG_TRANSFORM_UNKNOWN = 0;


  /**
    * A convenience attribute for <code>SVG_TRANSFORM_ROTATE</code>, <code>SVG_TRANSFORM_SKEWX</code> and <code>SVG_TRANSFORM_SKEWY</code>. It holds the angle that was specified.<br> <br> For <code>SVG_TRANSFORM_MATRIX</code>, <code>SVG_TRANSFORM_TRANSLATE</code> and <code>SVG_TRANSFORM_SCALE</code>, <code>angle</code> will be zero.
    */
  float getAngle();


  /**
    * <p>The matrix that represents this transformation. The matrix object is live, meaning that any changes made to the <code>SVGTransform</code> object are immediately reflected in the matrix object and vice versa. In case the matrix object is changed directly (i.e., without using the methods on the <code>SVGTransform</code> interface itself) then the type of the <code>SVGTransform</code> changes to <code>SVG_TRANSFORM_MATRIX</code>.</p> <ul> <li>For <code>SVG_TRANSFORM_MATRIX</code>, the matrix contains the a, b, c, d, e, f values supplied by the user.</li> <li>For <code>SVG_TRANSFORM_TRANSLATE</code>, e and f represent the translation amounts (a=1, b=0, c=0 and d=1).</li> <li>For <code>SVG_TRANSFORM_SCALE</code>, a and d represent the scale amounts (b=0, c=0, e=0 and f=0).</li> <li>For <code>SVG_TRANSFORM_SKEWX</code> and <code>SVG_TRANSFORM_SKEWY</code>, a, b, c and d represent the matrix which will result in the given skew (e=0 and f=0).</li> <li>For <code>SVG_TRANSFORM_ROTATE</code>, a, b, c, d, e and f together represent the matrix which will result in the given rotation. When the rotation is around the center point (0, 0), e and f will be zero.</li> </ul>
    */
  SVGMatrix getMatrix();


  /**
    * The type of the value as specified by one of the SVG_TRANSFORM_* constants defined on this interface.
    */
  int getType();


  /**
    * <p>Sets the transform type to <code>SVG_TRANSFORM_ROTATE</code>, with parameter <code>angle</code> defining the rotation angle and parameters <code>cx</code> and <code>cy</code> defining the optional center of rotation.</p> <p><strong>Exceptions:</strong></p> <ul> <li>a <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/DOMException">DOMException</a></code>
 with code <code>NO_MODIFICATION_ALLOWED_ERR</code> is raised when attempting to modify a read only attribute or when the object itself is read only.</li> </ul>
    */
  void setRotate(float angle, float cx, float cy);


  /**
    * <p>Sets the transform type to <code>SVG_TRANSFORM_SCALE</code>, with parameters <code>sx</code> and <code>sy</code> defining the scale amounts.</p> <p><strong>Exceptions:</strong></p> <ul> <li>a <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/DOMException">DOMException</a></code>
 with code <code>NO_MODIFICATION_ALLOWED_ERR</code> is raised when attempting to modify a read only attribute or when the object itself is read only.</li> </ul>
    */
  void setScale(float sx, float sy);


  /**
    * <p>Sets the transform type to <code>SVG_TRANSFORM_SKEWX</code>, with parameter <code>angle</code> defining the amount of skew.</p> <p><strong>Exceptions:</strong></p> <ul> <li>a <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/DOMException">DOMException</a></code>
 with code <code>NO_MODIFICATION_ALLOWED_ERR</code> is raised when attempting to modify a read only attribute or when the object itself is read only.</li> </ul>
    */
  void setSkewX(float angle);


  /**
    * <p>Sets the transform type to <code>SVG_TRANSFORM_SKEWY</code>, with parameter <code>angle</code> defining the amount of skew.</p> <p><strong>Exceptions:</strong></p> <ul> <li>a <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/DOMException">DOMException</a></code>
 with code <code>NO_MODIFICATION_ALLOWED_ERR</code> is raised when attempting to modify a read only attribute or when the object itself is read only.</li> </ul>
    */
  void setSkewY(float angle);


  /**
    * <p>Sets the transform type to <code>SVG_TRANSFORM_TRANSLATE</code>, with parameters <code>tx</code> and <code>ty</code> defining the translation amounts.</p> <p><strong>Exceptions:</strong></p> <ul> <li>a <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/DOMException">DOMException</a></code>
 with code <code>NO_MODIFICATION_ALLOWED_ERR</code> is raised when attempting to modify a read only attribute or when the object itself is read only.</li> </ul>
    */
  void setTranslate(float tx, float ty);
}
