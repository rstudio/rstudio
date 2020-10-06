/*
 * SafeMap.java
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.container;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

// A class similar in spirit to java.util.HashMap, but enforces
// type safety on the various methods (ie, no Object allowed).
public class SafeMap<K, V>
{
   public SafeMap()
   {
      data_ = new HashMap<K, V>();
   }

   public V get(K key)
   {
      if (!data_.containsKey(key))
         return null;
      return data_.get(key);
   }

   public void put(K key, V value)
   {
      data_.put(key, value);
   }

   public void remove(K key)
   {
      data_.remove(key);
   }

   public void clear()
   {
      data_.clear();
   }

   public boolean isEmpty()
   {
      return data_.isEmpty();
   }

   public int size()
   {
      return data_.size();
   }

   public boolean containsKey(K key)
   {
      return data_.containsKey(key);
   }

   public boolean containsValue(V value)
   {
      return data_.containsValue(value);
   }

   public Set<Entry<K, V>> entrySet()
   {
      return data_.entrySet();
   }

   private final Map<K, V> data_;
}
