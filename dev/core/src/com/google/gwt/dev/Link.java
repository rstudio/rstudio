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
package com.google.gwt.dev;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.SelectionProperty;
import com.google.gwt.core.ext.linker.impl.StandardCompilationResult;
import com.google.gwt.core.ext.linker.impl.StandardLinkerContext;
import com.google.gwt.dev.CompileTaskRunner.CompileTask;
import com.google.gwt.dev.cfg.BindingProperty;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.cfg.StaticPropertyOracle;
import com.google.gwt.dev.jjs.JJSOptions;
import com.google.gwt.dev.jjs.PermutationResult;
import com.google.gwt.dev.jjs.impl.CodeSplitter;
import com.google.gwt.dev.util.FileBackedObject;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.arg.ArgHandlerExtraDir;
import com.google.gwt.dev.util.arg.ArgHandlerWarDir;
import com.google.gwt.dev.util.arg.OptionExtraDir;
import com.google.gwt.dev.util.arg.OptionOutDir;
import com.google.gwt.dev.util.arg.OptionWarDir;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Performs the last phase of compilation, merging the compilation outputs.
 */
public class Link {
  /**
   * Options for Link.
   */
  @Deprecated
  public interface LegacyLinkOptions extends CompileTaskOptions, OptionOutDir {
  }

  /**
   * Options for Link.
   */
  public interface LinkOptions extends CompileTaskOptions, OptionExtraDir,
      OptionWarDir, LegacyLinkOptions {
  }

  static class ArgProcessor extends CompileArgProcessor {
    @SuppressWarnings("deprecation")
    public ArgProcessor(LinkOptions options) {
      super(options);
      registerHandler(new ArgHandlerExtraDir(options));
      registerHandler(new ArgHandlerWarDir(options));
      registerHandler(new ArgHandlerOutDirDeprecated(options));
    }

    @Override
    protected String getName() {
      return Link.class.getName();
    }
  }

  /**
   * Concrete class to implement link options.
   */
  static class LinkOptionsImpl extends CompileTaskOptionsImpl implements
      LinkOptions {

    private File extraDir;
    private File warDir;
    private File outDir;

    public LinkOptionsImpl() {
    }

    public LinkOptionsImpl(LinkOptions other) {
      copyFrom(other);
    }

    public void copyFrom(LinkOptions other) {
      super.copyFrom(other);
      setExtraDir(other.getExtraDir());
      setWarDir(other.getWarDir());
      setOutDir(other.getOutDir());
    }

    public File getExtraDir() {
      return extraDir;
    }

    @Deprecated
    public File getOutDir() {
      return outDir;
    }

    public File getWarDir() {
      return warDir;
    }

    public void setExtraDir(File extraDir) {
      this.extraDir = extraDir;
    }

    @Deprecated
    public void setOutDir(File outDir) {
      this.outDir = outDir;
    }

    public void setWarDir(File warDir) {
      this.warDir = warDir;
    }
  }

  public static void legacyLink(TreeLogger logger, ModuleDef module,
      ArtifactSet generatedArtifacts, Permutation[] permutations,
      List<FileBackedObject<PermutationResult>> resultFiles, File outDir,
      JJSOptions precompileOptions) throws UnableToCompleteException {
    StandardLinkerContext linkerContext = new StandardLinkerContext(logger,
        module, precompileOptions);
    ArtifactSet artifacts = doLink(logger, linkerContext, generatedArtifacts,
        permutations, resultFiles);
    doProduceLegacyOutput(logger, artifacts, linkerContext, module, outDir);
  }

  public static void link(TreeLogger logger, ModuleDef module,
      ArtifactSet generatedArtifacts, Permutation[] permutations,
      List<FileBackedObject<PermutationResult>> resultFiles, File outDir,
      File extrasDir, JJSOptions precompileOptions)
      throws UnableToCompleteException {
    StandardLinkerContext linkerContext = new StandardLinkerContext(logger,
        module, precompileOptions);
    ArtifactSet artifacts = doLink(logger, linkerContext, generatedArtifacts,
        permutations, resultFiles);
    doProduceOutput(logger, artifacts, linkerContext, module, outDir, extrasDir);
  }

