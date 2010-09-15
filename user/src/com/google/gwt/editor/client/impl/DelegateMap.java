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
package com.google.gwt.editor.client.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Allows fast traversal of an Editor hierarchy.
 */
public class DelegateMap {
  /**
   * 
   */
  public interface KeyMethod {
    Object key(Object object);
  }

  public static final KeyMethod IDENTITY = new KeyMethod() {
    public Object key(Object object) {
      return object;
    }
  };

  private final Map<Object, List<AbstractEditorDelegate<?, ?>>> map = new HashMap<Object, List<AbstractEditorDelegate<?, ?>>>();
  private final Map<String, List<AbstractEditorDelegate<?, ?>>> paths = new HashMap<String, List<AbstractEditorDelegate<?, ?>>>();
  private final KeyMethod keyMethod;

  public DelegateMap(KeyMethod key) {
    this.keyMethod = key;
  }

  public List<AbstractEditorDelegate<?, ?>> get(Object object) {
    Object key = keyMethod.key(object);
    return key == null ? null : map.get(key);
  }

  /**
   * Returns a list of Editors available at a particular absolute path.
   */
  public List<AbstractEditorDelegate<?, ?>> getPath(String path) {
    return paths.get(path);
  }

  /**
   * Accesses the delegate map without using the KeyMethod.
   */
  public List<AbstractEditorDelegate<?, ?>> getRaw(Object key) {
    return map.get(key);
  }

  public <T> void put(T object, AbstractEditorDelegate<T, ?> delegate) {
    {
      List<AbstractEditorDelegate<?, ?>> list = paths.get(delegate.getPath());
      if (list == null) {
        list = new ArrayList<AbstractEditorDelegate<?, ?>>();
        paths.put(delegate.getPath(), list);
      }
      list.add(delegate);
    }
    Object key = keyMethod.key(object);
    if (key == null) {
      return;
    }
    {
      List<AbstractEditorDelegate<?, ?>> list = map.get(key);
      if (list == null) {
        list = new ArrayList<AbstractEditorDelegate<?, ?>>();
        map.put(key, list);
      }
      list.add(delegate);
    }
  }
}