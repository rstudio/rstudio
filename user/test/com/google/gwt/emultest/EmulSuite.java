/*
 * Copyright 2007 Google Inc.
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

import com.google.gwt.emultest.java.lang.CharacterTest;
import com.google.gwt.emultest.java.lang.IntegerTest;
import com.google.gwt.emultest.java.lang.ObjectTest;
import com.google.gwt.emultest.java.lang.StringBufferTest;
import com.google.gwt.emultest.java.lang.StringTest;
import com.google.gwt.emultest.java.util.ArraysTest;
import com.google.gwt.emultest.java.util.DateTest;
import com.google.gwt.emultest.java.util.HashMapTest;
import com.google.gwt.emultest.java.util.HashSetTest;
import com.google.gwt.emultest.java.util.StackTest;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests all classes in GWT JRE emulation library.
 */
public class EmulSuite {

  public static Test suite() {
    TestSuite suite = new TestSuite("Tests for com.google.gwt.emul.java");

    // $JUnit-BEGIN$
    suite.addTestSuite(ArraysTest.class);
    suite.addTestSuite(HashMapTest.class);
    suite.addTestSuite(StringBufferTest.class);
    suite.addTestSuite(StringTest.class);
    suite.addTestSuite(CharacterTest.class);
    suite.addTestSuite(StackTest.class);
    suite.addTestSuite(IntegerTest.class);
    suite.addTestSuite(DateTest.class);
    suite.addTestSuite(HashSetTest.class);
    suite.addTestSuite(ObjectTest.class);
    // $JUnit-END$

    return suite;
  }

}
