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
import java.util.Map;

/**
 * A memory-efficient identity hash map.
 * 
 * @param <K> the key type
 * @param <V> the value type
 */
public class IdentityHashMap<K, V> extends HashMap<K, V> {

  public IdentityHashMap() {
  }

  public IdentityHashMap(Map<? extends K, ? extends V> m) {
    super(m);
  }

  @Override
  protected boolean keyEquals(Object a, Object b) {
    return a == b;
  }

  @Override
  protected int keyHashCode(Object k) {
    return System.identityHashCode(k);
  }

  @Override
  protected boolean valueEquals(Object a, Object b) {
    return a == b;
  }

  @Override
  protected int valueHashCode(Object k) {
    return System.identityHashCode(k);
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
