/*
 * Copyright 2008 Google Inc.
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

import com.google.gwt.emultest.java.lang.BooleanTest;
import com.google.gwt.emultest.java.lang.ByteTest;
import com.google.gwt.emultest.java.lang.CharacterTest;
import com.google.gwt.emultest.java.lang.CompilerConstantStringTest;
import com.google.gwt.emultest.java.lang.DoubleTest;
import com.google.gwt.emultest.java.lang.FloatTest;
import com.google.gwt.emultest.java.lang.IntegerTest;
import com.google.gwt.emultest.java.lang.LongTest;
import com.google.gwt.emultest.java.lang.MathTest;
import com.google.gwt.emultest.java.lang.ObjectTest;
import com.google.gwt.emultest.java.lang.ShortTest;
import com.google.gwt.emultest.java.lang.StringBufferDefaultImplTest;
import com.google.gwt.emultest.java.lang.StringBufferTest;
import com.google.gwt.emultest.java.lang.StringTest;
import com.google.gwt.emultest.java.lang.SystemTest;
import com.google.gwt.emultest.java.math.MathContextTest;
import com.google.gwt.emultest.java.math.RoundingModeTest;
import com.google.gwt.emultest.java.security.MessageDigestTest;
import com.google.gwt.emultest.java.sql.SqlDateTest;
import com.google.gwt.emultest.java.sql.SqlTimeTest;
import com.google.gwt.emultest.java.sql.SqlTimestampTest;
import com.google.gwt.emultest.java.util.ApacheMapTest;
import com.google.gwt.emultest.java.util.ArrayListTest;
import com.google.gwt.emultest.java.util.ArraysTest;
import com.google.gwt.emultest.java.util.CollectionsTest;
import com.google.gwt.emultest.java.util.ComparatorTest;
import com.google.gwt.emultest.java.util.DateTest;
import com.google.gwt.emultest.java.util.EnumMapTest;
import com.google.gwt.emultest.java.util.EnumSetTest;
import com.google.gwt.emultest.java.util.HashMapTest;
import com.google.gwt.emultest.java.util.HashSetTest;
import com.google.gwt.emultest.java.util.IdentityHashMapTest;
import com.google.gwt.emultest.java.util.LinkedHashMapTest;
import com.google.gwt.emultest.java.util.LinkedListTest;
import com.google.gwt.emultest.java.util.PriorityQueueTest;
import com.google.gwt.emultest.java.util.RandomTest;
import com.google.gwt.emultest.java.util.StackTest;
import com.google.gwt.junit.tools.GWTTestSuite;

import junit.framework.Test;

/**
 * Test JRE emulations.
 */
public class EmulSuite {

  /**
   * Note: due to compiler error, only can use one Test Case at a time.
   */
  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite("Tests for com.google.gwt.emul.java");

    // $JUnit-BEGIN$
    //-- java.lang
    suite.addTestSuite(BooleanTest.class);
    suite.addTestSuite(ByteTest.class);
    suite.addTestSuite(CharacterTest.class);
    suite.addTestSuite(CompilerConstantStringTest.class);
    suite.addTestSuite(DoubleTest.class);
    suite.addTestSuite(FloatTest.class);
    suite.addTestSuite(LongTest.class);
    suite.addTestSuite(MathTest.class);
    suite.addTestSuite(IntegerTest.class);
    suite.addTestSuite(ObjectTest.class);
    suite.addTestSuite(ShortTest.class);
    suite.addTestSuite(StringBufferTest.class);
    suite.addTestSuite(StringBufferDefaultImplTest.class);
    suite.addTestSuite(StringTest.class);
    suite.addTestSuite(SystemTest.class);

    //-- java.math
    // BigDecimal is tested in {@link BigDecimalSuite}
    // BigInteger is tested in {@link BigIntegerSuite}
    suite.addTestSuite(RoundingModeTest.class);
    suite.addTestSuite(MathContextTest.class);

    //-- java.util
    suite.addTestSuite(ApacheMapTest.class);
    suite.addTestSuite(ArrayListTest.class);
    suite.addTestSuite(ArraysTest.class);
    suite.addTestSuite(CollectionsTest.class);
    suite.addTestSuite(ComparatorTest.class);
    suite.addTestSuite(DateTest.class);
    suite.addTestSuite(EnumMapTest.class);
    suite.addTestSuite(EnumSetTest.class);
    suite.addTestSuite(HashMapTest.class);
    suite.addTestSuite(HashSetTest.class);
    suite.addTestSuite(IdentityHashMapTest.class);
    suite.addTestSuite(LinkedHashMapTest.class);
    suite.addTestSuite(LinkedListTest.class);
    suite.addTestSuite(PriorityQueueTest.class);
    suite.addTestSuite(RandomTest.class);
    suite.addTestSuite(StackTest.class);
    suite.addTestSuite(SqlDateTest.class);
    suite.addTestSuite(SqlTimeTest.class);
    suite.addTestSuite(SqlTimestampTest.class);
    suite.addTest(TreeMapSuiteSub.suite());
    suite.addTest(TreeSetSuiteSub.suite());

    //-- java.security
    suite.addTestSuite(MessageDigestTest.class);
    // $JUnit-END$

    return suite;
  }
}
