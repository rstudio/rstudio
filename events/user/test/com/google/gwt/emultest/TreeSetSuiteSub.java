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

import com.google.gwt.emultest.java.util.TreeSetIntegerTest;
import com.google.gwt.emultest.java.util.TreeSetIntegerWithComparatorTest;
import com.google.gwt.junit.tools.GWTTestSuite;

import junit.framework.Test;

/**
 * Tests <code>TreeSet</code>.
 */
public class TreeSetSuiteSub {
  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite("Tests for com.google.gwt.emul.java.util.TreeSet");
    suite.addTestSuite(TreeSetIntegerTest.class);
    suite.addTestSuite(TreeSetIntegerWithComparatorTest.class);
    return suite;
  }
}
