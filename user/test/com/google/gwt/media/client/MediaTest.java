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
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Timer;

import junit.framework.Assert;

/**
 * Base test for {@link MediaElement}.
 * 
 * Do not call this class directly. To use, extend this class and override the
 * getElement and isSupported methods.
 * 
 * Because HtmlUnit does not support HTML5, you will need to run these tests
 * manually in order to have them run. To do that, go to "run configurations" or
 * "debug configurations", select the test you would like to run, and put this
 * line in the VM args under the arguments tab: -Dgwt.args="-runStyle Manual:1"
 */
@DoNotRunWith(Platform.HtmlUnitUnknown)
public abstract class MediaTest extends GWTTestCase {

  native boolean isOldFirefox() /*-{
    return @com.google.gwt.dom.client.DOMImplMozilla::isGecko191OrBefore()();
  }-*/;

  native boolean isFirefox40OrEarlier() /*-{
    return @com.google.gwt.dom.client.DOMImplMozilla::isGecko2OrBefore()();
  }-*/;

  public void disabled_testCurrentTime() {
    MediaElement element = getElement();
    if (element == null) {
      return; // don't continue if not supported
    }

    Assert.assertTrue("currentTime must be positive.",
        element.getCurrentTime() >= 0.0);

    double seekTime = 2.0; // seconds
    element.setCurrentTime(seekTime);
    Assert.assertEquals("currentTime must be able to be set.", seekTime,
        element.getCurrentTime());
  }

  /**
   * Return the MediaElement associated with the test.
   * 
   * @return the MediaElement associated with the test.
   */
  public MediaElement getElement() {
    return null;
  }

  public abstract String getElementState();

  @Override
  public String getModuleName() {
    return "com.google.gwt.media.MediaTest";
  }

  public void testAutoPlay() {
    MediaElement element = getElement();
    if (element == null) {
      return; // don't continue if not supported
    }

    element.setAutoplay(false);
    assertFalse("Autoplay should be off.", element.isAutoplay());
    element.setAutoplay(true);
    assertTrue("Autoplay should be on.", element.isAutoplay());
  }

  public void testControls() {
    MediaElement element = getElement();
    if (element == null) {
      return; // don't continue if not supported
    }

    element.setControls(false);
    assertFalse("Controls should be off.", element.hasControls());
    element.setControls(true);
    assertTrue("Controls should be on.", element.hasControls());
  }

  public void testCurrentSrc() {
    MediaElement element = getElement();
    if (element == null) {
      return; // don't continue if not supported
    }

    element.load();
    Assert.assertNotNull("currentSrc should be set in these tests.",
        element.getCurrentSrc());
  }

  public void testLoop() {
    MediaElement element = getElement();
    if (element == null) {
      return; // don't continue if not supported
    }

    element.setLoop(false);
    assertFalse("Loop should be off.", element.isLoop());
    element.setLoop(true);
    assertTrue("Loop should be on.", element.isLoop());
  }

  public void testMuted() {
    MediaElement element = getElement();
    if (element == null) {
      return; // don't continue if not supported
    }

    element.setMuted(true);
    assertTrue("Muted should be true.", element.isMuted());
    element.setMuted(false);
    assertFalse("Muted should be false.", element.isMuted());
  }

  public void testNetworkState() {
    MediaElement element = getElement();
    if (element == null) {
      return; // don't continue if not supported
    }
    int state = element.getNetworkState();
    assertTrue("Illegal network state", state == MediaElement.NETWORK_EMPTY
        || state == MediaElement.NETWORK_IDLE
        || state == MediaElement.NETWORK_LOADING
        || state == MediaElement.NETWORK_NO_SOURCE);
  }

  public void testPlay() {
    MediaElement element = getElement();
    if (element == null) {
      return; // don't continue if not supported
    }

    int waitMillis = 10000;
    delayTestFinish(3 * waitMillis);

    element.setPlaybackRate(1.0);
    element.play();

    // wait a little, then make sure it played
    new Timer() {
      @Override
      public void run() {
        finishTest();
      }
    }.schedule(waitMillis);
  }

  public void testPlaybackRate() {
    final MediaElement element = getElement();
    if (element == null) {
      return; // don't continue if not supported
    }

    int waitMillis = 5000;
    delayTestFinish(3 * waitMillis);

    assertEquals("Default playback rate should be 1.0", 1.0,
        element.getDefaultPlaybackRate());

    element.play();

    // wait a little, then make sure it played
    new Timer() {
      @Override
      public void run() {
        // set rate to 2.0
        double rate = 2.0;
        element.setPlaybackRate(rate);
        assertEquals("Should be able to change playback rate", rate,
            element.getPlaybackRate());

        // return to 1.0
        rate = 1.0;
        element.setPlaybackRate(rate);
        assertEquals("Should be able to change playback rate", rate,
            element.getPlaybackRate());

        finishTest();
      }
    }.schedule(waitMillis);
  }

  public void disabled_testPreload() {
    MediaElement element = getElement();
    if (element == null) {
      return; // don't continue if not supported
    }
    if (isFirefox40OrEarlier()) {
      return; // don't continue on older versions of Firefox.
    }
    
    String state = element.getPreload();
    assertNotNull(state);
    assertTrue("Illegal preload state", state.equals(MediaElement.PRELOAD_AUTO)
        || state.equals(MediaElement.PRELOAD_METADATA)
        || state.equals(MediaElement.PRELOAD_NONE));

    element.setPreload(MediaElement.PRELOAD_METADATA);
    assertEquals("Preload should be able to be set.",
        MediaElement.PRELOAD_METADATA, element.getPreload());
  }

  public void testReadyState() {
    MediaElement element = getElement();
    if (element == null) {
      return; // don't continue if not supported
    }

    int state = element.getReadyState();
    assertTrue("Illegal ready state", state == MediaElement.HAVE_CURRENT_DATA
        || state == MediaElement.HAVE_ENOUGH_DATA
        || state == MediaElement.HAVE_FUTURE_DATA
        || state == MediaElement.HAVE_METADATA
        || state == MediaElement.HAVE_NOTHING);
  }

  public void testVolume() {
    MediaElement element = getElement();
    if (element == null) {
      return; // don't continue if not supported
    }

    element.setVolume(0.5);
    assertEquals("Volume should be at one-half loudness.", 0.5,
        element.getVolume());
    element.setVolume(0.75);
    assertEquals("Volume should be at three-quarters loudness.", 0.75,
        element.getVolume());
  }
}
