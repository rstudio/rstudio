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
  * <p>The <code>SVGLength</code> interface correspond to the <a title="https://developer.mozilla.org/en/SVG/Content_type#Length" rel="internal" href="https://developer.mozilla.org/en/SVG/Content_type#Length">&lt;length&gt;</a> basic data type.</p>
<p>An <code>SVGLength</code> object can be designated as read only, which means that attempts to modify the object will result in an exception being thrown.</p>
  */
public interface SVGLength {

  /**
    * A value was specified using the cm units defined in CSS2.
    */

    static final int SVG_LENGTHTYPE_CM = 6;

  /**
    * A value was specified using the em units defined in CSS2.
    */

    static final int SVG_LENGTHTYPE_EMS = 3;

  /**
    * A value was specified using the ex units defined in CSS2.
    */

    static final int SVG_LENGTHTYPE_EXS = 4;

  /**
    * A value was specified using the in units defined in CSS2.
    */

    static final int SVG_LENGTHTYPE_IN = 8;

  /**
    * A value was specified using the mm units defined in CSS2.
    */

    static final int SVG_LENGTHTYPE_MM = 7;

  /**
    * No unit type was provided (i.e., a unitless value was specified), which indicates a value in user units.
    */

    static final int SVG_LENGTHTYPE_NUMBER = 1;

  /**
    * A value was specified using the pc units defined in CSS2.
    */

    static final int SVG_LENGTHTYPE_PC = 10;

  /**
    * A percentage value was specified.
    */

    static final int SVG_LENGTHTYPE_PERCENTAGE = 2;

  /**
    * A value was specified using the pt units defined in CSS2.
    */

    static final int SVG_LENGTHTYPE_PT = 9;

  /**
    * A value was specified using the px units defined in CSS2.
    */

    static final int SVG_LENGTHTYPE_PX = 5;

  /**
    * The unit type is not one of predefined unit types. It is invalid to attempt to define a new value of this type or to attempt to switch an existing value to this type.
    */

    static final int SVG_LENGTHTYPE_UNKNOWN = 0;


  /**
    * The type of the value as specified by one of the SVG_LENGTHTYPE_* constants defined on this interface.
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
 with code <code>SYNTAX_ERR</code> is raised if the assigned string cannot be parsed as a valid <a title="https://developer.mozilla.org/en/SVG/Content_type#Length" rel="internal" href="https://developer.mozilla.org/en/SVG/Content_type#Length">&lt;length&gt;</a>.</li> <li>a <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/DOMException">DOMException</a></code>
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
    * Preserve the same underlying stored value, but reset the stored unit identifier to the given <code><em>unitType</em></code>. Object attributes <code>unitType</code>, <code>valueInSpecifiedUnits</code> and <code>valueAsString</code> might be modified as a result of this method. For example, if the original value were "<em>0.5cm</em>" and the method was invoked to convert to millimeters, then the <code>unitType</code> would be changed to <code>SVG_LENGTHTYPE_MM</code>, <code>valueInSpecifiedUnits</code> would be changed to the numeric value 5 and <code>valueAsString</code> would be changed to "<em>5mm</em>".
    */
  void convertToSpecifiedUnits(int unitType);


  /**
    * <p>Reset the value as a number with an associated unitType, thereby replacing the values for all of the attributes on the object.</p> <p><strong>Exceptions:</strong></p> <ul> <li>a <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/DOMException">DOMException</a></code>
 with code <code>NOT_SUPPORTED_ERR</code> is raised if <code>unitType</code> is <code>SVG_LENGTHTYPE_UNKNOWN</code> or not a valid unit type constant (one of the other <code>SVG_LENGTHTYPE_*</code> constants defined on this interface).</li> <li>a <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/DOMException">DOMException</a></code>
 with code <code>NO_MODIFICATION_ALLOWED_ERR</code> is raised when the length corresponds to a read only attribute or when the object itself is read only.</li> </ul>
    */
  void newValueSpecifiedUnits(int unitType, float valueInSpecifiedUnits);
}
