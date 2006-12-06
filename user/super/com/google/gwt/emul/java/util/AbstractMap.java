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
 * Abstract base class for map implementations.
 */
public abstract class AbstractMap implements Map {

  private static final String MSG_CANNOT_MODIFY = "This map implementation does not support modification";

  public void clear() {
    entrySet().clear();
  }

  public boolean containsKey(Object key) {
    return implFindEntry(key, false) != null;
  }

  public boolean containsValue(Object value) {
    for (Iterator iter = entrySet().iterator(); iter.hasNext();) {
      Map.Entry entry = (Map.Entry) iter.next();
      Object v = entry.getValue();
      if (value == null ? v == null : value.equals(v)) {
        return true;
      }
    }
    return false;
  }

  public abstract Set entrySet();

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

  public Object get(Object key) {
    Map.Entry entry = implFindEntry(key, false);
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

  public Set keySet() {
    final Set entrySet = entrySet();
    return new AbstractSet() {
      public boolean contains(Object key) {
        return containsKey(key);
      }

      public Iterator iterator() {
        final Iterator outerIter = entrySet.iterator();
        return new Iterator() {
          public boolean hasNext() {
            return outerIter.hasNext();
          }

          public Object next() {
            Map.Entry entry = (Entry) outerIter.next();
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

  public Object put(Object key, Object value) {
    throw new UnsupportedOperationException(MSG_CANNOT_MODIFY);
  }

  public void putAll(Map t) {
    throw new UnsupportedOperationException(MSG_CANNOT_MODIFY);
  }

  public Object remove(Object key) {
    Map.Entry entry = implFindEntry(key, true);
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

  public Collection values() {
    final Set entrySet = entrySet();
    return new AbstractCollection() {
      public boolean contains(Object value) {
        return containsValue(value);
      }

      public Iterator iterator() {
        final Iterator outerIter = entrySet.iterator();
        return new Iterator() {
          public boolean hasNext() {
            return outerIter.hasNext();
          }

          public Object next() {
            Object value = ((Entry) outerIter.next()).getValue();
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

  private Map.Entry implFindEntry(Object key, boolean remove) {
    for (Iterator iter = entrySet().iterator(); iter.hasNext();) {
      Map.Entry entry = (Map.Entry) iter.next();
      Object k = entry.getKey();
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
