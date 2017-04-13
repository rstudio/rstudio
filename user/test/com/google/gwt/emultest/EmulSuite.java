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

import com.google.gwt.emultest.java.internal.CoercionsTest;
import com.google.gwt.emultest.java.io.ByteArrayInputStreamTest;
import com.google.gwt.emultest.java.io.ByteArrayOutputStreamTest;
import com.google.gwt.emultest.java.io.FilterInputStreamTest;
import com.google.gwt.emultest.java.io.FilterOutputStreamTest;
import com.google.gwt.emultest.java.io.InputStreamTest;
import com.google.gwt.emultest.java.io.OutputStreamTest;
import com.google.gwt.emultest.java.lang.BooleanTest;
import com.google.gwt.emultest.java.lang.ByteTest;
import com.google.gwt.emultest.java.lang.CharacterTest;
import com.google.gwt.emultest.java.lang.CompilerConstantStringTest;
import com.google.gwt.emultest.java.lang.DoubleTest;
import com.google.gwt.emultest.java.lang.FloatTest;
import com.google.gwt.emultest.java.lang.IntegerTest;
import com.google.gwt.emultest.java.lang.JsExceptionTest;
import com.google.gwt.emultest.java.lang.LongTest;
import com.google.gwt.emultest.java.lang.MathTest;
import com.google.gwt.emultest.java.lang.NullPointerExceptionTest;
import com.google.gwt.emultest.java.lang.ObjectTest;
import com.google.gwt.emultest.java.lang.ShortTest;
import com.google.gwt.emultest.java.lang.StringBufferTest;
import com.google.gwt.emultest.java.lang.StringTest;
import com.google.gwt.emultest.java.lang.SystemTest;
import com.google.gwt.emultest.java.lang.ThrowableStackTraceEmulTest;
import com.google.gwt.emultest.java.lang.ThrowableTest;
import com.google.gwt.emultest.java.lang.TypeTest;
import com.google.gwt.emultest.java.math.MathContextTest;
import com.google.gwt.emultest.java.math.MathContextWithObfuscatedEnumsTest;
import com.google.gwt.emultest.java.math.RoundingModeTest;
import com.google.gwt.emultest.java.nio.charset.CharsetTest;
import com.google.gwt.emultest.java.nio.charset.StandardCharsetsTest;
import com.google.gwt.emultest.java.security.MessageDigestTest;
import com.google.gwt.emultest.java.sql.SqlDateTest;
import com.google.gwt.emultest.java.sql.SqlTimeTest;
import com.google.gwt.emultest.java.sql.SqlTimestampTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Test JRE emulations.
 */
@RunWith(Suite.class)
@SuiteClasses({
  CoercionsTest.class,

  //-- java.io
  ByteArrayInputStreamTest.class,
  ByteArrayOutputStreamTest.class,
  FilterInputStreamTest.class,
  FilterOutputStreamTest.class,
  InputStreamTest.class,
  OutputStreamTest.class,

  //-- java.lang
  BooleanTest.class,
  ByteTest.class,
  CharacterTest.class,
  CompilerConstantStringTest.class,
  DoubleTest.class,
  FloatTest.class,
  IntegerTest.class,
  JsExceptionTest.class,
  LongTest.class,
  MathTest.class,
  NullPointerExceptionTest.class,
  ObjectTest.class,
  ShortTest.class,
  StringBufferTest.class,
  StringTest.class,
  SystemTest.class,
  ThrowableTest.class,
  ThrowableStackTraceEmulTest.class,
  TypeTest.class,

  //-- java.math
  // BigDecimal is tested in {@link BigDecimalSuite}
  // BigInteger is tested in {@link BigIntegerSuite}
  RoundingModeTest.class,
  MathContextTest.class,

  //-- java.nio
  CharsetTest.class,
  StandardCharsetsTest.class,

  //-- java.security
  MessageDigestTest.class,

  //-- java.sql
  SqlDateTest.class,
  SqlTimeTest.class,
  SqlTimestampTest.class,

  // Put last to reduce number of times the test framework switches modules
  MathContextWithObfuscatedEnumsTest.class,
})
public class EmulSuite { }
