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
package com.google.gwt.sample.mobilewebapp.client.ui;

import com.google.gwt.dom.client.AudioElement;
import com.google.gwt.media.client.Audio;

/**
 * A bundle of sound effects that can be used in the application.
 */
public class SoundEffects {

  private static SoundEffects instance;
  private static boolean isSupported;

  /**
   * Get the singleton instance.
   * 
   * @return the singleton instance
   */
  public static SoundEffects get() {
    if (instance == null) {
      isSupported = Audio.isSupported();
      instance = new SoundEffects();
    }
    return instance;
  }

  private Audio error;

  /**
   * Construct using {@link #get()}.
   */
  private SoundEffects() {
  }

  /**
   * Play an error sound, if audio is supported.
   */
  public void playError() {
    prefetchError();
    playAudio(error);
  }

  /**
   * Prefetch the error sound.
   */
  public void prefetchError() {
    if (isSupported && error == null) {
      error = Audio.createIfSupported();
      error.addSource("audio/error.ogg", AudioElement.TYPE_OGG);
      error.addSource("audio/error.mp3", AudioElement.TYPE_MP3);
      error.addSource("audio/error.wav", AudioElement.TYPE_WAV);
      prefetchAudio(error);
    }
  }

  /**
   * Play an audio.
   * 
   * @param audio the audio to play, or null if not supported
   */
  private void playAudio(Audio audio) {
    if (audio == null) {
      return;
    }
  
    // Pause current progress.
    audio.pause();
  
    // Reset the source.
    // TODO(jlabanca): Is cache-control=private making the source unseekable?
    audio.setSrc(audio.getCurrentSrc());
  
    // Unmute because we muted in createAudioElement.
    audio.play();
  }

  /**
   * Prefetch an audio.
   * 
   * @param audio the audio to prefetch, or null if not supported
   */
  private void prefetchAudio(Audio audio) {
    if (audio != null) {
      audio.load();
    }
  }
}
