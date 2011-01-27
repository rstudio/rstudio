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
package com.google.gwt.place.impl;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.gwt.place.shared.PlaceHistoryMapperWithFactory;
import com.google.gwt.place.shared.WithTokenizers;
import com.google.gwt.place.testplacemappers.NoFactory;
import com.google.gwt.place.testplacemappers.WithFactory;
import com.google.gwt.place.testplaces.Place1;
import com.google.gwt.place.testplaces.Place2;
import com.google.gwt.place.testplaces.Place3;
import com.google.gwt.place.testplaces.Place4;
import com.google.gwt.place.testplaces.Place5;
import com.google.gwt.place.testplaces.Place6;
import com.google.gwt.place.testplaces.Tokenizer2;
import com.google.gwt.place.testplaces.Tokenizer3;
import com.google.gwt.place.testplaces.Tokenizer4;
import com.google.gwt.place.testplaces.TokenizerFactory;

/**
 * Functional test of PlaceHistoryMapperGenerator.
 */
public class PlaceHistoryMapperGeneratorTest extends GWTTestCase {
  @WithTokenizers({
      Place1.Tokenizer.class, Tokenizer2.class, Tokenizer3.class,
      Tokenizer4.class, Place6.Tokenizer.class})
  interface LocalNoFactory extends PlaceHistoryMapper {
  };

  @WithTokenizers({Tokenizer4.class, Place6.Tokenizer.class})
  interface LocalWithFactory extends
      PlaceHistoryMapperWithFactory<TokenizerFactory> {
  };

  /**
   * The goal is only to test that the generator doesn't fail (but doesn't
   * generate anything either).
   */
  static class LocalConcreteClass implements LocalNoFactory {
    public Place getPlace(String token) {
      return null;
    }
    public String getToken(Place place) {
      return null;
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.place.PlaceSuite";
  }

  Place1 place1 = new Place1("able");
  Place2 place2 = new Place2("baker");
  Place3 place3 = new Place3("charlie");
  Place4 place4 = new Place4("delta");
  Place5 place5 = new Place5("echo");
  Place6 place6 = new Place6("foxtrot");

  public void testTopLevelWithoutFactory() {
    AbstractPlaceHistoryMapper<?> subject = GWT.create(NoFactory.class);

    doTest(subject, null);
  }

  public void testTopLevelWithFactory() {
    AbstractPlaceHistoryMapper<TokenizerFactory> subject = GWT.create(WithFactory.class);
    TokenizerFactory factory = new TokenizerFactory();
    subject.setFactory(factory);

    doTest(subject, factory);
  }

  public void testNestedWithoutFactory() {
    AbstractPlaceHistoryMapper<?> subject = GWT.create(LocalNoFactory.class);

    doTest(subject, null);
  }

  public void testNestedWithFactory() {
    AbstractPlaceHistoryMapper<TokenizerFactory> subject = GWT.create(LocalWithFactory.class);
    TokenizerFactory factory = new TokenizerFactory();
    subject.setFactory(factory);

    doTest(subject, factory);
  }

  /**
   * When asked to GWT.create a concrete implementation of PlaceHistoryMapper,
   * the generator politely instantiates it. This is to make life easier
   * for GIN users. See 
   * http://code.google.com/p/google-web-toolkit/issues/detail?id=5563
   */
  public void testNotAnInterface() {
    PlaceHistoryMapper subject = GWT.create(LocalConcreteClass.class);
    assertNull(subject.getToken(null));
    assertNull(subject.getPlace(null));
  }

  // CHECKSTYLE_OFF
  private void doTest(AbstractPlaceHistoryMapper<?> subject,
      TokenizerFactory factory) {
    String history1 = subject.getPrefixAndToken(place1).toString();
    assertEquals(Place1.Tokenizer.PREFIX + ":" + place1.content, history1);

    String history2 = subject.getPrefixAndToken(place2).toString();
    if (factory != null) {
      assertEquals(TokenizerFactory.PLACE2_PREFIX + ":" + place2.content,
          history2);
    } else {
      assertEquals("Place2:" + place2.content, history2);
    }

    String history3 = subject.getPrefixAndToken(place3).toString();
    assertEquals("Place3:" + place3.content, history3);

    String history4 = subject.getPrefixAndToken(place4).toString();
    assertEquals("Place4:" + place4.content, history4);

    // Place 5 extends Place3 and does not have its own PlaceTokenizer
    String history5 = subject.getPrefixAndToken(place5).toString();
    assertEquals("Place3:" + place5.content, history5);

    if (factory != null) {
      assertEquals(factory.tokenizer,
          subject.getTokenizer(Place1.Tokenizer.PREFIX));
      assertEquals(factory.tokenizer2,
          subject.getTokenizer(TokenizerFactory.PLACE2_PREFIX));
      assertEquals(factory.tokenizer3, subject.getTokenizer("Place3"));
    } else {
      assertTrue(subject.getTokenizer(Place1.Tokenizer.PREFIX) instanceof Place1.Tokenizer);
      assertTrue(subject.getTokenizer("Place2") instanceof Tokenizer2);
      assertTrue(subject.getTokenizer("Place3") instanceof Tokenizer3);
    }
    assertTrue(subject.getTokenizer("Place4") instanceof Tokenizer4);
    
    // Empty prefix
    String history6 = subject.getPrefixAndToken(place6).toString();
    assertEquals(place6.content, history6);
    assertTrue(subject.getTokenizer("") instanceof Place6.Tokenizer);
    assertTrue(subject.getPlace("noPrefix") instanceof Place6);

    Place place = new Place() {
    };
    assertNull(subject.getPrefixAndToken(place));
    assertNull(subject.getTokenizer("snot"));
  }
  // CHECKSTYLE_ON
}
