/*
 * Copyright 2007 Google Inc.
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
package java.util;

/**
 * Hash table and linked-list implementation of the Set interface with
 * predictable iteration order. <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/LinkedHashSet.html">[Sun
 * docs]</a>
 * 
 * @param <E> element type.
 */
public class LinkedHashSet<E> extends HashSet<E> implements Set<E>, Cloneable {

  public LinkedHashSet() {
    super(new LinkedHashMap<E, Object>(16, .75f));
  }

  /**
   * @param c
   */
  public LinkedHashSet(Collection<? extends E> c) {
    super(new LinkedHashMap<E, Object>(16, .75f));
    addAll(c);
  }

  /**
   * @param initialCapacity
   */
  public LinkedHashSet(int initialCapacity) {
    super(new LinkedHashMap<E, Object>(initialCapacity, .75f));
  }

  /**
   * @param initialCapacity
   * @param loadFactor
   */
  public LinkedHashSet(int initialCapacity, float loadFactor) {
    super(new LinkedHashMap<E, Object>(initialCapacity, loadFactor));
  }

}
