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
package com.google.gwt.validation.rebind;

import com.google.gwt.thirdparty.guava.common.base.Function;
import com.google.gwt.thirdparty.guava.common.base.Functions;
import com.google.gwt.thirdparty.guava.common.base.Predicate;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableList;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableSet;
import com.google.gwt.thirdparty.guava.common.collect.Iterables;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * <strong>EXPERIMENTAL</strong> and subject to change. Do not use this in
 * production code.
 * <p>
 * Static utilities for the validation rebind package.
 */
final class Util {

  /**
   * Creates a Predicate that returns false if source contains an associated
   * class that is a super type of the class associated with the tested T.
   *
   * @param <T> the type to test
   * @param source the set of <T> to look for class matches.
   * @param toClass Function from T to Class
   * @return newly create predicate.
   */
  static <T> Predicate<T> createMostSpecificMatchPredicate(
      final Iterable<T> source, final Function<T, Class<?>> toClass) {
    return new Predicate<T>() {

      public boolean apply(T input) {
        Class<?> inputClass = toClass.apply(input);
        for (Class<?> match : Iterables.transform(source, toClass)) {
          if (!inputClass.equals(match) && inputClass.isAssignableFrom(match)) {
            return false;
          }
        }
        return true;
      }
    };
  }

  /**
   * Selects first only the classes that are assignable from the target, and
   * then returns the most specific matching classes.
   *
   * @param target the Class to match
   * @param availableClasses classes to search
   * @return Set of only the most specific classes that match the target.
   */
  static Set<Class<?>> findBestMatches(Class<?> target,
      Set<Class<?>> availableClasses) {
    Set<Class<?>> matches = new HashSet<Class<?>>();
    if (availableClasses.contains(target)) {
      return ImmutableSet.<Class<?>> of(target);
    } else {
      for (Class<?> clazz : availableClasses) {
        if (clazz.isAssignableFrom(target)) {
          matches.add(clazz);
        }
      }
    }
    Predicate<Class<?>> moreSpecificClassPredicate = createMostSpecificMatchPredicate(
        matches, Functions.<Class<?>> identity());
    return Sets.filter(matches, moreSpecificClassPredicate);
  }

  /**
   * Returns a Immutable List sorted with the most specific associated class
   * first. Each element is guaranteed to not be assignable to any element that
   * appears before it in the list.
   */
  static <T> ImmutableList<T> sortMostSpecificFirst(Iterable<T> classes,
      Function<T, Class<?>> toClass) {
    List<T> working = Lists.newArrayList();
    // strip duplicates
    for (T t : classes) {
      if (!working.contains(t)) {
        working.add(t);
      }
    }
    List<T> sorted = Lists.newArrayList();
    Predicate<T> mostSpecific = createMostSpecificMatchPredicate(working,
        toClass);
    boolean changed = false;
    do {
      changed = false;
      for (Iterator<T> iterator = working.iterator(); iterator.hasNext();) {
        T t = iterator.next();
        if (mostSpecific.apply(t)) {
          sorted.add(t);
          iterator.remove();
          changed = true;
        }
      }
    } while (changed);
    if (!working.isEmpty()) {
      throw new IllegalStateException(
          "Unable to find a element that does not have a more specific element in the set "
              + working);
    }
    return ImmutableList.copyOf(sorted);
  }

  private Util() {
  }
}
