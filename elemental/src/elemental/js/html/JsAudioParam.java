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
package elemental.js.html;
import elemental.html.AudioParam;
import elemental.html.Float32Array;

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

public class JsAudioParam extends JsElementalMixinBase  implements AudioParam {
  protected JsAudioParam() {}

  public final native float getDefaultValue() /*-{
    return this.defaultValue;
  }-*/;

  public final native float getMaxValue() /*-{
    return this.maxValue;
  }-*/;

  public final native float getMinValue() /*-{
    return this.minValue;
  }-*/;

  public final native String getName() /*-{
    return this.name;
  }-*/;

  public final native int getUnits() /*-{
    return this.units;
  }-*/;

  public final native float getValue() /*-{
    return this.value;
  }-*/;

  public final native void setValue(float param_value) /*-{
    this.value = param_value;
  }-*/;

  public final native void cancelScheduledValues(float startTime) /*-{
    this.cancelScheduledValues(startTime);
  }-*/;

  public final native void exponentialRampToValueAtTime(float value, float time) /*-{
    this.exponentialRampToValueAtTime(value, time);
  }-*/;

  public final native void linearRampToValueAtTime(float value, float time) /*-{
    this.linearRampToValueAtTime(value, time);
  }-*/;

  public final native void setTargetValueAtTime(float targetValue, float time, float timeConstant) /*-{
    this.setTargetValueAtTime(targetValue, time, timeConstant);
  }-*/;

  public final native void setValueAtTime(float value, float time) /*-{
    this.setValueAtTime(value, time);
  }-*/;

  public final native void setValueCurveAtTime(Float32Array values, float time, float duration) /*-{
    this.setValueCurveAtTime(values, time, duration);
  }-*/;
}
