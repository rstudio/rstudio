/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.dev.util.xml;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Retains parsed information about a particular schema clas.
 */
public class HandlerClassInfo {
  private static final HandlerMethod[] EMPTY_ARRAY_HANDLERMETHOD = new HandlerMethod[0];
  private static Map<Class<?>,HandlerClassInfo> sClassInfoMap =
    new HashMap<Class<?>,HandlerClassInfo>();

  public static synchronized HandlerClassInfo getClassInfo(Class<?> c) {
    if (sClassInfoMap.containsKey(c)) {
      return sClassInfoMap.get(c);
    } else {
      throw new RuntimeException("The schema class '" + c.getName()
          + "' should have been registered prior to parsing");
    }
  }

  public static synchronized void registerClass(Class<?> c) {
    if (sClassInfoMap.containsKey(c)) {
      return;
    }

    // Put a guard null in so that recursive registration of the same
    // class won't die.
    //
    sClassInfoMap.put(c, null);
    HandlerClassInfo classInfo = createClassInfo(c);
    sClassInfoMap.put(c, classInfo);
  }

  private static HandlerClassInfo createClassInfo(Class<?> c) {
    Map<String,HandlerMethod> namedHandlerMethods = new HashMap<String,HandlerMethod>();
    try {
      loadClassInfoRecursive(namedHandlerMethods, c);
    } catch (Exception e) {
      throw new RuntimeException("Unable to use class '" + c.getName()
          + "' as a handler", e);
    }
    HandlerClassInfo classInfo = new HandlerClassInfo(namedHandlerMethods);
    return classInfo;
  }

  private static void loadClassInfoRecursive(Map<String,HandlerMethod> namedHandlerMethods,
      Class<?> c) {
    if (!Schema.class.isAssignableFrom(c)) {
      // Have gone up as far as we can go.
      //
      return;
    }

    Method[] methods = c.getDeclaredMethods();
    for (int i = 0, n = methods.length; i < n; ++i) {
      Method method = methods[i];
      HandlerMethod handlerMethod = HandlerMethod.tryCreate(method);
      if (handlerMethod != null) {
        // Put in the map, but only if that method isn't already there.
        // (Allows inheritance where most-derived class wins).
        //
        String name = method.getName();
        if (!namedHandlerMethods.containsKey(name)) {
          namedHandlerMethods.put(name, handlerMethod);
        }
      }
    }

    // Recurse into superclass.
    //
    Class<?> superclass = c.getSuperclass();
    if (superclass != null) {
      loadClassInfoRecursive(namedHandlerMethods, superclass);
    }
  }

  private final Map<String,HandlerMethod> namedHandlerMethods;

  // Nobody else can create one.
  private HandlerClassInfo(Map<String,HandlerMethod> namedHandlerMethods) {
    this.namedHandlerMethods = namedHandlerMethods;
  }

  public HandlerMethod getEndMethod(String localName) {
    String methodName = "__" + localName.replace('-', '_');
    return namedHandlerMethods.get(methodName + "_end");
  }

  public HandlerMethod[] getHandlerMethods() {
    return namedHandlerMethods.values().toArray(EMPTY_ARRAY_HANDLERMETHOD);
  }

  public HandlerMethod getStartMethod(String localName) {
    String methodName = "__" + localName.replace('-', '_');
    return namedHandlerMethods.get(methodName + "_begin");
  }

  public HandlerMethod getTextMethod() {
    return namedHandlerMethods.get("__text");
  }
}
