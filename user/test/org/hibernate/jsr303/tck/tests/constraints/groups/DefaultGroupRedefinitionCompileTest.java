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
package org.hibernate.jsr303.tck.tests.constraints.groups;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.util.UnitTestTreeLogger;

import static org.hibernate.jsr303.tck.util.TckGeneratorTestUtils.assertModuleFails;
import static org.hibernate.jsr303.tck.util.TckGeneratorTestUtils.getFullyQaulifiedModuleName;

import org.hibernate.jsr303.tck.util.TckCompileTestCase;

import javax.validation.GroupDefinitionException;

/**
 * Test wrapper for {@link DefaultGroupRedefinitionTest} tests that are meant to
 * fail to compile.
 */
public class DefaultGroupRedefinitionCompileTest extends TckCompileTestCase {

  public void testGroupSequenceContainingDefault()
      throws UnableToCompleteException {

    UnitTestTreeLogger.Builder builder = new UnitTestTreeLogger.Builder();
    builder.expect(TreeLogger.ERROR, //
        "Unable to create a validator for "
            + "org.hibernate.jsr303.tck.tests.constraints.groups."
            + "DefaultGroupRedefinitionTest.AddressWithDefaultInGroupSequence "
            + "because 'Default.class' cannot appear in default group "
            + "sequence list.", //
        GroupDefinitionException.class);
    builder.setLowestLogLevel(TreeLogger.INFO);
    UnitTestTreeLogger testLogger = builder.createLogger();
    assertModuleFails(
        testLogger,
        getFullyQaulifiedModuleName(getClass(),
            "GroupSequenceContainingDefaultTest"),
        GroupSequenceContainingDefaultValidatorFactory.GroupSequenceContainingDefaultValidator.class);
  }

  public void testGroupSequenceWithNoImplicitDefaultGroup()
      throws UnableToCompleteException {

    UnitTestTreeLogger.Builder builder = new UnitTestTreeLogger.Builder();
    builder.expect(TreeLogger.ERROR, //
        "Unable to create a validator for "
            + "org.hibernate.jsr303.tck.tests.constraints.groups."
            + "DefaultGroupRedefinitionTest.AddressWithDefaultInGroupSequence "
            + "because 'Default.class' cannot appear in default group "
            + "sequence list.", //
        GroupDefinitionException.class);
    builder.setLowestLogLevel(TreeLogger.INFO);
    UnitTestTreeLogger testLogger = builder.createLogger();
    assertModuleFails(
        testLogger,
        getFullyQaulifiedModuleName(getClass(),
            "GroupSequenceContainingDefaultTest"),
        GroupSequenceWithNoImplicitDefaultGroupValidatorFactory.TestValidator.class);
  }

}
