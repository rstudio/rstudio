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
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.codeserver.CompileDir.PolicyFile;
import com.google.gwt.dev.resource.impl.ResourceOracleImpl;
import com.google.gwt.thirdparty.guava.common.base.Charsets;
import com.google.gwt.thirdparty.guava.common.base.Preconditions;
import com.google.gwt.thirdparty.guava.common.io.Files;
import com.google.gwt.thirdparty.guava.common.io.Resources;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * An output directory where other servers pick up files from Super Dev Mode.
 * This often points to the -war directory for a DevMode server,
 * but it may point to a subdirectory.
 *
 * <p>Underneath this directory, the code server will write one subdirectory for each module.
 * A nocache.js file, public resource files, and GWT-RPC policy files will be written there.
 * This is usually enough to launch Super Dev Mode without running the
 * production GWT compiler first.
 */
class LauncherDir {
  private final File launcherDir;
  private final Options options;

  /**
   * @see #maybeCreate
   */
  private LauncherDir(Options options) {
    this.launcherDir = Preconditions.checkNotNull(options.getLauncherDir());
    this.options = options;
  }

  /**
   * Updates files after a successful compile (or on startup).
   * @param module the module that was compiled
   * @param compileDir the compiler's output directory
   */
  void update(ModuleDef module, CompileDir compileDir, TreeLogger logger)
      throws UnableToCompleteException {
    File moduleDir = new File(launcherDir + "/" + module.getName());
    if (!moduleDir.isDirectory()) {
      if (!moduleDir.mkdirs()) {
        logger.log(Type.ERROR, "Can't create launcher dir for module: " + moduleDir);
        throw new UnableToCompleteException();
      }
    }

    try {
      String stub = generateStubNocacheJs(module.getName(), options);

      final File noCacheJs = new File(moduleDir, module.getName() + ".nocache.js");
      Files.write(stub, noCacheJs, Charsets.UTF_8);

      // Remove gz file so it doesn't get used instead.
      // (We may be writing to an existing war directory.)
      final File noCacheJsGz = new File(noCacheJs.getPath() + ".gz");
      if (noCacheJsGz.exists()) {
        if (!noCacheJsGz.delete()) {
          logger.log(Type.ERROR, "cannot delete file: " + noCacheJsGz);
          throw new UnableToCompleteException();
        }
      }

      writePublicResources(moduleDir, module, logger);

      // Copy the GWT-RPC serialization policies so that the subclass of RemoteServiceServlet
      // can pick it up. (It expects to find policy files in the module directory.)
      // See RemoteServiceServlet.loadSerializationPolicy.
      // (An alternate approach is to set the gwt.codeserver.port environment variable
      // so that the other server downloads policies over HTTP.)
      for (PolicyFile policyFile : compileDir.readRpcPolicyManifest(module.getName())) {
        String filename = policyFile.getName();
        File src = new File(compileDir.getWarDir(), module.getName() + "/" + filename);
        File dest = new File(moduleDir, filename);
        Files.copy(src, dest);
      }

    } catch (IOException e) {
      logger.log(Type.ERROR, "Can't update launcher dir", e);
      throw new UnableToCompleteException();
    }
  }

  /**
   * Returns a new LauncherDir or null if not enabled.
   */
  static LauncherDir maybeCreate(Options options) {
    if (options.getLauncherDir() == null) {
      return null;
    }
    return new LauncherDir(options);
  }

  // TODO: figure out a better home for these static methods.

  /**
   * Returns the contents of a nocache.js file that will compile and then run a GWT application
   * using Super Dev Mode.
   */
  static String generateStubNocacheJs(String outputModuleName, Options options) throws IOException {
    URL url = Resources.getResource(Recompiler.class, "stub.nocache.js");
    final String template = Resources.toString(url, Charsets.UTF_8);
    return template
        .replace("__MODULE_NAME__", outputModuleName)
        .replace("__SUPERDEV_PORT__", String.valueOf(options.getPort()));
  }

  static void writePublicResources(File moduleOutputDir, ModuleDef module,
      TreeLogger compileLogger) throws UnableToCompleteException, IOException {
    // Copy the public resources to the output.
    ResourceOracleImpl publicResources = module.getPublicResourceOracle();
    for (String pathName : publicResources.getPathNames()) {
      File file = new File(moduleOutputDir, pathName);
      File parent = file.getParentFile();
      if (!parent.isDirectory() && !parent.mkdirs()) {
        compileLogger.log(Type.ERROR, "cannot create directory: " + parent);
        throw new UnableToCompleteException();
      }
      Files.asByteSink(file).writeFrom(publicResources.getResourceAsStream(pathName));
    }
  }
}
