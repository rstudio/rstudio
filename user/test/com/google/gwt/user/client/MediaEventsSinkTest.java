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

package com.google.gwt.user.client;

import com.google.gwt.event.dom.client.CanPlayThroughEvent;
import com.google.gwt.event.dom.client.CanPlayThroughHandler;
import com.google.gwt.event.dom.client.EndedEvent;
import com.google.gwt.event.dom.client.EndedHandler;
import com.google.gwt.event.dom.client.HasAllMediaHandlers;
import com.google.gwt.event.dom.client.ProgressEvent;
import com.google.gwt.event.dom.client.ProgressHandler;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.media.client.Audio;
import com.google.gwt.media.client.Video;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Test Case for sinking of media events.
 */
public class MediaEventsSinkTest extends GWTTestCase {

  private static class CanPlayThroughHandlerImpl extends HandlerImpl implements
      CanPlayThroughHandler {
    @Override
    public void onCanPlayThrough(CanPlayThroughEvent event) {
      eventFired();
    }
  }

  private static class EndedHandlerImpl extends HandlerImpl implements EndedHandler {
    @Override
    public void onEnded(EndedEvent event) {
      eventFired();
    }
  }

  private static class HandlerImpl {
    private boolean fired = false;

    public void eventFired() {
      fired = true;
    }

    boolean hasEventFired() {
      return fired;
    }
  }

  private static class ProgressHandlerImpl extends HandlerImpl implements ProgressHandler {
    @Override
    public void onProgress(ProgressEvent event) {
      eventFired();
    }
  }

  /**
   * Interface to create a widget.
   * 
   * @param <W> the widget type
   */
  private interface WidgetCreator<W extends Widget & HasAllMediaHandlers> {
    /**
     * Create a widget to test.
     * 
     * @return the new widget
     */
    W createWidget();
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  public void testAudioEventSink() {
    // skip tests on browsers that do not support audio
    if (!Audio.isSupported()) {
      return;
    }

    verifyMediaEventSink(new WidgetCreator<Audio>() {
      @Override
      public Audio createWidget() {
        return Audio.createIfSupported();
      }
    });
  }

  public void testEventBitsUnmapped() throws Exception {
    ProgressEvent.getType();
    assertEquals(-1, Event.getTypeInt("progress"));
    assertEquals(-1, Event.getTypeInt("ended"));
    assertEquals(-1, Event.getTypeInt("canplaythrough"));
  }

  public void testVideoEventSink() {
    // skip tests on browsers that do not support video
    if (!Video.isSupported()) {
      return;
    }

    verifyMediaEventSink(new WidgetCreator<Video>() {
      @Override
      public Video createWidget() {
        return Video.createIfSupported();
      }
    });
  }

  @Override
  protected void gwtTearDown() throws Exception {
    // clean up after ourselves
    RootPanel.get().clear();
    super.gwtTearDown();
  }

  private <W extends Widget & HasAllMediaHandlers> void verifyCanPlayThroughEventSink(W w) {
    CanPlayThroughHandlerImpl handler = new CanPlayThroughHandlerImpl();
    w.addCanPlayThroughHandler(handler);

    assertFalse(handler.hasEventFired());
    w.fireEvent(new CanPlayThroughEvent() {
    });
    assertTrue(handler.hasEventFired());
  }

  private <W extends Widget & HasAllMediaHandlers> void verifyEndedEventSink(W w) {
    EndedHandlerImpl handler = new EndedHandlerImpl();
    w.addEndedHandler(handler);

    assertFalse(handler.hasEventFired());
    w.fireEvent(new EndedEvent() {
    });
    assertTrue(handler.hasEventFired());
  }

  private void verifyMediaEventSink(WidgetCreator<?>... creators) {
    for (WidgetCreator<?> creator : creators) {
      verifyProgressEventSink(creator.createWidget());
      verifyCanPlayThroughEventSink(creator.createWidget());
      verifyEndedEventSink(creator.createWidget());
    }
  }

  private <W extends Widget & HasAllMediaHandlers> void verifyProgressEventSink(W w) {
    ProgressHandlerImpl handler = new ProgressHandlerImpl();
    w.addProgressHandler(handler);

    assertFalse(handler.hasEventFired());
    w.fireEvent(new ProgressEvent() {
    });
    assertTrue(handler.hasEventFired());
  }
}