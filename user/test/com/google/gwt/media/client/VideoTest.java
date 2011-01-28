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
import com.google.gwt.dom.client.VideoElement;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Tests {@link VideoElement}.
 * 
 * Because HtmlUnit does not support HTML5, you will need to run these tests
 * manually in order to have them run. To do that, go to "run configurations" or
 * "debug configurations", select the test you would like to run, and put this
 * line in the VM args under the arguments tab: -Dgwt.args="-runStyle Manual:1"
 */
@DoNotRunWith(Platform.HtmlUnitUnknown)
public class VideoTest extends MediaTest {
  Video video;

  final static String posterUrl = "poster.jpg";
  final static String videoUrlH264 = "testh264.mp4";
  final static String videoFormatH264 = "video/mp4; codecs=\"avc1.42E01E, mp4a.40.2\"";
  final static String videoUrlOgg = "testogg.ogv";
  final static String videoFormatOgg = "video/ogg; codecs=\"theora, vorbis\"";

  final static int videoWidth = 480;
  final static int videoHeight = 270;

  @Override
  public MediaElement getElement() {
    if (video == null) {
      return null;
    }
    return video.getVideoElement();
  }

  @Override
  public String getElementState() {
    StringBuilder sb = new StringBuilder();
    VideoElement e = video.getVideoElement();

    sb.append("AudioElement[");
    sb.append("currentSrc=");
    sb.append(e.getCurrentSrc());
    sb.append(",currentTime=");
    sb.append(e.getCurrentTime());
    sb.append(",defaultPlaybackRate=");
    sb.append(e.getDefaultPlaybackRate());
    sb.append(",duration=");
    sb.append(e.getDuration());
    sb.append(",height=");
    sb.append(e.getHeight());
    sb.append(",initialTime=");
    sb.append(e.getInitialTime());
    sb.append(",networkState=");
    sb.append(e.getNetworkState());
    sb.append(",playbackRate=");
    sb.append(e.getPlaybackRate());
    sb.append(",poster=");
    sb.append(e.getPoster());
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
    sb.append(",videoHeight=");
    sb.append(e.getVideoHeight());
    sb.append(",videoWidth=");
    sb.append(e.getVideoWidth());
    sb.append(",volume=");
    sb.append(e.getVolume());
    sb.append(",width=");
    sb.append(e.getWidth());
    sb.append("]");
    return sb.toString();
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.media.MediaTest";
  }

  public void testPoster() {
    if (video == null) {
      return; // don't continue if not supported
    }

    VideoElement element = video.getVideoElement();
    element.setPoster(posterUrl);
    String poster = element.getPoster();
    assertEquals(posterUrl, poster.substring(poster.lastIndexOf('/') + 1));
  }

  public void testSize() {
    if (video == null) {
      return; // don't continue if not supported
    }

    int width = 100;
    int height = 200;
    VideoElement element = video.getVideoElement();
    element.setWidth(width);
    element.setHeight(height);
    assertEquals(width, element.getWidth());
    assertEquals(height, element.getHeight());
  }

  public void testVideoSize() {
    if (video == null) {
      return; // don't continue if not supported
    }

    int waitMillis = 5000;
    delayTestFinish(3 * waitMillis);

    final VideoElement element = video.getVideoElement();
    element.play();

    // wait a little, then make sure it played
    new Timer() {
      @Override
      public void run() {
        assertEquals("Element = " + getElementState() + ", expected width "
            + videoWidth, videoWidth, element.getVideoWidth());
        assertEquals("Element = " + getElementState() + ", expected height "
            + videoHeight, videoHeight, element.getVideoHeight());
        finishTest();
      }
    }.schedule(waitMillis);
  }

  @Override
  protected void gwtSetUp() throws Exception {
    video = Video.createIfSupported();
    
    if (video == null) {
      return; // don't continue if not supported
    }

    VideoElement element = video.getVideoElement();
    if (!element.canPlayType(videoFormatH264).equalsIgnoreCase(
        MediaElement.CANNOT_PLAY)) {
      element.setSrc(videoUrlH264);
    } else if (!element.canPlayType(videoFormatOgg).equalsIgnoreCase(
        MediaElement.CANNOT_PLAY)) {
      element.setSrc(videoUrlOgg);
    } else {
      throw new Exception("Could not find suitable video format");
    }
    RootPanel.get().add(video);
  }

  @Override
  protected void gwtTearDown() throws Exception {
    if (video == null) {
      return; // don't continue if not supported
    }
    
    video.getVideoElement().pause();
    RootPanel.get().remove(video);
  }
}
