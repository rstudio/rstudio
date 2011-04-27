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
import com.google.gwt.dom.client.Document;
import com.google.gwt.media.client.Audio;

import java.util.ArrayList;
import java.util.List;

/**
 * A bundle of sound effects that can be used in the application.
 */
public class SoundEffects {

  /**
   * The source path and type of an audio source file.
   */
  private static class AudioSource {
    private final String source;
    private final AudioType type;

    public AudioSource(String source, AudioType type) {
      this.source = source;
      this.type = type;
    }
  }

  /**
   * The supported audio types.
   */
  private static enum AudioType {
    OGG("audio/ogg"), MP3("audio/mp3"), WAV("audio/wav");

    private final String mimeType;

    private AudioType(String mimeType) {
      this.mimeType = mimeType;
    }

    public String getMimeType() {
      return mimeType;
    }
  }

  private static List<AudioType> typePreference = new ArrayList<AudioType>();

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

      // Detect which audio types we support.
      if (isSupported) {
        AudioElement elem = Document.get().createAudioElement();

        // Prefer "can play probably" to "can play maybe".
        for (AudioType audioType : AudioType.values()) {
          if (AudioElement.CAN_PLAY_PROBABLY.equals(elem.canPlayType(audioType.getMimeType()))) {
            typePreference.add(audioType);
          }
        }

        // Use "can play maybe" if its the only thing available.
        for (AudioType audioType : AudioType.values()) {
          if (AudioElement.CAN_PLAY_MAYBE.equals(elem.canPlayType(audioType.getMimeType()))) {
            typePreference.add(audioType);
          }
        }
      }
    }
    return instance;
  }

  /**
   * Create and return an audio source.
   */
  private static AudioSource audioSource(String source, AudioType type) {
    return new AudioSource(source, type);
  }

  private AudioElement error;

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
      error =
          createAudioElement(audioSource("audio/error.ogg", AudioType.OGG), audioSource(
              "audio/error.mp3", AudioType.MP3), audioSource("audio/error.wav", AudioType.WAV));
    }
  }

  /**
   * Create an {@link Audio} that will play one of the specified source media
   * files. The sources will be tried in the order they are added until a
   * supported format is found.
   * 
   * <p>
   * This method will attempt to prefetch the audio sources by playing the file
   * muted.
   * </p>
   * 
   * @param sources the source files, of which one will be chosen
   * @return a new {@link AudioElement}, or null if not supported
   */
  private AudioElement createAudioElement(AudioSource... sources) {
    if (!isSupported) {
      return null;
    }

    AudioSource bestSource = null;
    for (int i = 0; i < typePreference.size() && bestSource == null; i++) {
      AudioType type = typePreference.get(i);
      for (AudioSource source : sources) {
        if (source.type == type) {
          bestSource = source;
          break;
        }
      }
    }

    // None of the source files are supported.
    if (bestSource == null) {
      return null;
    }

    // Create the audio element.
    AudioElement audio = Document.get().createAudioElement();
    audio.setSrc(bestSource.source);

    // Force the browser to fetch the source files.
    audio.setVolume(0.0);
    audio.play();

    return audio;
  }

  /**
   * Play an audio element.
   * 
   * @param audio the audio element to play, or null if not supported
   */
  private void playAudio(AudioElement audio) {
    if (audio == null) {
      return;
    }

    // Pause current progress.
    audio.pause();

    /*
     * Some browsers throw an error when we try to seek back to time 0, so reset
     * the source instead. The audio file should be loaded from the browser
     * cache.
     */
    audio.setSrc(audio.getSrc());

    // Unmute because we muted in createAudioElement.
    audio.setVolume(1.0);
    audio.play();
  }
}
