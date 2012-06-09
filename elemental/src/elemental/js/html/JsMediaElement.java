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
import elemental.js.dom.JsElement;
import elemental.dom.Element;
import elemental.html.MediaError;
import elemental.html.TextTrack;
import elemental.html.MediaController;
import elemental.html.TextTrackList;
import elemental.html.TimeRanges;
import elemental.events.EventListener;
import elemental.html.MediaElement;
import elemental.js.events.JsEventListener;
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

public class JsMediaElement extends JsElement  implements MediaElement {
  protected JsMediaElement() {}

  public final native boolean isAutoplay() /*-{
    return this.autoplay;
  }-*/;

  public final native void setAutoplay(boolean param_autoplay) /*-{
    this.autoplay = param_autoplay;
  }-*/;

  public final native JsTimeRanges getBuffered() /*-{
    return this.buffered;
  }-*/;

  public final native JsMediaController getController() /*-{
    return this.controller;
  }-*/;

  public final native void setController(MediaController param_controller) /*-{
    this.controller = param_controller;
  }-*/;

  public final native boolean isControls() /*-{
    return this.controls;
  }-*/;

  public final native void setControls(boolean param_controls) /*-{
    this.controls = param_controls;
  }-*/;

  public final native String getCurrentSrc() /*-{
    return this.currentSrc;
  }-*/;

  public final native float getCurrentTime() /*-{
    return this.currentTime;
  }-*/;

  public final native void setCurrentTime(float param_currentTime) /*-{
    this.currentTime = param_currentTime;
  }-*/;

  public final native boolean isDefaultMuted() /*-{
    return this.defaultMuted;
  }-*/;

  public final native void setDefaultMuted(boolean param_defaultMuted) /*-{
    this.defaultMuted = param_defaultMuted;
  }-*/;

  public final native float getDefaultPlaybackRate() /*-{
    return this.defaultPlaybackRate;
  }-*/;

  public final native void setDefaultPlaybackRate(float param_defaultPlaybackRate) /*-{
    this.defaultPlaybackRate = param_defaultPlaybackRate;
  }-*/;

  public final native float getDuration() /*-{
    return this.duration;
  }-*/;

  public final native boolean isEnded() /*-{
    return this.ended;
  }-*/;

  public final native JsMediaError getError() /*-{
    return this.error;
  }-*/;

  public final native double getInitialTime() /*-{
    return this.initialTime;
  }-*/;

  public final native boolean isLoop() /*-{
    return this.loop;
  }-*/;

  public final native void setLoop(boolean param_loop) /*-{
    this.loop = param_loop;
  }-*/;

  public final native String getMediaGroup() /*-{
    return this.mediaGroup;
  }-*/;

  public final native void setMediaGroup(String param_mediaGroup) /*-{
    this.mediaGroup = param_mediaGroup;
  }-*/;

  public final native boolean isMuted() /*-{
    return this.muted;
  }-*/;

  public final native void setMuted(boolean param_muted) /*-{
    this.muted = param_muted;
  }-*/;

  public final native int getNetworkState() /*-{
    return this.networkState;
  }-*/;

