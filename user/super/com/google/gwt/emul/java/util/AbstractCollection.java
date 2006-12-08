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
 * Abstract base class for collection implementations.
 */
public abstract class AbstractCollection implements Collection {

  public boolean add(Object o) {
    throw new UnsupportedOperationException("add");
  }

  public boolean addAll(Collection c) {
    Iterator iter = c.iterator();
    boolean changed = false;
    while (iter.hasNext()) {
      if (add(iter.next())) {
        changed = true;
      }
    }
    return changed;
  }

  public void clear() {
    Iterator iter = iterator();
    while (iter.hasNext()) {
      iter.next();
      iter.remove();
    }
  }

  public boolean contains(Object o) {
    Iterator iter = advanceToFind(iterator(), o);
    return iter == null ? false : true;
  }

  public boolean containsAll(Collection c) {
    Iterator iter = c.iterator();
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

  public abstract Iterator iterator();

  public boolean remove(Object o) {
    Iterator iter = advanceToFind(iterator(), o);
    if (iter != null) {
      iter.remove();
      return true;
    } else {
      return false;
    }
  }

  public boolean removeAll(Collection c) {
    Iterator iter = c.iterator();
    boolean changed = false;
    while (iter.hasNext()) {
      if (remove(iter.next())) {
        changed = true;
      }
    }
    return changed;
  }

  public boolean retainAll(Collection c) {
    Iterator iter = iterator();
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
    int n = size();
    int i = 0;
    Object[] array = new Object[n];
    for (Iterator iter = iterator(); iter.hasNext();) {
      Object o = iter.next();
      array[i++] = o;
    }
    return array;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    String comma = null;
    sb.append("[");
    Iterator iter = iterator();
    while (iter.hasNext()) {
      if (comma != null) {
        sb.append(comma);
      } else {
        comma = ", ";
      }
      sb.append(String.valueOf(iter.next()));
    }
    sb.append("]");
    return sb.toString();
  }

  private Iterator advanceToFind(Iterator iter, Object o) {
    while (iter.hasNext()) {
      Object t = iter.next();
      if (o == null ? t == null : o.equals(t)) {
        return iter;
      }
    }
    return null;
  }
}
