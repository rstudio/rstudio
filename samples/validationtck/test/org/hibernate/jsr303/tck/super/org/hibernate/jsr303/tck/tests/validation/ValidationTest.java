// $Id: ValidationTest.java 17620 2009-10-04 19:19:28Z hardy.ferentschik $
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
package org.hibernate.jsr303.tck.tests.validation;

import java.util.ArrayList;
import java.util.List;
import javax.validation.Configuration;
import javax.validation.Validation;
import javax.validation.ValidationProviderResolver;
import javax.validation.ValidatorFactory;
import javax.validation.bootstrap.GenericBootstrap;
import javax.validation.bootstrap.ProviderSpecificBootstrap;
import javax.validation.spi.ValidationProvider;

import org.jboss.test.audit.annotations.SpecAssertion;
import org.jboss.test.audit.annotations.SpecAssertions;
import org.jboss.testharness.AbstractTest;
import org.jboss.testharness.impl.packaging.Artifact;
import org.jboss.testharness.impl.packaging.ArtifactType;
import org.jboss.testharness.impl.packaging.Classes;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;
import org.testng.annotations.Test;

import org.hibernate.jsr303.tck.common.TCKValidationProvider;
import org.hibernate.jsr303.tck.common.TCKValidatorConfiguration;
import org.hibernate.jsr303.tck.util.TestUtil;

/**
 * Tests for the implementation of <code>Validation</code>.
 *
 *Modified by google.
 *<ul>
 *<li>Removed testVerifyMethodsOfValidationObjects
 *</ul
 *
 * @author Hardy Ferentschik
 */
@Artifact(artifactType = ArtifactType.JSR303)
@Classes({
    TestUtil.class, TestUtil.PathImpl.class, TestUtil.NodeImpl.class, TCKValidationProvider.class,
    TCKValidationProvider.DummyValidatorFactory.class, TCKValidatorConfiguration.class
})
public class ValidationTest extends AbstractTest {

  @Test
  @SpecAssertion(section = "4.4.5", id = "a")
  public void testBuildDefaultValidatorFactory() {
    ValidatorFactory defaultFactory = Validation.buildDefaultValidatorFactory();
    assertNotNull( defaultFactory, "We should be able to get a factory." );

    ValidatorFactory defaultProviderFactory = Validation.byDefaultProvider().configure().buildValidatorFactory();
    assertNotNull( defaultProviderFactory, "We should be able to get a factory." );

    assertEquals( defaultFactory.getClass(), defaultFactory.getClass(), "The factories have to be identical." );
  }

  @Test
  @SpecAssertions({
      @SpecAssertion(section = "4.4.5", id = "b"),
      @SpecAssertion(section = "4.4.5", id = "e")
  })
  public void testCustomValidationProviderResolution() {
    ValidationProviderResolver resolver = new ValidationProviderResolver() {

      public List<ValidationProvider<?>> getValidationProviders() {
        List<ValidationProvider<?>> list = new ArrayList<ValidationProvider<?>>();
        list.add( new TCKValidationProvider() );
        return list;
      }
    };

    GenericBootstrap bootstrap = Validation.byDefaultProvider();
    Configuration<?> config = bootstrap.providerResolver( resolver ).configure();

    ValidatorFactory factory = config.buildValidatorFactory();
    assertTrue( factory instanceof TCKValidationProvider.DummyValidatorFactory );
  }

  @Test
  @SpecAssertions({
      @SpecAssertion(section = "4.4.5", id = "c"),
      @SpecAssertion(section = "4.4.5", id = "e")
  })
  public void testSpecificValidationProvider() {
    ValidationProviderResolver resolver = new ValidationProviderResolver() {

      public List<ValidationProvider<?>> getValidationProviders() {
        List<ValidationProvider<?>> list = new ArrayList<ValidationProvider<?>>();
        list.add( new TCKValidationProvider() );
        return list;
      }
    };

    // with resolver
    ProviderSpecificBootstrap<TCKValidatorConfiguration> bootstrap = Validation.byProvider( TCKValidationProvider.class );
    Configuration<?> config = bootstrap.providerResolver( resolver ).configure();
    ValidatorFactory factory = config.buildValidatorFactory();
    assertTrue( factory instanceof TCKValidationProvider.DummyValidatorFactory );
  }
}
