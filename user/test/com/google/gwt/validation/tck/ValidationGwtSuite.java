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
package com.google.gwt.validation.tck;

import junit.framework.Test;

import org.hibernate.jsr303.tck.tests.validation.PropertyPathGwtTest;
import org.hibernate.jsr303.tck.tests.validation.UnknownProviderBootstrapCompileTest;
import org.hibernate.jsr303.tck.tests.validation.ValidateCompileTest;
import org.hibernate.jsr303.tck.tests.validation.ValidateGwtTest;
import org.hibernate.jsr303.tck.tests.validation.ValidatePropertyGwtTest;
import org.hibernate.jsr303.tck.tests.validation.ValidateValueGwtTest;
import org.hibernate.jsr303.tck.tests.validation.ValidateWithGroupsGwtTest;
import org.hibernate.jsr303.tck.tests.validation.ValidationGwtTest;
import org.hibernate.jsr303.tck.util.TckTestSuiteWrapper;

/**
 * Tck Tests for the {@code validation} package.
 */
public class ValidationGwtSuite {
  public static Test suite() {
    TckTestSuiteWrapper suite = new TckTestSuiteWrapper(
        "TCK for GWT Validation, validation package");
    suite.addTestSuite(PropertyPathGwtTest.class);
    suite.addTestSuite(UnknownProviderBootstrapCompileTest.class);
    suite.addTestSuite(ValidateGwtTest.class);
    suite.addTestSuite(ValidateCompileTest.class);
    suite.addTestSuite(ValidatePropertyGwtTest.class);
    suite.addTestSuite(ValidateValueGwtTest.class);
    suite.addTestSuite(ValidateWithGroupsGwtTest.class);
    suite.addTestSuite(ValidationGwtTest.class);
    return suite;
  }
}