  public static void main(String[] args) {
    /*
     * NOTE: main always exits with a call to System.exit to terminate any
     * non-daemon threads that were started in Generators. Typically, this is to
     * shutdown AWT related threads, since the contract for their termination is
     * still implementation-dependent.
     */
    final LinkOptions options = new LinkOptionsImpl();

    if (new ArgProcessor(options).processArgs(args)) {
      CompileTask task = new CompileTask() {
        public boolean run(TreeLogger logger) throws UnableToCompleteException {
          return new Link(options).run(logger);
        }
      };
      if (CompileTaskRunner.runWithAppropriateLogger(options, task)) {
        // Exit w/ success code.
        System.exit(0);
      }
    }
    // Exit w/ non-success code.
    System.exit(1);
  }

  private static ArtifactSet doLink(TreeLogger logger,
      StandardLinkerContext linkerContext, ArtifactSet generatedArtifacts,
      Permutation[] perms, List<FileBackedObject<PermutationResult>> resultFiles)
      throws UnableToCompleteException {
    if (perms.length != resultFiles.size()) {
      throw new IllegalArgumentException(
          "Mismatched resultFiles.length and permutation count");
    }

    for (int i = 0; i < perms.length; ++i) {
      finishPermuation(logger, perms[i], resultFiles.get(i), linkerContext);
    }

    linkerContext.addOrReplaceArtifacts(generatedArtifacts);
    return linkerContext.invokeLink(logger);
  }

  private static void doProduceLegacyOutput(TreeLogger logger,
      ArtifactSet artifacts, StandardLinkerContext linkerContext,
      ModuleDef module, File outDir) throws UnableToCompleteException {
    File moduleOutDir = new File(outDir, module.getName());
    File moduleExtraDir = new File(outDir, module.getName() + "-aux");
    Util.recursiveDelete(moduleOutDir, true);
    Util.recursiveDelete(moduleExtraDir, true);
    linkerContext.produceOutputDirectory(logger, artifacts, moduleOutDir);
    linkerContext.produceExtraDirectory(logger, artifacts, moduleExtraDir);
    logger.log(TreeLogger.INFO, "Link succeeded");
  }

  private static void doProduceOutput(TreeLogger logger, ArtifactSet artifacts,
      StandardLinkerContext linkerContext, ModuleDef module, File outDir,
      File extraDir) throws UnableToCompleteException {
    String outPath = outDir.getPath();
    if (!outDir.isDirectory()
        && (outPath.endsWith(".war") || outPath.endsWith(".jar") || outPath.endsWith(".zip"))) {
      linkerContext.produceOutputZip(logger, artifacts, outDir,
          module.getName() + '/');
    } else {
      File moduleOutDir = new File(outDir, module.getName());
      Util.recursiveDelete(moduleOutDir, true);
      linkerContext.produceOutputDirectory(logger, artifacts, moduleOutDir);
    }

    if (extraDir != null) {
      String extraPath = extraDir.getPath();
      if (!extraDir.isDirectory()
          && (extraPath.endsWith(".war") || extraPath.endsWith(".jar") || extraPath.endsWith(".zip"))) {
        linkerContext.produceExtraZip(logger, artifacts, extraDir,
            module.getName() + '/');
      } else {
        File moduleExtraDir = new File(extraDir, module.getName());
        Util.recursiveDelete(moduleExtraDir, true);
        linkerContext.produceExtraDirectory(logger, artifacts, moduleExtraDir);
      }
    }
    logger.log(TreeLogger.INFO, "Link succeeded");
  }

  private static void finishPermuation(TreeLogger logger, Permutation perm,
      FileBackedObject<PermutationResult> resultFile,
      StandardLinkerContext linkerContext) throws UnableToCompleteException {
    StandardCompilationResult compilation = linkerContext.getCompilation(
        logger, resultFile);
    StaticPropertyOracle[] propOracles = perm.getPropertyOracles();
    for (StaticPropertyOracle propOracle : propOracles) {
      BindingProperty[] orderedProps = propOracle.getOrderedProps();
      String[] orderedPropValues = propOracle.getOrderedPropValues();
      Map<SelectionProperty, String> unboundProperties = new HashMap<SelectionProperty, String>();
      for (int i = 0; i < orderedProps.length; i++) {
        SelectionProperty key = linkerContext.getProperty(orderedProps[i].getName());
        if (key.tryGetValue() != null) {
          /*
           * The view of the Permutation doesn't include properties with defined
           * values.
           */
          continue;
        } else if (key.isDerived()) {
          /*
           * The property provider does not need to be invoked, because the
           * value is determined entirely by other properties.
           */
          continue;
        }
        unboundProperties.put(key, orderedPropValues[i]);
      }
      compilation.addSelectionPermutation(unboundProperties);
    }
    logScriptSize(logger, perm.getId(), compilation);
  }

