/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.core.ext.linker.impl;

import com.google.gwt.core.ext.linker.StatementRanges;
import com.google.gwt.thirdparty.guava.common.collect.Lists;

import junit.framework.TestCase;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * Unit tests for StandardCompilationResult.
 */
public class StandardCompilationResultTest extends TestCase {

  private static byte[] getBytes(String string) {
    try {
      return string.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  public void testGetJs() {
    MockPermutationResult applicationPermutationResult = new MockPermutationResult(
        new byte[][] {getBytes("a bunch of JavaScript code")}, "", null, new byte[0]);
    StandardCompilationResult compilationResult =
        new StandardCompilationResult(applicationPermutationResult);

    assertTrue(Arrays.equals(
        new String[] {"a bunch of JavaScript code"}, compilationResult.getJavaScript()));
  }

  public void testGetStatementRanges() {
    StatementRanges[] statementRanges = new StatementRanges[] {
        new StandardStatementRanges(Lists.newArrayList(0), Lists.newArrayList(10))};

    MockPermutationResult applicationPermutationResult = new MockPermutationResult(
        new byte[][] {getBytes("a bunch of JavaScript code")}, "", statementRanges, new byte[0]);
    StandardCompilationResult compilationResult =
        new StandardCompilationResult(applicationPermutationResult);

    // Returns the unmodified statementRanges array.
    assertEquals(statementRanges, compilationResult.getStatementRanges());
  }

  public void testStrongName() {
    MockPermutationResult applicationPermutationResult =
        new MockPermutationResult(null, "Hercules", null, new byte[0]);
    StandardCompilationResult compilationResult =
        new StandardCompilationResult(applicationPermutationResult);

    assertEquals("Hercules", compilationResult.getStrongName());
  }
}
