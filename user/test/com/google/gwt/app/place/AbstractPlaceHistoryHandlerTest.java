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

import com.google.gwt.app.place.testplaces.Place1;
import com.google.gwt.app.place.testplaces.Place2;
import com.google.gwt.app.place.testplaces.Tokenizer2;
import com.google.gwt.app.place.testplaces.TokenizerFactory;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;

import junit.framework.TestCase;

import java.util.logging.Logger;

/**
 * Eponymous unit test.
 */
public class AbstractPlaceHistoryHandlerTest extends TestCase {
  private static class MockHistorian implements
      AbstractPlaceHistoryHandler.Historian {
    final HandlerRegistration registration = new Registration();

    ValueChangeHandler<String> handler;
    String token = "";

    public HandlerRegistration addValueChangeHandler(
        ValueChangeHandler<String> valueChangeHandler) {
      this.handler = valueChangeHandler;
      return registration;
    }

    public String getToken() {
      return token;
    }

    public void newItem(String token, boolean issueEvent) {
      assertFalse(issueEvent);
      this.token = token;
    }

    public void postToken(String string) {
      handler.onValueChange(new ValueChangeEvent<String>(string) {
      });
    }
  }

  private static class Registration implements HandlerRegistration {
    public void removeHandler() {
      throw new UnsupportedOperationException("Auto-generated method stub");
    }
  }

  private class Subject extends AbstractPlaceHistoryHandler<TokenizerFactory> {

    Subject(Historian historian) {
      super(historian);
    }

    @Override
    protected PrefixAndToken getPrefixAndToken(Place newPlace) {
      if (newPlace instanceof Place1) {
        return new PrefixAndToken(PREFIX1, factory.getTokenizer1().getToken(
            (Place1) newPlace));
      }
      if (newPlace instanceof Place2) {
        return new PrefixAndToken(PREFIX2,
            new Tokenizer2().getToken((Place2) newPlace));
      }

      return null;
    }

    @Override
    protected PlaceTokenizer<?> getTokenizer(String prefix) {
      if (PREFIX1.equals(prefix)) {
        return factory.getTokenizer1();
      }
      if (PREFIX2.equals(prefix)) {
        return new Tokenizer2();
      }

      return null;
    }

    TokenizerFactory getFactory() {
      return factory;
    }

    @Override
    Logger log() {
      return deadLogger;
    }
  }

  private static final String PREFIX1 = "t1";

  private static final String PREFIX2 = "token2";

  Logger deadLogger = new Logger("shut up", null) {
  };

  EventBus eventBus = new HandlerManager(null);

  PlaceController placeController = new PlaceController(eventBus,
      new MockPlaceControllerDelegate()) {
    @Override
    Logger log() {
      return deadLogger;
    }
  };

  MockHistorian historian = new MockHistorian();

  Subject subject = new Subject(historian);
  Place1 place1 = new Place1("able");
  Place2 place2 = new Place2("baker");

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
    placeController.goTo(place1);
    assertEquals(subject.getPrefixAndToken(place1).toString(), historian.token);
    placeController.goTo(place2);
    assertEquals(subject.getPrefixAndToken(place2).toString(), historian.token);
  }

  public void testProperToken() {
    historian.postToken(subject.getPrefixAndToken(place1).toString());
    assertEquals(place1.content, ((Place1) placeController.getWhere()).content);

    historian.postToken(subject.getPrefixAndToken(place2).toString());
    assertEquals(place2.content, ((Place2) placeController.getWhere()).content);
  }

  public void testTheTestSubjectAndPrefixAndTokenToString() {
    String history1 = subject.getPrefixAndToken(place1).toString();
    assertEquals(PREFIX1 + ":" + place1.content, history1);

    String history2 = subject.getPrefixAndToken(place2).toString();
    assertEquals(PREFIX2 + ":" + place2.content, history2);

    assertEquals(subject.getFactory().tokenizer, subject.getTokenizer(PREFIX1));
    assertTrue(subject.getTokenizer(PREFIX2) instanceof Tokenizer2);

    Place place = new Place() {
    };
    assertNull(subject.getPrefixAndToken(place));
    assertNull(subject.getTokenizer("snot"));
  }

  public void testUnknownToken() {
    historian.postToken("abcdefghijklmnop");
    assertEquals(defaultPlace, placeController.getWhere());
  }

  @Override
  protected void setUp() {
    subject.setFactory(new TokenizerFactory());
    subject.register(placeController, eventBus, defaultPlace);
  };
}
