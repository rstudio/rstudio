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
package org.hibernate.jsr303.tck.tests.messageinterpolation;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Wraps the German only part of {@link MessageInterpolationTest}.
 */
public class MessageInterpolation_de_GwtTest extends GWTTestCase {
  private final MessageInterpolationTest delegate = new MessageInterpolationTest();

  @Override
  public String getModuleName() {
    return "org.hibernate.jsr303.tck.tests.messageinterpolation.TckTest_de";
  }

  public void testMessageInterpolationWithLocale() {
    // Note this test doesn't actually pass in the de local, instead it sets the
    // default local as de.
    delegate.testMessageInterpolationWithLocale();
  }
}
