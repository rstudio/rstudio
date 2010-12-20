// $Id: MessageInterpolationTest.java 17620 2009-10-04 19:19:28Z hardy.ferentschik $
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
package org.hibernate.jsr303.tck.tests.messageinterpolation;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.shared.GwtLocale;
import com.google.gwt.i18n.shared.GwtLocaleFactory;

import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.MessageInterpolator;
import javax.validation.Validator;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Past;
import javax.validation.constraints.Size;
import javax.validation.metadata.ConstraintDescriptor;

import org.jboss.test.audit.annotations.SpecAssertion;
import org.jboss.test.audit.annotations.SpecAssertions;
import org.jboss.testharness.AbstractTest;
import org.jboss.testharness.impl.packaging.Artifact;
import org.jboss.testharness.impl.packaging.ArtifactType;
import org.jboss.testharness.impl.packaging.Classes;
import org.jboss.testharness.impl.packaging.IntegrationTest;
import org.jboss.testharness.impl.packaging.Resource;
import org.jboss.testharness.impl.packaging.Resources;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import org.testng.annotations.Test;

import org.hibernate.jsr303.tck.util.TestUtil;
import static org.hibernate.jsr303.tck.util.TestUtil.assertCorrectConstraintViolationMessages;
import static org.hibernate.jsr303.tck.util.TestUtil.assertCorrectNumberOfViolations;
import static org.hibernate.jsr303.tck.util.TestUtil.getDefaultMessageInterpolator;
import static org.hibernate.jsr303.tck.util.TestUtil.getValidatorUnderTest;

/**
 * Modified by Google:
 * <ul>
 * <li>Changed Local to GwtLocale</li>
 * </ul>
 * @author Hardy Ferentschik
 */
@Artifact(artifactType = ArtifactType.JSR303)
@Classes({ TestUtil.class, TestUtil.PathImpl.class, TestUtil.NodeImpl.class })
@Resources({
    @Resource(source = "ValidationMessages.properties",
        destination = "WEB-INF/classes/ValidationMessages.properties"),
    @Resource(source = "ValidationMessages_de.properties",
        destination = "WEB-INF/classes/ValidationMessages_de.properties")
})
@IntegrationTest
public class MessageInterpolationTest extends AbstractTest {

  @Test
  @SpecAssertion(section = "4.3.1", id = "a")
  public void testDefaultMessageInterpolatorIsNotNull() {
    MessageInterpolator interpolator = getDefaultMessageInterpolator();
    assertNotNull( interpolator, "Each bean validation provider must provide a default message interpolator." );
  }

  @Test
  @SpecAssertions({
      @SpecAssertion(section = "4.3.1", id = "e"),
      @SpecAssertion(section = "4.3.1.1", id = "a")
  })
  public void testSuccessfulInterpolationOfValidationMessagesValue() {

    MessageInterpolator interpolator = getDefaultMessageInterpolator();
    ConstraintDescriptor<?> descriptor = getDescriptorFor( DummyEntity.class, "foo" );
    MessageInterpolator.Context context = new TestContext( descriptor );

    String expected = "replacement worked";
    String actual = interpolator.interpolate( "{foo}", context );
    assertEquals( actual, expected, "Wrong substitution" );

    expected = "replacement worked replacement worked";
    actual = interpolator.interpolate( "{foo} {foo}", context );
    assertEquals( actual, expected, "Wrong substitution" );

    expected = "This replacement worked just fine";
    actual = interpolator.interpolate( "This {foo} just fine", context );
    assertEquals( actual, expected, "Wrong substitution" );

    expected = "{} replacement worked {unknown}";
    actual = interpolator.interpolate( "{} {foo} {unknown}", context );
    assertEquals( actual, expected, "Wrong substitution" );
  }

  @Test
  @SpecAssertion(section = "4.3.1.1", id = "b")
  public void testRecursiveMessageInterpolation() {
    MessageInterpolator interpolator = getDefaultMessageInterpolator();
    ConstraintDescriptor<?> descriptor = getDescriptorFor( DummyEntity.class, "fubar" );
    MessageInterpolator.Context context = new TestContext( descriptor );

    String expected = "recursion worked";
    String actual = interpolator.interpolate( ( String ) descriptor.getAttributes().get( "message" ), context );
    assertEquals(
        expected, actual, "Expansion should be recursive"
    );
  }

  @Test
  @SpecAssertion(section = "4.3.1", id = "d")
  public void testMessagesCanBeOverriddenAtConstraintLevel() {
    Validator validator = TestUtil.getValidatorUnderTest();
    Set<ConstraintViolation<DummyEntity>> constraintViolations = validator.validateProperty(
        new DummyEntity(), "snafu"
    );
    assertCorrectNumberOfViolations( constraintViolations, 1 );
    assertCorrectConstraintViolationMessages(
        constraintViolations, "messages can also be overridden at constraint declaration."
    );
  }


  @Test
  @SpecAssertions({
      @SpecAssertion(section = "4.3.1", id = "f"),
      @SpecAssertion(section = "4.3.1", id = "g"),
      @SpecAssertion(section = "4.3.1", id = "h")
  })
  public void testLiteralCurlyBraces() {

    MessageInterpolator interpolator = getDefaultMessageInterpolator();
    ConstraintDescriptor<?> descriptor = getDescriptorFor( DummyEntity.class, "foo" );
    MessageInterpolator.Context context = new TestContext( descriptor );

    String expected = "{";
    String actual = interpolator.interpolate( "\\{", context );
    assertEquals( actual, expected, "Wrong substitution" );

    expected = "}";
    actual = interpolator.interpolate( "\\}", context );
    assertEquals( actual, expected, "Wrong substitution" );

    expected = "\\";
    actual = interpolator.interpolate( "\\", context );
    assertEquals( actual, expected, "Wrong substitution" );
  }

