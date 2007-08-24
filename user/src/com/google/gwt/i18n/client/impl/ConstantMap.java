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
package com.google.gwt.i18n.client.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Read only Map used when returning <code>Constants</code> maps. Preserves
 * order. ConstantMap should only be created or modified by GWT, as constant
 * maps are constructed using a very stereotyped algorithm, which allows
 * <code>ConstantMap</code> to maintain order with very little code. In
 * specific, no elements are every removed from them and all elements are added
 * before the first user operation.
 */
public class ConstantMap extends HashMap<String, String> {

  private static class DummyMapEntry implements Map.Entry<String, String> {
    private final String key;

    private final String value;

    DummyMapEntry(String key, String value) {
      this.key = key;
      this.value = value;
    }

    public String getKey() {
      return key;
    }

    public String getValue() {
      return value;
    }

    public String setValue(String arg0) {
      throw new UnsupportedOperationException();
    }
  }

  private class OrderedConstantSet<T> extends ArrayList<T> implements Set<T> {
    private class ImmutableIterator implements Iterator<T> {
      private final Iterator<T> base;

      ImmutableIterator(Iterator<T> base) {
        this.base = base;
      }

      public boolean hasNext() {
        return base.hasNext();
      }

      public T next() {
        return base.next();
      }

      public void remove() {
        throw new UnsupportedOperationException("Immutable set");
      }
    }

    @Override
    public void clear() {
      throw new UnsupportedOperationException("Immutable set");
    }

    @Override
    public Iterator<T> iterator() {
      Iterator<T> base = super.iterator();
      return new ImmutableIterator(base);
    }
  }

  private OrderedConstantSet<Map.Entry<String, String>> entries;

  private final OrderedConstantSet<String> keys = new OrderedConstantSet<String>();

  private OrderedConstantSet<String> values;

  @Override
  public void clear() {
    throw unsupported("clear");
  }

  @Override
  public Set<Map.Entry<String, String>> entrySet() {
    if (entries == null) {
      entries = new OrderedConstantSet<Map.Entry<String, String>>();
      for (int i = 0; i < keys.size(); i++) {
        String key = keys.get(i);
        String value = get(key);
        entries.add(new DummyMapEntry(key, value));
      }
    }
    return entries;
  }

  @Override
  public Set<String> keySet() {
    return keys;
  }

  @Override
  public String put(String key, String value) {
    // We may want to find a more efficient implementation later.
    boolean exists = keys.contains(key);
    if (!exists) {
      keys.add(key);
    }
    return super.put(key, value);
  }

  @Override
  public String remove(Object key) {
    throw unsupported("remove");
  }

  @Override
  public Collection<String> values() {
    if (values == null) {
      values = new OrderedConstantSet<String>();
      for (int i = 0; i < keys.size(); i++) {
        Object element = keys.get(i);
        values.add(this.get(element));
      }
    }
    return values;
  }

  private UnsupportedOperationException unsupported(String operation) {
    return new UnsupportedOperationException(operation
        + " not supported on a constant map");
  }
}
