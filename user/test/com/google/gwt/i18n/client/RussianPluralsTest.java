/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.i18n.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests Russian plurals, which is one of the most complicated.
 */
public class RussianPluralsTest extends GWTTestCase {

  /**
   * Shorter message for just testing that Russian plurals work properly.
   */
  public interface PluralMessage extends Messages {
    @DefaultMessage("{0} widgets-few")
    @AlternateMessage({"one", "{0} widgets-one"})
    String pluralWidgetsOther(@PluralCount int count);
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.i18n.I18NTest_ru";
  }

  public void testPlurals() {
    // Note that all text is actually in English, but written according to
    // Russian plural rules.
    PluralMessage m = GWT.create(PluralMessage.class);
    assertEquals("0 widgets-many", m.pluralWidgetsOther(0));
    assertEquals("1 widgets-one", m.pluralWidgetsOther(1));
    assertEquals("2 widgets-few", m.pluralWidgetsOther(2));
    assertEquals("3 widgets-few", m.pluralWidgetsOther(3));
    assertEquals("4 widgets-few", m.pluralWidgetsOther(4));
    assertEquals("5 widgets-many", m.pluralWidgetsOther(5));

    assertEquals("10 widgets-many", m.pluralWidgetsOther(10));
    assertEquals("11 widgets-many", m.pluralWidgetsOther(11));
    assertEquals("12 widgets-many", m.pluralWidgetsOther(12));
    assertEquals("13 widgets-many", m.pluralWidgetsOther(13));
    assertEquals("14 widgets-many", m.pluralWidgetsOther(14));
    assertEquals("15 widgets-many", m.pluralWidgetsOther(15));

    assertEquals("20 widgets-many", m.pluralWidgetsOther(20));
    assertEquals("21 widgets-one", m.pluralWidgetsOther(21));
    assertEquals("22 widgets-few", m.pluralWidgetsOther(22));
    assertEquals("23 widgets-few", m.pluralWidgetsOther(23));
    assertEquals("24 widgets-few", m.pluralWidgetsOther(24));
    assertEquals("25 widgets-many", m.pluralWidgetsOther(25));

    assertEquals("99 widgets-many", m.pluralWidgetsOther(99));
    assertEquals("100 widgets-many", m.pluralWidgetsOther(100));
    assertEquals("101 widgets-one", m.pluralWidgetsOther(101));
    assertEquals("102 widgets-few", m.pluralWidgetsOther(102));
    assertEquals("103 widgets-few", m.pluralWidgetsOther(103));
    assertEquals("104 widgets-few", m.pluralWidgetsOther(104));
    assertEquals("105 widgets-many", m.pluralWidgetsOther(105));

    assertEquals("110 widgets-many", m.pluralWidgetsOther(110));
    assertEquals("111 widgets-many", m.pluralWidgetsOther(111));
    assertEquals("112 widgets-many", m.pluralWidgetsOther(112));
    assertEquals("113 widgets-many", m.pluralWidgetsOther(113));
    assertEquals("114 widgets-many", m.pluralWidgetsOther(114));
    assertEquals("115 widgets-many", m.pluralWidgetsOther(115));

    assertEquals("120 widgets-many", m.pluralWidgetsOther(120));
    assertEquals("121 widgets-one", m.pluralWidgetsOther(121));
    assertEquals("122 widgets-few", m.pluralWidgetsOther(122));
    assertEquals("123 widgets-few", m.pluralWidgetsOther(123));
    assertEquals("124 widgets-few", m.pluralWidgetsOther(124));
    assertEquals("125 widgets-many", m.pluralWidgetsOther(125));
  }
}
