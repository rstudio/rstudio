/*
 * Copyright 2009 Google Inc.
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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.jjs.JsOutputOption;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.arg.SourceLevel;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.thirdparty.guava.common.collect.Sets;
import com.google.gwt.util.tools.Utility;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * Test for {@link Compiler}.
 */
public class CompilerTest extends ArgProcessorTestBase {

  public static final String GWT_PERSISTENTUNITCACHE = "gwt.persistentunitcache";
  private final Compiler.ArgProcessor argProcessor;
  private final CompilerOptionsImpl options = new CompilerOptionsImpl();

  public CompilerTest() {
    argProcessor = new Compiler.ArgProcessor(options);
  }

  public void testAllValidArgs() {
    assertProcessSuccess(argProcessor, new String[] {"-logLevel", "DEBUG", "-style",
        "PRETTY", "-ea", "-XdisableAggressiveOptimization", "-gen", "myGen",
        "-war", "myWar", "-workDir", "myWork", "-extra", "myExtra",
        "-localWorkers", "2", "-sourceLevel", "1.7", "c.g.g.h.H", "my.Module"});

    assertEquals(new File("myGen").getAbsoluteFile(),
        options.getGenDir().getAbsoluteFile());
    assertEquals(new File("myWar"), options.getWarDir());
    assertEquals(new File("myWork"), options.getWorkDir());
    assertEquals(new File("myExtra"), options.getExtraDir());

    assertEquals(2, options.getLocalWorkers());

    assertEquals(TreeLogger.DEBUG, options.getLogLevel());
    assertEquals(JsOutputOption.PRETTY, options.getOutput());
    assertTrue(options.isEnableAssertions());
    assertFalse(options.shouldClusterSimilarFunctions());
    assertFalse(options.shouldInlineLiteralParameters());
    assertFalse(options.shouldOptimizeDataflow());
    assertFalse(options.shouldOrdinalizeEnums());
    assertFalse(options.shouldRemoveDuplicateFunctions());

    assertEquals(SourceLevel.JAVA7, options.getSourceLevel());

    assertEquals(2, options.getModuleNames().size());
    assertEquals("c.g.g.h.H", options.getModuleNames().get(0));
    assertEquals("my.Module", options.getModuleNames().get(1));
  }

  public void testDefaultArgs() {
    assertProcessSuccess(argProcessor, new String[] {"c.g.g.h.H"});

    assertEquals(null, options.getGenDir());
    assertEquals(new File("war").getAbsoluteFile(),
        options.getWarDir().getAbsoluteFile());
    assertEquals(null, options.getWorkDir());
    assertEquals(null, options.getExtraDir());

    assertEquals(TreeLogger.INFO, options.getLogLevel());
    assertEquals(JsOutputOption.OBFUSCATED, options.getOutput());
    assertFalse(options.isEnableAssertions());
    assertTrue(options.shouldClusterSimilarFunctions());
    assertTrue(options.shouldInlineLiteralParameters());
    assertTrue(options.shouldOptimizeDataflow());
    assertTrue(options.shouldOrdinalizeEnums());
    assertTrue(options.shouldRemoveDuplicateFunctions());

    assertEquals(1, options.getLocalWorkers());

    assertEquals(1, options.getModuleNames().size());
    assertEquals("c.g.g.h.H", options.getModuleNames().get(0));
  }

  public void testForbiddenArgs() {
    assertProcessFailure(argProcessor, "Unknown argument", new String[] {"-out", "www"});
    assertProcessFailure(argProcessor, "Source level must be one of",
        new String[] {"-sourceLevel", "ssss"});
    assertProcessFailure(argProcessor, "Source level must be one of",
        new String[] {"-sourceLevel", "1.5"});
  }

