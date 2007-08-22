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
 * Abstract parent of classes mapping keys to values.
 * 
 * @deprecated use @see{java.util.Map} instead.
 * @link http://java.sun.com/j2se/1.5.0/docs/api/java/util/Dictionary.html
 *
 * @param <K> key type.
 * @param <V> value type.
 */
@Deprecated
public abstract class Dictionary<K, V> {

  public Dictionary() {
  }
  
  public abstract Enumeration<V> elements();
  
  public abstract V get(Object key);
  
  public abstract boolean isEmpty();
  
  public abstract Enumeration<K> keys();
  
  public abstract V put(K key, V value);
  
  public abstract V remove(Object key);
  
  public abstract int size();
}
