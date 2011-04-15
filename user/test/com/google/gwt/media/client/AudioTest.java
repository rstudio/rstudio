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

import com.google.gwt.dom.client.MediaElement;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Tests {@link Audio}.
 *
 *  Because HtmlUnit does not support HTML5, you will need to run these tests
 * manually in order to have them run. To do that, go to "run configurations" or
 * "debug configurations", select the test you would like to run, and put this
 * line in the VM args under the arguments tab: -Dgwt.args="-runStyle Manual:1"
 */
@DoNotRunWith(Platform.HtmlUnitUnknown)
public class AudioTest extends MediaTest {
  protected Audio audio;

  final static String audioUrlMp3 = "smallmp3.mp3";
  final static String audioFormatMp3 = "audio/mpeg";
  final static String audioUrlOgg = "smallogg.ogg";
  final static String audioFormatOgg = "audio/ogg";

  @Override
  public MediaBase getMedia() {
    return audio;
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

    String canPlayMp3 = audio.canPlayType(audioFormatMp3);
    String canPlayOgg = audio.canPlayType(audioFormatOgg);
    if (canPlayMp3.equals(MediaElement.CAN_PLAY_PROBABLY)) {
      audio.setSrc(audioUrlMp3);
    } else if (canPlayOgg.equals(MediaElement.CAN_PLAY_PROBABLY)) {
      audio.setSrc(audioUrlOgg);
    } else if (canPlayMp3.equals(MediaElement.CAN_PLAY_MAYBE)) {
      audio.setSrc(audioUrlMp3);
    } else if (canPlayOgg.equals(MediaElement.CAN_PLAY_MAYBE)) {
      audio.setSrc(audioUrlOgg);
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

    // clean up
    audio.pause();
    audio.setSrc("");
    audio.load();
    RootPanel.get().remove(audio);
  }
}
