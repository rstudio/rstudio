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
import elemental.html.MediaController;
import elemental.js.events.JsEvent;
import elemental.html.TimeRanges;
import elemental.events.EventListener;
import elemental.js.events.JsEventListener;
import elemental.events.Event;

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

public class JsMediaController extends JsElementalMixinBase  implements MediaController {
  protected JsMediaController() {}

  public final native JsTimeRanges getBuffered() /*-{
    return this.buffered;
  }-*/;

  public final native double getCurrentTime() /*-{
    return this.currentTime;
  }-*/;

  public final native void setCurrentTime(double param_currentTime) /*-{
    this.currentTime = param_currentTime;
  }-*/;

  public final native double getDefaultPlaybackRate() /*-{
    return this.defaultPlaybackRate;
  }-*/;

  public final native void setDefaultPlaybackRate(double param_defaultPlaybackRate) /*-{
    this.defaultPlaybackRate = param_defaultPlaybackRate;
  }-*/;

  public final native double getDuration() /*-{
    return this.duration;
  }-*/;

  public final native boolean isMuted() /*-{
    return this.muted;
  }-*/;

  public final native void setMuted(boolean param_muted) /*-{
    this.muted = param_muted;
  }-*/;

  public final native boolean isPaused() /*-{
    return this.paused;
  }-*/;

  public final native double getPlaybackRate() /*-{
    return this.playbackRate;
  }-*/;

  public final native void setPlaybackRate(double param_playbackRate) /*-{
    this.playbackRate = param_playbackRate;
  }-*/;

  public final native JsTimeRanges getPlayed() /*-{
    return this.played;
  }-*/;

  public final native JsTimeRanges getSeekable() /*-{
    return this.seekable;
  }-*/;

  public final native double getVolume() /*-{
    return this.volume;
  }-*/;

  public final native void setVolume(double param_volume) /*-{
    this.volume = param_volume;
  }-*/;

  public final native void pause() /*-{
    this.pause();
  }-*/;

  public final native void play() /*-{
    this.play();
  }-*/;
}
