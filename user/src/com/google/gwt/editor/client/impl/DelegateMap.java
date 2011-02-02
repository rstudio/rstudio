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

import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorContext;
import com.google.gwt.editor.client.EditorDriver;
import com.google.gwt.editor.client.EditorVisitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Allows fast traversal of an Editor hierarchy.
 */
public class DelegateMap implements Iterable<AbstractEditorDelegate<?, ?>> {
  /**
   * Defines an equivalence relationship to allow objects with non-identity
   * equality to be used as data keys.
   */
  public interface KeyMethod {
    Object key(Object object);
  }

  private static class MapIterator implements
      Iterator<AbstractEditorDelegate<?, ?>> {
    private AbstractEditorDelegate<?, ?> next;
    private Iterator<AbstractEditorDelegate<?, ?>> list;
    private Iterator<List<AbstractEditorDelegate<?, ?>>> values;

    public MapIterator(DelegateMap map) {
      values = map.map.values().iterator();
      next();
    }

    public boolean hasNext() {
      return next != null;
    }

    public AbstractEditorDelegate<?, ?> next() {
      AbstractEditorDelegate<?, ?> toReturn = next;

      if (list != null && list.hasNext()) {
        // Simple case, just advance the pointer
        next = list.next();
      } else {
        // Uninitialized, or current list exhausted
        next = null;
        while (values.hasNext()) {
          // Find the next non-empty iterator
          list = values.next().iterator();
          if (list.hasNext()) {
            next = list.next();
            break;
          }
        }
      }
      return toReturn;
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  public static final KeyMethod IDENTITY = new KeyMethod() {
    public Object key(Object object) {
      return object;
    }
  };

  public static DelegateMap of(EditorDriver<?> driver, KeyMethod key) {
    final DelegateMap toReturn = new DelegateMap(key);
    driver.accept(new EditorVisitor() {
      @Override
      public <T> void endVisit(EditorContext<T> ctx) {
        toReturn.put(ctx.getAbsolutePath(), ctx.getEditor());
        @SuppressWarnings("unchecked")
        AbstractEditorDelegate<T, ?> delegate = (AbstractEditorDelegate<T, ?>) ctx.getEditorDelegate();
        if (delegate != null) {
          toReturn.put(delegate.getObject(), delegate);
        }
      }
    });
    return toReturn;
  }

  private final Map<Object, List<AbstractEditorDelegate<?, ?>>> map = new HashMap<Object, List<AbstractEditorDelegate<?, ?>>>();
  private final Map<String, List<AbstractEditorDelegate<?, ?>>> delegatesByPath = new HashMap<String, List<AbstractEditorDelegate<?, ?>>>();
  private final Map<String, List<Editor<?>>> editorsByPath = new HashMap<String, List<Editor<?>>>();

  private final KeyMethod keyMethod;

  DelegateMap(KeyMethod key) {
    this.keyMethod = key;
  }

  public List<AbstractEditorDelegate<?, ?>> get(Object object) {
    Object key = keyMethod.key(object);
    return key == null ? null : map.get(key);
  }

  /**
   * Returns a list of EditorDelegates available at a particular absolute path.
   */
  public List<AbstractEditorDelegate<?, ?>> getDelegatesByPath(String path) {
    return delegatesByPath.get(path);
  }

  /**
   * Returns a list of Editors available at a particular absolute path.
   */
  public List<Editor<?>> getEditorByPath(String path) {
    return editorsByPath.get(path);
  }

  /**
   * Accesses the delegate map without using the KeyMethod.
   */
  public List<AbstractEditorDelegate<?, ?>> getRaw(Object key) {
    return map.get(key);
  }

  public Iterator<AbstractEditorDelegate<?, ?>> iterator() {
    return new MapIterator(this);
  }

  <K, V> void add(Map<K, List<V>> map, K key, V value) {
    List<V> list = map.get(key);
    if (list == null) {
      list = new ArrayList<V>();
      map.put(key, list);
    }
    list.add(value);
  }

  <T> void put(String path, Editor<T> editor) {
    add(editorsByPath, path, editor);
  }

  <T> void put(T object, AbstractEditorDelegate<T, ?> delegate) {
    add(delegatesByPath, delegate.getPath(), delegate);

    Object key = keyMethod.key(object);
    if (key == null) {
      return;
    }

    add(map, key, delegate);
  }
}