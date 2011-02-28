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
package org.hibernate.jsr303.tck.tests.metadata;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Test wrapper for {@link ConstraintDescriptorTest}.
 */
public class ConstraintDescriptorGwtTest extends GWTTestCase {
  private final ConstraintDescriptorTest delegate = new ConstraintDescriptorTest();

  @Override
  public String getModuleName() {
    return "org.hibernate.jsr303.tck.tests.metadata.TckTest";
  }

  public void testAnnotationAndMapParametersReflectParameterOverriding() {
    delegate.testAnnotationAndMapParametersReflectParameterOverriding();
  }

  public void testComposingConstraints() {
    delegate.testComposingConstraints();
  }

  public void testComposingConstraintsPayload() {
    delegate.testComposingConstraintsPayload();
  }

  public void testDefaultGroupIsReturnedIfNoGroupSpecifiedInDeclaration() {
    delegate.testDefaultGroupIsReturnedIfNoGroupSpecifiedInDeclaration();
  }

  public void testEmptyComposingConstraints() {
    delegate.testEmptyComposingConstraints();
  }

  public void testGetAttributesFromConstraintDescriptor() {
    delegate.testGetAttributesFromConstraintDescriptor();
  }

  public void testGetGroups() {
    delegate.testGetGroups();
  }

  public void testGetGroupsOnInterface() {
    delegate.testGetGroupsOnInterface();
  }

  public void testGetGroupsWithImplicitGroup() {
    delegate.testGetGroupsWithImplicitGroup();
  }

  public void testPayload() {
    delegate.testPayload();
  }

  public void testReportAsSingleViolation() {
    delegate.testReportAsSingleViolation();
  }
}
