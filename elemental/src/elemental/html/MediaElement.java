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
package elemental.html;
import elemental.dom.Element;
import elemental.events.EventListener;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * HTML media elements (such as <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/audio">&lt;audio&gt;</a></code>
 and <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/video">&lt;video&gt;</a></code>
) expose the <code>HTMLMediaElement</code> interface which provides special properties and methods (beyond the regular <a href="https://developer.mozilla.org/en/DOM/element" rel="internal">element</a> object interface they also have available to them by inheritance) for manipulating the layout and presentation of media elements.
  */
public interface MediaElement extends Element {

    static final int EOS_DECODE_ERR = 2;

    static final int EOS_NETWORK_ERR = 1;

    static final int EOS_NO_ERROR = 0;

  /**
    * Data is available for the current playback position, but not enough to actually play more than one frame.
    */

    static final int HAVE_CURRENT_DATA = 2;

  /**
    * Enough data is available—and the download rate is high enough—that the media can be played through to the end without interruption.
    */

    static final int HAVE_ENOUGH_DATA = 4;

  /**
    * Data for the current playback position as well as for at least a little bit of time into the future is available (in other words, at least two frames of video, for example).
    */

    static final int HAVE_FUTURE_DATA = 3;

  /**
    * Enough of the media resource has been retrieved that the metadata attributes are initialized.&nbsp; Seeking will no longer raise an exception.
    */

    static final int HAVE_METADATA = 1;

  /**
    * No information is available about the media resource.
    */

    static final int HAVE_NOTHING = 0;

    static final int NETWORK_EMPTY = 0;

    static final int NETWORK_IDLE = 1;

    static final int NETWORK_LOADING = 2;

    static final int NETWORK_NO_SOURCE = 3;

    static final int SOURCE_CLOSED = 0;

    static final int SOURCE_ENDED = 2;

    static final int SOURCE_OPEN = 1;


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/video#attr-autoplay">autoplay</a></code>
 HTML&nbsp;attribute, indicating whether to begin playing as soon as enough media is available.
    */
  boolean isAutoplay();

  void setAutoplay(boolean arg);


  /**
    * The ranges of the media source that the browser has buffered, if any.
    */
  TimeRanges getBuffered();

  MediaController getController();

