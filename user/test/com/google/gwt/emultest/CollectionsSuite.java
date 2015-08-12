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
import com.google.gwt.emultest.java.util.VectorTest;
import com.google.gwt.junit.tools.GWTTestSuite;

import junit.framework.Test;

/**
 * Test JRE Collections emulation.
 */
public class CollectionsSuite {

  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite("Tests for emulation of Java Collections");

    // $JUnit-BEGIN$
    suite.addTestSuite(ArrayListTest.class);
    suite.addTestSuite(ArraysTest.class);
    suite.addTestSuite(BitSetTest.class);
    suite.addTestSuite(CollectionsTest.class);
    suite.addTestSuite(ComparatorTest.class);
    suite.addTestSuite(DateTest.class);
    suite.addTestSuite(EnumMapTest.class);
    suite.addTestSuite(EnumSetTest.class);
    suite.addTestSuite(HashMapSmokeTest.class);
    suite.addTestSuite(HashMapTest.class);
    suite.addTestSuite(HashSetTest.class);
    suite.addTestSuite(IdentityHashMapTest.class);
    suite.addTestSuite(LinkedHashMapTest.class);
    suite.addTestSuite(LinkedHashSetTest.class);
    suite.addTestSuite(LinkedListTest.class);
    suite.addTestSuite(ObjectsTest.class);
    suite.addTestSuite(PriorityQueueTest.class);
    suite.addTestSuite(RandomTest.class);
    suite.addTestSuite(StackTest.class);
    suite.addTestSuite(VectorTest.class);
    suite.addTest(TreeMapSuiteSub.suite());
    suite.addTest(TreeSetSuiteSub.suite());
    // $JUnit-END$

    return suite;
  }
}
