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
package elemental.js.css;
import elemental.css.Counter;
import elemental.css.RGBColor;
import elemental.css.CSSValue;
import elemental.css.CSSPrimitiveValue;
import elemental.css.Rect;

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

public class JsCSSPrimitiveValue extends JsCSSValue  implements CSSPrimitiveValue {
  protected JsCSSPrimitiveValue() {}

  public final native int getPrimitiveType() /*-{
    return this.primitiveType;
  }-*/;

  public final native JsCounter getCounterValue() /*-{
    return this.getCounterValue();
  }-*/;

  public final native float getFloatValue(int unitType) /*-{
    return this.getFloatValue(unitType);
  }-*/;

  public final native JsRGBColor getRGBColorValue() /*-{
    return this.getRGBColorValue();
  }-*/;

  public final native JsRect getRectValue() /*-{
    return this.getRectValue();
  }-*/;

  public final native String getStringValue() /*-{
    return this.getStringValue();
  }-*/;

  public final native void setFloatValue(int unitType, float floatValue) /*-{
    this.setFloatValue(unitType, floatValue);
  }-*/;

  public final native void setStringValue(int stringType, String stringValue) /*-{
    this.setStringValue(stringType, stringValue);
  }-*/;
}
