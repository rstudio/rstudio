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
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.impl.ResourceGeneratorUtilImpl;
import com.google.gwt.dev.BuildTarget.OutputFreshness;
import com.google.gwt.dev.cfg.Library;
import com.google.gwt.dev.cfg.LibraryGroup;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.cfg.Properties;
import com.google.gwt.dev.cfg.ResourceLoader;
import com.google.gwt.dev.resource.impl.ResourceOracleImpl;
import com.google.gwt.dev.resource.impl.ZipFileClassPathEntry;
import com.google.gwt.thirdparty.guava.common.annotations.VisibleForTesting;
import com.google.gwt.thirdparty.guava.common.base.Joiner;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Incrementally builds, links, and rebuilds module trees.
 */
public class IncrementalBuilder {

  /**
   * Represents a combination of whether a build succeeded and whether output changed.
   */
  public static enum BuildResultStatus {
    FAILED(false, false), SUCCESS_NO_CHANGES(false, true), SUCCESS_WITH_CHANGES(true, true);

    private static BuildResultStatus get(boolean success) {
      return success ? SUCCESS_WITH_CHANGES : FAILED;
    }

    private boolean outputChanged;
    private boolean success;

    private BuildResultStatus(boolean outputChanged, boolean success) {
      this.outputChanged = outputChanged;
      this.success = success;
    }

    public boolean isSuccess() {
      return success;
    }

    public boolean outputChanged() {
      return outputChanged;
    }
  }

  @VisibleForTesting
  static final String NO_FILES_HAVE_CHANGED = "No files have changed; all output is still fresh.";

  @VisibleForTesting
  protected static String formatCircularModulePathMessage(List<String> circularModulePath) {
    return "Can't compile because of a module circular reference:\n  "
        + Joiner.on("\n  ").join(circularModulePath);
  }

  private Map<String, BuildTarget> buildTargetsByCanonicalModuleName = Maps.newLinkedHashMap();
  private List<List<String>> circularReferenceModuleNameLoops = Lists.newArrayList();
  private Properties finalProperties;
  private String genDir;
  private Set<String> knownCircularlyReferentModuleNames = Sets.newHashSet();
  private Set<String> moduleReferencePath = Sets.newLinkedHashSet();
  private String outputDir;
  private final ResourceLoader resourceLoader;
  private BuildTarget rootBuildTarget;
  private ModuleDef rootModule;
  private final String rootModuleName;
  private String warDir;
  private BuildTargetOptions buildTargetOptions = new BuildTargetOptions() {

    @Override
    public Properties getFinalProperties() {
      return IncrementalBuilder.this.finalProperties;
    }

    @Override
    public String getGenDir() {
      return IncrementalBuilder.this.genDir;
    }

    @Override
    public String getOutputDir() {
      return IncrementalBuilder.this.outputDir;
    }

    @Override
    public ResourceLoader getResourceLoader() {
      return IncrementalBuilder.this.resourceLoader;
    }

    @Override
    public String getWarDir() {
      return IncrementalBuilder.this.warDir;
    }
  };

  public IncrementalBuilder(String rootModuleName, String warDir, String libDir, String genDir,
      ResourceLoader resourceLoader) {
    this.rootModuleName = rootModuleName;
    this.warDir = warDir;
    this.outputDir = libDir;
    this.genDir = genDir;
    this.resourceLoader = resourceLoader;
  }

