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
package org.hibernate.jsr303.tck.tests.xmlconfiguration;

import com.google.gwt.junit.client.GWTTestCase;

import org.hibernate.jsr303.tck.util.client.NonTckTest;
import org.hibernate.jsr303.tck.util.client.NotSupported;
import org.hibernate.jsr303.tck.util.client.NotSupported.Reason;

/**
 * Wraps {@link XmlConfigurationTest}.
 */
public class XmlConfigurationGwtTest extends
    GWTTestCase {
  XmlConfigurationTest d = null;

  @Override
  public String getModuleName() {
    return null;
  }

  @NotSupported(reason = Reason.XML)
  public void testAnnotationDefinedConstraintApplies() {
    fail("XML configuration is not supported");
  }

  @NotSupported(reason = Reason.XML)
  public void testCascadingConfiguredInXml() {
    fail("XML configuration is not supported");
  }

  @NotSupported(reason = Reason.XML)
  public void testClassConstraintDefinedInXml() {
    fail("XML configuration is not supported");
  }

  @NotSupported(reason = Reason.XML)
  public void testElementConversionInXmlConfiguredConstraint() {
    fail("XML configuration is not supported");
  }

  @NotSupported(reason = Reason.XML)
  public void testFieldConstraintDefinedInXml() {
    fail("XML configuration is not supported");
  }

  @NotSupported(reason = Reason.XML)
  public void testIgnoreValidationXml() {
    fail("XML configuration is not supported");
  }

  @NotSupported(reason = Reason.XML)
  public void testMappingFilesAddedViaConfigurationGetAddedToXmlConfiguredMappings() {
    fail("XML configuration is not supported");
  }

  @NotSupported(reason = Reason.XML)
  public void testPropertyConstraintDefinedInXml() {
    fail("XML configuration is not supported");
  }

  @NonTckTest
  public void testThereMustBeOnePassingTest() {
  }
}
