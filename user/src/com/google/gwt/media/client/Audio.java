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

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.AudioElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.PartialSupport;

/**
 * <p>
 * A widget representing an &lt;audio&gt; element.
 *
 * <p>
 * <span style="color:red">Experimental API: This API is still under development
 * and is subject to change. </span>
 * </p>
 *
 * This widget may not be supported on all browsers.
 */
@PartialSupport
public class Audio extends MediaBase {
  /**
   * Detector for permutations that might support {@link AudioElement}.
   */
  @SuppressWarnings("unused")
  private static class AudioElementSupportDetectedMaybe
      extends AudioElementSupportDetector {
    /**
     * Using a compile-time check, return true if {@link AudioElement} might be
     * supported.
     *
     * @return true if might be supported, false otherwise.
     */
    @Override
    boolean isSupportedCompileTime() {
      return true;
    }
  }

  /**
   * Detector for permutations that do not support {@link AudioElement}.
   */
  @SuppressWarnings("unused")
  private static class AudioElementSupportDetectedNo
      extends AudioElementSupportDetector {
    /**
     * Using a compile-time check, return true if {@link AudioElement} might be
     * supported.
     *
     * @return true if might be supported, false otherwise.
     */
    @Override
    boolean isSupportedCompileTime() {
      return false;
    }
  }

  /**
   * Detector for browser support of {@link AudioElement}.
   */
  private static class AudioElementSupportDetector {
    /**
     * Using a run-time check, return true if the {@link AudioElement} is
     * supported.
     *
     * @return true if supported, false otherwise.
     */
    static native boolean isSupportedRunTime(AudioElement element) /*-{
      return !!element.canPlayType;
    }-*/;

    /**
     * Using a compile-time check, return true if {@link AudioElement} might be
     * supported.
     *
     * @return true if might be supported, false otherwise.
     */
    boolean isSupportedCompileTime() {
      // will be true in AudioElementSupportDetectedMaybe
      // will be false in AudioElementSupportDetectedNo
      return false;
    }
  }

  private static AudioElementSupportDetector detector;

  /**
   * Return a new {@link Audio} if supported, and null otherwise.
   *
   * @return a new {@link Audio} if supported, and null otherwise
   */
  public static Audio createIfSupported() {
    if (detector == null) {
      detector = GWT.create(AudioElementSupportDetector.class);
    }
    if (!detector.isSupportedCompileTime()) {
      return null;
    }
    AudioElement element = Document.get().createAudioElement();
    if (!detector.isSupportedRunTime(element)) {
      return null;
    }
    return new Audio(element);
  }

  /**
   * Runtime check for whether the audio element is supported in this browser.
   *
   * @return whether the audio element is supported
   */
  public static boolean isSupported() {
    if (detector == null) {
      detector = GWT.create(AudioElementSupportDetector.class);
    }
    if (!detector.isSupportedCompileTime()) {
      return false;
    }
    AudioElement element = Document.get().createAudioElement();
    if (!detector.isSupportedRunTime(element)) {
      return false;
    }
    return true;
  }

  /**
   * Protected constructor. Use {@link #createIfSupported()} to create an Audio.
   */
  protected Audio(AudioElement element) {
    super(element);
  }

  /**
   * Returns the attached AudioElement.
   *
   * @return the AudioElement
   */
  public AudioElement getAudioElement() {
    return getMediaElement().cast();
  }
}