  @Test
  @SpecAssertion(section = "4.3.1.1", id = "a")
  public void testUnSuccessfulInterpolation() {
    MessageInterpolator interpolator = getDefaultMessageInterpolator();
    ConstraintDescriptor<?> descriptor = getDescriptorFor( DummyEntity.class, "foo" );
    MessageInterpolator.Context context = new TestContext( descriptor );

    String expected = "foo";  // missing {}
    String actual = interpolator.interpolate( "foo", context );
    assertEquals( actual, expected, "Wrong substitution" );

    expected = "#{foo  {}";
    actual = interpolator.interpolate( "#{foo  {}", context );
    assertEquals( actual, expected, "Wrong substitution" );
  }

  @Test
  @SpecAssertion(section = "4.3.1.1", id = "a")
  public void testUnknownTokenInterpolation() {
    MessageInterpolator interpolator = getDefaultMessageInterpolator();
    ConstraintDescriptor<?> descriptor = getDescriptorFor( DummyEntity.class, "foo" );
    MessageInterpolator.Context context = new TestContext( descriptor );

    String expected = "{bar}";  // unknown token {}
    String actual = interpolator.interpolate( "{bar}", context );
    assertEquals( actual, expected, "Wrong substitution" );
  }

  @Test
  @SpecAssertion(section = "4.3.1.1", id = "c")
  public void testParametersAreExtractedFromBeanValidationProviderBundle() {
    MessageInterpolator interpolator = getDefaultMessageInterpolator();
    ConstraintDescriptor<?> descriptor = getDescriptorFor( Person.class, "birthday" );
    MessageInterpolator.Context context = new TestContext( descriptor );

    String key = "{javax.validation.constraints.Past.message}"; // Past is a built-in constraint so the provider must provide a default message
    String actual = interpolator.interpolate( key, context );
    assertFalse(
        key.equals( actual ),
        "There should have been a message interpolation from the bean validator provider bundle."
    );
  }

  @Test
  @SpecAssertion(section = "4.3.1.1", id = "g")
  public void testConstraintAttributeValuesAreInterpolated() {
    MessageInterpolator interpolator = getDefaultMessageInterpolator();
    ConstraintDescriptor<?> descriptor = getDescriptorFor( DummyEntity.class, "bar" );
    MessageInterpolator.Context context = new TestContext( descriptor );

    String expected = "size must be between 5 and 10";
    String actual = interpolator.interpolate( ( String ) descriptor.getAttributes().get( "message" ), context );
    assertEquals( actual, expected, "Wrong substitution" );
  }

  @Test
  @SpecAssertion(section = "4.3.1.1", id = "h")
  public void testMessageInterpolationWithLocale() {
    MessageInterpolator interpolator = getDefaultMessageInterpolator();
    ConstraintDescriptor<?> descriptor = getDescriptorFor( DummyEntity.class, "foo" );
    MessageInterpolator.Context context = new TestContext( descriptor );

    String expected = "kann nicht null sein";
    // TODO(nchalko) i18n
    //GwtLocaleFactory localeFactory = GWT.create(GwtLocaleFactory.class);
    GwtLocale german = null;  //localeFactory.fromComponents("de","","","");
    String actual = interpolator.interpolate(
        ( String ) descriptor.getAttributes().get( "message" ), context, german
    );
    assertEquals( actual, expected, "Wrong substitution" );
  }

  @Test
  @SpecAssertion(section = "4.3.1.1", id = "i")
  public void testIfNoLocaleIsSpecifiedTheDefaultLocaleIsAssumed() {
    MessageInterpolator interpolator = getDefaultMessageInterpolator();
    ConstraintDescriptor<?> descriptor = getDescriptorFor( DummyEntity.class, "foo" );
    String messageTemplate = ( String ) descriptor.getAttributes().get( "message" );
    MessageInterpolator.Context context = new TestContext( descriptor );

    String messageInterpolatedWithNoLocale = interpolator.interpolate( messageTemplate, context );
    // TODO(nchalko) i18n
    //GwtLocaleFactory localeFactory = GWT.create(GwtLocaleFactory.class);
    GwtLocale defaultLocale = null; // localeFactory.getDefault();
    String messageInterpolatedWithDefaultLocale = interpolator.interpolate(
        messageTemplate, context,defaultLocale
    );

    assertEquals( messageInterpolatedWithNoLocale, messageInterpolatedWithDefaultLocale, "Wrong substitution" );
  }

  private ConstraintDescriptor<?> getDescriptorFor(Class<?> clazz, String propertyName) {
    Validator validator = getValidatorUnderTest();
    return validator.getConstraintsForClass( clazz )
        .getConstraintsForProperty( propertyName )
        .getConstraintDescriptors()
        .iterator()
        .next();
  }

  public class TestContext implements MessageInterpolator.Context {
    ConstraintDescriptor<?> descriptor;

    TestContext(ConstraintDescriptor<?> descriptor) {
      this.descriptor = descriptor;
    }

    public ConstraintDescriptor<?> getConstraintDescriptor() {
      return descriptor;
    }

    public Object getValidatedValue() {
      return null;
    }
  }

  public class DummyEntity {
    @NotNull
    String foo;

    @Size(min = 5, max = 10, message = "size must be between {min} and {max}")
    String bar;

    @Max(value = 10, message = "{replace.in.user.bundle1}")
    String fubar;

    @NotNull(message = "messages can also be overridden at constraint declaration.")
    String snafu;
  }

  public class Person {

    String name;

    @Past
    Date birthday;
  }
}
