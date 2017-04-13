/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.emultest;

import com.google.gwt.emultest.java.util.ArrayDequeTest;
import com.google.gwt.emultest.java.util.ArrayListTest;
import com.google.gwt.emultest.java.util.ArraysTest;
import com.google.gwt.emultest.java.util.BitSetTest;
import com.google.gwt.emultest.java.util.CollectionsTest;
import com.google.gwt.emultest.java.util.ComparatorTest;
import com.google.gwt.emultest.java.util.DateTest;
import com.google.gwt.emultest.java.util.EnumMapTest;
import com.google.gwt.emultest.java.util.EnumSetTest;
import com.google.gwt.emultest.java.util.HashMapSmokeTest;
import com.google.gwt.emultest.java.util.HashMapTest;
import com.google.gwt.emultest.java.util.HashSetTest;
import com.google.gwt.emultest.java.util.IdentityHashMapTest;
import com.google.gwt.emultest.java.util.LinkedHashMapTest;
import com.google.gwt.emultest.java.util.LinkedHashSetTest;
import com.google.gwt.emultest.java.util.LinkedListTest;
import com.google.gwt.emultest.java.util.ObjectsTest;
import com.google.gwt.emultest.java.util.PriorityQueueTest;
import com.google.gwt.emultest.java.util.RandomTest;
import com.google.gwt.emultest.java.util.StackTest;
import com.google.gwt.emultest.java.util.TreeMapIntegerDoubleTest;
import com.google.gwt.emultest.java.util.TreeMapIntegerDoubleWithComparatorTest;
import com.google.gwt.emultest.java.util.TreeMapStringStringTest;
import com.google.gwt.emultest.java.util.TreeMapStringStringWithComparatorTest;
import com.google.gwt.emultest.java.util.TreeSetIntegerTest;
import com.google.gwt.emultest.java.util.TreeSetIntegerWithComparatorTest;
import com.google.gwt.emultest.java.util.VectorTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/** Test JRE Collections emulation. */
@RunWith(Suite.class)
@SuiteClasses({
  ArrayDequeTest.class,
  ArrayListTest.class,
  ArraysTest.class,
  BitSetTest.class,
  CollectionsTest.class,
  ComparatorTest.class,
  DateTest.class,
  EnumMapTest.class,
  EnumSetTest.class,
  HashMapTest.class,
  HashSetTest.class,
  IdentityHashMapTest.class,
  LinkedHashMapTest.class,
  LinkedHashSetTest.class,
  LinkedListTest.class,
  ObjectsTest.class,
  PriorityQueueTest.class,
  RandomTest.class,
  StackTest.class,
  VectorTest.class,
  TreeMapStringStringTest.class,
  TreeMapStringStringWithComparatorTest.class,
  TreeMapIntegerDoubleTest.class,
  TreeMapIntegerDoubleWithComparatorTest.class,
  TreeSetIntegerTest.class,
  TreeSetIntegerWithComparatorTest.class,

  // Put last to reduce number of times the test framework switches modules
  HashMapSmokeTest.class,
})
public class CollectionsSuite { }
