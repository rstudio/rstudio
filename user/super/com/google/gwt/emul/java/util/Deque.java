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
 * A collection designed for holding elements prior to processing. <a
 * href="http://docs.oracle.com/javase/6/docs/api/java/util/Deque.html">Deque</a>
 *
 * @param <E> element type.
 */
public interface Deque<E> extends Queue<E> {

  void addFirst(E e);

  void addLast(E e);

  Iterator<E> descendingIterator();

  E getFirst();

  E getLast();

  boolean offerFirst(E e);

  boolean offerLast(E e);

  E peekFirst();

  E peekLast();

  E pollFirst();

  E pollLast();

  E pop();

  void push(E e);

  E removeFirst();

  boolean removeFirstOccurrence(Object o);

  E removeLast();

  boolean removeLastOccurrence(Object o);

}
