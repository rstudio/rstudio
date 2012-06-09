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
import elemental.html.AudioNode;
import elemental.html.BiquadFilterNode;

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

public class JsBiquadFilterNode extends JsAudioNode  implements BiquadFilterNode {
  protected JsBiquadFilterNode() {}

  public final native JsAudioParam getQ() /*-{
    return this.Q;
  }-*/;

  public final native JsAudioParam getFrequency() /*-{
    return this.frequency;
  }-*/;

  public final native JsAudioParam getGain() /*-{
    return this.gain;
  }-*/;

  public final native int getType() /*-{
    return this.type;
  }-*/;

  public final native void setType(int param_type) /*-{
    this.type = param_type;
  }-*/;

  public final native void getFrequencyResponse(Float32Array frequencyHz, Float32Array magResponse, Float32Array phaseResponse) /*-{
    this.getFrequencyResponse(frequencyHz, magResponse, phaseResponse);
  }-*/;
}
