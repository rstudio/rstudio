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
public class ConstantMap extends HashMap {

  private static class DummyMapEntry implements Map.Entry {
    private final Object key;

    private final Object value;

    DummyMapEntry(Object key, Object value) {
      this.key = key;
      this.value = value;
    }

    public Object getKey() {
      return key;
    }

    public Object getValue() {
      return value;
    }

    public Object setValue(Object arg0) {
      throw new UnsupportedOperationException();
    }
  }

  private class OrderedConstantSet extends ArrayList implements Set {
    private class ImmutableIterator implements Iterator {
      private final Iterator base;

      ImmutableIterator(Iterator base) {
        this.base = base;
      }

      public boolean hasNext() {
        return base.hasNext();
      }

      public Object next() {
        return base.next();
      }

      public void remove() {
        throw new UnsupportedOperationException("Immutable set");
      }
    }

    public void clear() {
      throw new UnsupportedOperationException("Immutable set");
    }

    public Iterator iterator() {
      Iterator base = super.iterator();
      return new ImmutableIterator(base);
    }
  }

  private OrderedConstantSet entries;

  private final OrderedConstantSet keys = new OrderedConstantSet();

  private OrderedConstantSet values;

  public void clear() {
    throw unsupported("clear");
  }

  public Set entrySet() {
    if (entries == null) {
      entries = new OrderedConstantSet();
      for (int i = 0; i < keys.size(); i++) {
        Object key = keys.get(i);
        Object value = get(key);
        entries.add(new DummyMapEntry(key, value));
      }
    }
    return entries;
  }

  public Set keySet() {
    return keys;
  }

  public Object put(Object key, Object value) {
    // We may want to find a more efficient implementation later.
    boolean exists = keys.contains(key);
    if (!exists) {
      keys.add(key);
    }
    return super.put(key, value);
  }

  public Object remove(Object key) {
    throw unsupported("remove");
  }

  public Collection values() {
    if (values == null) {
      values = new OrderedConstantSet();
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
