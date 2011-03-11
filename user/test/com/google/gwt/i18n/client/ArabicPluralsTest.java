/*
 * Copyright 2008 Google Inc.
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
 * Tests Arabic plurals, which are one of the most complicated.
 */
public class ArabicPluralsTest extends GWTTestCase {

  /**
   * Shorter message for just testing that Arabic plurals work properly.
   */
  public interface PluralMessage extends Messages {
    @DefaultMessage("{0} widgets")
    @AlternateMessage({"one", "A widget"})
    String pluralWidgetsOther(@PluralCount int count);
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.i18n.I18NTest_ar";
  }

  public void testPlurals() {
    // Note that all text is actually in English, but written according to
    // Arabic plural rules.
    PluralMessage m = GWT.create(PluralMessage.class);
    assertEquals("No widgets", m.pluralWidgetsOther(0));
    assertEquals("A widget", m.pluralWidgetsOther(1));
    assertEquals("Both widgets", m.pluralWidgetsOther(2));
    assertEquals("A few widgets - 3", m.pluralWidgetsOther(3));
    assertEquals("A few widgets - 10", m.pluralWidgetsOther(10));
    assertEquals("Many widgets - 11", m.pluralWidgetsOther(11));
    assertEquals("Many widgets - 99", m.pluralWidgetsOther(99));
    assertEquals("100 widgets", m.pluralWidgetsOther(100));
    assertEquals("101 widgets", m.pluralWidgetsOther(101));
    assertEquals("102 widgets", m.pluralWidgetsOther(102));
    assertEquals("A few widgets - 103", m.pluralWidgetsOther(103));
  }
}
