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
package com.google.gwt.dev.jjs.ast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * A list of {@link Holder} objects.
 */
public class HolderList/* <T extends JNode> */implements List/* <T> */,
    JVisitable {

  private final class ListIt implements ListIterator/* <T> */ {

    private final ListIterator/* <Mutator<T>> */itImpl;
    private final ListIterator/* <T> */itPeer;

    private ListIt(ListIterator/* <Mutator<T>> */itImpl,
        ListIterator/* <T> */itPeer) {
      this.itImpl = itImpl;
      this.itPeer = itPeer;
    }

    public void add(Object o) {
      itImpl.add(new MutatorImpl());
      itPeer.add(o);
    }

    public boolean hasNext() {
      return itPeer.hasNext();
    }

    public boolean hasPrevious() {
      return itPeer.hasPrevious();
    }

    public Object next() {
      itImpl.next();
      return itPeer.next();
    }

    public int nextIndex() {
      return itPeer.nextIndex();
    }

    public Object previous() {
      itImpl.previous();
      return itPeer.previous();
    }

    public int previousIndex() {
      return itPeer.previousIndex();
    }

    public void remove() {
      itImpl.remove();
      itPeer.remove();
    }

    public void set(Object o) {
      itPeer.set(o);
    }
  }

  private class MutatorImpl extends Mutator {

    public JExpression get() {
      int pos = impl.indexOf(this);
      if (pos < 0) {
        throw new IndexOutOfBoundsException();
      }
      return (JExpression) HolderList.this.get(pos);
    }

    public void insertAfter(JExpression node) {
      int pos = impl.indexOf(this);
      if (pos < 0) {
        throw new IndexOutOfBoundsException();
      }
      HolderList.this.add(pos, node);
    }

    public void insertBefore(JExpression node) {
      int pos = impl.indexOf(this);
      if (pos < 0) {
        throw new IndexOutOfBoundsException();
      }
      HolderList.this.add(pos + 1, node);
    }

    public void remove() {
      int pos = impl.indexOf(this);
      if (pos < 0) {
        throw new IndexOutOfBoundsException();
      }
      HolderList.this.remove(pos);
    }

    public JExpression set(JExpression value) {
      int pos = impl.indexOf(this);
      if (pos < 0) {
        throw new IndexOutOfBoundsException();
      }
      return (JExpression) HolderList.this.set(pos, value);
    }
  }

  private final ArrayList/* <Mutator<T>> */impl = new ArrayList/* <Mutator<T>> */();
  private final ArrayList/* <T> */peer = new ArrayList/* <T> */();

  public void add(int index, Object element) {
    impl.add(index, new MutatorImpl());
    peer.add(index, element);
  }

  public boolean add(Object o) {
    impl.add(new MutatorImpl());
    return peer.add(o);
  }

  public boolean addAll(Collection/* <? extends T> */c) {
    boolean result = false;
    for (Iterator it = c.iterator(); it.hasNext();) {
      JNode item = (JNode) it.next();
      this.add(item);
      result = true;
    }
    return result;
  }

  public boolean addAll(int index, Collection/* <? extends T> */c) {
    boolean result = false;
    for (Iterator it = c.iterator(); it.hasNext();) {
      JNode item = (JNode) it.next();
      this.add(index++, item);
      result = true;
    }
    return result;
  }

  public void clear() {
    peer.clear();
    impl.clear();
  }

  public boolean contains(Object o) {
    return peer.contains(o);
  }

  public boolean containsAll(Collection/* <?> */c) {
    throw new UnsupportedOperationException();
  }

  public Object get(int index) {
    return peer.get(index);
  }

  public JExpression getExpr(int index) {
    return (JExpression) peer.get(index);
  }

  public Mutator getMutator(int index) {
    return (Mutator) impl.get(index);
  }

  public List/* <Mutator<T>> */getMutators() {
    return impl;
  }

  public int indexOf(Object o) {
    return peer.indexOf(o);
  }

  public boolean isEmpty() {
    return peer.isEmpty();
  }

  public Iterator/* <T> */iterator() {
    return new ListIt(impl.listIterator(), peer.listIterator());
  }

  public int lastIndexOf(Object o) {
    return peer.lastIndexOf(o);
  }

  public ListIterator/* <T> */listIterator() {
    return new ListIt(impl.listIterator(), peer.listIterator());
  }

  public ListIterator/* <T> */listIterator(int index) {
    return new ListIt(impl.listIterator(index), peer.listIterator(index));
  }

  public Object remove(int index) {
    impl.remove(index);
    return peer.remove(index);
  }

  public boolean remove(Object o) {
    int i = peer.indexOf(o);
    if (i < 0) {
      return false;
    }
    impl.remove(i);
    peer.remove(i);
    return true;
  }

  public boolean removeAll(Collection/* <?> */c) {
    throw new UnsupportedOperationException();
  }

  public boolean retainAll(Collection/* <?> */c) {
    throw new UnsupportedOperationException();
  }

  public Object set(int index, Object element) {
    return peer.set(index, element);
  }

  public int size() {
    return peer.size();
  }

  public List/* <T> */subList(int fromIndex, int toIndex) {
    throw new UnsupportedOperationException();
  }

  public Object[] toArray() {
    return peer.toArray();
  }

  public/* <T> */Object[] toArray(Object[] a) {
    return peer.toArray(a);
  }

  public void traverse(JVisitor visitor) {
    for (int i = 0, c = impl.size(); i < c; ++i) {
      JExpression value = (JExpression) peer.get(i);
      value.traverse(visitor, (Mutator) impl.get(i));
    }
  }

}
