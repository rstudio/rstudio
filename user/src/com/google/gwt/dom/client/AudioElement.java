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
package com.google.gwt.dom.client;

/**
 * Audio element.
 * 
 * <p>
 * <span style="color:red">Experimental API: This API is still under development
 * and is subject to change.
 * </span>
 * </p>
 * 
 * @see <a href="http://www.w3.org/TR/html5/video.html#audio">W3C HTML 5 Specification</a>
 */
@TagName(AudioElement.TAG)
public class AudioElement extends MediaElement {

  /**
   * The tag for this element.
   */
  public static final String TAG = "audio";

  /**
   * The audio type of MP3 encoded audio.
   */
  public static final String TYPE_MP3 = "audio/mpeg";

  /**
   * The audio type of Ogg encoded audio.
   */
  public static final String TYPE_OGG = "audio/ogg";

  /**
   * The audio type of WAV encoded audio.
   */
  public static final String TYPE_WAV = "audio/wav";

  protected AudioElement() {
  }
}
