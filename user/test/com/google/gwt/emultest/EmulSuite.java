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
import com.google.gwt.emultest.java.lang.ObjectTest;
import com.google.gwt.emultest.java.lang.ShortTest;
import com.google.gwt.emultest.java.lang.StringBufferDefaultImplTest;
import com.google.gwt.emultest.java.lang.StringBufferTest;
import com.google.gwt.emultest.java.lang.StringTest;
import com.google.gwt.emultest.java.lang.SystemTest;
import com.google.gwt.emultest.java.math.BigDecimalArithmeticTest;
import com.google.gwt.emultest.java.math.BigDecimalCompareTest;
import com.google.gwt.emultest.java.math.BigDecimalConstructorsTest;
import com.google.gwt.emultest.java.math.BigDecimalConvertTest;
import com.google.gwt.emultest.java.math.BigDecimalScaleOperationsTest;
import com.google.gwt.emultest.java.math.BigIntegerAddTest;
import com.google.gwt.emultest.java.math.BigIntegerAndTest;
import com.google.gwt.emultest.java.math.BigIntegerCompareTest;
import com.google.gwt.emultest.java.math.BigIntegerConstructorsTest;
import com.google.gwt.emultest.java.math.BigIntegerConvertTest;
import com.google.gwt.emultest.java.math.BigIntegerDivideTest;
import com.google.gwt.emultest.java.math.BigIntegerHashCodeTest;
import com.google.gwt.emultest.java.math.BigIntegerModPowTest;
import com.google.gwt.emultest.java.math.BigIntegerMultiplyTest;
import com.google.gwt.emultest.java.math.BigIntegerNotTest;
import com.google.gwt.emultest.java.math.BigIntegerOperateBitsTest;
import com.google.gwt.emultest.java.math.BigIntegerOrTest;
import com.google.gwt.emultest.java.math.BigIntegerSubtractTest;
import com.google.gwt.emultest.java.math.BigIntegerToStringTest;
import com.google.gwt.emultest.java.math.BigIntegerXorTest;
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
 * Tests for JRE emulation classes.
 */
public class EmulSuite {

  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite("Tests for com.google.gwt.emul.java");

    // $JUnit-BEGIN$
    // java.lang
    suite.addTestSuite(BooleanTest.class);
    suite.addTestSuite(ByteTest.class);
    suite.addTestSuite(CharacterTest.class);
    suite.addTestSuite(CompilerConstantStringTest.class);
    suite.addTestSuite(DoubleTest.class);
    suite.addTestSuite(FloatTest.class);
    suite.addTestSuite(LongTest.class);
    suite.addTestSuite(IntegerTest.class);
    suite.addTestSuite(ObjectTest.class);
    suite.addTestSuite(ShortTest.class);
    suite.addTestSuite(StringBufferTest.class);
    suite.addTestSuite(StringBufferDefaultImplTest.class);
    suite.addTestSuite(StringTest.class);
    suite.addTestSuite(SystemTest.class);

    // java.math
    suite.addTestSuite(BigDecimalArithmeticTest.class);
    suite.addTestSuite(BigDecimalCompareTest.class);
    suite.addTestSuite(BigDecimalConstructorsTest.class);
    suite.addTestSuite(BigDecimalConvertTest.class);
    suite.addTestSuite(BigDecimalScaleOperationsTest.class);
    suite.addTestSuite(BigIntegerAddTest.class);
    suite.addTestSuite(BigIntegerAndTest.class);
    suite.addTestSuite(BigIntegerCompareTest.class);
    suite.addTestSuite(BigIntegerConstructorsTest.class);
    suite.addTestSuite(BigIntegerConvertTest.class);
    suite.addTestSuite(BigIntegerDivideTest.class);
    suite.addTestSuite(BigIntegerHashCodeTest.class);
    suite.addTestSuite(BigIntegerModPowTest.class);
    suite.addTestSuite(BigIntegerMultiplyTest.class);
    suite.addTestSuite(BigIntegerNotTest.class);
    suite.addTestSuite(BigIntegerOperateBitsTest.class);
    suite.addTestSuite(BigIntegerOrTest.class);
    suite.addTestSuite(BigIntegerSubtractTest.class);
    suite.addTestSuite(BigIntegerToStringTest.class);
    suite.addTestSuite(BigIntegerXorTest.class);

    // java.util
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
    // $JUnit-END$

    return suite;
  }
}
