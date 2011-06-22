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
package org.hibernate.jsr303.tck.tests.constraints.groups.groupsequence;

import com.google.gwt.junit.client.GWTTestCase;

import org.hibernate.jsr303.tck.util.client.Failing;
import org.hibernate.jsr303.tck.util.client.NonTckTest;

import javax.validation.GroupDefinitionException;

/**
 * Test wrapper for {@link SequenceResolutionTest}.
 */
public class SequenceResolutionGwtTest extends GWTTestCase {
  private final SequenceResolutionTest delegate = new SequenceResolutionTest();

  @Override
  public String getModuleName() {
    return "org.hibernate.jsr303.tck.tests.constraints.groups.groupsequence.TckTest";
  }

  @NonTckTest
  public void testDummy() {
    // There must be at least one passing test.
  }

  @Failing(issue = 6291)
  public void testGroupSequenceContainerOtherGroupSequences() {
    try {
      delegate.testGroupSequenceContainerOtherGroupSequences();
      fail("Expected a " + GroupDefinitionException.class);
    } catch (GroupDefinitionException expected) {
    }
  }

  @Failing(issue = 6291)
  public void testInvalidDefinitionOfDefaultSequenceInEntity() {
    try {
      delegate.testInvalidDefinitionOfDefaultSequenceInEntity();
      fail("Expected a " + GroupDefinitionException.class);
    } catch (GroupDefinitionException expected) {
    }
  }

  @Failing(issue = 6291)
  public void testOnlyFirstGroupInSequenceGetEvaluated() {
    delegate.testOnlyFirstGroupInSequenceGetEvaluated();
  }
}
