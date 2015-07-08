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
 * Represents a sequence of objects. <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/List.html">[Sun docs]</a>
 *
 * @param <E> element type
 */
public interface List<E> extends Collection<E> {

  @Override
  boolean add(E o);

  void add(int index, E element);

  @Override
  boolean addAll(Collection<? extends E> c);

  boolean addAll(int index, Collection<? extends E> c);

  @Override
  void clear();

  @Override
  boolean contains(Object o);

  @Override
  boolean containsAll(Collection<?> c);

  @Override
  boolean equals(Object o);

  E get(int index);

  @Override
  int hashCode();

  int indexOf(Object o);

  @Override
  boolean isEmpty();

  @Override
  Iterator<E> iterator();

  int lastIndexOf(Object o);

  ListIterator<E> listIterator();

  ListIterator<E> listIterator(int from);

  E remove(int index);

  @Override
  boolean remove(Object o);

  @Override
  boolean removeAll(Collection<?> c);

  @Override
  boolean retainAll(Collection<?> c);

  E set(int index, E element);

  @Override
  int size();

  List<E> subList(int fromIndex, int toIndex);

  @Override
  Object[] toArray();

  @Override
  <T> T[] toArray(T[] array);

}
