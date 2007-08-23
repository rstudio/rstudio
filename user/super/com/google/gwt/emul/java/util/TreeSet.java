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
 * Implements a set using a TreeMap. <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/TreeSet.html">[Sun
 * docs]</a>
 * 
 * @param <E> element type.
 */
public class TreeSet<E> extends AbstractSet<E> implements SortedSet<E> {

  public TreeSet() {
    // TODO(jat): implement
    throw new UnsupportedOperationException("TreeSet not implemented");
  }

  public TreeSet(Collection<? extends E> c) {
    // TODO(jat): implement
    throw new UnsupportedOperationException("TreeSet not implemented");
  }

  public TreeSet(Comparator<? super E> c) {
    // TODO(jat): implement
    throw new UnsupportedOperationException("TreeSet not implemented");
  }

  public TreeSet(SortedSet<E> s) {
    // TODO(jat): implement
    throw new UnsupportedOperationException("TreeSet not implemented");
  }

  public Comparator<? super E> comparator() {
    // TODO(jat): implement
    return null;
  }

  public E first() {
    // TODO(jat): implement
    return null;
  }

  public SortedSet<E> headSet(E toElement) {
    // TODO(jat): implement
    return null;
  }

  @Override
  public Iterator<E> iterator() {
    // TODO(jat): implement
    return null;
  }

  public E last() {
    // TODO(jat): implement
    return null;
  }

  @Override
  public int size() {
    // TODO(jat): implement
    return 0;
  }

  public SortedSet<E> subSet(E fromElement, E toElement) {
    // TODO(jat): implement
    return null;
  }

  public SortedSet<E> tailSet(E fromElement) {
    // TODO(jat): implement
    return null;
  }

}
