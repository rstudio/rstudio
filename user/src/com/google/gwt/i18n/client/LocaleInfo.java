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
import com.google.gwt.i18n.client.impl.CldrImpl;
import com.google.gwt.i18n.client.impl.LocaleInfoImpl;

/**
 * Provides access to the currently-running locale and the list of available
 * locales.
 */
public class LocaleInfo {

  /**
   * Currently we only support getting the currently running locale, so this
   * is a static.  In the future, we would need a hash map from locale names
   * to LocaleInfo instances.
   */
  private static LocaleInfo instance  = new LocaleInfo(
      (LocaleInfoImpl) GWT.create(LocaleInfoImpl.class),
      (CldrImpl) GWT.create(CldrImpl.class));

  /**
   * @return an array of available locales
   */
  public static String[] getAvailableLocaleNames() {
    /*
     * The set of all locales is constant across all permutations, so this
     * is static.  Ideally, the set of available locales would be generated
     * by a different GWT.create but that would slow the compilation process
     * unnecessarily.
     * 
     * This is static, and accesses infoImpl this way, with an eye towards
     * when we implement static LocaleInfo getLocale(String localeName) as
     * you might want to get the list of available locales in order to create
     * instances of each of them.
     */
    return instance.infoImpl.getAvailableLocaleNames();
  }
  
  /**
   * @return a LocaleInfo instance for the current locale.
   * 
   * In the future, we could make additional static methods which returned
   * a LocaleInfo instance for a specific locale (from the set of those
   * the app was compiled with), accessed via a method like:
   * <pre>
   *   public static LocaleInfo getLocale(String localeName)
   * </pre>
   */
  public static LocaleInfo getCurrentLocale() {
    return instance;
  }

  private final LocaleInfoImpl infoImpl;
  
  private final CldrImpl cldrImpl;
  
  /**
   * Constructor to be used by subclasses, such as mock classes for testing.
   * Any such subclass should override all methods.
   */
  protected LocaleInfo() {
    infoImpl = null;
    cldrImpl = null;
  }
  
  /**
   * Create a LocaleInfo instance, passing in the implementation classes.
   * 
   * @param impl LocaleInfoImpl instance to use
   * @param cldr CldrImpl instance to use
   */
  private LocaleInfo(LocaleInfoImpl impl, CldrImpl cldr) {
    this.infoImpl = impl;
    this.cldrImpl = cldr;
  }

  /**
   * @return the name of this locale, such as "default, "en_US", etc.
   */
  public String getLocaleName() {
    return infoImpl.getLocaleName();
  }
  
  /**
   * @return true if this locale is right-to-left instead of left-to-right.
   */
  public boolean isRTL() {
    return cldrImpl.isRTL();
  }
}
