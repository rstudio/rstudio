/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.media.client;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.MediaElement;
import com.google.gwt.dom.client.SourceElement;
import com.google.gwt.event.dom.client.CanPlayThroughEvent;
import com.google.gwt.event.dom.client.CanPlayThroughHandler;
import com.google.gwt.event.dom.client.EndedEvent;
import com.google.gwt.event.dom.client.EndedHandler;
import com.google.gwt.event.dom.client.HasAllMediaHandlers;
import com.google.gwt.event.dom.client.ProgressEvent;
import com.google.gwt.event.dom.client.ProgressHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.media.dom.client.MediaError;
import com.google.gwt.media.dom.client.TimeRanges;
import com.google.gwt.user.client.ui.FocusWidget;

/**
 * <p>
 * A widget representing a media element.
 *
 * <p>
 * <span style="color:red">Experimental API: This API is still under development
 * and is subject to change. </span>
 * </p>
 */
public abstract class MediaBase extends FocusWidget
    implements HasAllMediaHandlers {

  /**
   * Protected constructor.
   */
  protected MediaBase(MediaElement element) {
    setElement(element);
  }

  @Override
  public HandlerRegistration addCanPlayThroughHandler(
      CanPlayThroughHandler handler) {
    return addBitlessDomHandler(handler, CanPlayThroughEvent.getType());
  }

  @Override
  public HandlerRegistration addEndedHandler(EndedHandler handler) {
    return addBitlessDomHandler(handler, EndedEvent.getType());
  }

  @Override
  public HandlerRegistration addProgressHandler(ProgressHandler handler) {
    return addBitlessDomHandler(handler, ProgressEvent.getType());
  }

  /**
   * Add a source element to this media. The browser will request source files
   * from the server until it finds one it can play.
   * 
   * <p>
   * Only use this method if you do not know the type of the source file, as the
   * browser cannot determine the format from the filename and must download
   * each source until a compatible one is found. Instead, you should specify
   * the type for the media using {@link #addSource(String, String)} so the
   * browser can choose a source file without downloading the file.
   * </p>
   * 
   * @param url a String URL
   * @see #addSource(String, String)
   */
  public SourceElement addSource(String url) {
    SourceElement elem = Document.get().createSourceElement();
    elem.setSrc(url);
    getElement().appendChild(elem);
    return elem;
  }

  /**
   * Add a source element to this media, specifying the type (format) of the
   * media. The browser will choose a supported source file and download it.
   * 
   * <p>
   * The type is the format or encoding of the media represented by the source
   * element. For example, the type of an
   * {@link com.google.gwt.dom.client.AudioElement} could be one of
   * {@value com.google.gwt.dom.client.AudioElement#TYPE_OGG},
   * {@link com.google.gwt.dom.client.AudioElement#TYPE_MP3}, or
   * {@link com.google.gwt.dom.client.AudioElement#TYPE_WAV}.
   * </p>
   * 
   * <p>
   * You can also add the codec information to the type, giving the browser even
   * more information about whether or not it can play the file (Example: "
   * <code>audio/ogg; codec=vorbis</code>");
   * </p>
   * 
   * @param url a String URL
   * @param type the type (format) of the media
   * @see #getSrc()
   */
  public SourceElement addSource(String url, String type) {
    SourceElement elem = addSource(url);
    elem.setType(type);
    return elem;
  }
  
  /**
   * Returns {@code true} if the native player is capable of playing content of
   * the given MIME type.
   *
   * @param type a String representing a MIME type
   * @return one of {@link MediaElement#CAN_PLAY_PROBABLY},
   *         {@link MediaElement#CAN_PLAY_MAYBE}, or
   *         {@link MediaElement#CANNOT_PLAY}
   */
  public String canPlayType(String type) {
    return getMediaElement().canPlayType(type);
  }

  /**
   * Returns a {@link TimeRanges} object indicating which portions of the source
   * have been buffered locally.
   *
   * @return a {@link TimeRanges} instance, or {@code null}.
   */
  public TimeRanges getBuffered() {
    return getMediaElement().getBuffered();
  }

  /**
   * Returns the URL of the current media source, or the empty String if no
   * source is set.
   *
   * @return a String URL
   */
  public String getCurrentSrc() {
    return getMediaElement().getCurrentSrc();
  }

  /**
   * Returns the current time within the source media stream.
   *
   * @return the time, in seconds, as a double
   *
   * @see #setCurrentTime(double)
   */
  public double getCurrentTime() {
    return getMediaElement().getCurrentTime();
  }

  /**
   * Returns the default playback rate, where 1.0 corresponds to normal
   * playback. If no rate has been set, 1.0 is returned.
   *
   * @return the current default playback rate, or 1.0 if it has not been set
   *
   * @see #setDefaultPlaybackRate(double)
   */
  public double getDefaultPlaybackRate() {
    return getMediaElement().getDefaultPlaybackRate();
  }

  /**
   * Returns the duration of the source media stream, in seconds. If the
   * duration is unknown, {@link Double#NaN} is returned. For unbounded media
   * streams, {@link Double#POSITIVE_INFINITY} is returned.
   *
   * @return a positive duration in seconds, NaN, or Infinity
   */
  public double getDuration() {
    return getMediaElement().getDuration();
  }

  /**
   * Returns the type of error that has occurred while attempting to load and
   * play the media. If no error has occurred, {@code null} is returned.
   *
   * @return a {@link MediaError} instance, or {@code null}
   */
  public MediaError getError() {
    return getMediaElement().getError();
  }

  /**
   * Returns the time to which the media stream was seeked at the time it was
   * loaded, in seconds, or 0.0 if the position is unknown.
   *
   * @return the initial time, or 0.0 if unknown
   */
  public double getInitialTime() {
    return getMediaElement().getInitialTime();
  }

  /**
   * Returns the attached Media Element.
   *
   * @return the Media Element
   */
  public MediaElement getMediaElement() {
    return this.getElement().cast();
  }

  /**
   * Returns the network state, one of {@link MediaElement#NETWORK_EMPTY},
   * {@link MediaElement#NETWORK_IDLE}, {@link MediaElement#NETWORK_LOADING}, or
   * {@link MediaElement#NETWORK_NO_SOURCE}.
   *
   * @return an integer constant indicating the network state
   *
   * @see MediaElement#NETWORK_EMPTY
   * @see MediaElement#NETWORK_IDLE
   * @see MediaElement#NETWORK_LOADING
   * @see MediaElement#NETWORK_NO_SOURCE
   */
  public int getNetworkState() {
    return getMediaElement().getNetworkState();
  }

  /**
   * Returns the playback rate, where 1.0 corresponds to normal playback. If the
   * rate has not been set, 1.0 is returned.
   *
   * @return the playback rate, if known, otherwise 1.0
   *
   * @see #setPlaybackRate(double)
   */
  public double getPlaybackRate() {
    return getMediaElement().getPlaybackRate();
  }

  /**
   * Returns a {@link TimeRanges} object indicating which portions of the source
   * have been played.
   *
   * @return a {@link TimeRanges} instance, or {@code null}.
   */
  public TimeRanges getPlayed() {
    return getMediaElement().getPlayed();
  }

  /**
   * Returns the preload setting, one of {@link MediaElement#PRELOAD_AUTO},
   * {@link MediaElement#PRELOAD_METADATA}, or
   * {@link MediaElement#PRELOAD_NONE}.
   *
   * @return the preload setting
   *
   * @see #setPreload(String)
   * @see MediaElement#PRELOAD_AUTO
   * @see MediaElement#PRELOAD_METADATA
   * @see MediaElement#PRELOAD_NONE
   */
  public String getPreload() {
    return getMediaElement().getPreload();
  }

  /**
   * Returns the current state of the media with respect to rendering the
   * current playback position, as one of the constants
   * {@link MediaElement#HAVE_CURRENT_DATA},
   * {@link MediaElement#HAVE_ENOUGH_DATA},
   * {@link MediaElement#HAVE_FUTURE_DATA}, {@link MediaElement#HAVE_METADATA},
   * or {@link MediaElement#HAVE_NOTHING} .
   *
   * @return an integer constant indicating the ready state
   *
   * @see MediaElement#HAVE_CURRENT_DATA
   * @see MediaElement#HAVE_ENOUGH_DATA
   * @see MediaElement#HAVE_FUTURE_DATA
   * @see MediaElement#HAVE_METADATA
   * @see MediaElement#HAVE_NOTHING
   */
  public int getReadyState() {
    return getMediaElement().getReadyState();
  }

  /**
   * Returns a {@link TimeRanges} object indicating which portions of the source
   * are seekable.
   *
   * @return a {@link TimeRanges} instance, or {@code null}.
   */
  public TimeRanges getSeekable() {
    return getMediaElement().getSeekable();
  }

  /**
   * Returns the source URL for the media, or {@code null} if none is set.
   *
   * @return a String URL or {@code null}
   *
   * @see #setSrc(String)
   */
  public String getSrc() {
    return getMediaElement().getSrc();
  }

  /**
   * Returns the time corresponding to the zero time in the media timeline,
   * measured in seconds since midnight, January 1 1970 UTC, or
   * {@link Double#NaN} if none is specified.
   *
   * @return the start time
   */
  public double getStartOffsetTime() {
    return getMediaElement().getStartOffsetTime();
  }

  /**
   * Returns the current audio volume setting for the media, as a number between
   * 0.0 and 1.0.
   *
   * @return a number between 0.0 (silent) and 1.0 (loudest)
   *
   * @see #setVolume(double)
   */
  public double getVolume() {
    return getMediaElement().getVolume();
  }

  /**
   * Returns {@code true} if the media player should display interactive
   * controls (for example, to control play/pause, seek position, and volume),
   * {@code false} otherwise.
   *
   * @return whether controls should be displayed
   *
   * @see #setControls(boolean)
   */
  public boolean hasControls() {
    return getMediaElement().hasControls();
  }

  /**
   * Returns {@code true} if playback has reached the end of the media, {@code
   * false} otherwise.
   *
   * @return whether playback has ended
   */
  public boolean hasEnded() {
    return getMediaElement().hasEnded();
  }

  /**
   * Returns {@code true} if autoplay is enabled, {@code false} otherwise. When
   * autoplay is enabled, the user agent will begin playback automatically as
   * soon as it can do so without stopping.
   *
   * @return the autoplay setting
   *
   * @see #setAutoplay(boolean)
   */
  public boolean isAutoplay() {
    return getMediaElement().isAutoplay();
  }

  /**
   * Returns {@code true} if the user agent is to seek back to the start of the
   * media once playing has ended, {@code false} otherwise.
   *
   * @return the loop setting
   *
   * @see #setLoop(boolean)
   */
  public boolean isLoop() {
    return getMediaElement().isLoop();
  }

  /**
   * Returns {@code true} if the volume is to be muted (overriding the normal
   * volume setting), {@code false} otherwise.
   *
   * @return the muting setting
   *
   * @see #setMuted(boolean)
   * @see #getVolume()
   * @see #setVolume(double)
   */
  public boolean isMuted() {
    return getMediaElement().isMuted();
  }

  /**
   * Returns {@code true} if playback is paused, {@code false} otherwise.
   *
   * @return the paused setting
   *
   * @see #pause()
   * @see #play()
   */
  public boolean isPaused() {
    return getMediaElement().isPaused();
  }

  /**
   * Returns {@code true} if the playback position is in the process of changing
   * discontinuously, e.g., by use of the interactive controls, {@code false}
   * otherwise.
   *
   * @return the seeking status
   *
   * @see #setControls(boolean)
   * @see #hasControls()
   */
  public boolean isSeeking() {
    return getMediaElement().isSeeking();
  }

  /**
   * Causes the resource to be loaded.
   */
  public void load() {
    getMediaElement().load();
  }

  /**
   * Causes playback of the resource to be paused.
   */
  public void pause() {
    getMediaElement().pause();
  }

  /**
   * Causes playback of the resource to be started or resumed.
   */
  public void play() {
    getMediaElement().play();
  }

  /**
   * Remove the specified {@link SourceElement} from this media. If the source
   * element is not a child of this widget, it will not be removed.
   * 
   * @param source the source element to remove
   * @see #addSource(String, String)
   */
  public void removeSource(SourceElement source) {
    getElement().removeChild(source);
  }

  /**
   * Enables or disables autoplay of the resource.
   *
   * @param autoplay if {@code true}, enable autoplay
   *
   * @see #isAutoplay()
   */
  public void setAutoplay(boolean autoplay) {
    getMediaElement().setAutoplay(autoplay);
  }

  /**
   * Enables or disables interactive controls.
   *
   * @param controls if {@code true}, enable controls
   *
   * @see #hasControls()
   */
  public void setControls(boolean controls) {
    getMediaElement().setControls(controls);
  }

  /**
   * Sets the current playback time within the media stream, in seconds.
   *
   * @param time a number within the ranges given by {@link #getSeekable()}
   *
   * @see #getCurrentTime()
   */
  public void setCurrentTime(double time) {
    getMediaElement().setCurrentTime(time);
  }

  /**
   * Sets the default playback rate.
   *
   * @param rate a double value
   *
   * @see #getDefaultPlaybackRate()
   */
  public void setDefaultPlaybackRate(double rate) {
    getMediaElement().setDefaultPlaybackRate(rate);
  }

  /**
   * Enables or disables looping.
   *
   * @param loop if {@code true}, enable looping
   *
   * @see #isLoop()
   */
  public final void setLoop(boolean loop) {
    getMediaElement().setLoop(loop);
  }

  /**
   * Enables or disables muting.
   *
   * @param muted if {@code true}, enable muting
   *
   * @see #isMuted()
   */
  public void setMuted(boolean muted) {
    getMediaElement().setMuted(muted);
  }

  /**
   * Sets the playback rate.
   *
   * @param rate a double value
   *
   * @see #getPlaybackRate()
   */
  public void setPlaybackRate(double rate) {
    getMediaElement().setPlaybackRate(rate);
  }

  /**
   * Changes the preload setting to one of {@link MediaElement#PRELOAD_AUTO},
   * {@link MediaElement#PRELOAD_METADATA}, or
   * {@link MediaElement#PRELOAD_NONE}.
   *
   * @param preload a String constants
   *
   * @see #getPreload()
   * @see #setPreload(String)
   * @see MediaElement#PRELOAD_AUTO
   * @see MediaElement#PRELOAD_METADATA
   * @see MediaElement#PRELOAD_NONE
   */
  public void setPreload(String preload) {
    getMediaElement().setPreload(preload);
  }

  /**
   * Sets the source URL for the media.
   * 
   * <p>
   * Support for different media types varies between browsers. Instead of using
   * this method, you should encode your media in multiple formats and add all
   * of them using {@link #addSource(String, String)} so the browser can choose
   * a source that it supports.
   * </p>
   * 
   * @param url a String URL
   * 
   * @see #getSrc()
   * @see #addSource(String, String)
   */
  public void setSrc(String url) {
    getMediaElement().setSrc(url);
  }

  /**
   * Sets the playback volume.
   *
   * @param volume a value between 0.0 (silent) and 1.0 (loudest)
   *
   * @see #getVolume()
   */
  public void setVolume(double volume) {
    getMediaElement().setVolume(volume);
  }
}
