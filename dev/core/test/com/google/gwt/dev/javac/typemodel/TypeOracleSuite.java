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
package com.google.gwt.dev.javac.typemodel;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * A suite testing {@link TypeOracle}.
 */
public class TypeOracleSuite {
  public static Test suite() {
    TestSuite suite = new TestSuite("Test suite for TypeOracle");
    suite.addTestSuite(AnnotationsTest.class);
    suite.addTestSuite(JArrayTypeTest.class);
    suite.addTestSuite(JAbstractMethodTest.class);
    suite.addTestSuite(JClassTypeTest.class);
    suite.addTestSuite(JEnumTypeTest.class);
    suite.addTestSuite(JGenericTypeTest.class);
    suite.addTestSuite(JParameterizedTypeTest.class);
    suite.addTestSuite(JRawTypeTest.class);
    suite.addTestSuite(JTypeParameterTest.class);
    suite.addTestSuite(JWildcardTypeTest.class);
    suite.addTestSuite(TypeOracleAnnotationSupportTest.class);
    return suite;
  }
}
