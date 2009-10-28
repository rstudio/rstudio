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
import com.google.gwt.dev.Precompile.PrecompileOptions;
import com.google.gwt.dev.cfg.BindingProperty;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.cfg.StaticPropertyOracle;
import com.google.gwt.dev.jjs.JJSOptions;
import com.google.gwt.dev.jjs.PermutationResult;
import com.google.gwt.dev.jjs.impl.CodeSplitter;
import com.google.gwt.dev.util.FileBackedObject;
import com.google.gwt.dev.util.NullOutputFileSet;
import com.google.gwt.dev.util.OutputFileSet;
import com.google.gwt.dev.util.OutputFileSetOnDirectory;
import com.google.gwt.dev.util.OutputFileSetOnJar;
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
    private File outDir;
    private File warDir;

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
      ArtifactSet generatedArtifacts,
      List<FileBackedObject<PermutationResult>> resultFiles, File outDir,
      JJSOptions precompileOptions) throws UnableToCompleteException,
      IOException {
    StandardLinkerContext linkerContext = new StandardLinkerContext(logger,
        module, precompileOptions);
    ArtifactSet artifacts = doLink(logger, linkerContext, generatedArtifacts,
        resultFiles);
    OutputFileSet outFileSet = new OutputFileSetOnDirectory(outDir,
        module.getName() + "/");
    OutputFileSet extraFileSet = new OutputFileSetOnDirectory(outDir,
        module.getName() + "-aux/");
    doProduceOutput(logger, artifacts, linkerContext, outFileSet, extraFileSet);
  }

  public static void link(TreeLogger logger, ModuleDef module,
      ArtifactSet generatedArtifacts,
      List<FileBackedObject<PermutationResult>> resultFiles, File outDir,
      File extrasDir, JJSOptions precompileOptions)
      throws UnableToCompleteException, IOException {
    StandardLinkerContext linkerContext = new StandardLinkerContext(logger,
        module, precompileOptions);
    ArtifactSet artifacts = doLink(logger, linkerContext, generatedArtifacts,
        resultFiles);
    doProduceOutput(logger, artifacts, linkerContext, chooseOutputFileSet(
        outDir, module.getName() + "/"), chooseOutputFileSet(extrasDir,
        module.getName() + "/"));
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

  /**
   * Add to a compilation result all of the selection permutations from its
   * associated permutation.
   */
  private static void addSelectionPermutations(
      StandardCompilationResult compilation, Permutation perm,
      StandardLinkerContext linkerContext) {
    for (StaticPropertyOracle propOracle : perm.getPropertyOracles()) {
      compilation.addSelectionPermutation(computeSelectionPermutation(
          linkerContext, propOracle));
    }
  }

  /**
   * Choose an output file set for the given <code>dirOrJar</code> based on
   * its name, whether it's null, and whether it already exists as a directory.
   */
  private static OutputFileSet chooseOutputFileSet(File dirOrJar,
      String pathPrefix) throws IOException {
    return chooseOutputFileSet(dirOrJar, pathPrefix, pathPrefix);
  }

  /**
   * A version of {@link #chooseOutputFileSet(File, String)} that allows
   * choosing a separate path prefix depending on whether the output is a
   * directory or a jar file.
   */
  private static OutputFileSet chooseOutputFileSet(File dirOrJar,
      String jarPathPrefix, String dirPathPrefix) throws IOException {

    if (dirOrJar == null) {
      return new NullOutputFileSet();
    }

    String name = dirOrJar.getName();
    if (!dirOrJar.isDirectory()
        && (name.endsWith(".war") || name.endsWith(".jar") || name.endsWith(".zip"))) {
      return new OutputFileSetOnJar(dirOrJar, jarPathPrefix);
    } else {
      Util.recursiveDelete(new File(dirOrJar, dirPathPrefix), true);
      return new OutputFileSetOnDirectory(dirOrJar, dirPathPrefix);
    }
  }

  /**
   * Return a map giving the value of each non-trivial selection property.
   */
  private static Map<SelectionProperty, String> computeSelectionPermutation(
      StandardLinkerContext linkerContext, StaticPropertyOracle propOracle) {
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
         * The property provider does not need to be invoked, because the value
         * is determined entirely by other properties.
         */
        continue;
      }
      unboundProperties.put(key, orderedPropValues[i]);
    }
    return unboundProperties;
  }

  private static ArtifactSet doLink(TreeLogger logger,
      StandardLinkerContext linkerContext, ArtifactSet generatedArtifacts,
      List<FileBackedObject<PermutationResult>> resultFiles)
      throws UnableToCompleteException {
    linkerContext.addOrReplaceArtifacts(generatedArtifacts);
    for (FileBackedObject<PermutationResult> resultFile : resultFiles) {
      PermutationResult result = resultFile.newInstance(logger);
      finishPermutation(logger, result.getPermutation(), result, linkerContext);
    }
    return linkerContext.invokeLink(logger);
  }

  /**
   * Emit final output.
   */
  private static void doProduceOutput(TreeLogger logger, ArtifactSet artifacts,
      StandardLinkerContext linkerContext, OutputFileSet outFileSet,
      OutputFileSet extraFileSet) throws UnableToCompleteException, IOException {
    linkerContext.produceOutput(logger, artifacts, false, outFileSet);
    linkerContext.produceOutput(logger, artifacts, true, extraFileSet);

    outFileSet.close();
    extraFileSet.close();

    logger.log(TreeLogger.INFO, "Link succeeded");
  }

  /**
   * Add a compilation to a linker context.
   */
  private static void finishPermutation(TreeLogger logger, Permutation perm,
      PermutationResult permResult, StandardLinkerContext linkerContext) {
    StandardCompilationResult compilation = linkerContext.getCompilation(permResult);
    addSelectionPermutations(compilation, perm, linkerContext);
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
      ModuleDef module = ModuleDefLoader.loadFromClassPath(logger, moduleName);

      OutputFileSet outFileSet;
      OutputFileSet extraFileSet;
      try {
        if (options.getOutDir() == null) {
          outFileSet = chooseOutputFileSet(options.getWarDir(),
              module.getName() + "/");
          extraFileSet = chooseOutputFileSet(options.getExtraDir(),
              module.getName() + "/");
        } else {
          outFileSet = chooseOutputFileSet(options.getOutDir(),
              module.getName() + "/");
          if (options.getExtraDir() != null) {
            extraFileSet = chooseOutputFileSet(options.getExtraDir(),
                module.getName() + "-aux/", "");
          } else if (outFileSet instanceof OutputFileSetOnDirectory) {
            // Automatically emit extras into the output directory, if it's in
            // fact a directory
            extraFileSet = chooseOutputFileSet(options.getOutDir(),
                module.getName() + "-aux/");
          } else {
            extraFileSet = new NullOutputFileSet();
          }
        }
      } catch (IOException e) {
        logger.log(TreeLogger.ERROR,
            "Unexpected exception while producing output", e);
        throw new UnableToCompleteException();
      }

      List<Permutation> permsList = new ArrayList<Permutation>();
      ArtifactSet generatedArtifacts = new ArtifactSet();
      JJSOptions precompileOptions = null;

      File compilerWorkDir = options.getCompilerWorkDir(moduleName);
      List<Integer> permutationIds = new ArrayList<Integer>();
      PrecompilationResult precompileResults;
      try {
        precompileResults = Util.readFileAsObject(new File(compilerWorkDir,
            Precompile.PRECOMPILE_FILENAME), PrecompilationResult.class);
      } catch (ClassNotFoundException e) {
        logger.log(TreeLogger.ERROR, "Error reading "
            + Precompile.PRECOMPILE_FILENAME);
        return false;
      } catch (IOException e) {
        logger.log(TreeLogger.ERROR, "Error reading "
            + Precompile.PRECOMPILE_FILENAME);
        return false;
      }

      if (precompileResults instanceof PrecompileOptions) {
        /**
         * Precompiling happened on the shards.
         */
        precompileOptions = (JJSOptions) precompileResults;
        int numPermutations = module.getProperties().numPermutations();
        for (int i = 0; i < numPermutations; ++i) {
          permutationIds.add(i);
        }
      } else {
        /**
         * Precompiling happened on the start node.
         */
        Precompilation precompilation = (Precompilation) precompileResults;
        permsList.addAll(Arrays.asList(precompilation.getPermutations()));
        generatedArtifacts.addAll(precompilation.getGeneratedArtifacts());
        precompileOptions = precompilation.getUnifiedAst().getOptions();

        for (Permutation perm : precompilation.getPermutations()) {
          permutationIds.add(perm.getId());
        }
      }

      List<FileBackedObject<PermutationResult>> resultFiles = new ArrayList<FileBackedObject<PermutationResult>>(
          permutationIds.size());
      for (int id : permutationIds) {
        File f = CompilePerms.makePermFilename(compilerWorkDir, id);
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
          resultFiles);
      try {
        doProduceOutput(branch, artifacts, linkerContext, outFileSet,
            extraFileSet);
      } catch (IOException e) {
        logger.log(TreeLogger.ERROR,
            "Unexpected exception while producing output", e);
      }
    }
    return true;
  }
}
