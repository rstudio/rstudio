/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.requestfactory.client.impl;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.impl.Property;

import java.util.AbstractSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Set backed by a JSON Array.
 */
class JsoSet<T> extends AbstractSet<T> implements JsoCollection {

  private RequestFactoryJsonImpl rf;

  private final JavaScriptObject array;

  private HashSet<Object> set = new HashSet<Object>();

  private JsoList<T> list;

  public JsoSet(RequestFactoryJsonImpl rf, JavaScriptObject array) {
    this.rf = rf;
    this.array = array;
    this.list = new JsoList<T>(rf, array);
  }

  @Override
  public boolean add(T t) {
    Object key = key(t);
    if (!set.contains(key)) {
      set.add(key);
      list.add(t);
      checkList();
      return true;
    }
    return false;
  }

  public JavaScriptObject asJso() {
    return list.asJso();
  }

  @Override
  public boolean contains(Object o) {
    return set.contains(key(o));
  }

  @Override
  public boolean isEmpty() {
    return set.isEmpty();
  }

  @Override
  public Iterator<T> iterator() {
    return list.iterator();
  }

  @Override
  public boolean remove(Object o) {
    Object key = key(o);
    if (set.remove(key)) {
      for (int i = 0, j = list.size(); i < j; i++) {
        if (key.equals(key(list.get(i)))) {
          list.remove(i);
          checkList();
          return true;
        }
      }
      assert false : "Should not reach here";
    }
    return false;
  }

  public void setDependencies(Property<?> property, ProxyImpl proxy) {
    list = new JsoList<T>(rf, array);
    list.setDependencies(property, proxy);
    for (T t : list) {
      set.add(key(t));
    }
    checkList();
  }

  @Override
  public int size() {
    return list.size();
  }

  private void checkList() {
    if (JsoSet.class.desiredAssertionStatus()) {
      assert set.size() == list.size() : "Size mismatch " + set.size() + " "
          + list.size();
      Set<Object> allKeys = new HashSet<Object>();
      for (T t : list) {
        allKeys.add(key(t));
      }
      assert set.equals(allKeys);
    }
  }

  private Object key(Object source) {
    if (source instanceof EntityProxy) {
      return ((EntityProxy) source).stableId();
    }
    return source;
  }
}
