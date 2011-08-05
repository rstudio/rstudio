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
package org.hibernate.jsr303.tck.tests.traversableresolver;

import com.google.gwt.junit.client.GWTTestCase;

import org.hibernate.jsr303.tck.util.client.Failing;

/**
 * Test wrapper for {@link TraversableResolverTest}.
 */
public class TraversableResolverGwtTest extends GWTTestCase {
  private final TraversableResolverTest delegate = new TraversableResolverTest();

  @Override
  public String getModuleName() {
    return "org.hibernate.jsr303.tck.tests.traversableresolver.TckTest";
  }

  @Failing(issue = 6544)
  public void testCorrectNumberOfCallsToIsReachableAndIsCascadable() {
    delegate.testCorrectNumberOfCallsToIsReachableAndIsCascadable();
  }

  @Failing(issue = 6544)
  public void testCustomTraversableResolverViaConfiguration() {
    delegate.testCustomTraversableResolverViaConfiguration();
  }

  public void testResolverExceptionsGetWrappedInValidationException() {
    delegate.testResolverExceptionsGetWrappedInValidationException();
  }

}
