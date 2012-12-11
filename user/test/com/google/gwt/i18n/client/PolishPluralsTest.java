/*
 * Copyright 2012 Google Inc.
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
 * Tests Polish plurals, which is one of the most complicated.
 */
public class PolishPluralsTest extends GWTTestCase {

  /**
   * Shorter message for just testing that Polish plurals work properly.
   */
  public interface PluralMessage extends Messages {
    @DefaultMessage("{0} widgets-few")
    @AlternateMessage({"one", "A widget"})
    String pluralWidgetsOther(@PluralCount int count);
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.i18n.I18NTest_pl";
  }

  public void testPlurals() {
    // Note that all text is actually in English, but written according to
    // Polish plural rules.
    PluralMessage m = GWT.create(PluralMessage.class);
    assertEquals("(Polish many) 0 widgets", m.pluralWidgetsOther(0));
    assertEquals("(Polish one) A widget", m.pluralWidgetsOther(1));
    assertEquals("(Polish few) 2 widgets", m.pluralWidgetsOther(2));
    assertEquals("(Polish few) 3 widgets", m.pluralWidgetsOther(3));
    assertEquals("(Polish few) 4 widgets", m.pluralWidgetsOther(4));
    assertEquals("(Polish many) 5 widgets", m.pluralWidgetsOther(5));
    assertEquals("(Polish many) 10 widgets", m.pluralWidgetsOther(10));
    assertEquals("(Polish many) 11 widgets", m.pluralWidgetsOther(11));
    assertEquals("(Polish many) 12 widgets", m.pluralWidgetsOther(12));
    assertEquals("(Polish many) 13 widgets", m.pluralWidgetsOther(13));
    assertEquals("(Polish many) 14 widgets", m.pluralWidgetsOther(14));
    assertEquals("(Polish many) 15 widgets", m.pluralWidgetsOther(15));
    assertEquals("(Polish many) 20 widgets", m.pluralWidgetsOther(20));
    assertEquals("(Polish many) 21 widgets", m.pluralWidgetsOther(21));
    assertEquals("(Polish few) 22 widgets", m.pluralWidgetsOther(22));
    assertEquals("(Polish few) 23 widgets", m.pluralWidgetsOther(23));
    assertEquals("(Polish few) 24 widgets", m.pluralWidgetsOther(24));
    assertEquals("(Polish many) 25 widgets", m.pluralWidgetsOther(25));
    assertEquals("(Polish many) 99 widgets", m.pluralWidgetsOther(99));
    assertEquals("(Polish many) 100 widgets", m.pluralWidgetsOther(100));
    assertEquals("(Polish many) 101 widgets", m.pluralWidgetsOther(101));
    assertEquals("(Polish few) 102 widgets", m.pluralWidgetsOther(102));
    assertEquals("(Polish few) 103 widgets", m.pluralWidgetsOther(103));
    assertEquals("(Polish few) 104 widgets", m.pluralWidgetsOther(104));
    assertEquals("(Polish many) 105 widgets", m.pluralWidgetsOther(105));
  }
}
