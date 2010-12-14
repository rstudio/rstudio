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

import java.lang.reflect.Constructor;

/**
 * Provides access to reflection capability, but only when running from 
 * bytecode.
 */
public class ReflectionHelper {

  /**
   * Loads {@code klass} using Class.forName.
   */
  public static Class<?> loadClass(String klass) throws Exception {
    return Class.forName(klass);
  }

  /**
   * Creates a new instance of {@code klass}. The class must have a no-arg
   * constructor. The constructor may have any access modifier (for example,
   * private).
   */
  public static <T> T newInstance(Class<T> klass)
      throws Exception {
    Constructor<T> c = klass.getDeclaredConstructor();
    c.setAccessible(true);
    return c.newInstance();
  }  
}