  /**
   * Logs the total script size for this permutation, as calculated by
   * {@link CodeSplitter#totalScriptSize(int[])}.
   */
  private static void logScriptSize(TreeLogger logger, int permId,
      StandardCompilationResult compilation) {
    if (!logger.isLoggable(TreeLogger.TRACE)) {
      return;
    }

    String[] javaScript = compilation.getJavaScript();

    int[] jsLengths = new int[javaScript.length];
    for (int i = 0; i < javaScript.length; i++) {
      jsLengths[i] = javaScript[i].length();
    }

    int totalSize = CodeSplitter.totalScriptSize(jsLengths);

    logger.log(TreeLogger.TRACE, "Permutation " + permId + " (strong name "
        + compilation.getStrongName() + ") has an initial download size of "
        + javaScript[0].length() + " and total script size of " + totalSize);
  }

  private final LinkOptionsImpl options;

  public Link(LinkOptions options) {
    this.options = new LinkOptionsImpl(options);
  }

  public boolean run(TreeLogger logger) throws UnableToCompleteException {
    for (String moduleName : options.getModuleNames()) {
      File compilerWorkDir = options.getCompilerWorkDir(moduleName);
      Collection<PrecompilationFile> precomps;
      try {
        precomps = PrecompilationFile.scanJarFile(new File(compilerWorkDir,
            Precompile.PRECOMPILE_FILENAME));
      } catch (IOException e) {
        logger.log(TreeLogger.ERROR, "Failed to scan "
            + Precompile.PRECOMPILE_FILENAME, e);
        return false;
      }

      ModuleDef module = ModuleDefLoader.loadFromClassPath(logger, moduleName);

      if (precomps.isEmpty()) {
        logger.log(TreeLogger.ERROR, "No precompilation files found in '"
            + compilerWorkDir.getAbsolutePath()
            + "'; please run Precompile first");
        return false;
      }

      List<Permutation> permsList = new ArrayList<Permutation>();
      ArtifactSet generatedArtifacts = new ArtifactSet();
      JJSOptions precompileOptions = null;

      for (PrecompilationFile precompilationFile : precomps) {
        Precompilation precompilation;
        try {
          precompilation = precompilationFile.newInstance(logger);
        } catch (UnableToCompleteException e) {
          return false;
        }
        permsList.addAll(Arrays.asList(precompilation.getPermutations()));
        generatedArtifacts.addAll(precompilation.getGeneratedArtifacts());
        precompileOptions = precompilation.getUnifiedAst().getOptions();
      }

      Permutation[] perms = permsList.toArray(new Permutation[permsList.size()]);

      List<FileBackedObject<PermutationResult>> resultFiles = new ArrayList<FileBackedObject<PermutationResult>>(
          perms.length);
      for (int i = 0; i < perms.length; ++i) {
        File f = CompilePerms.makePermFilename(compilerWorkDir,
            perms[i].getId());
        if (!f.exists()) {
          logger.log(TreeLogger.ERROR, "File not found '" + f.getAbsolutePath()
              + "'; please compile all permutations");
          return false;
        }
        resultFiles.add(new FileBackedObject<PermutationResult>(
            PermutationResult.class, f));
      }

      TreeLogger branch = logger.branch(TreeLogger.INFO, "Linking module "
          + module.getName());
      StandardLinkerContext linkerContext = new StandardLinkerContext(branch,
          module, precompileOptions);

      ArtifactSet artifacts = doLink(branch, linkerContext, generatedArtifacts,
          perms, resultFiles);

      if (options.getOutDir() == null) {
        doProduceOutput(branch, artifacts, linkerContext, module,
            options.getWarDir(), options.getExtraDir());
      } else {
        doProduceLegacyOutput(branch, artifacts, linkerContext, module,
            options.getOutDir());
      }
    }
    return true;
  }
}
