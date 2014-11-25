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
package com.google.gwt.dev.codeserver;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.MinimalRebuildCacheManager;
import com.google.gwt.dev.codeserver.Job.Result;
import com.google.gwt.dev.javac.UnitCache;
import com.google.gwt.dev.javac.UnitCacheSingleton;
import com.google.gwt.dev.javac.testing.impl.JavaResourceBase;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.dev.javac.testing.impl.MockResource;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.thirdparty.guava.common.base.Charsets;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.io.Files;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for {@link Recompiler}
 */
public class RecompilerTest extends TestCase {

  private static void writeResourcesTo(List<MockResource> resources, File dir) throws IOException {
    for (MockResource applicationResource : resources) {
      File resourceFile =
          new File(dir.getAbsolutePath() + File.separator + applicationResource.getPath());
      resourceFile.getParentFile().mkdirs();
      Files.write(applicationResource.getContent(), resourceFile, Charsets.UTF_8);
    }
  }

  private MockJavaResource barReferencesBazResource =
      JavaResourceBase.createMockJavaResource("com.foo.Bar",
          "package com.foo;",
          "public class Bar {",
          "  Baz baz = new Baz();",
          "}");

  private MockJavaResource bazReferencesFooResource =
      JavaResourceBase.createMockJavaResource("com.foo.Baz",
          "package com.foo;",
          "public class Baz {",
          "  Foo foo = new Foo();",
          "}");

  private MockJavaResource fooResource =
      JavaResourceBase.createMockJavaResource("com.foo.Foo",
          "package com.foo;",
          "public class Foo {}");

  private MockJavaResource nonCompilableFooResource =
      JavaResourceBase.createMockJavaResource("com.foo.Foo",
          "package com.foo;",
          "import com.google.gwt.core.client.impl.SpecializeMethod;",
          "public class Foo {",
          "  // This will throw an error in UnifyAst.",
          "  @SpecializeMethod()",
          "  public void run() {}",
          "}");

  private MockJavaResource referencesBarEntryPointResource =
      JavaResourceBase.createMockJavaResource("com.foo.TestEntryPoint",
          "package com.foo;",
          "import com.google.gwt.core.client.EntryPoint;",
          "public class TestEntryPoint implements EntryPoint {",
          "  @Override",
          "  public void onModuleLoad() {",
          "    Bar bar = new Bar();",
          "  }",
          "}");

  private MockResource simpleModuleResource =
      JavaResourceBase.createMockResource("com/foo/SimpleModule.gwt.xml",
          "<module>",
          "<source path=''/>",
          "<entry-point class='com.foo.TestEntryPoint'/>",
          "</module>");

  public void testIncrementalRecompile_compileErrorDoesntCorruptMinimalRebuildCache()
      throws UnableToCompleteException, IOException, InterruptedException {
    PrintWriterTreeLogger logger = new PrintWriterTreeLogger();
    logger.setMaxDetail(TreeLogger.ERROR);

    File sourcePath = Files.createTempDir();
    // Setup options to perform a per-file compile and compile the given module.
    Options options = new Options();
    options.parseArgs(new String[] {
        "-incremental", "-src", sourcePath.getAbsolutePath(), "com.foo.SimpleModule"});

    // Prepare the basic resources in the test application.
    List<MockResource> originalResources = Lists.newArrayList(simpleModuleResource,
        referencesBarEntryPointResource, barReferencesBazResource, bazReferencesFooResource,
        fooResource);
    writeResourcesTo(originalResources, sourcePath);

    File baseCacheDir = Files.createTempDir();
    UnitCache unitCache = UnitCacheSingleton.get(logger, null, baseCacheDir);
    MinimalRebuildCacheManager minimalRebuildCacheManager =
        new MinimalRebuildCacheManager(logger, baseCacheDir);
    Recompiler recompiler = new Recompiler(OutboxDir.create(Files.createTempDir(), logger), null,
        "com.foo.SimpleModule", options, unitCache, minimalRebuildCacheManager);
    Outbox outbox = new Outbox("Transactional Cache", recompiler, options, logger);
    OutboxTable outboxes = new OutboxTable();
    outboxes.addOutbox(outbox);
    JobRunner runner = new JobRunner(new JobEventTable(), minimalRebuildCacheManager);

    // Perform a first compile. This should pass since all resources are valid.
    Result result =
        compileWithChanges(logger, runner, outbox, sourcePath, Lists.<MockResource> newArrayList());
    assertTrue(result.isOk());

    // Recompile should fail since the provided Foo is not compilable.
    result = compileWithChanges(logger, runner, outbox, sourcePath,
        Lists.<MockResource> newArrayList(nonCompilableFooResource));
    assertFalse(result.isOk());

    // Recompile with a modified entry point. This should fail again since Foo is still
    // bad, but if transactionality protection failed on the minimalRebuildCache this compile will
    // succeed because it will think that it has "already processed" Foo.
    result = compileWithChanges(logger, runner, outbox, sourcePath,
        Lists.<MockResource> newArrayList(referencesBarEntryPointResource));
    assertFalse(result.isOk());
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    // Make sure we're using a MemoryUnitCache.
    System.setProperty(UnitCacheSingleton.GWT_PERSISTENTUNITCACHE, "false");
  }

  private Result compileWithChanges(TreeLogger logger, JobRunner runner, Outbox outbox,
      File sourcePath, List<MockResource> changedResources) throws InterruptedException,
      IOException {
    // Wait 1 second so that any new file modification times are actually different.
    Thread.sleep(1001);

    // Write the Java/XML/etc resources that make up the test application.
    writeResourcesTo(changedResources, sourcePath);

    // Compile and return success status.
    Map<String, String> bindingProperties = new HashMap<String, String>();
    Job job = outbox.makeJob(bindingProperties, logger);
    runner.submit(job);
    return job.waitForResult();
  }
}
