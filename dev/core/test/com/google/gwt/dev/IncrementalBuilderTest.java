/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.cfg.LibraryGroup;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.cfg.ResourceLoader;
import com.google.gwt.dev.cfg.ResourceLoaders;
import com.google.gwt.dev.util.UnitTestTreeLogger;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.io.Files;
import com.google.gwt.thirdparty.guava.common.io.Resources;

import junit.framework.TestCase;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * Test for {@link IncrementalBuilder}.
 */
public class IncrementalBuilderTest extends TestCase {

  private static void assertBuildResult(String rootModuleName, boolean expectedBuildSuccess,
      String... expectedErrorMessages) throws MalformedURLException {
    UnitTestTreeLogger.Builder loggerBuilder = new UnitTestTreeLogger.Builder();
    loggerBuilder.setLowestLogLevel(TreeLogger.INFO);
    for (String expectedErrorMessage : expectedErrorMessages) {
      loggerBuilder.expectError(expectedErrorMessage, null);
    }
    UnitTestTreeLogger testLogger = loggerBuilder.createLogger();

    IncrementalBuilder incrementalBuilder =
        createIncrementalBuilder(rootModuleName, createGwtClassPathResourceLoader());
    incrementalBuilder.clean();

    boolean actualBuildSuccess = incrementalBuilder.build(testLogger).isSuccess();

    assertEquals(expectedBuildSuccess, actualBuildSuccess);
    testLogger.assertLogEntriesContainExpected();
  }

  private static ResourceLoader createGwtClassPathResourceLoader() throws MalformedURLException {
    String gwtClassPathProperty = System.getProperty("gwt.cp");
    String[] gwtClassPathStrings = gwtClassPathProperty.split(";");
    final List<URL> gwtClassPathEntries = Lists.newArrayList();
    for (String gwtClassPathString : gwtClassPathStrings) {
      gwtClassPathEntries.add(new File(gwtClassPathString).toURI().toURL());
    }
    ResourceLoader gwtClassPathResourceLoader = new ResourceLoader() {
        @Override
      public List<URL> getClassPath() {
        return gwtClassPathEntries;
      }

        @Override
      public URL getResource(String resourceName) {
        return Thread.currentThread().getContextClassLoader().getResource(resourceName);
      }
    };
    return gwtClassPathResourceLoader;
  }

  private static ResourceLoader createGwtClassPathResourceLoaderWithMocks(
      String... mockResourceNames) throws FileNotFoundException, IOException {
    final ResourceLoader gwtClassPathResourceLoader = createGwtClassPathResourceLoader();
    File mockResourcesDirectory = Files.createTempDir();

    for (String mockResourceName : mockResourceNames) {
      File mockFooFile = new File(mockResourcesDirectory, mockResourceName);
      mockFooFile.getParentFile().mkdirs();
      URL realFooResource = gwtClassPathResourceLoader.getResource(mockResourceName);
      Resources.copy(realFooResource, new FileOutputStream(mockFooFile));
    }

    return ResourceLoaders.forPathAndFallback(Lists.newArrayList(mockResourcesDirectory),
        gwtClassPathResourceLoader);
  }

  private static IncrementalBuilder createIncrementalBuilder(String rootModuleName,
      ResourceLoader resourceLoader) {
    String warDir = Files.createTempDir().getAbsolutePath();
    String libDir = "/tmp/gwt/lib";
    String genDir = "/tmp/gwt/gen";
    IncrementalBuilder incrementalBuilder =
        new IncrementalBuilder(rootModuleName, warDir, libDir, genDir, resourceLoader);
    return incrementalBuilder;
  }