  void setController(MediaController arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/video#attr-controls">controls</a></code>
 HTML attribute, indicating whether user interface items for controlling the resource should be displayed.
    */
  boolean isControls();

  void setControls(boolean arg);


  /**
    * The absolute URL of the chosen media resource (if, for example, the server selects a media file based on the resolution of the user's display), or an empty string if the <code>networkState</code> is <code>EMPTY</code>.
    */
  String getCurrentSrc();


  /**
    * The current playback time, in seconds.&nbsp; Setting this value seeks the media to the new time.
    */
  float getCurrentTime();

  void setCurrentTime(float arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/video#attr-muted">muted</a></code>
 HTML attribute, indicating whether the media element's audio output should be muted by default. Changing the value dynamically will not unmute the audio (it only controls the default state).
    */
  boolean isDefaultMuted();

  void setDefaultMuted(boolean arg);


  /**
    * The default playback rate for the media.&nbsp; The Ogg backend does not support this.&nbsp; 1.0 is "normal speed," values lower than 1.0 make the media play slower than normal, higher values make it play faster.&nbsp; The value 0.0 is invalid and throws a <code>NOT_SUPPORTED_ERR</code>&nbsp;exception.
    */
  float getDefaultPlaybackRate();

  void setDefaultPlaybackRate(float arg);


  /**
    * The length of the media in seconds, or zero if no media data is available.&nbsp; If the media data is available but the length is unknown, this value is <code>NaN</code>.&nbsp; If the media is streamed and has no predefined length, the value is <code>Inf</code>.
    */
  float getDuration();


  /**
    * Indicates whether the media element has ended playback.
    */
  boolean isEnded();


  /**
    * The media error object for the most recent error, or null if there has not been an error.
    */
  MediaError getError();

  double getInitialTime();


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/video#attr-loop">loop</a></code>
 HTML&nbsp;attribute, indicating whether the media element should start over when it reaches the end.
    */
  boolean isLoop();

  void setLoop(boolean arg);

  String getMediaGroup();

  void setMediaGroup(String arg);


  /**
    * <code>true</code> if the audio is muted, and <code>false</code> otherwise.
    */
  boolean isMuted();

  void setMuted(boolean arg);


  /**
    * <p>The current state of fetching the media over the network.</p> <table class="standard-table"> <tbody> <tr> <td class="header">Constant</td> <td class="header">Value</td> <td class="header">Description</td> </tr> <tr> <td><code>EMPTY</code></td> <td>0</td> <td>There is no data yet.&nbsp; The <code>readyState</code> is also <code>HAVE_NOTHING</code>.</td> </tr> <tr> <td><code>LOADING</code></td> <td>1</td> <td>The media is loading.</td> </tr> <tr> <td><code>LOADED_METADATA</code></td> <td>2</td> <td>The media's metadata has been loaded.</td> </tr> <tr> <td><code>LOADED_FIRST_FRAME</code></td> <td>3</td> <td>The media's first frame has been loaded.</td> </tr> <tr> <td><code>LOADED</code></td> <td>4</td> <td>The media has been fully loaded.</td> </tr> </tbody> </table>
    */
  int getNetworkState();

  EventListener getOnwebkitkeyadded();

  void setOnwebkitkeyadded(EventListener arg);

  EventListener getOnwebkitkeyerror();

  void setOnwebkitkeyerror(EventListener arg);

  EventListener getOnwebkitkeymessage();

  void setOnwebkitkeymessage(EventListener arg);

  EventListener getOnwebkitneedkey();

  void setOnwebkitneedkey(EventListener arg);

  EventListener getOnwebkitsourceclose();

  void setOnwebkitsourceclose(EventListener arg);

  EventListener getOnwebkitsourceended();

  void setOnwebkitsourceended(EventListener arg);

  EventListener getOnwebkitsourceopen();

  void setOnwebkitsourceopen(EventListener arg);


  /**
    * Indicates whether the media element is paused.
    */
  boolean isPaused();


  /**
    * The current rate at which the media is being played back. This is used to implement user controls for fast forward, slow motion, and so forth. The normal playback rate is multiplied by this value to obtain the current rate, so a value of 1.0 indicates normal speed.&nbsp; Not supported by the Ogg backend.
    */
  float getPlaybackRate();

  void setPlaybackRate(float arg);


  /**
    * The ranges of the media source that the browser has played, if any.
    */
  TimeRanges getPlayed();


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/video#attr-preload">preload</a></code>
 HTML attribute, indicating what data should be preloaded at page-load time, if any.
    */
  String getPreload();

  void setPreload(String arg);


  /**
    * <p>The readiness state of the media:</p> <table class="standard-table"> <tbody> <tr> <td class="header">Constant</td> <td class="header">Value</td> <td class="header">Description</td> </tr> <tr> <td><code>HAVE_NOTHING</code></td> <td>0</td> <td>No information is available about the media resource.</td> </tr> <tr> <td><code>HAVE_METADATA</code></td> <td>1</td> <td>Enough of the media resource has been retrieved that the metadata attributes are initialized.&nbsp; Seeking will no longer raise an exception.</td> </tr> <tr> <td><code>HAVE_CURRENT_DATA</code></td> <td>2</td> <td>Data is available for the current playback position, but not enough to actually play more than one frame.</td> </tr> <tr> <td><code>HAVE_FUTURE_DATA</code></td> <td>3</td> <td>Data for the current playback position as well as for at least a little bit of time into the future is available (in other words, at least two frames of video, for example).</td> </tr> <tr> <td><code>HAVE_ENOUGH_DATA</code></td> <td>4</td> <td>Enough data is available—and the download rate is high enough—that the media can be played through to the end without interruption.</td> </tr> </tbody> </table>
    */
  int getReadyState();


  /**
    * The time ranges that the user is able to seek to, if any.
    */
  TimeRanges getSeekable();


  /**
    * Indicates whether the media is in the process of seeking to a new position.
    */
  boolean isSeeking();


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/video#attr-src">src</a></code>
 HTML attribute, containing the URL of a media resource to use.
    */
  String getSrc();

  void setSrc(String arg);


  /**
    * The earliest possible position in the media, in seconds.
    */
  float getStartTime();

  TextTrackList getTextTracks();


  /**
    * The audio volume, from 0.0 (silent) to 1.0 (loudest).
    */
  float getVolume();

  void setVolume(float arg);

  int getWebkitAudioDecodedByteCount();

  boolean isWebkitClosedCaptionsVisible();

  void setWebkitClosedCaptionsVisible(boolean arg);

  boolean isWebkitHasClosedCaptions();

  String getWebkitMediaSourceURL();

  boolean isWebkitPreservesPitch();

  void setWebkitPreservesPitch(boolean arg);

  int getWebkitSourceState();

  int getWebkitVideoDecodedByteCount();

  TextTrack addTextTrack(String kind);

  TextTrack addTextTrack(String kind, String label);

  TextTrack addTextTrack(String kind, String label, String language);


  /**
    * Determines whether the specified media type can be played back.
    */
  String canPlayType(String type, String keySystem);


  /**
    * Begins loading the media content from the server.
    */
  void load();


  /**
    * Pauses the media playback.
    */
  void pause();


  /**
    * Begins playback of the media. If you have changed the <strong>src</strong> attribute of the media element since the page was loaded, you must call load() before play(), otherwise the original media plays again.
    */
  void play();

  void webkitAddKey(String keySystem, Uint8Array key);

  void webkitAddKey(String keySystem, Uint8Array key, Uint8Array initData, String sessionId);

  void webkitCancelKeyRequest(String keySystem, String sessionId);

  void webkitGenerateKeyRequest(String keySystem);

  void webkitGenerateKeyRequest(String keySystem, Uint8Array initData);

  void webkitSourceAbort(String id);

  void webkitSourceAddId(String id, String type);

  void webkitSourceAppend(String id, Uint8Array data);

  TimeRanges webkitSourceBuffered(String id);

  void webkitSourceEndOfStream(int status);

  void webkitSourceRemoveId(String id);
}
