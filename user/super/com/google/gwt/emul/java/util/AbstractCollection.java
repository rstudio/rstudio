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

import com.google.gwt.lang.Array;

/**
 * Skeletal implementation of the Collection interface. <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/AbstractCollection.html">[Sun
 * docs]</a>
 *
 * @param <E> the element type.
 *
 */
public abstract class AbstractCollection<E> implements Collection<E> {

  protected AbstractCollection() {
  }

  public boolean add(E o) {
    throw new UnsupportedOperationException("Add not supported on this collection");
  }

  public boolean addAll(Collection<? extends E> c) {
    Iterator<? extends E> iter = c.iterator();
    boolean changed = false;
    while (iter.hasNext()) {
      if (add(iter.next())) {
        changed = true;
      }
    }
    return changed;
  }

  public void clear() {
    Iterator<E> iter = iterator();
    while (iter.hasNext()) {
      iter.next();
      iter.remove();
    }
  }

  public boolean contains(Object o) {
    Iterator<E> iter = advanceToFind(iterator(), o);
    return iter == null ? false : true;
  }

  public boolean containsAll(Collection<?> c) {
    Iterator<?> iter = c.iterator();
    while (iter.hasNext()) {
      if (!contains(iter.next())) {
        return false;
      }
    }
    return true;
  }

  public boolean isEmpty() {
    return size() == 0;
  }

  public abstract Iterator<E> iterator();

  public boolean remove(Object o) {
    Iterator<E> iter = advanceToFind(iterator(), o);
    if (iter != null) {
      iter.remove();
      return true;
    } else {
      return false;
    }
  }

  public boolean removeAll(Collection<?> c) {
    Iterator<?> iter = iterator();
    boolean changed = false;
    while (iter.hasNext()) {
      if (c.contains(iter.next())) {
        iter.remove();
        changed = true;
      }
    }
    return changed;
  }

  public boolean retainAll(Collection<?> c) {
    Iterator<?> iter = iterator();
    boolean changed = false;
    while (iter.hasNext()) {
      if (!c.contains(iter.next())) {
        iter.remove();
        changed = true;
      }
    }
    return changed;
  }

  public abstract int size();

  public Object[] toArray() {
    return toArray(new Object[size()]);
  }

  public <T> T[] toArray(T[] a) {
    int size = size();
    if (a.length < size) {
      a = Array.createFrom(a, size);
    }
    Object[] result = a;
    Iterator<E> it = iterator();
    for (int i = 0; i < size; ++i) {
      result[i] = it.next();
    }
    if (a.length > size) {
      a[size] = null;
    }
    return a;
  }

  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();
    String comma = null;
    sb.append("[");
    Iterator<E> iter = iterator();
    while (iter.hasNext()) {
      if (comma != null) {
        sb.append(comma);
      } else {
        comma = ", ";
      }
      E value = iter.next();
      sb.append(value == this ? "(this Collection)" : String.valueOf(value));
    }
    sb.append("]");
    return sb.toString();
  }

  private <T> Iterator<T> advanceToFind(Iterator<T> iter, Object o) {
    while (iter.hasNext()) {
      T t = iter.next();
      if (o == null ? t == null : o.equals(t)) {
        return iter;
      }
    }
    return null;
  }
}
