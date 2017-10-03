// CHECKSTYLE_OFF: Copyrighted to Guava Authors.
/*
 * Copyright (C) 2015 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// CHECKSTYLE_ON

package java.util.concurrent.atomic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * GWT emulated version of {@link AtomicReferenceArray}.
 *
 * @param <V> the element type.
 */
public class AtomicReferenceArray<V> {

  private final List<V> values;

  public AtomicReferenceArray(V[] array) {
    values = new ArrayList<V>(Arrays.asList(array));
  }

  public AtomicReferenceArray(int length) {
    values = new ArrayList<V>(Collections.<V>nCopies(length, null));
  }

  public boolean compareAndSet(int i, V expect, V update) {
    if (values.get(i) == expect) {
      values.set(i, update);
      return true;
    }
    return false;
  }

  public V get(int i) {
    return values.get(i);
  }

  public V getAndSet(int i, V x) {
    return values.set(i, x);
  }

  public void lazySet(int i, V x) {
    values.set(i, x);
  }

  public int length() {
    return values.size();
  }

  public void set(int i, V x) {
    values.set(i, x);
  }

  public boolean weakCompareAndSet(int i, V expect, V update) {
    return compareAndSet(i, expect, update);
  }

  @Override
  public String toString() {
    return values.toString();
  }
}
