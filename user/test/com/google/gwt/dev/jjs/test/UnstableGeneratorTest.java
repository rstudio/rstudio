/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.dev.jjs.test;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests that we get a consistent result for a single compilation even in the
 * face of an unstable generator.
 */
public class UnstableGeneratorTest extends GWTTestCase {

  /**
   * Marker interface for generating an unstable class.
   */
  public interface UnstableResult {
    String get();
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  public void testMultipleRebindsWithUnstableGenerator() {
    String firstResult = GWT.<UnstableResult> create(UnstableResult.class).get();
    assertEquals(firstResult, GWT.<UnstableResult> create(UnstableResult.class).get());
    assertEquals(firstResult, GWT.<UnstableResult> create(UnstableResult.class).get());
    assertEquals(firstResult, GWT.<UnstableResult> create(UnstableResult.class).get());
    assertEquals(firstResult, GWT.<UnstableResult> create(UnstableResult.class).get());
  }
}
