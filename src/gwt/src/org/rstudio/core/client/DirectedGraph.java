/*
 * DirectedGraph.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.core.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DirectedGraph<K, V>
{
   public DirectedGraph()
   {
      this(null, null);
   }
   
   public DirectedGraph(V data)
   {
      this(null, data);
   }
   
   private DirectedGraph(DirectedGraph<K, V> parent, V data)
   {
      parent_ = parent;
      children_ = new HashMap<K, DirectedGraph<K, V>>();
      data_ = data;
   }
   
   public DirectedGraph<K, V> addChild(K key, V data)
   {
      DirectedGraph<K, V> node = new DirectedGraph<K, V>(this, data);
      children_.put(key, node);
      return node;
   }
   
   public boolean hasChild(K key)
   {
      return children_.containsKey(key);
   }
   
   public DirectedGraph<K, V> getChild(K key)
   {
      if (!hasChild(key))
         return addChild(key, null);
      return children_.get(key);
   }
   
   public DirectedGraph<K, V> findNode(K key)
   {
      List<K> keyList = new ArrayList<K>();
      return findNode(keyList);
   }
   
   public DirectedGraph<K, V> findNode(List<K> keyList)
   {
      DirectedGraph<K, V> node = this;
      for (int i = 0; i < keyList.size(); i++)
      {
         K key = keyList.get(i);
         if (!node.hasChild(key))
            return null;
         
         node = node.getChild(key);
      }
      
      return node;
   }
   
   public V getData() { return data_; }
   public void setData(V data) { data_ = data; }
   
   public boolean isRoot() { return parent_ == null; }

   private final DirectedGraph<K, V> parent_;
   private final Map<K, DirectedGraph<K, V>> children_;
   private V data_;
}