  public BuildResultStatus build(TreeLogger logger) {
    try {
      logger = logger.branch(TreeLogger.INFO, "Performing an incremental build");

      CompilerContext compilerContext = new CompilerContext.Builder().compileMonolithic(false)
          .libraryGroup(LibraryGroup.fromLibraries(Lists.<Library> newArrayList(), false)).build();
      long beforeLoadRootModuleMs = System.currentTimeMillis();
      rootModule = ModuleDefLoader.loadFromResources(logger, compilerContext, rootModuleName,
          resourceLoader, false);
      finalProperties = rootModule.getProperties();
      long loadRootModuleDurationMs = System.currentTimeMillis() - beforeLoadRootModuleMs;
      logger.log(TreeLogger.INFO, String.format(
          "%.3fs -- Parsing and loading root module definition in %s",
          loadRootModuleDurationMs / 1000d, rootModuleName));

      long beforeCreateTargetGraphMs = System.currentTimeMillis();
      rootBuildTarget = createBuildTarget(logger, rootModuleName);
      rootBuildTarget.setModule(rootModule);
      long createdTargetGraphDurationMs = System.currentTimeMillis() - beforeCreateTargetGraphMs;
      logger.log(TreeLogger.INFO, String.format("%.3fs -- Creating target graph (%s targets)",
          createdTargetGraphDurationMs / 1000d, buildTargetsByCanonicalModuleName.size()));

      if (!circularReferenceModuleNameLoops.isEmpty()) {
        for (List<String> circularReferenceModuleNameLoop : circularReferenceModuleNameLoops) {
          logger.log(TreeLogger.ERROR,
              formatCircularModulePathMessage(circularReferenceModuleNameLoop));
        }
        throw new UnableToCompleteException();
      }
      logLoadedBuildTargetGraph(logger, buildTargetsByCanonicalModuleName);

      long beforeComputeOutputFreshnessMs = System.currentTimeMillis();
      ModuleDefLoader.clearModuleCache();
      rootBuildTarget.computeOutputFreshness(logger);
      long computeOutputFreshnessDurationMs =
          System.currentTimeMillis() - beforeComputeOutputFreshnessMs;
      logger.log(TreeLogger.INFO, String.format("%.3fs -- Computing per-target output freshness",
          computeOutputFreshnessDurationMs / 1000d));

      TreeLogger branch = logger.branch(TreeLogger.INFO, "Compiling target graph");
      boolean success = rootBuildTarget.link(branch);
      return BuildResultStatus.get(success);
    } catch (UnableToCompleteException e) {
      // The real cause has been logged.
      return BuildResultStatus.FAILED;
    }
  }

  public String getRootModuleName() {
    if (rootModule == null) {
      return "UNKNOWN";
    }
    return rootModule.getName();
  }

  public boolean isRootModuleKnown() {
    return rootModule != null;
  }

  public BuildResultStatus rebuild(TreeLogger logger) {
    logger = logger.branch(TreeLogger.INFO, "Performing an incremental rebuild");

    ResourceOracleImpl.clearCache();
    ZipFileClassPathEntry.clearCache();
    ModuleDefLoader.clearModuleCache();
    ResourceGeneratorUtilImpl.clearGeneratedFilesByName();

    long beforeComputeOutputFreshnessMs = System.currentTimeMillis();
    forgetAllOutputFreshness();
    rootBuildTarget.computeOutputFreshness(logger);
    long computeOutputFreshnessDurationMs =
        System.currentTimeMillis() - beforeComputeOutputFreshnessMs;
    logger.log(TreeLogger.INFO, String.format("%.3fs -- Computing per-target output freshness",
        computeOutputFreshnessDurationMs / 1000d));

    if (rootBuildTarget.isOutputFresh()) {
      logger.log(TreeLogger.INFO, NO_FILES_HAVE_CHANGED);
      return BuildResultStatus.SUCCESS_NO_CHANGES;
    }

    TreeLogger branch = logger.branch(TreeLogger.INFO, "Compiling target graph");
    boolean success = rootBuildTarget.link(branch);
    return BuildResultStatus.get(success);
  }

  public void setWarDir(String warDir) {
    this.warDir = warDir;
  }

  @VisibleForTesting
  void clean() {
    File[] files = new File(outputDir).listFiles();
    if (files == null) {
      // nothing to delete
      return;
    }
    for (File file : files) {
      file.delete();
    }
  }

  private BuildTarget createBuildTarget(String canonicalModuleName, BuildTarget... buildTargets) {
    if (!buildTargetsByCanonicalModuleName.containsKey(canonicalModuleName)) {
      buildTargetsByCanonicalModuleName.put(canonicalModuleName,
          new BuildTarget(canonicalModuleName, buildTargetOptions, buildTargets));
    }
    return buildTargetsByCanonicalModuleName.get(canonicalModuleName);
  }

