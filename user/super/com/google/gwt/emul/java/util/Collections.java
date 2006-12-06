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
 * Utility methods that operate on collections.
 */
public class Collections {

  public static final Set EMPTY_SET = new HashSet();
  public static final Map EMPTY_MAP = new HashMap();
  public static final List EMPTY_LIST = new ArrayList();

  public static void reverse(List l) {
    int lastPos = l.size() - 1;
    for (int i = 0; i < l.size() / 2; i++) {
      Object element = l.get(i);
      int swapPos = lastPos - i;
      assert (swapPos > i);
      Object swappedWith = l.get(swapPos);
      l.set(i, swappedWith);
      l.set(swapPos, element);
    }
  }

  public static void sort(List target) {
    Object[] x = target.toArray();
    Arrays.sort(x);
    replaceContents(target, x);
  }

  public static void sort(List target, Comparator y) {
    Object[] x = target.toArray();
    Arrays.sort(x, y);
    replaceContents(target, x);
  }

  private static void replaceContents(List target, Object[] x) {
    int size = target.size();
    assert (x.length == size);
    for (int i = 0; i < size; i++) {
      target.set(i, x[i]);
    }
  }
}
