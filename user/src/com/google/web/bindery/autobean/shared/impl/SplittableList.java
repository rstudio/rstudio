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

import java.util.AbstractList;

/**
 * A list implementation that lazily reifies its constituent elements.
 * 
 * @param <E> the element type
 */
public class SplittableList<E> extends AbstractList<E> implements HasSplittable {
  static <Q> Q reify(EncodeState state, Splittable data, int index, Coder coder) {
    if (data.isNull(index)) {
      return null;
    }
    @SuppressWarnings("unchecked")
    Q toReturn = (Q) coder.decode(state, data.get(index));
    data.setReified(String.valueOf(index), toReturn);
    return toReturn;
  }

  static void set(EncodeState state, Splittable data, int index, Coder coder, Object value) {
    data.setReified(String.valueOf(index), value);
    if (value == null) {
      Splittable.NULL.assign(data, index);
      return;
    }
    Splittable backing = coder.extractSplittable(state, value);
    if (backing == null) {
      /*
       * External data type, such as an ArrayList or a concrete implementation
       * of a setter's interface type. This means that a slow serialization pass
       * is necessary.
       */
      data.setReified(AbstractAutoBean.UNSPLITTABLE_VALUES_KEY, true);
    } else {
      backing.assign(data, index);
    }
  }

  private Splittable data;
  private final Coder elementCoder;
  private final EncodeState state;

  public SplittableList(Splittable data, Coder elementCoder, EncodeState state) {
    assert data.isIndexed() : "Expecting indexed data";
    this.data = data;
    this.elementCoder = elementCoder;
    this.state = state;
  }

  @Override
  public void add(int index, E element) {
    set(state, data, index, elementCoder, element);
  }

  @Override
  public E get(int index) {
    if (data.isReified(String.valueOf(index))) {
      @SuppressWarnings("unchecked")
      E toReturn = (E) data.getReified(String.valueOf(index));
      return toReturn;
    }
    // javac generics bug
    return SplittableList.<E> reify(state, data, index, elementCoder);
  }

  public Splittable getSplittable() {
    return data;
  }

  @Override
  public E remove(int index) {
    E toReturn = get(index);
    // XXX This is terrible, use Array.splice
    int newSize = data.size() - 1;
    for (int i = index; i < newSize; i++) {
      data.get(i + 1).assign(data, i);
      data.setReified(String.valueOf(i), data.getReified(String.valueOf(i + 1)));
    }
    data.setSize(newSize);
    return toReturn;
  }

  @Override
  public E set(int index, E element) {
    E previous = get(index);
    set(state, data, index, elementCoder, element);
    return previous;
  }

  @Override
  public int size() {
    return data.size();
  }
}
