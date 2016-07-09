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

import static javaemul.internal.InternalPreconditions.checkNotNull;

import java.util.function.UnaryOperator;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsType;

/**
 * Represents a sequence of objects.
 * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/List.html">
 * the official Java API doc</a> for details.
 *
 * @param <E> element type
 */
@JsType
public interface List<E> extends Collection<E> {

  @JsMethod(name = "addAtIndex")
  void add(int index, E element);

  @JsMethod(name = "addAllAtIndex")
  boolean addAll(int index, Collection<? extends E> c);

  @JsMethod(name = "getAtIndex")
  E get(int index);

  int indexOf(Object o);

  int lastIndexOf(Object o);

  @JsIgnore
  ListIterator<E> listIterator();

  @JsIgnore
  ListIterator<E> listIterator(int from);

  @JsMethod(name = "removeAtIndex")
  E remove(int index);

  @JsIgnore
  default void replaceAll(UnaryOperator<E> operator) {
    checkNotNull(operator);
    for (int i = 0, size = size(); i < size; i++) {
      set(i, operator.apply(get(i)));
    }
  }

  @JsMethod(name = "setAtIndex")
  E set(int index, E element);

  @JsIgnore
  @SuppressWarnings("unchecked")
  default void sort(Comparator<? super E> c) {
    Object[] a = toArray();
    Arrays.sort(a, (Comparator<Object>) c);
    for (int i = 0; i < a.length; i++) {
      set(i, (E) a[i]);
    }
  }

  @JsIgnore
  @Override
  default Spliterator<E> spliterator() {
    return Spliterators.spliterator(this, Spliterator.ORDERED);
  }

  List<E> subList(int fromIndex, int toIndex);
}
