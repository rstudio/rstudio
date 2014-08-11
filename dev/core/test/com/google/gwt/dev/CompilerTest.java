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
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.cfg.ResourceLoader;
import com.google.gwt.dev.cfg.ResourceLoaders;
import com.google.gwt.dev.javac.testing.impl.JavaResourceBase;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.dev.javac.testing.impl.MockResource;
import com.google.gwt.dev.jjs.JsOutputOption;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.arg.SourceLevel;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.thirdparty.guava.common.base.Charsets;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableList;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Sets;
import com.google.gwt.thirdparty.guava.common.io.Files;
import com.google.gwt.util.tools.Utility;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Test for {@link Compiler}.
 */
public class CompilerTest extends ArgProcessorTestBase {

  public static final String GWT_PERSISTENTUNITCACHE = "gwt.persistentunitcache";
  private final Compiler.ArgProcessor argProcessor;
  private final CompilerOptionsImpl options = new CompilerOptionsImpl();

  private MockJavaResource jsoOne =
      JavaResourceBase.createMockJavaResource("com.foo.JsoOne",
          "package com.foo;",
          "import com.google.gwt.core.client.JavaScriptObject;",
          "public class JsoOne extends JavaScriptObject {",
          "  protected JsoOne() {",
          "  }",
          "}");

  private MockJavaResource jsoTwo_before =
      JavaResourceBase.createMockJavaResource("com.foo.JsoTwo",
          "package com.foo;",
          "public class JsoTwo {",
          "  protected JsoTwo() {",
          "  }",
          "}");

  private MockJavaResource jsoTwo_after =
      JavaResourceBase.createMockJavaResource("com.foo.JsoTwo",
          "package com.foo;",
          "import com.google.gwt.core.client.JavaScriptObject;",
          "public class JsoTwo extends JavaScriptObject {",
          "  protected JsoTwo() {",
          "  }",
          "}");

  private MockJavaResource someClassReferringToJsoOneArrays =
      JavaResourceBase.createMockJavaResource("com.foo.SomeClassReferringToJsoOneArrays",
          "package com.foo;",
          "public class SomeClassReferringToJsoOneArrays {",
          "  public static Object createJsoOneArray() { return new JsoOne[30]; }",
          "}");

  private MockJavaResource someClassReferringToJsoTwoArrays =
      JavaResourceBase.createMockJavaResource("com.foo.SomeClassReferringToJsoTwoArrays",
          "package com.foo;",
          "public class SomeClassReferringToJsoTwoArrays {",
          "  public static Object createJsoTwoArray() { return new JsoTwo[30]; }",
          "}");

  private MockJavaResource jsoArrayTestEntryPointResource =
      JavaResourceBase.createMockJavaResource("com.foo.TestEntryPoint",
          "package com.foo;",
          "import com.google.gwt.core.client.EntryPoint;",
          "public class TestEntryPoint implements EntryPoint {",
          "  @Override",
          "  public void onModuleLoad() {",
          "    Object o1 = SomeClassReferringToJsoOneArrays.createJsoOneArray();",
          "    Object o2 = SomeClassReferringToJsoTwoArrays.createJsoTwoArray();",
          "  }",
          "}");

  private MockJavaResource simpleModelEntryPointResource =
      JavaResourceBase.createMockJavaResource("com.foo.TestEntryPoint",
          "package com.foo;",
          "import com.google.gwt.core.client.EntryPoint;",
          "public class TestEntryPoint implements EntryPoint {",
          "  @Override",
          "  public void onModuleLoad() {",
          "    SimpleModel simpleModel1 = new SimpleModel();",
          "    SimpleModel simpleModel2 = new SimpleModel();",
          "    simpleModel2.copyFrom(simpleModel1);",
          "  }",
          "}");

  private MockJavaResource simpleModelResource =
      JavaResourceBase.createMockJavaResource("com.foo.SimpleModel",
          "package com.foo;",
          "public class SimpleModel {",
          "  public void copyFrom(Object object) {}",
          "}");

  private MockJavaResource modifiedFunctionSignatureSimpleModelResource =
      JavaResourceBase.createMockJavaResource("com.foo.SimpleModel",
          "package com.foo;",
          "public class SimpleModel {",
          "  public void copyFrom(SimpleModel that) {}",
          "}");

  private MockResource simpleModuleResource =
      JavaResourceBase.createMockResource("com/foo/SimpleModule.gwt.xml",
          "<module>",
          "<source path=''/>",
          "<entry-point class='com.foo.TestEntryPoint'/>",
          "</module>");

