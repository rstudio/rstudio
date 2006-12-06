// Copyright 2006 Google Inc. All Rights Reserved.
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

public class EmulSuite {

  /** Note: due to compiler error, only can use one Test Case at a time */
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
