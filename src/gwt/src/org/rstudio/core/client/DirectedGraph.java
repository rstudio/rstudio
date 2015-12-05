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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DirectedGraph<K, V>
{
   public DirectedGraph()
   {
      this(null, null, null);
   }
   
   public DirectedGraph(V value)
   {
      this(null, null, value);
   }
   
   private DirectedGraph(DirectedGraph<K, V> parent, K key, V value)
   {
      parent_ = parent;
      children_ = new HashMap<K, DirectedGraph<K, V>>();
      
      key_ = key;
      value_ = value;
   }
   
   public DirectedGraph<K, V> ensureNode(K key)
   {
      return ensureChild(key);
   }
   
   public DirectedGraph<K, V> ensureNode(Collection<K> keys)
   {
      DirectedGraph<K, V> node = this;
      for (K key : keys)
         node = node.ensureChild(key);
      
      return node;
   }
   
   public DirectedGraph<K, V> findNode(K key)
   {
      return getChild(key);
   }
   
   public DirectedGraph<K, V> findNode(Collection<K> keys)
   {
      DirectedGraph<K, V> node = this;
      for (K key : keys)
      {
         if (!node.hasChild(key))
            return null;
         
         node = node.getChild(key);
      }
      
      return node;
   }
   
   private DirectedGraph<K, V> addChild(K key)
   {
      DirectedGraph<K, V> child = new DirectedGraph<K, V>(this, key, null);
      children_.put(key, child);
      return child;
   }
   
   public DirectedGraph<K, V> getChild(K key)
   {
      return children_.get(key);
   }
   
   public boolean hasChild(K key)
   {
      return children_.containsKey(key);
   }
   
   private DirectedGraph<K, V> ensureChild(K key)
   {
      if (hasChild(key))
         return getChild(key);
      else
         return addChild(key);
   }
   
   public List<Pair<List<K>, V>> flatten()
   {
      List<Pair<List<K>, V>> list = new ArrayList<Pair<List<K>, V>>();
      List<K> keys = new ArrayList<K>();
      fillRecursive(list, keys, this);
      return list;
   }
   
   private static <K, V> void fillRecursive(List<Pair<List<K>, V>> list,
                                            List<K> keys,
                                            DirectedGraph<K, V> node)
   {
      keys.add(node.getKey());
      list.add(new Pair<List<K>, V>(new ArrayList<K>(keys), node.getValue()));
      
      for (Map.Entry<K, DirectedGraph<K, V>> entry : node.getChildren().entrySet())
         fillRecursive(list, keys, entry.getValue());
   }
   
   public List<K> getKeyChain()
   {
      List<K> chain = new ArrayList<K>();
      DirectedGraph<K, V> node = this;
      while (!node.isRoot())
      {
         chain.add(node.getKey());
         node = node.getParent();
      }
      
      Collections.reverse(chain);
      return chain;
   }
   
   public V getValue() { return value_; }
   public void setValue(V value) { value_ = value; }
   
   public K getKey() { return key_; }
   
   public DirectedGraph<K, V> getParent() { return parent_; }
   public boolean isRoot() { return parent_ == null; }

   private Map<K, DirectedGraph<K, V>> getChildren() { return children_; }
   
   private final DirectedGraph<K, V> parent_;
   private final Map<K, DirectedGraph<K, V>> children_;
   
   private final K key_;
   private V value_;
}
