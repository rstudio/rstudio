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
package com.google.gwt.benchmarks.client.impl;

import java.util.Arrays;

/**
 * Provides convenience methods for adapting various values to the Iterable
 * interface.
 * 
 */
public class IterableAdapter {

  /**
   * Returns an <code>Iterable</code> from an array.
   * 
   * @param array a not <code>null</code> array
   * @return an <code>Iterable</code> that wraps the array
   */
  public static <T> Iterable<T> toIterable(T[] array) {
    return Arrays.asList(array);
  }

  /**
   * Returns <code>iterable</code> as itself. Useful for code-gen situations.
   * 
   * @param iterable a maybe <code>null</code> <code>Iterable</code>
   * @return <code>iterable</code>
   */
  public static <T> Iterable<T> toIterable(Iterable<T> iterable) {
    return iterable;
  }
}
