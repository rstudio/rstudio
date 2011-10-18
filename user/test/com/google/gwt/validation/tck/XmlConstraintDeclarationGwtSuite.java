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

import org.hibernate.jsr303.tck.tests.xmlconfiguration.constraintdeclaration.ConfigurationViaXmlAndAnnotationsGwtTest;
import org.hibernate.jsr303.tck.tests.xmlconfiguration.constraintdeclaration.ConfiguredBeanNotInClassPathGwtTest;
import org.hibernate.jsr303.tck.tests.xmlconfiguration.constraintdeclaration.ConstraintDeclarationGwtTest;
import org.hibernate.jsr303.tck.tests.xmlconfiguration.constraintdeclaration.DefaultSequenceDefinedInXmlGwtTest;
import org.hibernate.jsr303.tck.tests.xmlconfiguration.constraintdeclaration.MandatoryNameAttributeGwtTest;
import org.hibernate.jsr303.tck.tests.xmlconfiguration.constraintdeclaration.MissingMandatoryElementGwtTest;
import org.hibernate.jsr303.tck.tests.xmlconfiguration.constraintdeclaration.ReservedElementNameGwtTest;
import org.hibernate.jsr303.tck.tests.xmlconfiguration.constraintdeclaration.clazzlevel.ClassLevelOverridingGwtTest;
import org.hibernate.jsr303.tck.tests.xmlconfiguration.constraintdeclaration.fieldlevel.ExcludeFieldLevelAnnotationsDueToBeanDefaultsGwtTest;
import org.hibernate.jsr303.tck.tests.xmlconfiguration.constraintdeclaration.fieldlevel.FieldLevelOverridingGwtTest;
import org.hibernate.jsr303.tck.tests.xmlconfiguration.constraintdeclaration.fieldlevel.IncludeFieldLevelAnnotationsDueToBeanDefaultsGwtTest;
import org.hibernate.jsr303.tck.tests.xmlconfiguration.constraintdeclaration.fieldlevel.WrongFieldNameGwtTest;
import org.hibernate.jsr303.tck.tests.xmlconfiguration.constraintdeclaration.propertylevel.ExcludePropertyLevelAnnotationsDueToBeanDefaultsGwtTest;
import org.hibernate.jsr303.tck.tests.xmlconfiguration.constraintdeclaration.propertylevel.IncludePropertyLevelAnnotationsDueToBeanDefaultsGwtTest;
import org.hibernate.jsr303.tck.tests.xmlconfiguration.constraintdeclaration.propertylevel.PropertyLevelOverridingGwtTest;
import org.hibernate.jsr303.tck.tests.xmlconfiguration.constraintdeclaration.propertylevel.WrongPropertyNameGwtTest;
import org.hibernate.jsr303.tck.util.TckTestSuiteWrapper;

/**
 * Tck Tests for the {@code xml configuration constraint declaration} package.
 */
public class XmlConstraintDeclarationGwtSuite {
  public static Test suite() {
    TckTestSuiteWrapper suite = new TckTestSuiteWrapper(
        "TCK for GWT Validation, xml configuration constraint declaration package");
    suite.addTestSuite(ConfigurationViaXmlAndAnnotationsGwtTest.class);
    suite.addTestSuite(ConfiguredBeanNotInClassPathGwtTest.class);
    suite.addTestSuite(ConstraintDeclarationGwtTest.class);
    suite.addTestSuite(DefaultSequenceDefinedInXmlGwtTest.class);
    suite.addTestSuite(MandatoryNameAttributeGwtTest.class);
    suite.addTestSuite(MissingMandatoryElementGwtTest.class);
    suite.addTestSuite(ReservedElementNameGwtTest.class);

    // Class level
    suite.addTestSuite(ClassLevelOverridingGwtTest.class);

    // Field Level
    suite
        .addTestSuite(ExcludeFieldLevelAnnotationsDueToBeanDefaultsGwtTest.class);
    suite.addTestSuite(FieldLevelOverridingGwtTest.class);
    suite
        .addTestSuite(IncludeFieldLevelAnnotationsDueToBeanDefaultsGwtTest.class);
    suite.addTestSuite(WrongFieldNameGwtTest.class);

    // Property Level
    suite
        .addTestSuite(ExcludePropertyLevelAnnotationsDueToBeanDefaultsGwtTest.class);
    suite
        .addTestSuite(IncludePropertyLevelAnnotationsDueToBeanDefaultsGwtTest.class);
    suite.addTestSuite(PropertyLevelOverridingGwtTest.class);
    suite.addTestSuite(WrongPropertyNameGwtTest.class);

    return suite;
  }
}
