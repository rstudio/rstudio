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
 * Skeletal implementation for Map implementations.
 * 
 * @link http://java.sun.com/j2se/1.5.0/docs/api/java/util/AbstractMap.html
 * 
 * @param <K> the key type.
 * @param <V> the value type.
 */
public abstract class AbstractMap<K,V> implements Map<K,V> {

  private static final String MSG_CANNOT_MODIFY =
    "This map implementation does not support modification";

  public void clear() {
    entrySet().clear();
  }

  public boolean containsKey(Object key) {
    return implFindEntry(key, false) != null;
  }
  
  public boolean containsValue(Object value) {
    for (Iterator<Entry<K,V>> iter = entrySet().iterator(); iter.hasNext();) {
      Entry<K,V> entry = iter.next();
      V v = entry.getValue();
      if (value == null ? v == null : value.equals(v)) {
        return true;
      }
    }
    return false;
  }

  public abstract Set<Entry<K,V>> entrySet();

  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof Map)) {
      return false;
    }
    Map otherMap = ((Map) obj);
    Set keys = keySet();
    Set otherKeys = otherMap.keySet();
    if (!keys.equals(otherKeys)) {
      return false;
    }
    for (Iterator iter = keys.iterator(); iter.hasNext();) {
      Object key = iter.next();
      Object value = get(key);
      Object otherValue = otherMap.get(key);
      if (value == null ? otherValue != null : !value.equals(otherValue)) {
        return false;
      }
    }
    return true;
  }

  public V get(Object key) {
    Map.Entry<K,V> entry = implFindEntry(key, false);
    return (entry == null ? null : entry.getValue());
  }

  public int hashCode() {
    int hashCode = 0;
    for (Iterator iter = entrySet().iterator(); iter.hasNext();) {
      Map.Entry entry = (Map.Entry) iter.next();
      hashCode += entry.hashCode();
    }
    return hashCode;
  }

  public boolean isEmpty() {
    return size() == 0;
  }

  public Set<K> keySet() {
    final Set<Entry<K,V>> entrySet = entrySet();
    return new AbstractSet<K>() {
      public boolean contains(Object key) {
        return containsKey(key);
      }

      public Iterator<K> iterator() {
        final Iterator<Entry<K,V>> outerIter = entrySet.iterator();
        return new Iterator<K>() {
          public boolean hasNext() {
            return outerIter.hasNext();
          }

          public K next() {
            Map.Entry<K,V> entry = outerIter.next();
            return entry.getKey();
          }

          public void remove() {
            outerIter.remove();
          }
        };
      }

      public int size() {
        return entrySet.size();
      }
    };
  }

  public V put(K key, V value) {
    throw new UnsupportedOperationException(MSG_CANNOT_MODIFY);
  }

  public <OK extends K,OV extends V> void putAll(Map<OK,OV> t) {
    for (Iterator<Entry<OK,OV>> iter
        = t.entrySet().iterator(); iter.hasNext();) {
      Entry<OK,OV> e = iter.next();
      put(e.getKey(), e.getValue());
    }
  }

  public V remove(Object key) {
    Map.Entry<K,V> entry = implFindEntry(key, true);
    return (entry == null ? null : entry.getValue());
  }

  public int size() {
    return entrySet().size();
  }

  public String toString() {
    String s = "{";
    boolean comma = false;
    for (Iterator iter = entrySet().iterator(); iter.hasNext();) {
      Map.Entry entry = (Map.Entry) iter.next();
      if (comma) {
        s += ", ";
      } else {
        comma = true;
      }
      s += String.valueOf(entry.getKey());
      s += "=";
      s += String.valueOf(entry.getValue());
    }
    return s + "}";
  }

  public Collection<V> values() {
    final Set<Entry<K,V>> entrySet = entrySet();
    return new AbstractCollection<V>() {
      public boolean contains(Object value) {
        return containsValue(value);
      }

      public Iterator<V> iterator() {
        final Iterator<Entry<K,V>> outerIter = entrySet.iterator();
        return new Iterator<V>() {
          public boolean hasNext() {
            return outerIter.hasNext();
          }

          public V next() {
            V value = outerIter.next().getValue();
            return value;
          }

          public void remove() {
            outerIter.remove();
          }
        };
      }

      public int size() {
        return entrySet.size();
      }
    };
  }

  protected Object clone() throws CloneNotSupportedException {
    // TODO(jat): implement
    throw new CloneNotSupportedException("clone not supported");
  }

  private Entry<K,V> implFindEntry(Object key, boolean remove) {
    for (Iterator<Entry<K,V>> iter = entrySet().iterator(); iter.hasNext();) {
      Entry<K,V> entry = iter.next();
      K k = entry.getKey();
      if (key == null ? k == null : key.equals(k)) {
        if (remove) {
          iter.remove();
        }
        return entry;
      }
    }
    return null;
  }
}
