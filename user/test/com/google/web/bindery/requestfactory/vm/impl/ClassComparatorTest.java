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

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

/**
 * Tests class comparison used to merge {@link Deobfuscator}.
 */
public class ClassComparatorTest extends TestCase {
  public void testCompare() {
    List<Class<?>> classes = new ArrayList<Class<?>>();
    classes.add(String.class);
    classes.add(Integer.class);
    classes.add(Object.class);
    classes.add(CharSequence.class);
    classes.add(Object.class);
    classes.add(Number.class);
    classes.add(Long.class);
    classes.add(Number.class);

    // add class names to treeset
    ClassLoader classLoader = this.getClass().getClassLoader();
    TreeSet<String> orderedClasses = new TreeSet<String>(new ClassComparator(classLoader));
    for (Class<?> clazz : classes) {
      orderedClasses.add(clazz.getName());
    }

    // check ordering and duplication
    assertEquals(6, orderedClasses.size());
    Iterator<String> it = orderedClasses.iterator();
    assertEquals(Integer.class.getName(), it.next());
    assertEquals(Long.class.getName(), it.next());
    assertEquals(Number.class.getName(), it.next());
    assertEquals(String.class.getName(), it.next());
    assertEquals(CharSequence.class.getName(), it.next());
    assertEquals(Object.class.getName(), it.next());
  }
}
