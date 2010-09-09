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
package com.google.gwt.i18n.rebind;

import com.google.gwt.core.ext.ConfigurationProperty;
import com.google.gwt.core.ext.SelectionProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * A LocaleUtils specific context for caching.
 */
public class LocaleInfoContext {
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

  private final Map<CacheKey, LocaleUtils> localeUtilsCache = new HashMap<
      CacheKey, LocaleUtils>();

  public LocaleUtils getLocaleUtils(SelectionProperty localeProperty,
      ConfigurationProperty runtimeLocaleProperty) {
    CacheKey key = new CacheKey(localeProperty, runtimeLocaleProperty);
    return localeUtilsCache.get(key);
  }
  
  public void putLocaleUtils(SelectionProperty localeProperty,
      ConfigurationProperty runtimeLocaleProperty, LocaleUtils localeUtils) {
    CacheKey key = new CacheKey(localeProperty, runtimeLocaleProperty);
    localeUtilsCache.put(key, localeUtils);
  }

}
