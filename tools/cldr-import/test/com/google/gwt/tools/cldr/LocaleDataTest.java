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
package com.google.gwt.tools.cldr;

import com.google.gwt.i18n.server.GwtLocaleFactoryImpl;
import com.google.gwt.i18n.shared.GwtLocale;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

/**
 * Test the LocaleData class.
 */
public class LocaleDataTest extends TestCase {

  private static final GwtLocaleFactoryImpl localeFactory;

  static {
    localeFactory = new GwtLocaleFactoryImpl();
  }

  /**
   * Test method for {@link LocaleData#getAllLocales()}.
   */
  public void testGetAllLocales() {
    LocaleData localeData =
        new LocaleData(localeFactory, Arrays.asList("root", "en", "en_US", "ar", "ar_IQ"));
    Set<GwtLocale> locales = localeData.getAllLocales();
    assertEquals(5, locales.size());
    GwtLocale localeEn = localeFactory.fromString("en");
    localeData.addEntry("foo", localeEn, "k1", "v1");
    locales = localeData.getAllLocales();
    assertEquals(5, locales.size());
    GwtLocale localeAr = localeFactory.fromString("ar");
    localeData.addEntry("bar", localeAr, "k1", "v1");
    locales = localeData.getAllLocales();
    assertEquals(5, locales.size());
  }

  /**
   * Test method for {@link LocaleData#getNonEmptyLocales()}.
   */
  public void testGetNonEmptyLocales() {
    LocaleData localeData =
        new LocaleData(localeFactory, Arrays.asList("root", "en", "en_US", "ar", "ar_IQ"));
    Iterator<GwtLocale> it = localeData.getNonEmptyLocales().iterator();
    assertFalse(it.hasNext());
    GwtLocale localeEn = localeFactory.fromString("en");
    localeData.addEntry("foo", localeEn, "k1", "v1");
    it = localeData.getNonEmptyLocales().iterator();
    assertTrue(it.hasNext());
    assertEquals(localeEn, it.next());
    assertFalse(it.hasNext());
    GwtLocale localeAr = localeFactory.fromString("ar");
    localeData.addEntry("bar", localeAr, "k1", "v1");
    it = localeData.getNonEmptyLocales().iterator();
    assertTrue(it.hasNext());
    assertNotNull(it.next());
    assertTrue(it.hasNext());
    assertNotNull(it.next());
    assertFalse(it.hasNext());
  }

  /**
   * Test method for {@link LocaleData#getNonEmptyLocales(java.lang.String)}.
   */
  public void testGetNonEmptyLocalesString() {
    LocaleData localeData =
        new LocaleData(localeFactory, Arrays.asList("root", "en", "en_US", "ar", "ar_IQ"));
    Iterator<GwtLocale> it = localeData.getNonEmptyLocales("foo").iterator();
    assertFalse(it.hasNext());
    GwtLocale localeEn = localeFactory.fromString("en");
    localeData.addEntry("foo", localeEn, "k1", "v1");
    it = localeData.getNonEmptyLocales("foo").iterator();
    assertTrue(it.hasNext());
    assertEquals(localeEn, it.next());
    assertFalse(it.hasNext());
    it = localeData.getNonEmptyLocales("bar").iterator();
    assertFalse(it.hasNext());
    GwtLocale localeAr = localeFactory.fromString("ar");
    localeData.addEntry("bar", localeAr, "k1", "v1");
    it = localeData.getNonEmptyLocales("foo").iterator();
    assertTrue(it.hasNext());
    assertEquals(localeEn, it.next());
    assertFalse(it.hasNext());
    it = localeData.getNonEmptyLocales("bar").iterator();
    assertTrue(it.hasNext());
    assertEquals(localeAr, it.next());
    assertFalse(it.hasNext());
    it = localeData.getNonEmptyLocales("baz").iterator();
    assertFalse(it.hasNext());
  }

