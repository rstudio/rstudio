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
package java.util;

/**
 * A set known to be in ascending order. <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/SortedSet.html">[Sun
 * docs]</a>
 * 
 * @param <E> element type.
 */
public interface SortedSet<E> extends Set<E> {

  Comparator<? super E> comparator();

  E first();

  SortedSet<E> headSet(E toElement);

  E last();

  SortedSet<E> subSet(E fromElement, E toElement);

  SortedSet<E> tailSet(E fromElement);
}
