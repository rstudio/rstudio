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
import elemental.html.AudioSourceNode;
import elemental.html.AudioBufferSourceNode;
import elemental.html.AudioGain;
import elemental.html.AudioBuffer;

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

public class JsAudioBufferSourceNode extends JsAudioSourceNode  implements AudioBufferSourceNode {
  protected JsAudioBufferSourceNode() {}

  public final native JsAudioBuffer getBuffer() /*-{
    return this.buffer;
  }-*/;

  public final native void setBuffer(AudioBuffer param_buffer) /*-{
    this.buffer = param_buffer;
  }-*/;

  public final native JsAudioGain getGain() /*-{
    return this.gain;
  }-*/;

  public final native boolean isLoop() /*-{
    return this.loop;
  }-*/;

  public final native void setLoop(boolean param_loop) /*-{
    this.loop = param_loop;
  }-*/;

  public final native boolean isLooping() /*-{
    return this.looping;
  }-*/;

  public final native void setLooping(boolean param_looping) /*-{
    this.looping = param_looping;
  }-*/;

  public final native JsAudioParam getPlaybackRate() /*-{
    return this.playbackRate;
  }-*/;

  public final native int getPlaybackState() /*-{
    return this.playbackState;
  }-*/;

  public final native void noteGrainOn(double when, double grainOffset, double grainDuration) /*-{
    this.noteGrainOn(when, grainOffset, grainDuration);
  }-*/;

  public final native void noteOff(double when) /*-{
    this.noteOff(when);
  }-*/;

  public final native void noteOn(double when) /*-{
    this.noteOn(when);
  }-*/;
}
