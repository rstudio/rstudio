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

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  public void testAudioMediaEventsSinkByAddingHandler() {
    if (!Audio.isSupported()) {
      return;
    }
    verifyProgressEventSinkOnAddHandler(Audio.createIfSupported());
    verifyEndedEventSinkOnAddHandler(Audio.createIfSupported());
    verifyCanPlayThroughEventSinkOnAddHandler(Audio.createIfSupported());
  }

  public void testVideoMediaEventsSinkByAddingHandler() {
    if (!Audio.isSupported()) {
      return;
    }
    verifyProgressEventSinkOnAddHandler(Video.createIfSupported());
    verifyEndedEventSinkOnAddHandler(Video.createIfSupported());
    verifyCanPlayThroughEventSinkOnAddHandler(Video.createIfSupported());
  }

  public void testMediaEventBitFieldsNotTriviallyZero() {
    assertNotSame(0, Event.ONCANPLAYTHROUGH);
    assertNotSame(0, Event.ONPROGRESS);
    assertNotSame(0, Event.ONENDED);
  }

  @Override
  protected void gwtTearDown() throws Exception {
    // clean up after ourselves
    RootPanel.get().clear();
    super.gwtTearDown();
  }

  private <W extends Widget & HasAllMediaHandlers> void assertNotSunkAfterAttach(
      W w, String eventName, boolean isSunk) {
    assertFalse("Event should not be sunk on " + w.getClass().getName()
        + " until a " + eventName + " handler has been added", isSunk);
  }

  private <W extends Widget & HasAllMediaHandlers> void assertSunkAfterAddHandler(
      W w, String eventName, boolean isSunk) {
    assertTrue("Event should have been sunk on " + w.getClass().getName()
        + " once the widget has been attached and a " + eventName
        + " handler has been added", isSunk);
  }

  private boolean isCanPlayThroughEventSunk(Element e) {
    return (DOM.getEventsSunk(e) & Event.ONCANPLAYTHROUGH) != 0;
  }

  private boolean isEndedEventSunk(Element e) {
    return (DOM.getEventsSunk(e) & Event.ONENDED) != 0;
  }

  private boolean isProgressEventSunk(Element e) {
    return (DOM.getEventsSunk(e) & Event.ONPROGRESS) != 0;
  }

  private <W extends Widget & HasAllMediaHandlers> void verifyCanPlayThroughEventSinkOnAddHandler(
      W w) {
    verifyCanPlayThroughEventSinkOnAddHandler(w, w.getElement());
  }

  private <W extends Widget & HasAllMediaHandlers> void verifyCanPlayThroughEventSinkOnAddHandler(
      W w, Element e) {
    RootPanel.get().add(w);

    assertNotSunkAfterAttach(w, CanPlayThroughEvent.getType().getName(),
        isCanPlayThroughEventSunk(e));

    w.addCanPlayThroughHandler(new CanPlayThroughHandler() {
      public void onCanPlayThrough(CanPlayThroughEvent event) {
      }
    });

    assertSunkAfterAddHandler(w, CanPlayThroughEvent.getType().getName(),
        isCanPlayThroughEventSunk(e));
  }

  private <W extends Widget & HasAllMediaHandlers> void verifyEndedEventSinkOnAddHandler(
      W w) {
    verifyEndedEventSinkOnAddHandler(w, w.getElement());
  }

  private <W extends Widget & HasAllMediaHandlers> void verifyEndedEventSinkOnAddHandler(
      W w, Element e) {
    RootPanel.get().add(w);

    assertNotSunkAfterAttach(w, EndedEvent.getType().getName(),
        isEndedEventSunk(e));

    w.addEndedHandler(new EndedHandler() {
      public void onEnded(EndedEvent event) {
      }
    });

    assertSunkAfterAddHandler(w, EndedEvent.getType().getName(),
        isEndedEventSunk(e));
  }

  private <W extends Widget & HasAllMediaHandlers> void verifyProgressEventSinkOnAddHandler(
      W w) {
    verifyProgressEventSinkOnAddHandler(w, w.getElement());
  }

  private <W extends Widget & HasAllMediaHandlers> void verifyProgressEventSinkOnAddHandler(
      W w, Element e) {
    RootPanel.get().add(w);

    assertNotSunkAfterAttach(w, ProgressEvent.getType().getName(),
        isProgressEventSunk(e));

    w.addProgressHandler(new ProgressHandler() {
      public void onProgress(ProgressEvent event) {
      }
    });

    assertSunkAfterAddHandler(w, ProgressEvent.getType().getName(),
        isProgressEventSunk(e));
  }
}