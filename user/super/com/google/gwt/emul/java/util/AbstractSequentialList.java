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

/**
 * Skeletal implementation of the List interface. <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/AbstractSequentialList.html">[Sun
 * docs]</a>
 * 
 * @param <E> element type.
 */
public abstract class AbstractSequentialList<E> extends AbstractList<E> {

  // Should not be instantiated directly.
  protected AbstractSequentialList() {
  }

  @Override
  public void add(int index, E element) {
    ListIterator<E> iter = listIterator(index);
    iter.add(element);
  }

  @Override
  public boolean addAll(int index, Collection<? extends E> c) {
    checkNotNull(c);

    boolean modified = false;
    ListIterator<E> iter = listIterator(index);
    for (E e : c) {
      iter.add(e);
      modified = true;
    }
    return modified;
  }

  @Override
  public E get(int index) {
    ListIterator<E> iter = listIterator(index);
    try {
      return iter.next();
    } catch (NoSuchElementException e) {
      throw new IndexOutOfBoundsException("Can't get element " + index);
    }
  }

  @Override
  public Iterator<E> iterator() {
    return listIterator();
  }

  @Override
  public abstract ListIterator<E> listIterator(int index);

  @Override
  public E remove(int index) {
    ListIterator<E> iter = listIterator(index);
    try {
      E old = iter.next();
      iter.remove();
      return old;
    } catch (NoSuchElementException e) {
      throw new IndexOutOfBoundsException("Can't remove element " + index);
    }
  }

  @Override
  public E set(int index, E element) {
    ListIterator<E> iter = listIterator(index);
    try {
      E old = iter.next();
      iter.set(element);
      return old;
    } catch (NoSuchElementException e) {
      throw new IndexOutOfBoundsException("Can't set element " + index);
    }
  }

  @Override
  public abstract int size();

}
