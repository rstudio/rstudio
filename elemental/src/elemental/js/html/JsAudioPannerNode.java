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
import elemental.html.AudioNode;
import elemental.html.AudioPannerNode;
import elemental.html.AudioGain;

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

public class JsAudioPannerNode extends JsAudioNode  implements AudioPannerNode {
  protected JsAudioPannerNode() {}

  public final native JsAudioGain getConeGain() /*-{
    return this.coneGain;
  }-*/;

  public final native float getConeInnerAngle() /*-{
    return this.coneInnerAngle;
  }-*/;

  public final native void setConeInnerAngle(float param_coneInnerAngle) /*-{
    this.coneInnerAngle = param_coneInnerAngle;
  }-*/;

  public final native float getConeOuterAngle() /*-{
    return this.coneOuterAngle;
  }-*/;

  public final native void setConeOuterAngle(float param_coneOuterAngle) /*-{
    this.coneOuterAngle = param_coneOuterAngle;
  }-*/;

  public final native float getConeOuterGain() /*-{
    return this.coneOuterGain;
  }-*/;

  public final native void setConeOuterGain(float param_coneOuterGain) /*-{
    this.coneOuterGain = param_coneOuterGain;
  }-*/;

  public final native JsAudioGain getDistanceGain() /*-{
    return this.distanceGain;
  }-*/;

  public final native int getDistanceModel() /*-{
    return this.distanceModel;
  }-*/;

  public final native void setDistanceModel(int param_distanceModel) /*-{
    this.distanceModel = param_distanceModel;
  }-*/;

  public final native float getMaxDistance() /*-{
    return this.maxDistance;
  }-*/;

  public final native void setMaxDistance(float param_maxDistance) /*-{
    this.maxDistance = param_maxDistance;
  }-*/;

  public final native int getPanningModel() /*-{
    return this.panningModel;
  }-*/;

  public final native void setPanningModel(int param_panningModel) /*-{
    this.panningModel = param_panningModel;
  }-*/;

  public final native float getRefDistance() /*-{
    return this.refDistance;
  }-*/;

  public final native void setRefDistance(float param_refDistance) /*-{
    this.refDistance = param_refDistance;
  }-*/;

  public final native float getRolloffFactor() /*-{
    return this.rolloffFactor;
  }-*/;

  public final native void setRolloffFactor(float param_rolloffFactor) /*-{
    this.rolloffFactor = param_rolloffFactor;
  }-*/;

  public final native void setOrientation(float x, float y, float z) /*-{
    this.setOrientation(x, y, z);
  }-*/;

  public final native void setPosition(float x, float y, float z) /*-{
    this.setPosition(x, y, z);
  }-*/;

  public final native void setVelocity(float x, float y, float z) /*-{
    this.setVelocity(x, y, z);
  }-*/;
}
