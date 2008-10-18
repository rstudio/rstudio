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
 * Maintains a last-in, first-out collection of objects. <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/Stack.html">[Sun
 * docs]</a>
 * 
 * @param <E> element type.
 */
public class Stack<E> extends Vector<E> {

  @Override
  public Object clone() {
    Stack<E> s = new Stack<E>();
    s.addAll(this);
    return s;
  }

  public boolean empty() {
    return isEmpty();
  }

  public E peek() {
    int sz = size();
    if (sz > 0) {
      return get(sz - 1);
    } else {
      throw new EmptyStackException();
    }
  }

  public E pop() {
    int sz = size();
    if (sz > 0) {
      return remove(sz - 1);
    } else {
      throw new EmptyStackException();
    }
  }

  public E push(E o) {
    add(o);
    return o;
  }

  public int search(Object o) {
    int pos = lastIndexOf(o);
    if (pos >= 0) {
      return size() - pos;
    }
    return -1;
  }

}