  /**
   * Test method for
   * {@link LocaleData#inheritsFrom(com.google.gwt.i18n.shared.GwtLocale)}.
   */
  public void testInheritsFrom() {
    LocaleData localeData =
        new LocaleData(localeFactory, Arrays.asList("root", "en", "en_US", "en_US_VARIANT", "ar",
            "ar_IQ"));
    GwtLocale localeEn = localeFactory.fromString("en");
    localeData.addEntry("foo", localeEn, "k1", "v1");
    GwtLocale localeEnUs = localeFactory.fromString("en_us");
    GwtLocale localeEnUsVariant = localeFactory.fromString("en_us_variant");
    GwtLocale localeDefault = localeFactory.getDefault();
    assertEquals(null, localeData.inheritsFrom(localeDefault));
    assertEquals(localeEn, localeData.inheritsFrom(localeEnUs));
    assertEquals(localeEn, localeData.inheritsFrom(localeEnUsVariant));
    assertEquals(localeDefault, localeData.inheritsFrom(localeEn));
    GwtLocale localeAr = localeFactory.fromString("ar");
    assertEquals(localeDefault, localeData.inheritsFrom(localeAr));
  }

  /**
   * Test method for
   * {@link LocaleData#inheritsFrom(java.lang.String, com.google.gwt.i18n.shared.GwtLocale)}
   * .
   */
  public void testInheritsFromWithCategory() {
    LocaleData localeData =
        new LocaleData(localeFactory, Arrays.asList("root", "en", "en_US", "en_US_VARIANT", "ar",
            "ar_IQ"));
    GwtLocale localeEn = localeFactory.fromString("en");
    localeData.addEntry("foo", localeEn, "k1", "v1");
    GwtLocale localeEnUs = localeFactory.fromString("en_us");
    GwtLocale localeEnUsVariant = localeFactory.fromString("en_us_variant");
    GwtLocale localeDefault = localeFactory.getDefault();
    assertEquals(localeEn, localeData.inheritsFrom("foo", localeEnUs));
    assertEquals(localeEn, localeData.inheritsFrom("foo", localeEnUsVariant));
    assertEquals(localeDefault, localeData.inheritsFrom("foo", localeEn));
    assertEquals(localeDefault, localeData.inheritsFrom("bar", localeEnUs));
    assertEquals(localeDefault, localeData.inheritsFrom("bar", localeEnUsVariant));
    GwtLocale localeAr = localeFactory.fromString("ar");
    assertEquals(localeDefault, localeData.inheritsFrom("foo", localeAr));
    assertEquals(localeDefault, localeData.inheritsFrom("bar", localeAr));
  }

  /**
   * Test method for {@link LocaleData#removeCompleteDuplicates()}.
   */
  public void testRemoveCompleteDuplicates() {
    LocaleData localeData =
        new LocaleData(localeFactory, Arrays.asList("root", "en", "en_US", "ar", "ar_IQ"));
    GwtLocale localeEn = localeFactory.fromString("en");
    GwtLocale localeEnUs = localeFactory.fromString("en_us");
    localeData.addEntry("foo", localeEn, "k1", "v1");
    localeData.addEntry("foo", localeEn, "k2", "v2");
    localeData.addEntry("foo", localeEnUs, "k1", "v1");
    localeData.addEntry("foo", localeEnUs, "k2", "v2");
    localeData.removeCompleteDuplicates();
    Set<GwtLocale> locales = localeData.getNonEmptyLocales();
    assertEquals(1, locales.size());
    localeData.addEntry("foo", localeEnUs, "k2", "v2a");
    locales = localeData.getNonEmptyLocales();
    assertEquals(2, locales.size());
  }

  /**
   * Test method for {@link LocaleData#removeDuplicates()}.
   */
  public void testRemoveDuplicates() {
    LocaleData localeData =
        new LocaleData(localeFactory, Arrays.asList("root", "en", "en_US", "ar", "ar_IQ"));
    GwtLocale localeEn = localeFactory.fromString("en");
    GwtLocale localeEnUs = localeFactory.fromString("en_us");
    localeData.addEntry("foo", localeEn, "k1", "v1");
    localeData.addEntry("foo", localeEn, "k2", "v2");
    localeData.addEntry("foo", localeEnUs, "k1", "v1");
    localeData.addEntry("foo", localeEnUs, "k2", "v2a");
    localeData.removeDuplicates();
    Set<GwtLocale> locales = localeData.getNonEmptyLocales();
    assertEquals(2, locales.size());
    assertEquals("v2a", localeData.getEntry("foo", localeEnUs, "k2"));
    localeData.addEntry("foo", localeEnUs, "k2", "v2");
    localeData.removeDuplicates();
    locales = localeData.getNonEmptyLocales();
    assertEquals(1, locales.size());
    assertNull(localeData.getEntry("foo", localeEnUs, "k2"));
  }
}