  private MockJavaResource modifiedSuperEntryPointResource =
      JavaResourceBase.createMockJavaResource("com.foo.TestEntryPoint",
          "package com.foo;",
          "import com.google.gwt.core.client.EntryPoint;",
          "public class TestEntryPoint implements EntryPoint {",
          "  @Override",
          "  public void onModuleLoad() {",
          "    Object d = new ModelD();",
          "    if (d instanceof ModelA) {",
          "      // ModelD extends ModelA;",
          "    } else {",
          "      // ModelD does not ModelA;",
          "    }",
          "  }",
          "}");

  private MockJavaResource modelAResource =
      JavaResourceBase.createMockJavaResource("com.foo.ModelA",
          "package com.foo;",
          "public class ModelA {}");

  private MockJavaResource modelBResource =
      JavaResourceBase.createMockJavaResource("com.foo.ModelB",
          "package com.foo;",
          "public class ModelB extends ModelA {}");

  private MockJavaResource modelCResource =
      JavaResourceBase.createMockJavaResource("com.foo.ModelC",
          "package com.foo;",
          "public class ModelC {}");

  private MockJavaResource modifiedSuperModelCResource =
      JavaResourceBase.createMockJavaResource("com.foo.ModelC",
          "package com.foo;",
          "public class ModelC extends ModelA {}");

  private MockJavaResource modelDResource =
      JavaResourceBase.createMockJavaResource("com.foo.ModelD",
          "package com.foo;",
          "public class ModelD extends ModelC {}");

  private MockJavaResource modifiedJsoIntfDispatchEntryPointResource =
      JavaResourceBase.createMockJavaResource("com.foo.TestEntryPoint",
          "package com.foo;",
          "import com.google.gwt.core.client.EntryPoint;",
          "public class TestEntryPoint implements EntryPoint {",
          "  @Override",
          "  public void onModuleLoad() {",
          "    Caller.call(this);",
          "  }",
          "",
          "  public void runTest(FooInterface fooInterface) {",
          "    fooInterface.run();",
          "  }",
          "}");

  private MockJavaResource callerResource =
      JavaResourceBase.createMockJavaResource("com.foo.Caller",
          "package com.foo;",
          "public class Caller {",
          "  public static void call(TestEntryPoint testEntryPoint) {",
          "    testEntryPoint.runTest(Foo.createFoo());",
          "  }",
          "}");

  private MockJavaResource fooInterfaceResource =
      JavaResourceBase.createMockJavaResource("com.foo.FooInterface",
          "package com.foo;",
          "public interface FooInterface {",
          "  void run();",
          "}");

  private MockJavaResource jsoFooResource =
      JavaResourceBase.createMockJavaResource("com.foo.Foo",
          "package com.foo;",
          "import com.google.gwt.core.client.JavaScriptObject;",
          "public final class Foo extends JavaScriptObject implements FooInterface {",
          "  public static native Foo createFoo() /*-{",
          "    return {};",
          "  }-*/;",
          "",
          "  protected Foo() {}",
          "",
          "  @Override",
          "  public void run() {}",
          "}");

  private MockJavaResource nonJsoFooResource =
      JavaResourceBase.createMockJavaResource("com.foo.Foo",
          "package com.foo;",
          "public class Foo implements FooInterface {",
          "  public static Foo createFoo() {",
          "    return new Foo();",
          "  }",
          "",
          "  @Override",
          "  public void run() {}",
          "}");

  private MockJavaResource regularFooImplemetorResource =
      JavaResourceBase.createMockJavaResource("com.foo.FooImplementor",
          "package com.foo;",
          "public class FooImplementor implements FooInterface {",
          "  @Override",
          "  public void run() {}",
          "}");

  public CompilerTest() {
    argProcessor = new Compiler.ArgProcessor(options);
  }

