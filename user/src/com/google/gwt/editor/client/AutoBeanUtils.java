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
package com.google.gwt.editor.client;

import com.google.gwt.core.client.impl.WeakMapping;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility methods for working with AutoBeans.
 */
public final class AutoBeanUtils {

  /**
   * Returns a map of properties that differ between two AutoBeans. The keys are
   * property names and the values are the value of the property in
   * <code>b</code>. Properties present in <code>a</code> but missing in
   * <code>b</code> will be represented by <code>null</code> values. This
   * implementation will compare AutoBeans of different parameterizations,
   * although the diff produced is likely meaningless.
   * <p>
   * This will work for both simple and wrapper AutoBeans.
   */
  public static Map<String, Object> diff(AutoBean<?> a, AutoBean<?> b) {
    // Fast check for comparing an object to itself
    if (a.equals(b)) {
      return Collections.emptyMap();
    }
    final Map<String, Object> toReturn = getAllProperties(b);

    // Remove the entries that are equal, adding nulls for missing properties
    a.accept(new AutoBeanVisitor() {
      @Override
      public boolean visitReferenceProperty(String propertyName,
          AutoBean<?> previousValue, PropertyContext ctx) {
        if (toReturn.containsKey(propertyName)) {
          if (equal(propertyName, previousValue)) {
            // No change
            toReturn.remove(propertyName);
          }
        } else {
          // The predecessor has a value that this object doesn't.
          toReturn.put(propertyName, null);
        }
        return false;
      }

      @Override
      public boolean visitValueProperty(String propertyName,
          Object previousValue, PropertyContext ctx) {
        if (toReturn.containsKey(propertyName)) {
          if (equal(propertyName, previousValue)) {
            // No change
            toReturn.remove(propertyName);
          }
        } else {
          // The predecessor has a value that this object doesn't.
          toReturn.put(propertyName, null);
        }
        return false;
      }

      private boolean equal(String propertyName, AutoBean<?> previousValue) {
        return previousValue == null && toReturn.get(propertyName) == null
            || previousValue != null && equal(propertyName, previousValue.as());
      }

      private boolean equal(String propertyName, Object previousValue) {
        return previousValue == null && toReturn.get(propertyName) == null
            || previousValue != null
            && previousValue.equals(toReturn.get(propertyName));
      }
    });
    return toReturn;
  }

  /**
   * Returns a map that is a copy of the properties contained in an AutoBean.
   * The returned map is mutable, but editing it will not have any effect on the
   * bean that produced it.
   */
  public static Map<String, Object> getAllProperties(AutoBean<?> bean) {
    final Map<String, Object> toReturn = new LinkedHashMap<String, Object>();

    // Look at the previous value of all properties
    bean.accept(new AutoBeanVisitor() {
      @Override
      public boolean visitReferenceProperty(String propertyName,
          AutoBean<?> value, PropertyContext ctx) {
        toReturn.put(propertyName, value == null ? null : value.as());
        return false;
      }

      @Override
      public boolean visitValueProperty(String propertyName, Object value,
          PropertyContext ctx) {
        toReturn.put(propertyName, value);
        return false;
      }
    });
    return toReturn;
  }

  /**
   * Return the single AutoBean wrapper that is observing the delegate object or
   * <code>null</code> if the parameter is <code>null</code> or not wrapped by
   * an AutoBean.
   */
  @SuppressWarnings("unchecked")
  public static <T, U extends T> AutoBean<T> getAutoBean(U delegate) {
    return delegate == null ? null : (AutoBean<T>) WeakMapping.get(delegate,
        AutoBean.class.getName());
  }

  /**
   * Utility class.
   */
  private AutoBeanUtils() {
  }
}