  public void testBuildRebuildEditRebuild() throws FileNotFoundException, IOException {
    // Setup logging expectations for a full compile with no caching.
    UnitTestTreeLogger.Builder loggerBuilder = new UnitTestTreeLogger.Builder();
    loggerBuilder.setLowestLogLevel(TreeLogger.INFO);
    loggerBuilder.expectInfo(BuildTarget.formatCompilingModuleMessage("com.google.gwt.core.Core"),
        null);
    loggerBuilder.expectInfo(BuildTarget.formatCompilingModuleMessage(
        "com.google.gwt.dev.testdata.incrementalbuildsystem.SimpleBottom"), null);
    loggerBuilder.expectInfo(BuildTarget.formatCompilingModuleMessage(
        "com.google.gwt.dev.testdata.incrementalbuildsystem.SimpleMid"), null);
    loggerBuilder.expectInfo(BuildTarget.formatCompilingModuleMessage(
        "com.google.gwt.dev.testdata.incrementalbuildsystem.SimpleTop"), null);
    UnitTestTreeLogger testLogger = loggerBuilder.createLogger();

    // Prepare the compiler.
    ResourceLoader resourceLoader = createGwtClassPathResourceLoaderWithMocks(
        "com/google/gwt/dev/testdata/incrementalbuildsystem/Foo.java");
    IncrementalBuilder incrementalBuilder = createIncrementalBuilder(
        "com.google.gwt.dev.testdata.incrementalbuildsystem.SimpleTop", resourceLoader);
    incrementalBuilder.clean();

    // Compile.
    boolean actualBuildSuccess = incrementalBuilder.build(testLogger).isSuccess();

    // Assert compile succeeded and was a full compile with no caching.
    assertEquals(true, actualBuildSuccess);
    testLogger.assertLogEntriesContainExpected();

    // Setup logging expectations for a fully cached compile.
    loggerBuilder = new UnitTestTreeLogger.Builder();
    loggerBuilder.setLowestLogLevel(TreeLogger.INFO);
    loggerBuilder.expectInfo(IncrementalBuilder.NO_FILES_HAVE_CHANGED, null);
    testLogger = loggerBuilder.createLogger();

    // Recompile without changes.
    actualBuildSuccess = incrementalBuilder.rebuild(testLogger).isSuccess();

    // Assert compile succeeded and was fully cached.
    assertEquals(true, actualBuildSuccess);
    testLogger.assertLogEntriesContainExpected();

    // Change modification date of the Foo.java seen by SimpleMid.gwt.xml
    File fooFile = new File(resourceLoader.getResource(
        "com/google/gwt/dev/testdata/incrementalbuildsystem/Foo.java").getPath());
    fooFile.setLastModified(System.currentTimeMillis() + 60 * 1000);

    // Setup logging expectations for partially cached recompile.
    loggerBuilder = new UnitTestTreeLogger.Builder();
    loggerBuilder.setLowestLogLevel(TreeLogger.SPAM);
    loggerBuilder.expectSpam(
        BuildTarget.formatReusingCachedLibraryMessage("com.google.gwt.core.Core"), null);
    loggerBuilder.expectSpam(BuildTarget.formatReusingCachedLibraryMessage(
        "com.google.gwt.dev.testdata.incrementalbuildsystem.SimpleBottom"), null);
    loggerBuilder.expectInfo(BuildTarget.formatCompilingModuleMessage(
        "com.google.gwt.dev.testdata.incrementalbuildsystem.SimpleMid"), null);
    loggerBuilder.expectInfo(BuildTarget.formatCompilingModuleMessage(
        "com.google.gwt.dev.testdata.incrementalbuildsystem.SimpleTop"), null);
    testLogger = loggerBuilder.createLogger();

    // Recompile with changes.
    actualBuildSuccess = incrementalBuilder.rebuild(testLogger).isSuccess();

    // Assert compile succeeded and was partially cached.
    assertEquals(true, actualBuildSuccess);
    testLogger.assertLogEntriesContainExpected();
  }

  public void testCircularReference() throws MalformedURLException {
    List<String> circularModulePath = Arrays.asList(new String[] {
        "com.google.gwt.dev.testdata.incrementalbuildsystem.CircularRoot",
        "com.google.gwt.dev.testdata.incrementalbuildsystem.CircularB <loop>",
        "com.google.gwt.dev.testdata.incrementalbuildsystem.CircularC",
        "com.google.gwt.dev.testdata.incrementalbuildsystem.CircularFilesetD <fileset>",
        "com.google.gwt.dev.testdata.incrementalbuildsystem.CircularB <loop>"});
    assertBuildResult("com.google.gwt.dev.testdata.incrementalbuildsystem.CircularRoot", false,
        IncrementalBuilder.formatCircularModulePathMessage(circularModulePath));
  }

  public void testContinuesAfterFirstFailure() throws MalformedURLException {
    UnitTestTreeLogger.Builder loggerBuilder = new UnitTestTreeLogger.Builder();
    loggerBuilder.setLowestLogLevel(TreeLogger.INFO);
    loggerBuilder.expectInfo(BuildTarget.formatCompilingModuleMessage(
        "com.google.gwt.dev.testdata.incrementalbuildsystem.MultipleFailLeft"), null);
    loggerBuilder.expectInfo(BuildTarget.formatCompilingModuleMessage(
        "com.google.gwt.dev.testdata.incrementalbuildsystem.MultipleFailRight"), null);
    UnitTestTreeLogger testLogger = loggerBuilder.createLogger();

    IncrementalBuilder incrementalBuilder = createIncrementalBuilder(
        "com.google.gwt.dev.testdata.incrementalbuildsystem.MultipleFailTop",
        createGwtClassPathResourceLoader());
    incrementalBuilder.clean();

    boolean buildSucceeded = incrementalBuilder.build(testLogger).isSuccess();
    assertFalse(buildSucceeded);
    testLogger.assertLogEntriesContainExpected();
  }

  public void testDuplicateGeneratorOutput() throws MalformedURLException {
    String duplicateCompilationUnitError = LibraryGroup.formatDuplicateCompilationUnitMessage(
        "com.google.gwt.dev.Bar", "com.google.gwt.dev.testdata.incrementalbuildsystem.ParallelLeft",
        "com.google.gwt.dev.testdata.incrementalbuildsystem.ParallelRight");
    assertBuildResult("com.google.gwt.dev.testdata.incrementalbuildsystem.ParallelRoot", false,
        duplicateCompilationUnitError);
  }

  public void testDuplicateSourceInclusion() throws MalformedURLException {
    String duplicateCompilationUnitError = LibraryGroup.formatDuplicateCompilationUnitMessage(
        "com.google.gwt.dev.testdata.incrementalbuildsystem.Foo",
        "com.google.gwt.dev.testdata.incrementalbuildsystem.DuplicateLeft",
        "com.google.gwt.dev.testdata.incrementalbuildsystem.DuplicateRight");
    assertBuildResult("com.google.gwt.dev.testdata.incrementalbuildsystem.DuplicateRoot", false,
        duplicateCompilationUnitError);
  }

  public void testUnableToFindModule() throws MalformedURLException {
    String unableToFindModuleMessage = ModuleDefLoader.formatUnableToFindModuleMessage(
        "com/google/gwt/dev/testdata/incrementalbuildsystem/NoSuchModule.gwt.xml");
    assertBuildResult("com.google.gwt.dev.testdata.incrementalbuildsystem.NoSuchModule", false,
        unableToFindModuleMessage);
  }
}
