/*
 * Copyright 2006 Google Inc.
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
 * Abstract base class for list implementations.
 */
public abstract class AbstractList extends AbstractCollection implements List {

  private final class IteratorImpl implements Iterator {

    int i = 0, last = -1;

    public boolean hasNext() {
      return i < size();
    }

    public Object next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      return get(last = i++);
    }

    public void remove() {
      if (last < 0) {
        throw new IllegalStateException();
      }
      AbstractList.this.remove(i - 1);
      --i;
      last = -1;
    }
  }

  public void add(int index, Object element) {
    throw new UnsupportedOperationException("add");
  }

  public boolean add(Object obj) {
    add(size(), obj);
    return true;
  }

  public boolean addAll(int index, Collection c) {
    Iterator iter = c.iterator();
    while (iter.hasNext()) {
      add(index, iter.next());
      ++index;
    }
    return !c.isEmpty();
  }

  public void clear() {
    removeRange(0, size());
  }

  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }

    if (!(o instanceof List)) {
      return false;
    }

    List other = (List) o;
    if (size() != other.size()) {
      return false;
    }

    Iterator iter = iterator();
    Iterator iterOther = other.iterator();

    while (iter.hasNext()) {
      Object elem = iter.next();
      Object elemOther = iterOther.next();

      if (!(elem == null ? elemOther == null : elem.equals(elemOther))) {
        return false;
      }
    }

    return true;
  }

  public abstract Object get(int index);

  public int hashCode() {
    int k = 1;
    final int coeff = 31;
    Iterator iter = iterator();
    while (iter.hasNext()) {
      Object obj = iter.next();
      k = coeff * k + (obj == null ? 0 : obj.hashCode());
    }
    return k;
  }

  public int indexOf(Object toFind) {
    for (int i = 0, n = size(); i < n; ++i) {
      if (toFind == null ? get(i) == null : toFind.equals(get(i))) {
        return i;
      }
    }
    return -1;
  }

  public Iterator iterator() {
    return new IteratorImpl();
  }

  public int lastIndexOf(Object toFind) {
    for (int i = size() - 1; i > -1; --i) {
      if (toFind == null ? get(i) == null : toFind.equals(get(i))) {
        return i;
      }
    }
    return -1;
  }

  public Object remove(int index) {
    throw new UnsupportedOperationException("remove");
  }

  public Object set(int index, Object element) {
    throw new UnsupportedOperationException("set");
  }

  protected void removeRange(int fromIndex, int endIndex) {
    throw new UnsupportedOperationException("removeRange");
  }

}
