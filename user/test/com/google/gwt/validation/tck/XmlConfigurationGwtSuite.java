/*
 * Copyright 2011 Google Inc.
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

import org.hibernate.jsr303.tck.tests.xmlconfiguration.ConstraintValidatorFactorySpecifiedInValidationXmlGwtTest;
import org.hibernate.jsr303.tck.tests.xmlconfiguration.DefaultProviderSpecifiedInValidationXmlGwtTest;
import org.hibernate.jsr303.tck.tests.xmlconfiguration.DuplicateConfigurationGwtTest;
import org.hibernate.jsr303.tck.tests.xmlconfiguration.InvalidXmlConfigurationGwtTest;
import org.hibernate.jsr303.tck.tests.xmlconfiguration.MessageInterpolatorSpecifiedInValidationXmlGwtTest;
import org.hibernate.jsr303.tck.tests.xmlconfiguration.MessageInterpolatorSpecifiedInValidationXmlNoDefaultConstructorGwtTest;
import org.hibernate.jsr303.tck.tests.xmlconfiguration.MissingClassNameOnBeanNodeGwtTest;
import org.hibernate.jsr303.tck.tests.xmlconfiguration.OptionalValidationXmlGwtTest;
import org.hibernate.jsr303.tck.tests.xmlconfiguration.TraversableResolverSpecifiedInValidationXmlGwtTest;
import org.hibernate.jsr303.tck.tests.xmlconfiguration.TraversableResolverSpecifiedInValidationXmlNoDefaultConstructorGwtTest;
import org.hibernate.jsr303.tck.tests.xmlconfiguration.XmlConfigurationGwtTest;
import org.hibernate.jsr303.tck.util.TckTestSuiteWrapper;

/**
 * Tck Tests for the {@code xml configuration} package.
 */
public class XmlConfigurationGwtSuite {
  public static Test suite() {
    TckTestSuiteWrapper suite = new TckTestSuiteWrapper(
        "TCK for GWT Validation, xml configuration package");
    suite
        .addTestSuite(ConstraintValidatorFactorySpecifiedInValidationXmlGwtTest.class);
    suite.addTestSuite(DefaultProviderSpecifiedInValidationXmlGwtTest.class);
    suite.addTestSuite(DuplicateConfigurationGwtTest.class);
    suite.addTestSuite(InvalidXmlConfigurationGwtTest.class);
    suite
        .addTestSuite(MessageInterpolatorSpecifiedInValidationXmlNoDefaultConstructorGwtTest.class);
    suite
        .addTestSuite(MessageInterpolatorSpecifiedInValidationXmlGwtTest.class);
    suite.addTestSuite(MissingClassNameOnBeanNodeGwtTest.class);
    suite.addTestSuite(OptionalValidationXmlGwtTest.class);
    suite
        .addTestSuite(TraversableResolverSpecifiedInValidationXmlNoDefaultConstructorGwtTest.class);
    suite
        .addTestSuite(TraversableResolverSpecifiedInValidationXmlGwtTest.class);
    suite.addTestSuite(XmlConfigurationGwtTest.class);
    return suite;
  }
}
