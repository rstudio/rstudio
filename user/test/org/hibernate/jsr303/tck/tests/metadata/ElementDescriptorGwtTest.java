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
 * Test wrapper for {@link ElementDescriptorTest}.
 */
public class ElementDescriptorGwtTest extends GWTTestCase {
  private final ElementDescriptorTest delegate = new ElementDescriptorTest();

  @Override
  public String getModuleName() {
    return "org.hibernate.jsr303.tck.tests.metadata.TckTest";
  }

  public void testDeclaredOn() {
    delegate.testDeclaredOn();
  }

  public void testGetConstraintDescriptors() {
    delegate.testGetConstraintDescriptors();
  }

  public void testGetElementClass() {
    delegate.testGetElementClass();
  }

  public void testHasConstraints() {
    delegate.testHasConstraints();
  }

  public void testLookingAt() {
    delegate.testLookingAt();
  }

  public void testUnorderedAndMatchingGroups() {
    delegate.testUnorderedAndMatchingGroups();
  }

  public void testUnorderedAndMatchingGroupsWithDefaultGroupOverriding() {
    delegate.testUnorderedAndMatchingGroupsWithDefaultGroupOverriding();
  }

  public void testUnorderedAndMatchingGroupsWithInheritance() {
    delegate.testUnorderedAndMatchingGroupsWithInheritance();
  }

}
