/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.i18n.rebind;

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.ConfigurationProperty;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.SelectionProperty;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.i18n.server.GwtLocaleFactoryImpl;
import com.google.gwt.i18n.shared.GwtLocale;
import com.google.gwt.i18n.shared.GwtLocaleFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

/**
 * Utility methods for dealing with locales.
 */
public class LocaleUtils {

  /**
   * A key for lookup of computed values in a cache.
   */
  private static class CacheKey {
    private final SelectionProperty localeProperty;
    private final ConfigurationProperty runtimeLocaleProperty;

    /**
     * Create a key for cache lookup.
     * 
     * @param localeProperty "locale" property, must not be null
     * @param runtimeLocaleProperty "runtime.locales" property, must not be null
     */
    public CacheKey(SelectionProperty localeProperty,
        ConfigurationProperty runtimeLocaleProperty) {
      this.localeProperty = localeProperty;
      this.runtimeLocaleProperty = runtimeLocaleProperty;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      CacheKey other = (CacheKey) obj;
      return localeProperty.equals(other.localeProperty)
          && runtimeLocaleProperty.equals(other.runtimeLocaleProperty);
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + localeProperty.hashCode();
      result = prime * result + runtimeLocaleProperty.hashCode();
      return result;
    }
  }

  private static final GwtLocaleFactoryImpl factory = new GwtLocaleFactoryImpl();

  private static final Object cacheLock = new Object[0];
  
  private static Map<CacheKey, LocaleUtils> cache;

  /**
   * The token representing the locale property controlling Localization.
   */
  private static final String PROP_LOCALE = "locale";

  /**
   * The token representing the runtime.locales configuration property.
   */
  private static final String PROP_RUNTIME_LOCALES = "runtime.locales";

  /**
   * Clear any static state associated with LocaleUtils.
   */
  public static synchronized void clear() {
    factory.clear();
    synchronized (cacheLock) {
      cache = null;
    }
  }

  /**
   * Create a new LocaleUtils instance for the given PropertyOracle.  Returned
   * instances will be immutable and can be shared across threads.
   * 
   * @param logger
   * @param propertyOracle
   * @return LocaleUtils instance
   */
  public static LocaleUtils getInstance(TreeLogger logger,
      PropertyOracle propertyOracle) {
    try {
      SelectionProperty localeProp
          = propertyOracle.getSelectionProperty(logger, PROP_LOCALE);
      ConfigurationProperty runtimeLocaleProp
          = propertyOracle.getConfigurationProperty(PROP_RUNTIME_LOCALES);
      CacheKey key = new CacheKey(localeProp, runtimeLocaleProp);
      synchronized (cacheLock) {
        if (cache == null) {
          cache = new HashMap<CacheKey, LocaleUtils>();
        }
        LocaleUtils localeUtils = cache.get(key);
        if (localeUtils == null) {
          localeUtils = createInstance(localeProp, runtimeLocaleProp);
          cache.put(key, localeUtils);
        }
        return localeUtils;
      }
    } catch (BadPropertyValueException e) {
      // if we don't have locale properties defined, just return a basic one
      logger.log(TreeLogger.WARN,
          "Unable to get locale properties, using defaults", e);
      GwtLocale defaultLocale = factory.fromString("default");
      Set<GwtLocale> allLocales = new HashSet<GwtLocale>();
      allLocales.add(defaultLocale);
      return new LocaleUtils(defaultLocale, allLocales, allLocales,
          Collections.<GwtLocale>emptySet());
    }
  }

  /**
   * Get a shared GwtLocale factory so instances are cached between all uses.
   * 
   * @return singleton GwtLocaleFactory instance.
   */
  public static synchronized GwtLocaleFactory getLocaleFactory() {
    return factory;
  }

  private static LocaleUtils createInstance(SelectionProperty localeProp,
      ConfigurationProperty prop) {
    GwtLocale compileLocale = null;
    Set<GwtLocale> allLocales = new HashSet<GwtLocale>();
    Set<GwtLocale> allCompileLocales = new HashSet<GwtLocale>();
    Set<GwtLocale> runtimeLocales = new HashSet<GwtLocale>();
    String localeName = localeProp.getCurrentValue();
    SortedSet<String> localeValues = localeProp.getPossibleValues();

    GwtLocaleFactory factoryInstance = getLocaleFactory();
    GwtLocale newCompileLocale = factoryInstance.fromString(localeName);
    compileLocale = newCompileLocale;
    for (String localeValue : localeValues) {
      allCompileLocales.add(factoryInstance.fromString(localeValue));
    }
    allLocales.addAll(allCompileLocales);

    List<String> rtLocaleNames = prop.getValues();
    if (rtLocaleNames != null) {
      for (String rtLocale : rtLocaleNames) {
        GwtLocale locale = factoryInstance.fromString(rtLocale);
        // TODO(jat): remove use of labels
        existingLocales:
        for (GwtLocale existing : allCompileLocales) {
          for (GwtLocale alias : existing.getAliases()) {
            if (!alias.isDefault() && locale.inheritsFrom(alias)
                && locale.usesSameScript(alias)) {
              allLocales.add(locale);
              break existingLocales;
            }
          }
        }
        if (!compileLocale.isDefault()
            && locale.inheritsFrom(compileLocale)
            && locale.usesSameScript(compileLocale)) {
          // TODO(jat): don't include runtime locales which also inherit
          // from a more-specific compile locale than this one
          runtimeLocales.add(locale);
        }
      }
    }
    return new LocaleUtils(compileLocale, allLocales, allCompileLocales,
        runtimeLocales);
  }

  private final Set<GwtLocale> allCompileLocales;

  private final Set<GwtLocale> allLocales;

  private final GwtLocale compileLocale;

  private final Set<GwtLocale> runtimeLocales;

  private LocaleUtils(GwtLocale compileLocale, Set<GwtLocale> allLocales,
      Set<GwtLocale> allCompileLocales, Set<GwtLocale> runtimeLocales) {
    this.compileLocale = compileLocale;
    this.allLocales = Collections.unmodifiableSet(allLocales);
    this.allCompileLocales = Collections.unmodifiableSet(allCompileLocales);
    this.runtimeLocales = Collections.unmodifiableSet(runtimeLocales);
  }

  /**
   * Returns the set of all compile-time locales.
   * 
   * @return unmodifiable set of all compile-time locales
   */
  public Set<GwtLocale> getAllCompileLocales() {
    return allCompileLocales;
  }

  /**
   * Returns the set of all available locales, whether compile-time locales or
   * runtime locales.
   * 
   * @return unmodifiable set of all locales
   */
  public Set<GwtLocale> getAllLocales() {
    return allLocales;
  }

  /**
   * @return the static compile-time locale for this permutation.
   */
  public GwtLocale getCompileLocale() {
    return compileLocale;
  }

  /**
   * Returns a list of locales which are children of the current compile-time
   * locale.
   * 
   * @return unmodifiable list of matching locales
   */
  public Set<GwtLocale> getRuntimeLocales() {
    return runtimeLocales;
  }
}
