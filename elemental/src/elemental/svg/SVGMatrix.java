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
  * <p>Many of SVG's graphics operations utilize 2x3 matrices of the form:</p>
<pre>[a c e]
[b d f]</pre>
<p>which, when expanded into a 3x3 matrix for the purposes of matrix arithmetic, become:</p>
<pre>[a c e]
[b d f]
[0 0 1]
</pre>
<p>An <code>SVGMatrix</code> object can be designated as read only, which means that attempts to modify the object will result in an exception being thrown.</p>
  */
public interface SVGMatrix {

  double getA();

  void setA(double arg);

  double getB();

  void setB(double arg);

  double getC();

  void setC(double arg);

  double getD();

  void setD(double arg);

  double getE();

  void setE(double arg);

  double getF();

  void setF(double arg);


  /**
    * Post-multiplies the transformation [-1&nbsp;0&nbsp;0&nbsp;1&nbsp;0&nbsp;0] and returns the resulting matrix.
    */
  SVGMatrix flipX();


  /**
    * Post-multiplies the transformation [1&nbsp;0&nbsp;0&nbsp;-1&nbsp;0&nbsp;0] and returns the resulting matrix.
    */
  SVGMatrix flipY();


  /**
    * <p>Return the inverse matrix</p> <p><strong>Exceptions:</strong></p> <ul> <li>a <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/DOMException">DOMException</a></code>
 with code <code>SVG_MATRIX_NOT_INVERTABLE</code> is raised if the matrix is not invertable.</li> </ul>
    */
  SVGMatrix inverse();


  /**
    * Performs matrix multiplication. This matrix is post-multiplied by another matrix, returning the resulting new matrix.
    */
  SVGMatrix multiply(SVGMatrix secondMatrix);


  /**
    * Post-multiplies a rotation transformation on the current matrix and returns the resulting matrix.
    */
  SVGMatrix rotate(float angle);


  /**
    * <p>Post-multiplies a rotation transformation on the current matrix and returns the resulting matrix. The rotation angle is determined by taking (+/-) atan(y/x). The direction of the vector (x,&nbsp;y) determines whether the positive or negative angle value is used.</p> <p><strong>Exceptions:</strong></p> <ul> <li>a <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/DOMException">DOMException</a></code>
 with code <code>SVG_INVALID_VALUE_ERR</code> is raised if one of the parameters has an invalid value.</li> </ul>
    */
  SVGMatrix rotateFromVector(float x, float y);


  /**
    * Post-multiplies a uniform scale transformation on the current matrix and returns the resulting matrix.
    */
  SVGMatrix scale(float scaleFactor);


  /**
    * Post-multiplies a non-uniform scale transformation on the current matrix and returns the resulting matrix.
    */
  SVGMatrix scaleNonUniform(float scaleFactorX, float scaleFactorY);


  /**
    * Post-multiplies a skewX transformation on the current matrix and returns the resulting matrix.
    */
  SVGMatrix skewX(float angle);


  /**
    * Post-multiplies a skewY transformation on the current matrix and returns the resulting matrix.
    */
  SVGMatrix skewY(float angle);


  /**
    * Post-multiplies a translation transformation on the current matrix and returns the resulting matrix.
    */
  SVGMatrix translate(float x, float y);
}
