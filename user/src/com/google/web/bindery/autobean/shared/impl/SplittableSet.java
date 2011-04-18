/*
 * Copyright 2011 Google Inc.
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
package com.google.web.bindery.autobean.shared.impl;

import com.google.web.bindery.autobean.shared.Splittable;
import com.google.web.bindery.autobean.shared.impl.AutoBeanCodexImpl.Coder;
import com.google.web.bindery.autobean.shared.impl.AutoBeanCodexImpl.EncodeState;

import java.util.AbstractSet;
import java.util.Iterator;

/**
 * This type is optimized for the read-only case and has {@code O(n)} insertion
 * / lookup performance since computing hashcodes for the elements would require
 * up-front reification.
 * 
 * @param <E> the element type
 */
public class SplittableSet<E> extends AbstractSet<E> implements HasSplittable {
  private SplittableList<E> data;

  public SplittableSet(Splittable data, Coder elementCoder, EncodeState state) {
    this.data = new SplittableList<E>(data, elementCoder, state);
  }

  @Override
  public boolean add(E e) {
    if (!data.contains(e)) {
      data.add(e);
      return true;
    }
    return false;
  }

  @Override
  public void clear() {
    data.clear();
  }

  public Splittable getSplittable() {
    return data.getSplittable();
  }

  @Override
  public Iterator<E> iterator() {
    return data.iterator();
  }

  @Override
  public boolean remove(Object o) {
    return data.remove(o);
  }

  @Override
  public int size() {
    return data.size();
  }
}