  private BuildTarget createBuildTarget(TreeLogger logger, String moduleName)
      throws UnableToCompleteException {
    if (isCircularlyReferent(moduleName)) {
      // Allow the target graph creation to continue so that all of the circular reference loops can
      // be gathered.
      return null;
    }
    if (buildTargetsByCanonicalModuleName.containsKey(moduleName)) {
      return buildTargetsByCanonicalModuleName.get(moduleName);
    }

    logger.log(TreeLogger.SPAM, String.format("Adding target %s to build graph.", moduleName));
    moduleReferencePath.add(moduleName);

    List<BuildTarget> dependencyBuildTargets = Lists.newArrayList();
    for (String dependencyModuleName : rootModule.getDirectDependencies(moduleName)) {
      dependencyBuildTargets.add(createBuildTarget(logger, dependencyModuleName));
    }
    moduleReferencePath.remove(moduleName);

    return createBuildTarget(moduleName, dependencyBuildTargets.toArray(new BuildTarget[0]));
  }

  private void forgetAllOutputFreshness() {
    for (BuildTarget buildTarget : buildTargetsByCanonicalModuleName.values()) {
      buildTarget.setOutputFreshness(OutputFreshness.UNKNOWN);
    }
  }

  private boolean isCircularlyReferent(String potentialDuplicateModuleName) {
    if (knownCircularlyReferentModuleNames.contains(potentialDuplicateModuleName)) {
      return true;
    }
    if (!moduleReferencePath.contains(potentialDuplicateModuleName)) {
      return false;
    }

    List<String> circularModuleReferencePath = Lists.newArrayList(moduleReferencePath);

    // Attach the duplicate module name to the end of the loop.
    circularModuleReferencePath.add(potentialDuplicateModuleName);

    List<String> annotatedCircularModuleReferencePath = Lists.newArrayList();
    // The current module path only includes libraries but the connections between libraries might
    // be silently flowing through filesets. Add filesets to the path so that the output is more
    // readable.
    for (int moduleNameIndex = 0; moduleNameIndex < circularModuleReferencePath.size() - 1;
        moduleNameIndex++) {
      String thisModuleName = circularModuleReferencePath.get(moduleNameIndex);
      String nextModuleName = circularModuleReferencePath.get(moduleNameIndex + 1);

      annotatedCircularModuleReferencePath.add(
          thisModuleName + (thisModuleName.equals(potentialDuplicateModuleName) ? " <loop>" : ""));

      List<String> fileSetPath = rootModule.getFileSetPathBetween(thisModuleName, nextModuleName);
      if (fileSetPath != null) {
        for (String fileSetModuleName : fileSetPath) {
          annotatedCircularModuleReferencePath.add(fileSetModuleName + " <fileset>");
        }
      }
    }

    // Attach the duplicate module name to the end of the loop.
    annotatedCircularModuleReferencePath.add(potentialDuplicateModuleName + " <loop>");

    knownCircularlyReferentModuleNames.addAll(annotatedCircularModuleReferencePath);
    circularReferenceModuleNameLoops.add(annotatedCircularModuleReferencePath);
    return true;
  }

  private void logLoadedBuildTargetGraph(TreeLogger logger,
      Map<String, BuildTarget> buildTargetsByCanonicalModuleName) {
    logger.log(TreeLogger.SPAM, "Loaded build target graph:");
    for (String canonicalModuleName : buildTargetsByCanonicalModuleName.keySet()) {
      logger.log(TreeLogger.SPAM, "\t" + canonicalModuleName);
      BuildTarget gwtTarget = buildTargetsByCanonicalModuleName.get(canonicalModuleName);
      for (BuildTarget dependencyBuildTarget : gwtTarget.getDependencyBuildTargets()) {
        logger.log(TreeLogger.SPAM, "\t\t" + dependencyBuildTarget.getCanonicalModuleName());
      }
    }
  }
}
