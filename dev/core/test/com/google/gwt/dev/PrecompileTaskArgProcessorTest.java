/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.dev;

import junit.framework.TestCase;

/**
 * Test for PrecompileTaskArgProcessor.
 */
public class PrecompileTaskArgProcessorTest extends TestCase {

  private PrecompileTaskOptions defaultOptions = new PrecompileTaskOptionsImpl();
  private PrecompileTaskOptions handledOptions = new PrecompileTaskOptionsImpl();
  private PrecompileTaskArgProcessor precompileTaskArgProcessor;

  private static void assertNotEquals(boolean expected, boolean actual) {
    assertTrue(expected != actual);
  }

  @Override
  protected void setUp() throws Exception {
    precompileTaskArgProcessor = new PrecompileTaskArgProcessor(handledOptions);
  }

  public void testFlagBackwardCompatibility() {
    // Set a bunch of boolean flags using old-style tags.
    precompileTaskArgProcessor.processArgs("-workDir", "/tmp", "-XcompilerMetrics",
        "-XdisableCastChecking", "-XdisableClassMetadata", "-XdisableClusterSimilarFunctions",
        "-XdisableInlineLiteralParameters", "-XdisableOptimizeDataflow", "-XdisableOrdinalizeEnums",
        "-XdisableRemoveDuplicateFunctions", "-XdisableRunAsync", "-XdisableSoycHtml",
        "-XdisableUpdateCheck", "-ea", "-XenableClosureCompiler", "-soyc", "-XsoycDetailed",
        "-XenableJsonSoyc", "-strict", "com.google.gwt.dev.DevModule");

    // Show that the flags were recognized and ended up modifying options.
    assertNotEquals(
        defaultOptions.isCompilerMetricsEnabled(), handledOptions.isCompilerMetricsEnabled());
    assertNotEquals(
        defaultOptions.isCastCheckingDisabled(), handledOptions.isCastCheckingDisabled());
    assertNotEquals(
        defaultOptions.isClassMetadataDisabled(), handledOptions.isClassMetadataDisabled());
    assertNotEquals(defaultOptions.shouldClusterSimilarFunctions(),
        handledOptions.shouldClusterSimilarFunctions());
    assertNotEquals(defaultOptions.shouldInlineLiteralParameters(),
        handledOptions.shouldInlineLiteralParameters());
    assertNotEquals(
        defaultOptions.shouldOptimizeDataflow(), handledOptions.shouldOptimizeDataflow());
    assertNotEquals(defaultOptions.shouldOrdinalizeEnums(), handledOptions.shouldOrdinalizeEnums());
    assertNotEquals(defaultOptions.shouldRemoveDuplicateFunctions(),
        handledOptions.shouldRemoveDuplicateFunctions());
    assertNotEquals(defaultOptions.isRunAsyncEnabled(), handledOptions.isRunAsyncEnabled());
    assertNotEquals(defaultOptions.isSoycHtmlDisabled(), handledOptions.isSoycHtmlDisabled());
    assertNotEquals(defaultOptions.isUpdateCheckDisabled(), handledOptions.isUpdateCheckDisabled());
    assertNotEquals(defaultOptions.isEnableAssertions(), handledOptions.isEnableAssertions());
    assertNotEquals(
        defaultOptions.isClosureCompilerEnabled(), handledOptions.isClosureCompilerEnabled());
    assertNotEquals(defaultOptions.isSoycEnabled(), handledOptions.isSoycEnabled());
    assertNotEquals(defaultOptions.isSoycExtra(), handledOptions.isSoycExtra());
    assertNotEquals(defaultOptions.isJsonSoycEnabled(), handledOptions.isJsonSoycEnabled());
    assertNotEquals(defaultOptions.isStrict(), handledOptions.isStrict());
  }

  @SuppressWarnings("deprecation")
  public void testFlagBackwardCompatibility_aggressiveOptimizations() {
    // Set aggressiveOptimizations using the old-style tag.
    precompileTaskArgProcessor.processArgs(
        "-workDir", "/tmp", "-XdisableAggressiveOptimization", "com.google.gwt.dev.DevModule");

    // Show that the flags were recognized and ended up modifying options.
    assertNotEquals(
        defaultOptions.isAggressivelyOptimize(), handledOptions.isAggressivelyOptimize());
  }

  @SuppressWarnings("deprecation")
  public void testFlagBackwardCompatibility_draftCompile() {
    // Set draftCompile using the old-style tag.
    precompileTaskArgProcessor.processArgs(
        "-workDir", "/tmp", "-draftCompile", "com.google.gwt.dev.DevModule");

    // Show that the flags were recognized and ended up modifying options.
    assertTrue(defaultOptions.getOptimizationLevel() != handledOptions.getOptimizationLevel());
    assertNotEquals(
        defaultOptions.isAggressivelyOptimize(), handledOptions.isAggressivelyOptimize());
  }
}
