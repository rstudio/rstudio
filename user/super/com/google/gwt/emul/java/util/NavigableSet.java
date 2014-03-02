/*
 * Copyright 2014 Google Inc.
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
 * A {@code SortedSet} with more flexible queries.
 *
 * @param <E> element type.
 */
public interface NavigableSet<E> extends SortedSet<E> {
  E ceiling(E e);

  Iterator<E> descendingIterator();

  NavigableSet<E> descendingSet();

  E floor(E e);

  NavigableSet<E> headSet(E toElement, boolean inclusive);

  E higher(E e);

  E lower(E e);

  E pollFirst();

  E pollLast();

  NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive);

  NavigableSet<E> tailSet(E fromElement, boolean inclusive);
}