  public void testAllValidArgs() {
    assertProcessSuccess(argProcessor, new String[] {"-logLevel", "DEBUG", "-style",
        "PRETTY", "-ea", "-XdisableAggressiveOptimization", "-gen", "myGen",
        "-war", "myWar", "-workDir", "myWork", "-extra", "myExtra", "-XcompilePerFile",
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
    assertTrue(options.shouldCompilePerFile());

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
    assertFalse(options.shouldCompilePerFile());

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

  // TODO(stalcup): add recompile tests for file deletion, JSO status changes and Generator input
  // resource edits.

  public void testPerFileRecompile_noop() throws UnableToCompleteException, IOException,
      InterruptedException {
    checkPerFileRecompile_noop(JsOutputOption.PRETTY);
    checkPerFileRecompile_noop(JsOutputOption.DETAILED);
  }

  public void testPerFileRecompile_dateStampChange() throws UnableToCompleteException, IOException,
      InterruptedException {
    checkPerFileRecompile_dateStampChange(JsOutputOption.PRETTY);
    checkPerFileRecompile_dateStampChange(JsOutputOption.DETAILED);
  }

  public void testPerFileRecompile_functionSignatureChange() throws UnableToCompleteException,
      IOException, InterruptedException {
    // Not testing recompile equality with Pretty output since the Pretty namer's behavior is order
    // dependent, and while still correct, will come out different in a recompile with this change
    // versus a from scratch compile with this change.
    checkPerFileRecompile_functionSignatureChange(JsOutputOption.DETAILED);
  }

  public void testPerFileRecompile_regularClassMadeIntoJsoClass() throws UnableToCompleteException,
      IOException, InterruptedException {
    // Not testing recompile equality with Pretty output since the Pretty namer's behavior is order
    // dependent, and while still correct, will come out different in a recompile with this change
    // versus a from scratch compile with this change.
    checkPerFileRecompile_regularClassMadeIntoJsoClass(JsOutputOption.DETAILED);
  }

  public void testPerFileRecompile_typeHierarchyChange() throws UnableToCompleteException,
      IOException, InterruptedException {
    checkPerFileRecompile_typeHierarchyChange(JsOutputOption.PRETTY);
    checkPerFileRecompile_typeHierarchyChange(JsOutputOption.DETAILED);
  }

  public void testPerFileRecompile_singleJsoIntfDispatchChange() throws UnableToCompleteException,
      IOException, InterruptedException {
    // Not testing recompile equality with Pretty output since the Pretty namer's behavior is order
    // dependent, and while still correct, will come out different in a recompile with this change
    // versus a from scratch compile with this change.
    checkPerFileRecompile_singleJsoIntfDispatchChange(JsOutputOption.DETAILED);
  }

  public void testPerFileRecompile_dualJsoIntfDispatchChange() throws UnableToCompleteException,
      IOException, InterruptedException {
    // Not testing recompile equality with Pretty output since the Pretty namer's behavior is order
    // dependent, and while still correct, will come out different in a recompile with this change
    // versus a from scratch compile with this change.
    checkPerFileRecompile_dualJsoIntfDispatchChange(JsOutputOption.DETAILED);
  }

  private void checkPerFileRecompile_noop(JsOutputOption output) throws UnableToCompleteException,
      IOException, InterruptedException {
    MinimalRebuildCache relinkMinimalRebuildCache = new MinimalRebuildCache();
    File relinkApplicationDir = Files.createTempDir();

    String originalJs = compileToJs(relinkApplicationDir, "com.foo.SimpleModule", Lists
        .newArrayList(simpleModuleResource, simpleModelEntryPointResource, simpleModelResource),
        relinkMinimalRebuildCache, output);

    // Compile again with absolutely no file changes and reusing the minimalRebuildCache.
    String relinkedJs = compileToJs(relinkApplicationDir, "com.foo.SimpleModule",
        Lists.<MockResource> newArrayList(), relinkMinimalRebuildCache, output);

    assertTrue(originalJs.equals(relinkedJs));
  }

  private void checkPerFileRecompile_dateStampChange(JsOutputOption output)
      throws UnableToCompleteException, IOException, InterruptedException {
    MinimalRebuildCache relinkMinimalRebuildCache = new MinimalRebuildCache();
    File relinkApplicationDir = Files.createTempDir();

    String originalJs = compileToJs(relinkApplicationDir, "com.foo.SimpleModule", Lists
        .newArrayList(simpleModuleResource, simpleModelEntryPointResource, simpleModelResource),
        relinkMinimalRebuildCache, output);

    // Compile again with the same source but a new date stamp on SimpleModel and reusing the
    // minimalRebuildCache.
    String relinkedJs = compileToJs(relinkApplicationDir, "com.foo.SimpleModule",
        Lists.<MockResource> newArrayList(simpleModelResource), relinkMinimalRebuildCache, output);

    assertTrue(originalJs.equals(relinkedJs));
  }

  public void checkPerFileRecompile_regularClassMadeIntoJsoClass(JsOutputOption output)
      throws UnableToCompleteException,
      IOException, InterruptedException {
    CompilerOptions compilerOptions = new CompilerOptionsImpl();
    compilerOptions.setUseDetailedTypeIds(true);

    checkRecompiledModifiedApp(compilerOptions, "com.foo.SimpleModule",
        Lists.newArrayList(simpleModuleResource, jsoArrayTestEntryPointResource,
            someClassReferringToJsoOneArrays, someClassReferringToJsoTwoArrays, jsoOne),
        jsoTwo_before, jsoTwo_after, output);
  }

  public void checkPerFileRecompile_functionSignatureChange(JsOutputOption output)
      throws UnableToCompleteException,
      IOException, InterruptedException {
    checkRecompiledModifiedApp("com.foo.SimpleModule",
        Lists.newArrayList(simpleModuleResource, simpleModelEntryPointResource),
        simpleModelResource, modifiedFunctionSignatureSimpleModelResource, output);
  }

  private void checkPerFileRecompile_typeHierarchyChange(JsOutputOption output)
      throws UnableToCompleteException, IOException, InterruptedException {
    checkRecompiledModifiedApp("com.foo.SimpleModule", Lists.newArrayList(simpleModuleResource,
        modifiedSuperEntryPointResource, modelAResource, modelBResource, modelDResource),
        modelCResource, modifiedSuperModelCResource, output);
  }

  private void checkPerFileRecompile_singleJsoIntfDispatchChange(JsOutputOption output)
      throws UnableToCompleteException, IOException, InterruptedException {
    CompilerOptions compilerOptions = new CompilerOptionsImpl();
    compilerOptions.setUseDetailedTypeIds(true);

    checkRecompiledModifiedApp(compilerOptions, "com.foo.SimpleModule", Lists.newArrayList(
        simpleModuleResource, modifiedJsoIntfDispatchEntryPointResource, callerResource,
        fooInterfaceResource), nonJsoFooResource, jsoFooResource, output);
  }

  private void checkPerFileRecompile_dualJsoIntfDispatchChange(JsOutputOption output)
      throws UnableToCompleteException, IOException, InterruptedException {
    CompilerOptions compilerOptions = new CompilerOptionsImpl();
    compilerOptions.setUseDetailedTypeIds(true);

    checkRecompiledModifiedApp(compilerOptions, "com.foo.SimpleModule", Lists.newArrayList(
        simpleModuleResource, modifiedJsoIntfDispatchEntryPointResource, callerResource,
        fooInterfaceResource, regularFooImplemetorResource), nonJsoFooResource, jsoFooResource,
        output);
  }

  private void assertDeterministicBuild(CompilerOptions options)
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
      if (oldPersistentUnitCacheValue == null) {
        System.clearProperty(GWT_PERSISTENTUNITCACHE);
      } else {
        System.setProperty(GWT_PERSISTENTUNITCACHE, oldPersistentUnitCacheValue);
      }
      Util.recursiveDelete(firstCompileWorkDir, false);
      Util.recursiveDelete(secondCompileWorkDir, false);
    }
  }

  private void checkRecompiledModifiedApp(String moduleName, List<MockResource> sharedResources,
      MockJavaResource originalResource, MockJavaResource modifiedResource, JsOutputOption output)
      throws IOException,
      UnableToCompleteException, InterruptedException {
    checkRecompiledModifiedApp(new CompilerOptionsImpl(), moduleName, sharedResources,
        originalResource, modifiedResource, output);
  }

  private void checkRecompiledModifiedApp(CompilerOptions compilerOptions, String moduleName,
      List<MockResource> sharedResources, MockJavaResource originalResource,
      MockJavaResource modifiedResource, JsOutputOption output) throws IOException,
      UnableToCompleteException, InterruptedException {
    List<MockResource> originalResources = Lists.newArrayList(sharedResources);
    originalResources.add(originalResource);

    List<MockResource> modifiedResources = Lists.newArrayList(sharedResources);
    modifiedResources.add(modifiedResource);

    // Compile the app with original files, modify a file and do a per-file recompile.
    MinimalRebuildCache relinkMinimalRebuildCache = new MinimalRebuildCache();
    File relinkApplicationDir = Files.createTempDir();
    String originalAppFromScratchJs =
        compileToJs(compilerOptions, relinkApplicationDir, moduleName, originalResources,
            relinkMinimalRebuildCache, output);
    String modifiedAppRelinkedJs = compileToJs(compilerOptions, relinkApplicationDir, moduleName,
        Lists.<MockResource> newArrayList(modifiedResource), relinkMinimalRebuildCache, output);

    // Compile the app from scratch with the modified file.
    MinimalRebuildCache fromScratchMinimalRebuildCache = new MinimalRebuildCache();
    File fromScratchApplicationDir = Files.createTempDir();
    String modifiedAppFromScratchJs = compileToJs(compilerOptions, fromScratchApplicationDir,
        moduleName, modifiedResources, fromScratchMinimalRebuildCache, output);

    // A resource was changed between the original compile and the relink compile. If the compile is
    // correct then the output JS will have changed.
    assertFalse(originalAppFromScratchJs.equals(modifiedAppRelinkedJs));

    // If per-file compiles properly avoids global-knowledge dependencies and correctly invalidates
    // referencing types when a type changes, then the relinked and from scratch JS will be
    // identical.
    assertTrue(modifiedAppRelinkedJs.equals(modifiedAppFromScratchJs));
  }

  private String compileToJs(File applicationDir, String moduleName,
      List<MockResource> applicationResources, MinimalRebuildCache minimalRebuildCache,
      JsOutputOption output)
      throws IOException, UnableToCompleteException, InterruptedException {
    return compileToJs(new CompilerOptionsImpl(), applicationDir, moduleName,  applicationResources,
        minimalRebuildCache, output);
  }

  private String compileToJs(CompilerOptions compilerOptions, File applicationDir,
      String moduleName, List<MockResource> applicationResources,
      MinimalRebuildCache minimalRebuildCache,  JsOutputOption output)
      throws IOException, UnableToCompleteException, InterruptedException {
    // Make sure we're using a MemoryUnitCache.
    System.setProperty(GWT_PERSISTENTUNITCACHE, "false");
    // Wait 1 second so that any new file modification times are actually different.
    Thread.sleep(1001);
    TreeLogger logger = TreeLogger.NULL;

    // We might be reusing the same application dir but we want to make sure that the output dir is
    // clean to avoid confusion when returning the output JS.
    File outputDir = new File(applicationDir.getPath() + File.separator + moduleName);
    if (outputDir.exists()) {
      Util.recursiveDelete(outputDir, true);
    }

    // Fake out the resource loader to read resources both from the normal classpath as well as this
    // new application directory.
    ResourceLoader resourceLoader = ResourceLoaders.forClassLoader(Thread.currentThread());
    resourceLoader =
        ResourceLoaders.forPathAndFallback(ImmutableList.of(applicationDir), resourceLoader);

    // Setup options to perform a per-file compile, output to this new application directory and
    // compile the given module.
    compilerOptions.setCompilePerFile(true);
    compilerOptions.setWarDir(applicationDir);
    compilerOptions.setModuleNames(ImmutableList.of(moduleName));
    compilerOptions.setOutput(output);

    CompilerContext compilerContext = new CompilerContext.Builder().options(compilerOptions)
        .minimalRebuildCache(minimalRebuildCache).build();

    // Write the Java/XML/etc resources that make up the test application.
    for (MockResource applicationResource : applicationResources) {
      writeResourceTo(applicationResource, applicationDir);
    }

    // Cause the module to be cached with a reference to the prefixed resource loader so that the
    // compile process will see those resources.
    ModuleDefLoader.clearModuleCache();
    ModuleDefLoader.loadFromResources(logger, compilerContext, moduleName, resourceLoader, true);

    // Run the compile.
    Compiler compiler = new Compiler(compilerOptions, minimalRebuildCache);
    compiler.run(logger);

    // Find, read and return the created JS.
    File outputJsFile = null;
    outputDir = new File(applicationDir.getPath() + File.separator + moduleName);
    if (outputDir.exists()) {
      for (File outputFile : outputDir.listFiles()) {
        if (outputFile.getPath().endsWith(".cache.js")) {
          outputJsFile = outputFile;
          break;
        }
      }
    }
    assertNotNull(outputJsFile);
    return Files.toString(outputJsFile, Charsets.UTF_8);
  }

  private void writeResourceTo(MockResource mockResource, File applicationDir) throws IOException {
    File resourceFile =
        new File(applicationDir.getAbsolutePath() + File.separator + mockResource.getPath());
    resourceFile.getParentFile().mkdirs();
    Files.write(mockResource.getContent(), resourceFile, Charsets.UTF_8);
  }
}
