// $Id: CustomMessageInterpolatorTest.java 17620 2009-10-04 19:19:28Z hardy.ferentschik $
/*
* JBoss, Home of Professional Open Source
* Copyright 2009, Red Hat, Inc. and/or its affiliates, and individual contributors
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.hibernate.jsr303.tck.tests.bootstrap;

import com.google.gwt.i18n.shared.GwtLocale;

import java.util.Set;
import javax.validation.Configuration;
import javax.validation.ConstraintViolation;
import javax.validation.MessageInterpolator;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;

import org.jboss.test.audit.annotations.SpecAssertion;
import org.jboss.test.audit.annotations.SpecAssertions;
import org.jboss.testharness.AbstractTest;
import org.jboss.testharness.impl.packaging.Artifact;
import org.jboss.testharness.impl.packaging.ArtifactType;
import org.jboss.testharness.impl.packaging.Classes;
import static org.testng.Assert.assertFalse;
import org.testng.annotations.Test;

import org.hibernate.jsr303.tck.util.TestUtil;
import static org.hibernate.jsr303.tck.util.TestUtil.assertCorrectConstraintViolationMessages;
import static org.hibernate.jsr303.tck.util.TestUtil.assertCorrectNumberOfViolations;

/**
 * @author Hardy Ferentschik
 */
@Artifact(artifactType = ArtifactType.JSR303)
@Classes({ TestUtil.class, TestUtil.PathImpl.class, TestUtil.NodeImpl.class })
public class CustomMessageInterpolatorTest extends AbstractTest {

  @Test
  @SpecAssertion(section = "4.3.2", id = "b")
  public void testCustomMessageInterpolatorViaConfiguration() {
    Configuration config = Validation.byDefaultProvider().configure();
    config = config.messageInterpolator( new DummyMessageInterpolator() );
    Validator validator = config.buildValidatorFactory().getValidator();

    assertCustomMessageInterpolatorUsed( validator );
  }

  @Test
  @SpecAssertions({
      @SpecAssertion(section = "4.4.2", id = "a"),
      @SpecAssertion(section = "4.4.2", id = "b"),
      @SpecAssertion(section = "4.3.2", id = "b")
  })
  public void testCustomMessageInterpolatorViaValidatorContext() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    DummyMessageInterpolator dummyMessageInterpolator = new DummyMessageInterpolator();
    Validator validator = factory.usingContext().messageInterpolator( dummyMessageInterpolator ).getValidator();
    assertCustomMessageInterpolatorUsed( validator );
    assertFalse(
        factory.getMessageInterpolator().equals( dummyMessageInterpolator ),
        "getMessageInterpolator() should return the default message interpolator."
    );
  }

  private void assertCustomMessageInterpolatorUsed(Validator validator) {
    Person person = new Person();
    person.setFirstName( "John" );
    person.setPersonalNumber( 1234567890l );

    Set<ConstraintViolation<Person>> constraintViolations = validator.validate( person );
    assertCorrectNumberOfViolations( constraintViolations, 1 );
    assertCorrectConstraintViolationMessages( constraintViolations, "my custom message" );
  }

  private static class DummyMessageInterpolator implements MessageInterpolator {
    public String interpolate(String message, Context context) {
      return "my custom message";
    }

    public String interpolate(String message, Context context, GwtLocale locale) {
      throw new UnsupportedOperationException( "No specific locale is possible" );
    }
  }

  public class Person {
    @NotNull
    private String firstName;

    @NotNull
    private String lastName;

    @Digits(integer = 10, fraction = 0)
    private long personalNumber;


    public String getFirstName() {
      return firstName;
    }

    public void setFirstName(String firstName) {
      this.firstName = firstName;
    }

    public String getLastName() {
      return lastName;
    }

    public void setLastName(String lastName) {
      this.lastName = lastName;
    }

    public long getPersonalNumber() {
      return personalNumber;
    }

    public void setPersonalNumber(long personalNumber) {
      this.personalNumber = personalNumber;
    }
  }
}
