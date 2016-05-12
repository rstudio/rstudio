/*
 * Copyright 2010 Google Inc.
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

import com.google.gwt.emultest.java.math.BigIntegerAddTest;
import com.google.gwt.emultest.java.math.BigIntegerAndTest;
import com.google.gwt.emultest.java.math.BigIntegerCompareTest;
import com.google.gwt.emultest.java.math.BigIntegerConstructorsTest;
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
import com.google.gwt.junit.tools.GWTTestSuite;

import junit.framework.Test;

/**
 * Test JRE emulation of BigInteger.
 */
public class BigIntegerSuite {

  /** Note: due to compiler error, only can use one Test Case at a time. */
  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite("Tests for BigInteger");
    suite.addTestSuite(BigIntegerAddTest.class);
    suite.addTestSuite(BigIntegerAndTest.class);
    suite.addTestSuite(BigIntegerCompareTest.class);
    suite.addTestSuite(BigIntegerConstructorsTest.class);
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
    return suite;
  }
}
