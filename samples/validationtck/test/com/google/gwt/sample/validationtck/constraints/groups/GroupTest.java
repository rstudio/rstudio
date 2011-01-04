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
package com.google.gwt.sample.validationtck.constraints.groups;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.sample.validationtck.util.Failing;

/**
 * Test wrapper for
 * {@link org.hibernate.jsr303.tck.tests.constraints.groups.GroupTest}.
 */
public class GroupTest extends GWTTestCase {
  private final org.hibernate.jsr303.tck.tests.constraints.groups.GroupTest delegate =
      new org.hibernate.jsr303.tck.tests.constraints.groups.GroupTest();

  @Override
  public String getModuleName() {
    return "com.google.gwt.sample.validationtck.constraints.groups.TckTest";
  }

  public void testConstraintCanBelongToMoreThanOneGroup() {
    delegate.testConstraintCanBelongToMoreThanOneGroup();
  }

  public void testConstraintWithNoExplicitlySpecifiedGroupBelongsToDefault() {
    delegate.testConstraintWithNoExplicitlySpecifiedGroupBelongsToDefault();
  }

  public void testCyclicGroupSequence() {
    delegate.testCyclicGroupSequence();
  }

  @Failing(issue = 5801)
  public void testGroups() {
    delegate.testGroups();
  }

  @Failing(issue = 5801)
  public void testGroupSequence() {
    delegate.testGroupSequence();
  }

  @Failing(issue = 5801)
  public void testGroupSequenceFollowedByGroup() {
    delegate.testGroupSequenceFollowedByGroup();
  }

  @Failing(issue = 5801)
  public void testImplicitGrouping() {
    delegate.testImplicitGrouping();
  }

  @Failing(issue = 5801)
  public void testValidateAgainstDifferentGroups() {
    delegate.testValidateAgainstDifferentGroups();
  }

  public void testValidationFailureInMultipleGroups() {
    delegate.testValidationFailureInMultipleGroups();
  }

}
