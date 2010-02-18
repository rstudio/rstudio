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
package com.google.gwt.junit.client;

import com.google.gwt.core.client.GWT;

/**
 * Test case running in pure Java mode (non-GWT) due to {@link #getModuleName}
 * returning <code>null</code>.
 */
public class NullModuleNameTest extends GWTTestCase {

  private int gwtSetUpCalls;

  @Override
  public String getModuleName() {
    return null;
  }

  @Override
  protected void gwtSetUp() throws Exception {
    gwtSetUpCalls++;
    super.gwtSetUp();
  }

  public void testGwtSetUpCalled() {
    assertEquals(1, gwtSetUpCalls);
  }

  public void testIsNotClient() {
    assertFalse(GWT.isClient());
  }
}
