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
 * Uses Java 1.5 ListIterator for documentation. The methods hasNext, next, and
 * remove are repeated to allow the specialized ListIterator documentation to be
 * associated with them. <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/ListIterator.html">[Sun
 * docs]</a>
 *
 * @param <E> element type.
 */
public interface ListIterator<E> extends Iterator<E> {

  void add(E o);

  @Override
  boolean hasNext();

  boolean hasPrevious();

  @Override
  E next();

  int nextIndex();

  E previous();

  int previousIndex();

  @Override
  void remove();

  void set(E o);
}
