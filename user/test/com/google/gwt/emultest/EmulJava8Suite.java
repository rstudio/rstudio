/*
 * Copyright 2015 Google Inc.
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

import com.google.gwt.emultest.java8.lang.DoubleTest;
import com.google.gwt.emultest.java8.lang.FloatTest;
import com.google.gwt.emultest.java8.lang.MathTest;
import com.google.gwt.emultest.java8.lang.StringTest;
import com.google.gwt.emultest.java8.math.BigIntegerConvertTest;
import com.google.gwt.emultest.java8.util.ArrayListTest;
import com.google.gwt.emultest.java8.util.ArraysTest;
import com.google.gwt.emultest.java8.util.ComparatorTest;
import com.google.gwt.emultest.java8.util.DoubleSummaryStatisticsTest;
import com.google.gwt.emultest.java8.util.HashMapTest;
import com.google.gwt.emultest.java8.util.IdentityHashMapTest;
import com.google.gwt.emultest.java8.util.IntSummaryStatisticsTest;
import com.google.gwt.emultest.java8.util.LinkedHashMapTest;
import com.google.gwt.emultest.java8.util.LinkedListTest;
import com.google.gwt.emultest.java8.util.ListTest;
import com.google.gwt.emultest.java8.util.LongSummaryStatisticsTest;
import com.google.gwt.emultest.java8.util.MapEntryTest;
import com.google.gwt.emultest.java8.util.MapTest;
import com.google.gwt.emultest.java8.util.OptionalDoubleTest;
import com.google.gwt.emultest.java8.util.OptionalIntTest;
import com.google.gwt.emultest.java8.util.OptionalLongTest;
import com.google.gwt.emultest.java8.util.OptionalTest;
import com.google.gwt.emultest.java8.util.PrimitiveIteratorTest;
import com.google.gwt.emultest.java8.util.SpliteratorsTest;
import com.google.gwt.emultest.java8.util.StringJoinerTest;
import com.google.gwt.emultest.java8.util.TreeMapTest;
import com.google.gwt.emultest.java8.util.VectorTest;
import com.google.gwt.emultest.java8.util.stream.CollectorsTest;
import com.google.gwt.emultest.java8.util.stream.DoubleStreamTest;
import com.google.gwt.emultest.java8.util.stream.IntStreamTest;
import com.google.gwt.emultest.java8.util.stream.LongStreamTest;
import com.google.gwt.emultest.java8.util.stream.StreamSupportTest;
import com.google.gwt.emultest.java8.util.stream.StreamTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/** Test JRE emulations. */
@RunWith(Suite.class)
@SuiteClasses({
  //-- java.lang
  DoubleTest.class,
  FloatTest.class,
  MathTest.class,
  StringTest.class,

  //-- java.math
  BigIntegerConvertTest.class,

  //-- java.util
  ArraysTest.class,
  ArrayListTest.class,
  LinkedListTest.class,
  ListTest.class,
  VectorTest.class,
  ComparatorTest.class,
  MapTest.class,
  MapEntryTest.class,
  HashMapTest.class,
  IdentityHashMapTest.class,
  LinkedHashMapTest.class,
  TreeMapTest.class,
  OptionalTest.class,
  OptionalIntTest.class,
  OptionalLongTest.class,
  OptionalDoubleTest.class,
  PrimitiveIteratorTest.class,
  SpliteratorsTest.class,
  StringJoinerTest.class,
  DoubleSummaryStatisticsTest.class,
  IntSummaryStatisticsTest.class,
  LongSummaryStatisticsTest.class,

  //-- java.util.stream
  CollectorsTest.class,
  DoubleStreamTest.class,
  IntStreamTest.class,
  LongStreamTest.class,
  StreamTest.class,
  StreamSupportTest.class,
})
public class EmulJava8Suite { }
