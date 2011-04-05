/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.i18n.server.impl;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * Reflection-oriented utilities for implementing the Message/etc API.
 */
public class ReflectionUtils {

  public static <A extends Annotation> A getAnnotation(Class<?> clazz,
      Class<A> annotClass, boolean forceInherit) {
    boolean inherited = forceInherit
        || annotClass.isAnnotationPresent(Inherited.class);
    LinkedList<Class<?>> workQueue = new LinkedList<Class<?>>();
    workQueue.add(clazz); 
    while (!workQueue.isEmpty()) {
      clazz = workQueue.removeFirst();
      A result = clazz.getAnnotation(annotClass);
      if (result != null || !inherited) {
        return result;
      }
      Class<?> superClass = clazz.getSuperclass();
      if (superClass != null) {
        workQueue.addLast(superClass);
      }
      workQueue.addAll(Arrays.asList((Class<?>[]) clazz.getInterfaces()));
    }
    return null;
  }
}
