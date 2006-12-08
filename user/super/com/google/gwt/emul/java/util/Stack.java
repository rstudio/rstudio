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
 * Maintains a last-in, first-out collection of objects.
 */
public class Stack extends Vector {

  public Object clone() {
    Stack s = new Stack();
    s.addAll(this);
    return s;
  }

  public boolean empty() {
    return isEmpty();
  }

  public Object peek() {
    int sz = size();
    if (sz > 0) {
      return get(sz - 1);
    } else {
      throw new EmptyStackException();
    }
  }

  public Object pop() {
    int sz = size();
    if (sz > 0) {
      return remove(sz - 1);
    } else {
      throw new EmptyStackException();
    }
  }

  public Object push(Object o) {
    add(o);
    return o;
  }

  public int search(Object o) {
    for (int i = 0, n = size(); i < n; ++i) {
      Object other = get(i);
      if (o == null ? other == null : o.equals(other)) {
        return n - i;
      }
    }
    return -1;
  }

}
