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
import elemental.html.Float32Array;
import elemental.html.AudioNode;
import elemental.html.RealtimeAnalyserNode;
import elemental.html.Uint8Array;

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

public class JsRealtimeAnalyserNode extends JsAudioNode  implements RealtimeAnalyserNode {
  protected JsRealtimeAnalyserNode() {}

  public final native int getFftSize() /*-{
    return this.fftSize;
  }-*/;

  public final native void setFftSize(int param_fftSize) /*-{
    this.fftSize = param_fftSize;
  }-*/;

  public final native int getFrequencyBinCount() /*-{
    return this.frequencyBinCount;
  }-*/;

  public final native float getMaxDecibels() /*-{
    return this.maxDecibels;
  }-*/;

  public final native void setMaxDecibels(float param_maxDecibels) /*-{
    this.maxDecibels = param_maxDecibels;
  }-*/;

  public final native float getMinDecibels() /*-{
    return this.minDecibels;
  }-*/;

  public final native void setMinDecibels(float param_minDecibels) /*-{
    this.minDecibels = param_minDecibels;
  }-*/;

  public final native float getSmoothingTimeConstant() /*-{
    return this.smoothingTimeConstant;
  }-*/;

  public final native void setSmoothingTimeConstant(float param_smoothingTimeConstant) /*-{
    this.smoothingTimeConstant = param_smoothingTimeConstant;
  }-*/;

  public final native void getByteFrequencyData(Uint8Array array) /*-{
    this.getByteFrequencyData(array);
  }-*/;

  public final native void getByteTimeDomainData(Uint8Array array) /*-{
    this.getByteTimeDomainData(array);
  }-*/;

  public final native void getFloatFrequencyData(Float32Array array) /*-{
    this.getFloatFrequencyData(array);
  }-*/;
}
