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

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.place.testplaces.Place1;
import com.google.gwt.place.testplaces.Place2;

import junit.framework.TestCase;

import java.util.logging.Logger;

/**
 * Eponymous unit test.
 */
public class PlaceHistoryHandlerTest extends TestCase {
  private static class MockHistorian implements
      PlaceHistoryHandler.Historian {
    final HandlerRegistration registration = new Registration();

    ValueChangeHandler<String> handler;
    String token = "";

    @Override
    public HandlerRegistration addValueChangeHandler(
        ValueChangeHandler<String> valueChangeHandler) {
      this.handler = valueChangeHandler;
      return registration;
    }

    @Override
    public String getToken() {
      return token;
    }

    @Override
    public void newItem(String token, boolean issueEvent) {
      assertFalse(issueEvent);
      this.token = token;
    }

    public void postToken(String string) {
      handler.onValueChange(new ValueChangeEvent<String>(string) {
      });
    }
  }

  private static class MockPlaceHistoryMapper implements PlaceHistoryMapper {

    @Override
    public Place getPlace(String token) {
      if (TOKEN1.equals(token)) {
        return PLACE1;
      }
      if (TOKEN2.equals(token)) {
        return PLACE2;
      }

      return null;
    }

    @Override
    public String getToken(Place place) {
      if (place == PLACE1) {
        return TOKEN1;
      }
      if (place == PLACE2) {
        return TOKEN2;
      }

      return null;
    }
  }

  private static class Registration implements HandlerRegistration {
    @Override
    public void removeHandler() {
      throw new UnsupportedOperationException("Auto-generated method stub");
    }
  }

  private class Subject extends PlaceHistoryHandler {

    Subject(PlaceHistoryMapper mapper, Historian historian) {
      super(mapper, historian);
    }

    @Override
    Logger log() {
      return deadLogger;
    }
  }

  private static final String TOKEN1 = "t1";

  private static final String TOKEN2 = "token2";

  private static final Place1 PLACE1 = new Place1("able");

  private static final Place2 PLACE2 = new Place2("baker");

  Logger deadLogger = new Logger("shut up", null) {
  };

  PlaceController placeController;

  MockHistorian historian;

  Subject subject;

  final Place defaultPlace = new Place() {
  };

  public void testEmptyToken() {
    historian.postToken("");
    assertEquals(defaultPlace, placeController.getWhere());
  }

  public void testGoToDefaultPlace() {
    placeController.goTo(defaultPlace);
    assertEquals("", historian.token);
  }

  public void testPlaceChange() {
    placeController.goTo(PLACE1);
    assertEquals(TOKEN1, historian.token);
    placeController.goTo(PLACE2);
    assertEquals(TOKEN2, historian.token);
  }

  public void testProperToken() {
    historian.postToken(TOKEN1);
    assertEquals(PLACE1, placeController.getWhere());

    historian.postToken(TOKEN2);
    assertEquals(PLACE2, placeController.getWhere());
  }

  public void testUnknownToken() {
    historian.postToken("abcdefghijklmnop");
    assertEquals(defaultPlace, placeController.getWhere());
  }

  @Override
  protected void setUp() {
    EventBus eventBus = new SimpleEventBus();
    historian = new MockHistorian();
    placeController = new PlaceController(eventBus,
        new MockPlaceControllerDelegate()) {
      @Override
      Logger log() {
        return deadLogger;
      }
    };
    subject = new Subject(new MockPlaceHistoryMapper(), historian);
    subject.register(placeController, eventBus, defaultPlace);
  };
}
