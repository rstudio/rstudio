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
  * <p>The <code>SVGAngle</code> interface correspond to the <a title="https://developer.mozilla.org/en/SVG/Content_type#Angle" rel="internal" href="https://developer.mozilla.org/en/SVG/Content_type#Angle">&lt;angle&gt;</a> basic data type.</p>
<p>An <code>SVGAngle</code> object can be designated as read only, which means that attempts to modify the object will result in an exception being thrown.</p>
  */
public interface SVGAngle {

  /**
    * The unit type was explicitly set to degrees.
    */

    static final int SVG_ANGLETYPE_DEG = 2;

  /**
    * The unit type is gradians.
    */

    static final int SVG_ANGLETYPE_GRAD = 4;

  /**
    * The unit type is radians.
    */

    static final int SVG_ANGLETYPE_RAD = 3;

  /**
    * The unit type is not one of predefined unit types. It is invalid to attempt to define a new value of this type or to attempt to switch an existing value to this type.
    */

    static final int SVG_ANGLETYPE_UNKNOWN = 0;

  /**
    * No unit type was provided (i.e., a unitless value was specified). For angles, a unitless value is treated the same as if degrees were specified.
    */

    static final int SVG_ANGLETYPE_UNSPECIFIED = 1;


  /**
    * The type of the value as specified by one of the SVG_ANGLETYPE_* constants defined on this interface.
    */
  int getUnitType();


  /**
    * <p>The value as a floating point value, in user units. Setting this attribute will cause <code>valueInSpecifiedUnits</code> and <code>valueAsString</code> to be updated automatically to reflect this setting.</p> <p><strong>Exceptions on setting:</strong> a <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/DOMException">DOMException</a></code>
 with code <code>NO_MODIFICATION_ALLOWED_ERR</code> is raised when the length corresponds to a read only attribute or when the object itself is read only.</p>
    */
  float getValue();

  void setValue(float arg);


  /**
    * <p>The value as a string value, in the units expressed by <code>unitType</code>. Setting this attribute will cause <code>value</code>, <code>valueInSpecifiedUnits</code> and <code>unitType</code> to be updated automatically to reflect this setting.</p> <p><strong>Exceptions on setting:</strong></p> <ul> <li>a <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/DOMException">DOMException</a></code>
 with code <code>SYNTAX_ERR</code> is raised if the assigned string cannot be parsed as a valid <a title="https://developer.mozilla.org/en/SVG/Content_type#Angle" rel="internal" href="https://developer.mozilla.org/en/SVG/Content_type#Angle">&lt;angle&gt;</a>.</li> <li>a <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/DOMException">DOMException</a></code>
 with code <code>NO_MODIFICATION_ALLOWED_ERR</code> is raised when the length corresponds to a read only attribute or when the object itself is read only.</li> </ul>
    */
  String getValueAsString();

  void setValueAsString(String arg);


  /**
    * <p>The value as a floating point value, in the units expressed by <code>unitType</code>. Setting this attribute will cause <code>value</code> and <code>valueAsString</code> to be updated automatically to reflect this setting.</p> <p><strong>Exceptions on setting:</strong> a <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/DOMException">DOMException</a></code>
 with code <code>NO_MODIFICATION_ALLOWED_ERR</code> is raised when the length corresponds to a read only attribute or when the object itself is read only.</p>
    */
  float getValueInSpecifiedUnits();

  void setValueInSpecifiedUnits(float arg);


  /**
    * Preserve the same underlying stored value, but reset the stored unit identifier to the given <code><em>unitType</em></code>. Object attributes <code>unitType</code>, <code>valueInSpecifiedUnits</code> and <code>valueAsString</code> might be modified as a result of this method.
    */
  void convertToSpecifiedUnits(int unitType);


  /**
    * <p>Reset the value as a number with an associated unitType, thereby replacing the values for all of the attributes on the object.</p> <p><strong>Exceptions:</strong></p> <ul> <li>a <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/DOMException">DOMException</a></code>
 with code <code>NOT_SUPPORTED_ERR</code> is raised if <code>unitType</code> is <code>SVG_ANGLETYPE_UNKNOWN</code> or not a valid unit type constant (one of the other <code>SVG_ANGLETYPE_*</code> constants defined on this interface).</li> <li>a <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/DOMException">DOMException</a></code>
 with code <code>NO_MODIFICATION_ALLOWED_ERR</code> is raised when the length corresponds to a read only attribute or when the object itself is read only.</li> </ul>
    */
  void newValueSpecifiedUnits(int unitType, float valueInSpecifiedUnits);
}