  public final native EventListener getOnwebkitkeyadded() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onwebkitkeyadded);
  }-*/;

  public final native void setOnwebkitkeyadded(EventListener listener) /*-{
    this.onwebkitkeyadded = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnwebkitkeyerror() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onwebkitkeyerror);
  }-*/;

  public final native void setOnwebkitkeyerror(EventListener listener) /*-{
    this.onwebkitkeyerror = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnwebkitkeymessage() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onwebkitkeymessage);
  }-*/;

  public final native void setOnwebkitkeymessage(EventListener listener) /*-{
    this.onwebkitkeymessage = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnwebkitneedkey() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onwebkitneedkey);
  }-*/;

  public final native void setOnwebkitneedkey(EventListener listener) /*-{
    this.onwebkitneedkey = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnwebkitsourceclose() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onwebkitsourceclose);
  }-*/;

  public final native void setOnwebkitsourceclose(EventListener listener) /*-{
    this.onwebkitsourceclose = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnwebkitsourceended() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onwebkitsourceended);
  }-*/;

  public final native void setOnwebkitsourceended(EventListener listener) /*-{
    this.onwebkitsourceended = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnwebkitsourceopen() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onwebkitsourceopen);
  }-*/;

  public final native void setOnwebkitsourceopen(EventListener listener) /*-{
    this.onwebkitsourceopen = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native boolean isPaused() /*-{
    return this.paused;
  }-*/;

  public final native float getPlaybackRate() /*-{
    return this.playbackRate;
  }-*/;

  public final native void setPlaybackRate(float param_playbackRate) /*-{
    this.playbackRate = param_playbackRate;
  }-*/;

  public final native JsTimeRanges getPlayed() /*-{
    return this.played;
  }-*/;

  public final native String getPreload() /*-{
    return this.preload;
  }-*/;

  public final native void setPreload(String param_preload) /*-{
    this.preload = param_preload;
  }-*/;

  public final native int getReadyState() /*-{
    return this.readyState;
  }-*/;

  public final native JsTimeRanges getSeekable() /*-{
    return this.seekable;
  }-*/;

  public final native boolean isSeeking() /*-{
    return this.seeking;
  }-*/;

  public final native String getSrc() /*-{
    return this.src;
  }-*/;

  public final native void setSrc(String param_src) /*-{
    this.src = param_src;
  }-*/;

  public final native float getStartTime() /*-{
    return this.startTime;
  }-*/;

  public final native JsTextTrackList getTextTracks() /*-{
    return this.textTracks;
  }-*/;

  public final native float getVolume() /*-{
    return this.volume;
  }-*/;

  public final native void setVolume(float param_volume) /*-{
    this.volume = param_volume;
  }-*/;

  public final native int getWebkitAudioDecodedByteCount() /*-{
    return this.webkitAudioDecodedByteCount;
  }-*/;

  public final native boolean isWebkitClosedCaptionsVisible() /*-{
    return this.webkitClosedCaptionsVisible;
  }-*/;

  public final native void setWebkitClosedCaptionsVisible(boolean param_webkitClosedCaptionsVisible) /*-{
    this.webkitClosedCaptionsVisible = param_webkitClosedCaptionsVisible;
  }-*/;

  public final native boolean isWebkitHasClosedCaptions() /*-{
    return this.webkitHasClosedCaptions;
  }-*/;

  public final native String getWebkitMediaSourceURL() /*-{
    return this.webkitMediaSourceURL;
  }-*/;

  public final native boolean isWebkitPreservesPitch() /*-{
    return this.webkitPreservesPitch;
  }-*/;

  public final native void setWebkitPreservesPitch(boolean param_webkitPreservesPitch) /*-{
    this.webkitPreservesPitch = param_webkitPreservesPitch;
  }-*/;

  public final native int getWebkitSourceState() /*-{
    return this.webkitSourceState;
  }-*/;

  public final native int getWebkitVideoDecodedByteCount() /*-{
    return this.webkitVideoDecodedByteCount;
  }-*/;

  public final native JsTextTrack addTextTrack(String kind) /*-{
    return this.addTextTrack(kind);
  }-*/;

  public final native JsTextTrack addTextTrack(String kind, String label) /*-{
    return this.addTextTrack(kind, label);
  }-*/;

  public final native JsTextTrack addTextTrack(String kind, String label, String language) /*-{
    return this.addTextTrack(kind, label, language);
  }-*/;

  public final native String canPlayType(String type, String keySystem) /*-{
    return this.canPlayType(type, keySystem);
  }-*/;

  public final native void load() /*-{
    this.load();
  }-*/;

  public final native void pause() /*-{
    this.pause();
  }-*/;

  public final native void play() /*-{
    this.play();
  }-*/;

  public final native void webkitAddKey(String keySystem, Uint8Array key) /*-{
    this.webkitAddKey(keySystem, key);
  }-*/;

  public final native void webkitAddKey(String keySystem, Uint8Array key, Uint8Array initData, String sessionId) /*-{
    this.webkitAddKey(keySystem, key, initData, sessionId);
  }-*/;

  public final native void webkitCancelKeyRequest(String keySystem, String sessionId) /*-{
    this.webkitCancelKeyRequest(keySystem, sessionId);
  }-*/;

  public final native void webkitGenerateKeyRequest(String keySystem) /*-{
    this.webkitGenerateKeyRequest(keySystem);
  }-*/;

  public final native void webkitGenerateKeyRequest(String keySystem, Uint8Array initData) /*-{
    this.webkitGenerateKeyRequest(keySystem, initData);
  }-*/;

  public final native void webkitSourceAbort(String id) /*-{
    this.webkitSourceAbort(id);
  }-*/;

  public final native void webkitSourceAddId(String id, String type) /*-{
    this.webkitSourceAddId(id, type);
  }-*/;

  public final native void webkitSourceAppend(String id, Uint8Array data) /*-{
    this.webkitSourceAppend(id, data);
  }-*/;

  public final native JsTimeRanges webkitSourceBuffered(String id) /*-{
    return this.webkitSourceBuffered(id);
  }-*/;

  public final native void webkitSourceEndOfStream(int status) /*-{
    this.webkitSourceEndOfStream(status);
  }-*/;

  public final native void webkitSourceRemoveId(String id) /*-{
    this.webkitSourceRemoveId(id);
  }-*/;
}
