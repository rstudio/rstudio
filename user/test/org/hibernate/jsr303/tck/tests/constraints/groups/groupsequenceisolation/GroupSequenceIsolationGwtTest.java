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
package org.hibernate.jsr303.tck.tests.constraints.groups.groupsequenceisolation;

import com.google.gwt.junit.client.GWTTestCase;

import org.hibernate.jsr303.tck.util.client.NonTckTest;

/**
 * Test wrapper for {@link GroupSequenceIsolationTest}.
 */
public class GroupSequenceIsolationGwtTest extends GWTTestCase {
  private final GroupSequenceIsolationTest delegate = new GroupSequenceIsolationTest();

  @Override
  public String getModuleName() {
    return "org.hibernate.jsr303.tck.tests.constraints.groups.groupsequenceisolation.TckTest";
  }

  public void testCorrectDefaultSequenceContainedCaseWithGroupRedefinitionOnContainedEntity() {
    delegate
        .testCorrectDefaultSequenceContainedCaseWithGroupRedefinitionOnContainedEntity();
  }

  public void testCorrectDefaultSequenceContainedCaseWithoutGroupRedefinitionOnContainedEntity() {
    delegate
        .testCorrectDefaultSequenceContainedCaseWithoutGroupRedefinitionOnContainedEntity();
  }

  public void testCorrectDefaultSequenceInheritance() {
    delegate.testCorrectDefaultSequenceInheritance();
  }

  public void testCorrectDefaultSequenceInheritance2() {
    delegate.testCorrectDefaultSequenceInheritance2();
  }

  public void testCorrectDefaultSequenceInheritance3() {
    delegate.testCorrectDefaultSequenceInheritance3();
  }

  @NonTckTest
  public void testDummy() {
    // There must be at least one passing test.
  }
}
