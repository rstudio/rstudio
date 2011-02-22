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
package org.hibernate.jsr303.tck.tests.validation;

import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.util.UnitTestTreeLogger;
import com.google.gwt.junit.client.GWTTestCase;

import static org.hibernate.jsr303.tck.util.TckGeneratorTestUtils.assertModuleFails;
import static org.hibernate.jsr303.tck.util.TckGeneratorTestUtils.getFullyQaulifiedModuleName;

import javax.validation.ValidationException;

/**
 * Test wrapper for {@link ValidateTest} methods that are suppose to fail to
 * compile.
 */
public class ValidateCompileTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return null; // null runs as normal JRE JunitTest
  }

  public void testValidatedPropertyDoesNotFollowJavaBeansConvention()
      throws UnableToCompleteException {
    String moduleName = "TckCompileTest";

    UnitTestTreeLogger.Builder builder = new UnitTestTreeLogger.Builder();
    builder.expect(
        Type.ERROR,
        "Unable to create a validator for org.hibernate.jsr303.tck.tests.validation.Boy because "
            + "Annotated methods must follow the JavaBeans naming convention. age() does not.",
        ValidationException.class);
    builder.setLowestLogLevel(Type.INFO);
    UnitTestTreeLogger testLogger = builder.createLogger();
    // TODO (nchalko) create this module in memory just for this test.
    assertModuleFails(testLogger,
        getFullyQaulifiedModuleName(getClass(), moduleName),
        TckCompileTestValidatorFactory.GwtValidator.class);
  }
}