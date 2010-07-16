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
package com.google.gwt.app.place;

import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Window.ClosingEvent;
import com.google.gwt.user.client.Window.ClosingHandler;

import junit.framework.TestCase;

/**
 * Eponymous test class.
 */
public class PlaceControllerTest extends TestCase {

  private final class Canceler implements
      PlaceChangeRequestedEvent.Handler<MyPlace> {
    MyPlace calledWith = null;
    String warning = "Stop fool!";

    public void onPlaceChangeRequested(PlaceChangeRequestedEvent<MyPlace> event) {
      calledWith = event.getNewPlace();
      event.setWarning(warning);
    }
  }

  private static class Delegate implements PlaceController.Delegate {
    String message = null;
    boolean confirm = false;
    ClosingHandler handler = null;

    public HandlerRegistration addWindowClosingHandler(ClosingHandler handler) {
      this.handler = handler;
      return new HandlerRegistration() {
        public void removeHandler() {
          throw new UnsupportedOperationException("Auto-generated method stub");
        }
      };
    }

    public void close() {
      ClosingEvent event = new ClosingEvent();
      handler.onWindowClosing(event);
      message = event.getMessage();
    }
    
    public boolean confirm(String message) {
      this.message = message;
      return confirm;
    }
  }

  private static class MyPlace extends Place {
  }

  private class SimpleHandler implements PlaceChangeEvent.Handler<MyPlace> {
    MyPlace calledWith = null;

    public void onPlaceChange(PlaceChangeEvent<MyPlace> event) {
      calledWith = event.getNewPlace();
    }
  }

  private HandlerManager eventBus = new HandlerManager(null);
  private Delegate delegate = new Delegate();
  private PlaceController<MyPlace> placeController = new PlaceController<MyPlace>(
      eventBus, delegate);

  public void testConfirmCancelOnUserNav() {
    SimpleHandler handler = new SimpleHandler();
    eventBus.addHandler(PlaceChangeEvent.TYPE, handler);

    Canceler canceler = new Canceler();
    eventBus.addHandler(PlaceChangeRequestedEvent.TYPE, canceler);

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
    eventBus.addHandler(PlaceChangeRequestedEvent.TYPE, canceler);

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
