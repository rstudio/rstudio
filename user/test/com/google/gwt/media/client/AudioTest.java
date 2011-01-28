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

import com.google.gwt.dom.client.AudioElement;
import com.google.gwt.dom.client.MediaElement;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Tests {@link AudioElement}.
 *
 * Because HtmlUnit does not support HTML5, you will need to run these tests
 * manually in order to have them run. To do that, go to "run configurations" or
 * "debug configurations", select the test you would like to run, and put this
 * line in the VM args under the arguments tab: -Dgwt.args="-runStyle Manual:1"
 */
@DoNotRunWith(Platform.HtmlUnitUnknown)
public class AudioTest extends MediaTest {
  Audio audio;

  final static String audioUrlMp3 = "jabberwocky.mp3";
  final static String audioFormatMp3 = "audio/mpeg";
  final static String audioUrlOgg = "jabberwocky.ogg";
  final static String audioFormatOgg = "audio/ogg";

  @Override
  public MediaElement getElement() {
    if (audio == null) {
      return null;
    }
    return audio.getAudioElement();
  }

  @Override
  public String getElementState() {
    StringBuilder sb = new StringBuilder();
    AudioElement e = audio.getAudioElement();
    sb.append("AudioElement[");
    sb.append("currentSrc=");
    sb.append(e.getCurrentSrc());
    sb.append(",currentTime=");
    sb.append(e.getCurrentTime());
    sb.append(",defaultPlaybackRate=");
    sb.append(e.getDefaultPlaybackRate());
    sb.append(",duration=");
    sb.append(e.getDuration());
    sb.append(",initialTime=");
    sb.append(e.getInitialTime());
    sb.append(",networkState=");
    sb.append(e.getNetworkState());
    sb.append(",playbackRate=");
    sb.append(e.getPlaybackRate());
    sb.append(",preload=");
    sb.append(e.getPreload());
    sb.append(",readyState=");
    sb.append(e.getReadyState());
    sb.append(",src=");
    sb.append(e.getSrc());
    sb.append(",startOffsetTime=");
    sb.append(e.getStartOffsetTime());
    sb.append(",seekable=");
    sb.append(e.getSeekable());
    sb.append(",volume=");
    sb.append(e.getVolume());
    sb.append("]");
    return sb.toString();
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.media.MediaTest";
  }

  @Override
  protected void gwtSetUp() throws Exception {
    audio = Audio.createIfSupported();
    
    if (audio == null) {
      return; // don't continue if not supported
    }

    AudioElement element = audio.getAudioElement();
    String canPlayMp3 = element.canPlayType(audioFormatMp3);
    String canPlayOgg = element.canPlayType(audioFormatOgg);
    if (!canPlayMp3.equalsIgnoreCase(MediaElement.CANNOT_PLAY)) {
      element.setSrc(audioUrlMp3);
    } else if (!canPlayOgg.equalsIgnoreCase(MediaElement.CANNOT_PLAY)) {
      element.setSrc(audioUrlOgg);
    } else {
      throw new Exception("Could not find suitable audio format");
    }
    RootPanel.get().add(audio);
  }

  @Override
  protected void gwtTearDown() throws Exception {
    if (audio == null) {
      return; // don't continue if not supported
    }
    
    audio.getAudioElement().pause();
    RootPanel.get().remove(audio);
  }
}
