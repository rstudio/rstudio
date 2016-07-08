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

import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsType;

/**
 * General-purpose interface for storing collections of objects.
 * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/Collection.html">
 * the official Java API doc</a> for details.
 *
 * @param <E> element type
 */
@JsType
public interface Collection<E> extends Iterable<E> {

  boolean add(E o);

  boolean addAll(Collection<? extends E> c);

  void clear();

  boolean contains(Object o);

  boolean containsAll(Collection<?> c);

  boolean isEmpty();

  @JsIgnore
  default Stream<E> parallelStream() {
    // no parallelism in gwt
    return stream();
  }

  boolean remove(Object o);

  boolean removeAll(Collection<?> c);

  @JsIgnore
  default boolean removeIf(Predicate<? super E> filter) {
    checkNotNull(filter);
    boolean removed = false;
    for (Iterator<E> it = iterator(); it.hasNext();) {
      if (filter.test(it.next())) {
        it.remove();
        removed = true;
      }
    }
    return removed;
  }

  boolean retainAll(Collection<?> c);

  int size();

  @JsIgnore
  @Override
  default Spliterator<E> spliterator() {
    return Spliterators.spliterator(this, 0);
  }

  @JsIgnore
  default Stream<E> stream() {
    return StreamSupport.stream(spliterator(), false);
  }

  Object[] toArray();

  @JsIgnore
  <T> T[] toArray(T[] a);
}
