/*
 * Copyright 2012 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.web.bindery.requestfactory.vm.impl;

import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Orders classes by assignability, with most-derived types ordered first, and then by name.
 */
class ClassComparator implements Comparator<String> {
  private static final Logger log = Logger.getLogger(ClassComparator.class.getName());

  private final ClassLoader resolveClassesWith;

  public ClassComparator(ClassLoader resolveClassesWith) {
    this.resolveClassesWith = resolveClassesWith;
  }

  @Override
  public int compare(String className1, String className2) {
    Class<?> class1 = forName(className1);
    Class<?> class2 = forName(className2);
    if (class1.equals(class2)) {
      return 0;
    } else if (class1.isAssignableFrom(class2)) {
      return 1;
    } else if (class2.isAssignableFrom(class1)) {
      return -1;
    }
    return className1.compareTo(className2);
  }

  private Class<?> forName(String name) {
    try {
      return Class.forName(name, false, resolveClassesWith);
    } catch (ClassNotFoundException e) {
      String msg = "Could not locate class " + name;
      log.log(Level.SEVERE, msg, e);
      throw new RuntimeException(msg, e);
    }
  }
};
