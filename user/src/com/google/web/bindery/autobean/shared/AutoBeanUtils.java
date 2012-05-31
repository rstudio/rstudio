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
package com.google.web.bindery.autobean.shared;

import com.google.gwt.core.client.impl.WeakMapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility methods for working with AutoBeans.
 */
public final class AutoBeanUtils {
  /*
   * TODO(bobv): Make Comparison a real type that holds a map contain the diff
   * between the two objects. Then export a Map of PendingComparison to
   * Comparisons as a public API to make it easy for developers to perform deep
   * diffs across a graph structure.
   * 
   * Three-way merge...
   */

  private enum Comparison {
    TRUE, FALSE, PENDING;
  }

  /**
   * A Pair where order does not matter and the objects are compared by
   * identity.
   */
  private static class PendingComparison {
    private final AutoBean<?> a;
    private final AutoBean<?> b;
    private final int hashCode;

    public PendingComparison(AutoBean<?> a, AutoBean<?> b) {
      this.a = a;
      this.b = b;
      // Don't make relatively prime since order does not matter
      hashCode = System.identityHashCode(a) + System.identityHashCode(b);
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof PendingComparison)) {
        return false;
      }
      PendingComparison other = (PendingComparison) o;
      return a == other.a && b == other.b || // Direct match
          a == other.b && b == other.a; // Swapped
    }

    @Override
    public int hashCode() {
      return hashCode;
    }
  }

  /**
   * Compare two graphs of AutoBeans based on values.
   * <p>
   * <ul>
   * <li>AutoBeans are compared based on type and property values</li>
   * <li>Lists are compared with element-order equality</li>
   * <li>Sets and all other Collection types are compare with bag equality</li>
   * <li>Maps are compared as a lists of keys-value pairs</li>
   * <li>{@link Splittable Splittables} are compared by value</li>
   * </ul>
   * <p>
   * This will work for both simple and wrapper AutoBeans.
   * <p>
   * This method may crawl the entire object graph reachable from the input
   * parameters and may be arbitrarily expensive to compute.
   * 
   * @param a an {@link AutoBean}
   * @param b an {@link AutoBean}
   * @return {@code false} if any values in the graph reachable through
   *         <code>a</code> are different from those reachable from
   *         <code>b</code>
   */
  public static boolean deepEquals(AutoBean<?> a, AutoBean<?> b) {
    return sameOrEquals(a, b, new HashMap<PendingComparison, Comparison>());
  }

  /**
   * Returns a map of properties that differ (via {@link Object#equals(Object)})
   * between two AutoBeans. The keys are property names and the values are the
   * value of the property in <code>b</code>. Properties present in
   * <code>a</code> but missing in <code>b</code> will be represented by
   * <code>null</code> values. This implementation will compare AutoBeans of
   * different parameterizations, although the diff produced is likely
   * meaningless.
   * <p>
   * This will work for both simple and wrapper AutoBeans.
   * 
   * @param a an {@link AutoBean}
   * @param b an {@link AutoBean}
   * @return a {@link Map} of differing properties
   */
  public static Map<String, Object> diff(AutoBean<?> a, AutoBean<?> b) {
    // Fast check for comparing an object to itself
    if (a == b) {
      return Collections.emptyMap();
    }
    final Map<String, Object> toReturn = getAllProperties(b);

    // Remove the entries that are equal, adding nulls for missing properties
    a.accept(new AutoBeanVisitor() {
      @Override
      public boolean visitReferenceProperty(String propertyName, AutoBean<?> previousValue,
          PropertyContext ctx) {
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
      public boolean visitValueProperty(String propertyName, Object previousValue,
          PropertyContext ctx) {
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
        return previousValue == null && toReturn.get(propertyName) == null || previousValue != null
            && equal(propertyName, previousValue.as());
      }

      private boolean equal(String propertyName, Object previousValue) {
        Object currentValue = toReturn.get(propertyName);
        return previousValue == null && currentValue == null || previousValue != null
            && previousValue.equals(currentValue);
      }
    });
    return toReturn;
  }

  /**
   * Returns a map that is a copy of the properties contained in an AutoBean.
   * The returned map is mutable, but editing it will not have any effect on the
   * bean that produced it.
   * 
   * @param bean an {@link AutoBean}
   * @return a {@link Map} of the bean's properties
   */
  public static Map<String, Object> getAllProperties(AutoBean<?> bean) {
    final Map<String, Object> toReturn = new LinkedHashMap<String, Object>();

    // Look at the previous value of all properties
    bean.accept(new AutoBeanVisitor() {
      @Override
      public boolean visitReferenceProperty(String propertyName, AutoBean<?> value,
          PropertyContext ctx) {
        toReturn.put(propertyName, value == null ? null : value.as());
        return false;
      }

      @Override
      public boolean visitValueProperty(String propertyName, Object value, PropertyContext ctx) {
        toReturn.put(propertyName, value);
        return false;
      }
    });
    return toReturn;
  }

  /**
   * Return the single AutoBean wrapper that is observing the delegate object or
   * {@code null} if the parameter is {@code null}or not wrapped by an AutoBean.
   * 
   * @param delegate a delegate object, or {@code null}
   * @return the {@link AutoBean} wrapper for the delegate, or {@code null}
   */
  @SuppressWarnings("unchecked")
  public static <T, U extends T> AutoBean<T> getAutoBean(U delegate) {
    return delegate == null ? null : (AutoBean<T>) WeakMapping.get(delegate, AutoBean.class
        .getName());
  }

  /**
   * Compare two AutoBeans, this method has the type fan-out.
   */
  static boolean sameOrEquals(Object value, Object otherValue,
      Map<PendingComparison, Comparison> pending) {
    if (value == otherValue) {
      // Fast exit
      return true;
    }

    if (value instanceof Collection<?> && otherValue instanceof Collection<?>) {
      // Check collections
      return sameOrEquals((Collection<?>) value, (Collection<?>) otherValue, pending, null);
    }

    if (value instanceof Map<?, ?> && otherValue instanceof Map<?, ?>) {
      // Check maps
      return sameOrEquals((Map<?, ?>) value, (Map<?, ?>) otherValue, pending);
    }

    if (value instanceof Splittable && otherValue instanceof Splittable) {
      return sameOrEquals((Splittable) value, (Splittable) otherValue, pending);
    }

    // Possibly substitute the AutoBean for its shim
    {
      AutoBean<?> maybeValue = AutoBeanUtils.getAutoBean(value);
      AutoBean<?> maybeOther = AutoBeanUtils.getAutoBean(otherValue);
      if (maybeValue != null && maybeOther != null) {
        value = maybeValue;
        otherValue = maybeOther;
      }
    }

    if (value instanceof AutoBean<?> && otherValue instanceof AutoBean<?>) {
      // Check ValueProxies
      return sameOrEquals((AutoBean<?>) value, (AutoBean<?>) otherValue, pending);
    }

    if (value == null ^ otherValue == null) {
      // One is null, the other isn't
      return false;
    }

    if (value != null && !value.equals(otherValue)) {
      // Regular object equality
      return false;
    }
    return true;
  }

  /**
   * If a comparison between two AutoBeans is currently pending, this method
   * will skip their comparison.
   */
  private static boolean sameOrEquals(AutoBean<?> value, AutoBean<?> otherValue,
      Map<PendingComparison, Comparison> pending) {
    if (value == otherValue) {
      // Simple case
      return true;
    } else if (!value.getType().equals(otherValue.getType())) {
      // Beans of different types
      return false;
    }

    /*
     * The PendingComparison key allows us to break reference cycles when
     * crawling the graph. Since the entire operation is essentially a
     * concatenated && operation, it's ok to speculatively return true for
     * repeated a.equals(b) tests.
     */
    PendingComparison key = new PendingComparison(value, otherValue);
    Comparison previous = pending.get(key);
    if (previous == null) {
      // Prevent the same comparison from being made
      pending.put(key, Comparison.PENDING);

      // Compare each property
      Map<String, Object> beanProperties = AutoBeanUtils.getAllProperties(value);
      Map<String, Object> otherProperties = AutoBeanUtils.getAllProperties(otherValue);
      for (Map.Entry<String, Object> entry : beanProperties.entrySet()) {
        Object property = entry.getValue();
        Object otherProperty = otherProperties.get(entry.getKey());
        if (!sameOrEquals(property, otherProperty, pending)) {
          pending.put(key, Comparison.FALSE);
          return false;
        }
      }
      pending.put(key, Comparison.TRUE);
      return true;
    } else {
      // Return true for TRUE or PENDING
      return !Comparison.FALSE.equals(previous);
    }
  }

  /**
   * Compare two collections by size, then by contents. List comparisons will
   * preserve order. All other collections will be treated with bag semantics.
   */
  private static boolean sameOrEquals(Collection<?> collection, Collection<?> otherCollection,
      Map<PendingComparison, Comparison> pending, Map<Object, Object> pairs) {
    if (collection.size() != otherCollection.size()) {
      return false;
    }

    if (collection instanceof List<?>) {
      // Lists we can simply iterate over
      Iterator<?> it = collection.iterator();
      Iterator<?> otherIt = otherCollection.iterator();
      while (it.hasNext()) {
        assert otherIt.hasNext();
        Object element = it.next();
        Object otherElement = otherIt.next();
        if (!sameOrEquals(element, otherElement, pending)) {
          return false;
        }
        if (pairs != null) {
          pairs.put(element, otherElement);
        }
      }
    } else {
      // Do an n*m comparison on any other collection type
      List<Object> values = new ArrayList<Object>(collection);
      List<Object> otherValues = new ArrayList<Object>(otherCollection);
      it : for (Iterator<Object> it = values.iterator(); it.hasNext();) {
        Object value = it.next();
        for (Iterator<Object> otherIt = otherValues.iterator(); otherIt.hasNext();) {
          Object otherValue = otherIt.next();
          if (sameOrEquals(value, otherValue, pending)) {
            if (pairs != null) {
              pairs.put(value, otherValue);
            }
            // If a match is found, remove both values from their lists
            it.remove();
            otherIt.remove();
            continue it;
          }
        }
        // A match for the value wasn't found
        return false;
      }
      assert values.isEmpty() && otherValues.isEmpty();
    }
    return true;
  }

  /**
   * Compare two Maps by size, and key-value pairs.
   */
  private static boolean sameOrEquals(Map<?, ?> map, Map<?, ?> otherMap,
      Map<PendingComparison, Comparison> pending) {
    if (map.size() != otherMap.size()) {
      return false;
    }
    Map<Object, Object> pairs = new IdentityHashMap<Object, Object>();
    if (!sameOrEquals(map.keySet(), otherMap.keySet(), pending, pairs)) {
      return false;
    }
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      Object otherValue = otherMap.get(pairs.get(entry.getKey()));
      if (!sameOrEquals(entry.getValue(), otherValue, pending)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Compare Splittables by kind and values.
   */
  private static boolean sameOrEquals(Splittable value, Splittable otherValue,
      Map<PendingComparison, Comparison> pending) {
    if (value == otherValue) {
      return true;
    }

    // Strings
    if (value.isString()) {
      if (!otherValue.isString()) {
        return false;
      }
      return value.asString().equals(otherValue.asString());
    }

    // Arrays
    if (value.isIndexed()) {
      if (!otherValue.isIndexed()) {
        return false;
      }

      if (value.size() != otherValue.size()) {
        return false;
      }

      for (int i = 0, j = value.size(); i < j; i++) {
        if (!sameOrEquals(value.get(i), otherValue.get(i), pending)) {
          return false;
        }
      }
      return true;
    }

    // Objects
    if (value.isKeyed()) {
      if (!otherValue.isKeyed()) {
        return false;
      }
      /*
       * We want to treat a missing property key as a null value, so we can't
       * just compare the key lists.
       */
      List<String> keys = value.getPropertyKeys();
      for (String key : keys) {
        if (value.isNull(key)) {
          // If value['foo'] is null, other['foo'] must also be null
          if (!otherValue.isNull(key)) {
            return false;
          }
        } else if (otherValue.isNull(key)
            || !sameOrEquals(value.get(key), otherValue.get(key), pending)) {
          return false;
        }
      }

      // Look at keys only in otherValue, and ensure nullness
      List<String> otherKeys = new ArrayList<String>(otherValue.getPropertyKeys());
      otherKeys.removeAll(keys);
      for (String key : otherKeys) {
        if (!value.isNull(key)) {
          return false;
        }
      }
      return true;
    }

    // Unexpected
    throw new UnsupportedOperationException("Splittable of unknown type");
  }

  /**
   * Utility class.
   */
  private AutoBeanUtils() {
  }
}
