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
package com.google.gwt.user.client.rpc.impl;

import com.google.gwt.dev.util.Pair;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;

/**
 * Provides access to reflection capability, but only when running from 
 * bytecode.
 */
public class ReflectionHelper {
  
  // Only used from single-threaded JS. Doesn't need to be thread-safe.
  private static class Cache<K, V extends AccessibleObject>    
      extends LinkedHashMap<K, V> {
    
    private static final int MAX_SIZE = 1024; 
    
    // These values lifted from defaults. 
    private static final int INITIAL_CAPACITY = 16;
    private static final float LOAD_FACTOR = 0.75f;
        
    public Cache() {
      super(INITIAL_CAPACITY, LOAD_FACTOR, true);
    }

    @Override
    protected boolean removeEldestEntry(
        java.util.Map.Entry<K, V> eldest) {
      return size() > MAX_SIZE;
    }
  }

  private static final Cache<Class<?>, Constructor<?>> constructorCache
      = new Cache<Class<?>, Constructor<?>>();

  private static final Cache<Pair<Class<?>,String>, Field> fieldCache
      = new Cache<Pair<Class<?>,String>, Field>();

  private static Field findField(Class<?> klass, String name) {
    Pair<Class<?>, String> key = Pair.<Class<?>,String>create(klass, name); 
    Field f = fieldCache.get(key);
    if (f == null) {
      try {
        f = klass.getDeclaredField(name);
      } catch (NoSuchFieldException ex) {
        throw new RuntimeException(
            "Unable to find field " + klass.getName() + "." + name, ex);
      }
      f.setAccessible(true);
      fieldCache.put(key, f);
    }
    return f;
  }
  
  /**
   * Gets the value of a field.
   */
  public static Object getField(Class<?> klass, Object obj, String name) {
    Field f = findField(klass, name);
    try {
      return f.get(obj);
    } catch (IllegalAccessException ex) {
      throw new RuntimeException("Unexpected failure", ex);
    }
  }

  /**
   * Loads {@code klass} using Class.forName.
   */
  public static Class<?> loadClass(String klass) {
    try {
      return Class.forName(klass);
    } catch (ClassNotFoundException ex) {
      throw new RuntimeException("Unable to find class " + klass, ex);
    }
  }

  /**
   * Creates a new instance of {@code klass}. The class must have a no-arg
   * constructor. The constructor may have any access modifier (for example,
   * private).
   */
  @SuppressWarnings("unchecked")
  public static <T> T newInstance(Class<T> klass) {
    Constructor<T> c = (Constructor<T>) constructorCache.get(klass);
    try {
      if (c == null) {
        c = klass.getDeclaredConstructor();
        c.setAccessible(true);
        constructorCache.put(klass, c);
      }
      return c.newInstance();
    } catch (Exception ex) {
      throw new RuntimeException("Unexpected failure", ex);
    }
  }

  /**
   * Sets the value of a field.
   */
  public static void setField(Class<?> klass, Object obj, String name,
      Object value) throws Exception {
    Field f = findField(klass, name);
    try {
      f.set(obj, value);
    } catch (IllegalAccessException ex) {
      throw new RuntimeException("Unexpected failure", ex);
    }
  }  
}
