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

import com.google.gwt.core.client.GwtScriptOnly;

/**
 * The script-mode equivalent for ReflectionHelper. This version throws
 * exceptions if used, because ReflectionHelper can only be used from bytecode.
 */
@GwtScriptOnly
public class ReflectionHelper {
  
  public static Object getField(Class<?> klass, Object obj, String name) {
    throw new RuntimeException("ReflectionHelper can't be used from web mode.");
  }

  /**
   * Loads {@code klass} using Class.forName.
   */
  public static Class<?> loadClass(String klass) {
    throw new RuntimeException("ReflectionHelper can't be used from web mode.");
  }

  /**
   * Creates a new instance of {@code klass}. The class must have a no-arg
   * constructor. The constructor may have any access modifier (for example,
   * private).
   */
  @SuppressWarnings("unchecked")
  public static <T> T newInstance(Class<T> klass) {
    throw new RuntimeException("ReflectionHelper can't be used from web mode.");
  }

  /**
   * Sets the value of a field.
   */
  public static void setField(Class<?> klass, Object obj, String name,
      Object value) {
    throw new RuntimeException("ReflectionHelper can't be used from web mode.");
  }
}
