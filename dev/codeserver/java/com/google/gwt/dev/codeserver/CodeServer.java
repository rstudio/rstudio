/*
 * Copyright 2011 Google Inc.
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
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.MinimalRebuildCacheManager;
import com.google.gwt.dev.javac.UnitCache;
import com.google.gwt.dev.javac.UnitCacheSingleton;
import com.google.gwt.dev.util.DiskCachingUtil;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.util.tools.Utility;

import java.io.File;
import java.io.IOException;

/**
 * <p>This class contains the {@link #main main method} that starts the code server for
 * "Super Dev Mode", a replacement for developer mode that doesn't require
 * browser plugins.</p>
 *
 * <p>This tool is EXPERIMENTAL. There is no authentication, no encryption, no XSS
 * protection,  and it makes all source code on the GWT compiler's classpath available
 * via HTTP. It is only safe to run on localhost (the default).</p>
 */
public class CodeServer {

  /**
   * Starts the code server. Shuts down the JVM if startup fails.
   * @param args Command-line options that can be parsed by {@link Options}.
   */
  public static void main(String[] args) throws Exception {

    Options options = new Options();
    if (!options.parseArgs(args)) {
      System.exit(1);
    }

    main(options);
  }

  /**
   * Starts the code server with the given options. Shuts down the JVM if startup fails.
   */
  public static void main(Options options) {
    if (options.isCompileTest()) {
      PrintWriterTreeLogger logger = new PrintWriterTreeLogger();
      logger.setMaxDetail(options.getLogLevel());

      OutboxTable outboxTable;

      try {
        File baseCacheDir =
            DiskCachingUtil.computePreferredCacheDir(options.getModuleNames(), logger);
        UnitCache unitCache = UnitCacheSingleton.get(logger, null, baseCacheDir);
        MinimalRebuildCacheManager minimalRebuildCacheManager =
            new MinimalRebuildCacheManager(logger, baseCacheDir);
        outboxTable = makeOutboxTable(options, logger, unitCache, minimalRebuildCacheManager);
      } catch (Throwable t) {
        t.printStackTrace();
        System.out.println("FAIL");
        System.exit(1);
        return;
      }

      int retries = options.getCompileTestRecompiles();
      for (int i = 0; i < retries; i++) {
        System.out.println("\n### Recompile " + (i + 1) + "\n");
        try {
          // TODO: actually test recompiling here.
          // (This is just running precompiles repeatedly.)
          outboxTable.defaultCompileAll(logger);
        } catch (Throwable t) {
          t.printStackTrace();
          System.out.println("FAIL");
          System.exit(1);
        }
      }

      System.out.println("PASS");
      System.exit(0);
    }

    try {
      start(options);

      String url = "http://" + options.getPreferredHost() + ":" + options.getPort() + "/";

      System.out.println();
      System.out.println("The code server is ready at " + url);
    } catch (UnableToCompleteException e) {
      // Already logged.
      System.exit(1);
    } catch (Throwable t) {
      t.printStackTrace();
      System.exit(1);
    }
  }

  /**
   * Starts the code server with the given command line options. To shut it down, see
   * {@link WebServer#stop}.
   *
   * <p>Only one code server should be started at a time because the GWT compiler uses
   * a lot of static variables.</p>
   */
  public static WebServer start(Options options) throws IOException, UnableToCompleteException {
    PrintWriterTreeLogger topLogger = new PrintWriterTreeLogger();
    topLogger.setMaxDetail(options.getLogLevel());

    TreeLogger startupLogger = topLogger.branch(Type.INFO, "Super Dev Mode starting up");
    File baseCacheDir =
        DiskCachingUtil.computePreferredCacheDir(options.getModuleNames(), startupLogger);
    UnitCache unitCache = UnitCacheSingleton.get(startupLogger, null, baseCacheDir);
    MinimalRebuildCacheManager minimalRebuildCacheManager =
        new MinimalRebuildCacheManager(topLogger, baseCacheDir);
    OutboxTable outboxTable =
        makeOutboxTable(options, startupLogger, unitCache, minimalRebuildCacheManager);

    JobEventTable eventTable = new JobEventTable();
    JobRunner runner = new JobRunner(eventTable, minimalRebuildCacheManager);

    JsonExporter exporter = new JsonExporter(options, outboxTable);

    SourceHandler sourceHandler = new SourceHandler(outboxTable, exporter);
    SymbolMapHandler symbolMapHandler = new SymbolMapHandler(outboxTable);
    WebServer webServer = new WebServer(sourceHandler, symbolMapHandler, exporter, outboxTable,
        runner, eventTable, options.getBindAddress(), options.getPort());
    webServer.start(topLogger);

    return webServer;
  }

  /**
   * Configures and compiles all the modules (unless {@link Options#getNoPrecompile} is false).
   */
  private static OutboxTable makeOutboxTable(Options options, TreeLogger logger,
      UnitCache unitCache, MinimalRebuildCacheManager minimalRebuildCacheManager)
      throws IOException, UnableToCompleteException {

    File workDir = ensureWorkDir(options);
    logger.log(Type.INFO, "workDir: " + workDir);

    LauncherDir launcherDir = LauncherDir.maybeCreate(options);

    int nextOutboxId = 1;
    OutboxTable outboxTable = new OutboxTable();
    for (String moduleName : options.getModuleNames()) {
      OutboxDir outboxDir = OutboxDir.create(new File(workDir, moduleName), logger);

      Recompiler recompiler = new Recompiler(outboxDir, launcherDir, moduleName,
          options, unitCache, minimalRebuildCacheManager);

      // The id should be treated as an opaque string since we will change it again.
      // TODO: change outbox id to include binding properties.
      String outboxId = moduleName + "_" + nextOutboxId;
      nextOutboxId++;

      outboxTable.addOutbox(new Outbox(outboxId, recompiler, options, logger));
    }
    return outboxTable;
  }

  /**
   * Ensures that we have a work directory. If specified via a flag, the
   * directory must already exist. Otherwise, create a temp directory.
   */
  private static File ensureWorkDir(Options options) throws IOException {
    File workDir = options.getWorkDir();
    if (workDir == null) {
      workDir = Utility.makeTemporaryDirectory(null, "gwt-codeserver-");
    } else {
      if (!workDir.isDirectory()) {
        throw new IOException("workspace directory doesn't exist: " + workDir);
      }
    }
    return workDir;
  }
}
