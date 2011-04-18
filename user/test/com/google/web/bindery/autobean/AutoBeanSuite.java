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
package com.google.web.bindery.autobean;

import com.google.web.bindery.autobean.gwt.client.AutoBeanTest;
import com.google.web.bindery.autobean.shared.AutoBeanCodexTest;
import com.google.web.bindery.autobean.shared.SplittableTest;
import com.google.web.bindery.autobean.vm.AutoBeanCodexJreTest;
import com.google.web.bindery.autobean.vm.AutoBeanJreTest;
import com.google.web.bindery.autobean.vm.SplittableJreTest;
import com.google.gwt.junit.tools.GWTTestSuite;

import junit.framework.Test;

/**
 * Tests of the Editor framework. These tests focus on core Editor behaviors,
 * rather than on integration with backing stores.
 */
public class AutoBeanSuite {
  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite("Test suite for AutoBean functions");
    suite.addTestSuite(AutoBeanCodexJreTest.class);
    suite.addTestSuite(AutoBeanCodexTest.class);
    suite.addTestSuite(AutoBeanJreTest.class);
    suite.addTestSuite(AutoBeanTest.class);
    suite.addTestSuite(SplittableJreTest.class);
    suite.addTestSuite(SplittableTest.class);
    return suite;
  }
}
