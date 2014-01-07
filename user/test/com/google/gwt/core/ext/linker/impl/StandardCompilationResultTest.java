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
import com.google.gwt.dev.jjs.PermutationResult;
import com.google.gwt.dev.util.Util;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

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

  public void testGetJsNoLibraries() {
    MockPermutationResult applicationPermutationResult = new MockPermutationResult(
        new byte[][] {getBytes("a bunch of JavaScript code")}, "", null, new byte[0]);
    StandardCompilationResult compilationResult =
        new StandardCompilationResult(applicationPermutationResult);

    assertTrue(Arrays.equals(
        new String[] {"a bunch of JavaScript code"}, compilationResult.getJavaScript()));
  }

  public void testGetJsWithLibraries() {
    MockPermutationResult applicationPermutationResult = new MockPermutationResult(
        new byte[][] {getBytes("[application JavaScriptCode]")}, "", null, new byte[0]);
    MockPermutationResult libraryPermutationResult1 = new MockPermutationResult(
        new byte[][] {getBytes("[library 1 JavaScript code]")}, "", null, new byte[0]);
    MockPermutationResult libraryPermutationResult2 = new MockPermutationResult(
        new byte[][] {getBytes("[library 2 JavaScript code]")}, "", null, new byte[0]);

    StandardCompilationResult compilationResult = new StandardCompilationResult(
        applicationPermutationResult, Sets.newLinkedHashSet(Lists.<PermutationResult>newArrayList(
            libraryPermutationResult1, libraryPermutationResult2)));

    assertTrue(Arrays.equals(new String[] {
        "[library 1 JavaScript code][library 2 JavaScript code][application JavaScriptCode]"},
        compilationResult.getJavaScript()));
  }

  public void testGetStatementRangesNoLibraries() {
    StatementRanges[] statementRanges = new StatementRanges[] {
        new StandardStatementRanges(Lists.newArrayList(0), Lists.newArrayList(10))};

    MockPermutationResult applicationPermutationResult = new MockPermutationResult(
        new byte[][] {getBytes("a bunch of JavaScript code")}, "", statementRanges, new byte[0]);
    StandardCompilationResult compilationResult =
        new StandardCompilationResult(applicationPermutationResult);

    // Returns the unmodified statementRanges array.
    assertEquals(statementRanges, compilationResult.getStatementRanges());
  }

  public void testGetStatementRangesWithLibraries() {
    StatementRanges[] applicationStatementRanges = new StatementRanges[] {
        new StandardStatementRanges(Lists.newArrayList(0), Lists.newArrayList(30))};
    StatementRanges[] libraryStatementRanges1 = new StatementRanges[] {
        new StandardStatementRanges(Lists.newArrayList(0), Lists.newArrayList(10))};
    StatementRanges[] libraryStatementRanges2 = new StatementRanges[] {
        new StandardStatementRanges(Lists.newArrayList(0), Lists.newArrayList(20))};

    MockPermutationResult applicationPermutationResult = new MockPermutationResult(
        new byte[][] {getBytes("[application JavaScriptCode]")}, "", applicationStatementRanges,
        new byte[0]);
    MockPermutationResult libraryPermutationResult1 = new MockPermutationResult(
        new byte[][] {getBytes("[library 1 JavaScript code]")}, "", libraryStatementRanges1,
        new byte[0]);
    MockPermutationResult libraryPermutationResult2 = new MockPermutationResult(
        new byte[][] {getBytes("[library 2 JavaScript code]")}, "", libraryStatementRanges2,
        new byte[0]);

    StandardCompilationResult compilationResult = new StandardCompilationResult(
        applicationPermutationResult, Sets.newLinkedHashSet(Lists.<PermutationResult>newArrayList(
            libraryPermutationResult1, libraryPermutationResult2)));

    StatementRanges combinedStatementRanges = compilationResult.getStatementRanges()[0];
    assertEquals(0, combinedStatementRanges.start(0));
    assertEquals(10, combinedStatementRanges.end(0));
    assertEquals(10, combinedStatementRanges.start(1));
    assertEquals(30, combinedStatementRanges.end(1));
    assertEquals(30, combinedStatementRanges.start(2));
    assertEquals(60, combinedStatementRanges.end(2));
  }

  public void testStrongNameNoLibraries() {
    MockPermutationResult applicationPermutationResult =
        new MockPermutationResult(null, "Hercules", null, new byte[0]);
    StandardCompilationResult compilationResult =
        new StandardCompilationResult(applicationPermutationResult);

    assertEquals("Hercules", compilationResult.getStrongName());
  }

  public void testStrongNameWithLibraries() {
    MockPermutationResult applicationPermutationResult =
        new MockPermutationResult(null, "Hercules", null, new byte[0]);
    MockPermutationResult libraryPermutationResult1 =
        new MockPermutationResult(null, "Samson", null, new byte[0]);
    MockPermutationResult libraryPermutationResult2 =
        new MockPermutationResult(null, "Arnold", null, new byte[0]);

    StandardCompilationResult compilationResult = new StandardCompilationResult(
        applicationPermutationResult, Sets.newLinkedHashSet(Lists.<PermutationResult>newArrayList(
            libraryPermutationResult1, libraryPermutationResult2)));

    assertEquals(Util.computeStrongName("HerculesSamsonArnold".getBytes()),
        compilationResult.getStrongName());
  }
}
