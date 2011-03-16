/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.dev.util.collect;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;

/**
 * A memory-efficient identity hash set.
 * 
 * @param <E> the element type
 */
public class IdentityHashSet<E> extends HashSet<E> {
  public IdentityHashSet() {
  }

  public IdentityHashSet(Collection<? extends E> c) {
    super(c);
  }

  /**
   * Works just like {@link #HashSet(Collection)}, but for arrays. Used to avoid
   * having to synthesize a collection in {@link IdentitySets}.
   */
  IdentityHashSet(E[] c) {
    super(c);
  }

  @Override
  protected boolean itemEquals(Object a, Object b) {
    return a == b;
  }

  @Override
  protected int itemHashCode(Object o) {
    return System.identityHashCode(o);
  }

  private void readObject(ObjectInputStream in) throws IOException,
      ClassNotFoundException {
    in.defaultReadObject();
    doReadObject(in);
  }

  private void writeObject(ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
    doWriteObject(out);
  }
}