  /**
   * Tests ordering for emum {@link SourceLevel}.
   */
  public void testSourceLevelOrdering() {
    SourceLevel[] sourceLevels = SourceLevel.values();
    SourceLevel previousSourceLevel = sourceLevels[0];
    for (int i = 1; i < sourceLevels.length; i++) {
      assertTrue(Utility.versionCompare(previousSourceLevel.getStringValue(),
          sourceLevels[i].getStringValue()) < 0);
      previousSourceLevel = sourceLevels[i];
    }
  }

  public void testSourceLevelSelection() {
    // We are not able to compile to less that Java 6 so, we might as well do Java7 on
    // these cases.
    assertEquals(SourceLevel.JAVA7, SourceLevel.getBestMatchingVersion("1.4"));
    assertEquals(SourceLevel.JAVA7, SourceLevel.getBestMatchingVersion("1.5"));

    assertEquals(SourceLevel.JAVA6, SourceLevel.getBestMatchingVersion("1.6"));
    assertEquals(SourceLevel.JAVA6, SourceLevel.getBestMatchingVersion("1.6_26"));
    assertEquals(SourceLevel.JAVA7, SourceLevel.getBestMatchingVersion("1.7"));
    assertEquals(SourceLevel.JAVA7, SourceLevel.getBestMatchingVersion("1.8"));

    // not proper version strings => default to JAVA7.
    assertEquals(SourceLevel.JAVA7, SourceLevel.getBestMatchingVersion("1.6u3"));
    assertEquals(SourceLevel.JAVA7, SourceLevel.getBestMatchingVersion("1.6b3"));
    assertEquals(SourceLevel.JAVA7, SourceLevel.getBestMatchingVersion("1.7b3"));
  }

  public void testDeterministicBuild_Draft() throws UnableToCompleteException, IOException {
    final CompilerOptionsImpl options = new CompilerOptionsImpl();
    options.setOptimizationLevel(0);
    assertDeterministicBuild(options);
  }

  public void testDeterministicBuild_Optimized() throws UnableToCompleteException, IOException {
    final CompilerOptionsImpl options = new CompilerOptionsImpl();
    options.setOptimizationLevel(9);
    assertDeterministicBuild(options);
  }

  public void assertDeterministicBuild(CompilerOptions options)
      throws UnableToCompleteException, IOException {
    File firstCompileWorkDir = Utility.makeTemporaryDirectory(null, "hellowork");
    File secondCompileWorkDir = Utility.makeTemporaryDirectory(null, "hellowork");
    String oldPersistentUnitCacheValue = System.setProperty(GWT_PERSISTENTUNITCACHE, "false");
    try {
      options.addModuleName("com.google.gwt.sample.hello.Hello");
      options.setWarDir(new File(firstCompileWorkDir, "war"));
      options.setExtraDir(new File(firstCompileWorkDir, "extra"));
      PrintWriterTreeLogger logger = new PrintWriterTreeLogger();
      logger.setMaxDetail(TreeLogger.ERROR);

      // Run the compiler once here.
      new Compiler(options).run(logger);
      Set<String> firstTimeOutput =
          Sets.newHashSet(new File(options.getWarDir() + "/hello").list());

      options.setWarDir(new File(secondCompileWorkDir, "war"));
      options.setExtraDir(new File(secondCompileWorkDir, "extra"));
      // Run the compiler for a second time here.
      new Compiler(options).run(logger);
      Set<String> secondTimeOutput =
          Sets.newHashSet(new File(options.getWarDir() + "/hello").list());

      // It is only necessary to check that the filenames in the output directory are the same
      // because the names of the files for the JavaScript outputs are the hash of its contents.
      assertEquals("First and second compile produced different outputs",
          firstTimeOutput, secondTimeOutput);
    } finally {
      System.setProperty(GWT_PERSISTENTUNITCACHE, oldPersistentUnitCacheValue);
      Util.recursiveDelete(firstCompileWorkDir, false);
      Util.recursiveDelete(secondCompileWorkDir, false);
    }
  }
}
