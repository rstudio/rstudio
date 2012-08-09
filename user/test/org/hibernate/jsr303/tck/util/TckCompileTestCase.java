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
 */package org.hibernate.jsr303.tck.util;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.javac.StandardGeneratorContext;
import com.google.gwt.dev.util.UnitTestTreeLogger;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.validation.rebind.BeanHelperCache;
import com.google.gwt.validation.rebind.GwtSpecificValidatorGenerator;
import com.google.gwt.validation.rebind.ValidatorGenerator;

import junit.framework.Assert;

import static org.hibernate.jsr303.tck.util.TckGeneratorTestUtils.createFailOnErrorLogger;
import static org.hibernate.jsr303.tck.util.TckGeneratorTestUtils.createGeneratorContext;
import static org.hibernate.jsr303.tck.util.TckGeneratorTestUtils.createTestLogger;
import static org.hibernate.jsr303.tck.util.TckGeneratorTestUtils.getFullyQualifiedModuleName;

import javax.validation.ValidationException;
import javax.validation.Validator;

/**
 * Abstract TestCase for TCK tests that are expected to fail to compile.
 */
public abstract class TckCompileTestCase extends GWTTestCase {

  private StandardGeneratorContext context;
  private TreeLogger failOnErrorLogger;

  protected TckCompileTestCase() {
    super();
  }

  @Override
  public final String getModuleName() {
    return null; // Run as JRE tests
  }

  protected void assertBeanValidatorFailsToCompile(
      Class<? extends Validator> validatorClass, Class<?> beanType,
      Class<? extends ValidationException> expectedException,
      String expectedMessage) throws UnableToCompleteException {
    ValidatorGenerator generator = new ValidatorGenerator();
    generator.generate(failOnErrorLogger, context,
        validatorClass.getCanonicalName());
    context.finish(failOnErrorLogger);

    // Now create the validator that is going to fail
    GwtSpecificValidatorGenerator specificGenerator = new GwtSpecificValidatorGenerator();
    String beanHelperName = createBeanHelper(beanType);
    assertUnableToComplete(expectedException, expectedMessage,
        specificGenerator, beanHelperName);
  }

  protected void assertValidatorFailsToCompile(
      Class<? extends Validator> validatorClass,
      Class<? extends ValidationException> expectedException,
      String expectedMessage) {
    ValidatorGenerator generator = new ValidatorGenerator();
    assertUnableToComplete(expectedException, expectedMessage, generator,
        validatorClass.getCanonicalName());
  }

  @Override
  protected void gwtSetUp() throws Exception {
    super.gwtSetUp();
    BeanHelperCache.getForThread().clear();
    failOnErrorLogger = createFailOnErrorLogger();
    context = createGeneratorContext(getTckTestModuleName(), failOnErrorLogger);
  }

  @Override
  protected void gwtTearDown() throws Exception {
    BeanHelperCache.getForThread().clear();
    super.gwtTearDown();
  }

  private void assertUnableToComplete(
      Class<? extends ValidationException> expectedException,
      String expectedMessage, Generator generator, final String typeName) {
    UnitTestTreeLogger testLogger = createTestLogger(expectedException,
        expectedMessage);

    try {
      generator.generate(testLogger, context, typeName);
      Assert.fail("Expected a " + UnableToCompleteException.class);
    } catch (UnableToCompleteException expected) {
      // expected
    }
    testLogger.assertCorrectLogEntries();
  }

  private String createBeanHelper(Class<?> beanType)
      throws UnableToCompleteException {
    return BeanHelperCache.getForThread().createHelper(beanType, failOnErrorLogger, context)
        .getFullyQualifiedValidatorName();
  }

  private String getTckTestModuleName() {
    return getFullyQualifiedModuleName(getClass(), "TckTest");
  }
}