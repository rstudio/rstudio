/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.place.shared;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;

import junit.framework.TestCase;

import java.util.logging.Logger;

/**
 * Eponymous test class.
 */
public class PlaceControllerTest extends TestCase {

  private final class Canceler implements
      PlaceChangeRequestEvent.Handler {
    Place calledWith = null;
    String warning = "Stop fool!";

    public void onPlaceChangeRequest(PlaceChangeRequestEvent event) {
      calledWith = event.getNewPlace();
      event.setWarning(warning);
    }
  }

  private static class MyPlace extends Place {
  }

  private class SimpleHandler implements PlaceChangeEvent.Handler {
    MyPlace calledWith = null;

    public void onPlaceChange(PlaceChangeEvent event) {
      calledWith = (MyPlace) event.getNewPlace();
    }
  }

  private Logger deadLogger = new Logger("shut up", null) {
  };

  private EventBus eventBus = new SimpleEventBus();
  private MockPlaceControllerDelegate delegate = new MockPlaceControllerDelegate();
  private PlaceController placeController = new PlaceController(
      eventBus, delegate) {
    @Override
    Logger log() {
      return deadLogger;
    }
  };

  public void testConfirmCancelOnUserNav() {
    SimpleHandler handler = new SimpleHandler();
    eventBus.addHandler(PlaceChangeEvent.TYPE, handler);

    Canceler canceler = new Canceler();
    eventBus.addHandler(PlaceChangeRequestEvent.TYPE, canceler);

    MyPlace place = new MyPlace();

    placeController.goTo(place);
    assertNull(handler.calledWith);
    assertEquals(place, canceler.calledWith);
    assertEquals(canceler.warning, delegate.message);

    delegate.confirm = true;

    placeController.goTo(place);
    assertEquals(place, canceler.calledWith);
  }

  public void testConfirmCancelOnWindowClose() {
    SimpleHandler handler = new SimpleHandler();
    eventBus.addHandler(PlaceChangeEvent.TYPE, handler);

    Canceler canceler = new Canceler();
    eventBus.addHandler(PlaceChangeRequestEvent.TYPE, canceler);

    assertNull(handler.calledWith);
    assertNull(delegate.message);
    delegate.close();
    assertEquals(canceler.warning, delegate.message);
    assertNull(handler.calledWith);
  }

  public void testSimple() {
    SimpleHandler handler = new SimpleHandler();
    eventBus.addHandler(PlaceChangeEvent.TYPE, handler);
    MyPlace place1 = new MyPlace();
    MyPlace place2 = new MyPlace();
    placeController.goTo(place1);
    assertEquals(place1, handler.calledWith);
    placeController.goTo(place2);
    assertEquals(place2, handler.calledWith);
  }
}
