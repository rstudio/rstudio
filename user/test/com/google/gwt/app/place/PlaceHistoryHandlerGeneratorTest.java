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

import com.google.gwt.app.place.testplacehandler.NoFactory;
import com.google.gwt.app.place.testplacehandler.WithFactory;
import com.google.gwt.app.place.testplaces.Place1;
import com.google.gwt.app.place.testplaces.Place2;
import com.google.gwt.app.place.testplaces.Place3;
import com.google.gwt.app.place.testplaces.Place4;
import com.google.gwt.app.place.testplaces.Tokenizer2;
import com.google.gwt.app.place.testplaces.Tokenizer3;
import com.google.gwt.app.place.testplaces.Tokenizer4;
import com.google.gwt.app.place.testplaces.TokenizerFactory;
import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Functional test of PlaceHistoryHandlerGenerator.
 */
public class PlaceHistoryHandlerGeneratorTest extends GWTTestCase {
  @WithTokenizers( {
      Place1.Tokenizer.class, Tokenizer2.class, Tokenizer3.class,
      Tokenizer4.class})
  interface LocalNoFactory extends PlaceHistoryHandler {
  };

  @WithTokenizers(Tokenizer4.class)
  interface LocalWithFactory extends
      PlaceHistoryHandlerWithFactory<TokenizerFactory> {
  };

  @Override
  public String getModuleName() {
    return "com.google.gwt.app.AppSuite";
  }

  Place1 place1 = new Place1("able");
  Place2 place2 = new Place2("baker");
  Place3 place3 = new Place3("charlie");
  Place4 place4 = new Place4("delta");

  public void testTopLevelWithoutFactory() {
    AbstractPlaceHistoryHandler<?> subject = GWT.create(NoFactory.class);

    doTest(subject, null);
  }

  public void testTopLevelWithFactory() {
    AbstractPlaceHistoryHandler<TokenizerFactory> subject = GWT.create(WithFactory.class);
    TokenizerFactory factory = new TokenizerFactory();
    subject.setFactory(factory);

    doTest(subject, factory);
  }

  public void testNestedWithoutFactory() {
    AbstractPlaceHistoryHandler<?> subject = GWT.create(LocalNoFactory.class);

    doTest(subject, null);
  }

  public void testNestedWithFactory() {
    AbstractPlaceHistoryHandler<TokenizerFactory> subject = GWT.create(LocalWithFactory.class);
    TokenizerFactory factory = new TokenizerFactory();
    subject.setFactory(factory);

    doTest(subject, factory);
  }

  // CHECKSTYLE_OFF
  private void doTest(AbstractPlaceHistoryHandler<?> subject,
      TokenizerFactory factory) {
    String history1 = subject.getPrefixAndToken(place1).toString();
    assertEquals(Place1.Tokenizer.PREFIX + ":" + place1.content, history1);

    String history2 = subject.getPrefixAndToken(place2).toString();
    if (factory != null) {
      assertEquals(TokenizerFactory.PLACE2_PREFIX + ":" + place2.content,
          history2);
    } else {
      // CHECKSTYLE_OFF
      assertEquals("Place2:" + place2.content, history2);
      // CHECKSTYLE_ON
    }

    String history3 = subject.getPrefixAndToken(place3).toString();
    // CHECKSTYLE_OFF
    assertEquals("Place3:" + place3.content, history3);
    // CHECKSTYLE_ON

    String history4 = subject.getPrefixAndToken(place4).toString();
    // CHECKSTYLE_OFF
    assertEquals("Place4:" + place4.content, history4);
    // CHECKSTYLE_ON

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

    Place place = new Place() {
    };
    assertNull(subject.getPrefixAndToken(place));
    assertNull(subject.getTokenizer("snot"));
  }
}
