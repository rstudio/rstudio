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
package elemental.js.svg;
import elemental.svg.SVGAngle;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.js.stylesheets.*;
import elemental.js.events.*;
import elemental.js.util.*;
import elemental.js.dom.*;
import elemental.js.html.*;
import elemental.js.css.*;
import elemental.js.stylesheets.*;

import java.util.Date;

public class JsSVGAngle extends JsElementalMixinBase  implements SVGAngle {
  protected JsSVGAngle() {}

  public final native int getUnitType() /*-{
    return this.unitType;
  }-*/;

  public final native float getValue() /*-{
    return this.value;
  }-*/;

  public final native void setValue(float param_value) /*-{
    this.value = param_value;
  }-*/;

  public final native String getValueAsString() /*-{
    return this.valueAsString;
  }-*/;

  public final native void setValueAsString(String param_valueAsString) /*-{
    this.valueAsString = param_valueAsString;
  }-*/;

  public final native float getValueInSpecifiedUnits() /*-{
    return this.valueInSpecifiedUnits;
  }-*/;

  public final native void setValueInSpecifiedUnits(float param_valueInSpecifiedUnits) /*-{
    this.valueInSpecifiedUnits = param_valueInSpecifiedUnits;
  }-*/;

  public final native void convertToSpecifiedUnits(int unitType) /*-{
    this.convertToSpecifiedUnits(unitType);
  }-*/;

  public final native void newValueSpecifiedUnits(int unitType, float valueInSpecifiedUnits) /*-{
    this.newValueSpecifiedUnits(unitType, valueInSpecifiedUnits);
  }-*/;
}
