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

import com.google.gwt.core.ext.Linker;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.impl.PropertiesUtil;
import com.google.gwt.core.linker.CrossSiteIframeLinker;
import com.google.gwt.core.linker.IFrameLinker;
import com.google.gwt.dev.Compiler;
import com.google.gwt.dev.CompilerContext;
import com.google.gwt.dev.CompilerOptions;
import com.google.gwt.dev.MinimalRebuildCache;
import com.google.gwt.dev.MinimalRebuildCacheManager;
import com.google.gwt.dev.NullRebuildCache;
import com.google.gwt.dev.cfg.BindingProperty;
import com.google.gwt.dev.cfg.ConfigurationProperties;
import com.google.gwt.dev.cfg.ConfigurationProperty;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.cfg.PropertyCombinations;
import com.google.gwt.dev.cfg.PropertyCombinations.PermutationDescription;
import com.google.gwt.dev.cfg.ResourceLoader;
import com.google.gwt.dev.cfg.ResourceLoaders;
import com.google.gwt.dev.codeserver.Job.Result;
import com.google.gwt.dev.codeserver.JobEvent.CompileStrategy;
import com.google.gwt.dev.javac.UnitCache;
import com.google.gwt.dev.resource.impl.ResourceOracleImpl;
import com.google.gwt.dev.resource.impl.ZipFileClassPathEntry;
import com.google.gwt.dev.util.log.CompositeTreeLogger;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.thirdparty.guava.common.base.Charsets;
import com.google.gwt.thirdparty.guava.common.base.Joiner;
import com.google.gwt.thirdparty.guava.common.base.Objects;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableMap;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.io.Files;
import com.google.gwt.thirdparty.guava.common.io.Resources;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Recompiles a GWT module on demand.
 */
public class Recompiler {

  private final OutboxDir outboxDir;
  private final LauncherDir launcherDir;
  private final MinimalRebuildCacheManager minimalRebuildCacheManager;
  private final String inputModuleName;

  private String serverPrefix;
  private int compilesDone = 0;

  // after renaming
  private AtomicReference<String> outputModuleName = new AtomicReference<String>(null);

  private final AtomicReference<CompileDir> lastBuild = new AtomicReference<CompileDir>();

  private InputSummary previousInputSummary;

  private CompileDir publishedCompileDir;
  private final AtomicReference<ResourceLoader> resourceLoader =
      new AtomicReference<ResourceLoader>();
  private final CompilerContext.Builder compilerContextBuilder = new CompilerContext.Builder();
  private CompilerContext compilerContext;
  private final Options options;
  private final UnitCache unitCache;

  Recompiler(OutboxDir outboxDir, LauncherDir launcherDir,
      String inputModuleName, Options options,
      UnitCache unitCache, MinimalRebuildCacheManager minimalRebuildCacheManager) {
    this.outboxDir = outboxDir;
    this.launcherDir = launcherDir;
    this.inputModuleName = inputModuleName;
    this.options = options;
    this.unitCache = unitCache;
    this.minimalRebuildCacheManager = minimalRebuildCacheManager;
    this.serverPrefix = options.getPreferredHost() + ":" + options.getPort();
    compilerContext = compilerContextBuilder.build();
  }

  /**
   * Forces the next recompile even if no input files have changed.
   */
  void forceNextRecompile() {
    previousInputSummary = null;
  }

  /**
   * Compiles the first time, while Super Dev Mode is starting up.
   * Either this method or {@link #initWithoutPrecompile} should be called first.
   */
  synchronized Job.Result precompile(Job job) throws UnableToCompleteException {
    Result result = compile(job);
    job.onFinished(result);
    assert result.isOk();
    return result;
  }

  /**
   * Recompiles the module.
   *
   * <p>Prerequisite: either {@link #precompile} or {@link #initWithoutPrecompile} should have been
   * called first.
   *
   * <p>Sets the job's result and returns normally whether the compile succeeds or not.
   *
   * @param job should already be in the "in progress" state.
   */
  synchronized Job.Result recompile(Job job) {

    Job.Result result;
    try {
      result = compile(job);
    } catch (UnableToCompleteException e) {
      // No point in logging a stack trace for this exception
      job.getLogger().log(TreeLogger.Type.WARN, "recompile failed");
      result = new Result(null, null, e);
    } catch (Throwable error) {
      job.getLogger().log(TreeLogger.Type.WARN, "recompile failed", error);
      result = new Result(null, null, error);
    }

    job.onFinished(result);
    return result;
  }

  /**
   * Calls the GWT compiler with the appropriate settings.
   * Side-effect: a MinimalRebuildCache for the current binding properties will be found or created.
   *
   * @param job used for reporting progress. (Its result will not be set.)
   * @return a non-error Job.Result if successful.
   * @throws UnableToCompleteException for compile failures.
   */
  private Job.Result compile(Job job) throws UnableToCompleteException {

    assert job.wasSubmitted();

    if (compilesDone == 0) {
      System.setProperty("java.awt.headless", "true");
      if (System.getProperty("gwt.speedtracerlog") == null) {
        System.setProperty("gwt.speedtracerlog",
            outboxDir.getSpeedTracerLogFile().getAbsolutePath());
      }
      compilerContext = compilerContextBuilder.unitCache(unitCache).build();
    }

    long startTime = System.currentTimeMillis();
    int compileId = ++compilesDone;
    CompileDir compileDir = outboxDir.makeCompileDir(job.getLogger());
    TreeLogger compileLogger = makeCompileLogger(compileDir, job.getLogger());
    try {
      job.onStarted(compileId, compileDir);
      boolean success = doCompile(compileLogger, compileDir, job);
      if (!success) {
        compileLogger.log(TreeLogger.Type.ERROR, "Compiler returned false");
        throw new UnableToCompleteException();
      }
    } finally {
      // Make the error log available no matter what happens
      lastBuild.set(compileDir);
    }

    long elapsedTime = System.currentTimeMillis() - startTime;
    compileLogger.log(TreeLogger.Type.INFO,
        String.format("%.3fs total -- Compile completed", elapsedTime / 1000d));

    return new Result(publishedCompileDir, outputModuleName.get(), null);
  }

  /**
   * Creates a dummy output directory without compiling the module.
   * Either this method or {@link #precompile} should be called first.
   */
  synchronized Job.Result initWithoutPrecompile(TreeLogger logger)
      throws UnableToCompleteException {

    long startTime = System.currentTimeMillis();
    CompileDir compileDir = outboxDir.makeCompileDir(logger);
    TreeLogger compileLogger = makeCompileLogger(compileDir, logger);
    ModuleDef module;
    try {
      module = loadModule(compileLogger);

      logger.log(TreeLogger.INFO, "Loading Java files in " + inputModuleName + ".");
      CompilerOptions loadOptions = new CompilerOptionsImpl(compileDir, inputModuleName, options);
      compilerContext = compilerContextBuilder.options(loadOptions).unitCache(unitCache).build();

      // Loads and parses all the Java files in the GWT application using the JDT.
      // (This is warmup to make compiling faster later; we stop at this point to avoid
      // needing to know the binding properties.)
      module.getCompilationState(compileLogger, compilerContext);

      setUpCompileDir(compileDir, module, compileLogger);
      if (launcherDir != null) {
        launcherDir.update(module, compileDir, compileLogger);
      }

      outputModuleName.set(module.getName());
    } finally {
      // Make the compile log available no matter what happens.
      lastBuild.set(compileDir);
    }

    long elapsedTime = System.currentTimeMillis() - startTime;
    compileLogger.log(TreeLogger.Type.INFO, "Module setup completed in " + elapsedTime + " ms");

    return new Result(compileDir, module.getName(), null);
  }

  /**
   * Prepares a stub compile directory.
   * It will include all "public" resources and a nocache.js file that invokes the compiler.
   */
  private void setUpCompileDir(CompileDir compileDir, ModuleDef module,
      TreeLogger compileLogger) throws UnableToCompleteException {
    try {
      String currentModuleName = module.getName();

      // Create the directory.
      File outputDir = new File(
          compileDir.getWarDir().getCanonicalPath() + "/" + currentModuleName);
      if (!outputDir.exists()) {
        if (!outputDir.mkdir()) {
          compileLogger.log(Type.WARN, "cannot create directory: " + outputDir);
        }
      }
      LauncherDir.writePublicResources(outputDir, module, compileLogger);

      // write no cache that will inject recompile.nocache.js
      String stub = LauncherDir.generateStubNocacheJs(module.getName(), options);
      File noCacheJs = new File(outputDir.getCanonicalPath(), module.getName() + ".nocache.js");
      Files.write(stub, noCacheJs, Charsets.UTF_8);

      // Create a "module_name.recompile.nocache.js" that calculates the permutation
      // and forces a recompile.
      String recompileNoCache = generateModuleRecompileJs(module, compileLogger);
      writeRecompileNoCacheJs(outputDir, currentModuleName, recompileNoCache, compileLogger);
    } catch (IOException e) {
      compileLogger.log(Type.ERROR, "Error creating stub compile directory.", e);
      UnableToCompleteException wrapped = new UnableToCompleteException();
      wrapped.initCause(e);
      throw wrapped;
    }
  }

  /**
   * Generates the nocache.js file to use when precompile is not on.
   */
  private static String generateModuleRecompileJs(ModuleDef module, TreeLogger compileLogger)
      throws UnableToCompleteException {

    String outputModuleName = module.getName();
    try {
      String templateJs = Resources.toString(
          Resources.getResource(Recompiler.class, "recompile_template.js"), Charsets.UTF_8);
      String propertyProviders = PropertiesUtil.generatePropertiesSnippet(module, compileLogger);
      String libJs = Resources.toString(
          Resources.getResource(Recompiler.class, "recompile_lib.js"), Charsets.UTF_8);
      String recompileJs = Resources.toString(
          Resources.getResource(Recompiler.class, "recompile_main.js"), Charsets.UTF_8);
      templateJs = templateJs.replace("__MODULE_NAME__", "'" + outputModuleName + "'");
      templateJs = templateJs.replace("__PROPERTY_PROVIDERS__", propertyProviders);
      templateJs = templateJs.replace("__LIB_JS__", libJs);
      templateJs = templateJs.replace("__MAIN__", recompileJs);

      return templateJs;

    } catch (IOException e) {
      compileLogger.log(Type.ERROR, "Can not generate + " + outputModuleName + " recompile js", e);
      throw new UnableToCompleteException();
    }
  }

  synchronized String getRecompileJs(TreeLogger logger) throws UnableToCompleteException {
    ModuleDef loadModule = loadModule(logger);
    return generateModuleRecompileJs(loadModule, logger);
  }

  private boolean doCompile(TreeLogger compileLogger, CompileDir compileDir, Job job)
      throws UnableToCompleteException {

    job.onProgress("Loading modules");

    CompilerOptions loadOptions = new CompilerOptionsImpl(compileDir, inputModuleName, options);
    compilerContext = compilerContextBuilder.options(loadOptions).build();

    ModuleDef module = loadModule(compileLogger);

    // We need to generate the stub before restricting permutations
    String recompileJs = generateModuleRecompileJs(module, compileLogger);

    Map<String, String> bindingProperties =
        restrictPermutations(compileLogger, module, job.getBindingProperties());

    // Propagates module rename.
    String newModuleName = module.getName();
    outputModuleName.set(newModuleName);

    // Check if the module definition + job specific binding property restrictions expanded to more
    // than permutation.
    PropertyCombinations propertyCombinations =
        new PropertyCombinations(module.getProperties(), module.getActiveLinkerNames());
    List<PropertyCombinations> permutationPropertySets = propertyCombinations.collapseProperties();
    if (options.isIncrementalCompileEnabled() && permutationPropertySets.size() > 1) {
      compileLogger.log(Type.INFO,
          "Current binding properties are expanding to more than one permutation "
          + "but incremental compilation requires that each compile operate on only "
          + "one permutation.");
      job.setCompileStrategy(CompileStrategy.SKIPPED);
      return true;
    }
    PropertyCombinations permutationPropertySet = permutationPropertySets.get(0);
    PermutationDescription permutationDescription =
        permutationPropertySet.getPermutationDescription(0);

    // Check if we can skip the compile altogether.
    InputSummary inputSummary = new InputSummary(bindingProperties, module);
    if (inputSummary.equals(previousInputSummary)) {
      compileLogger.log(Type.INFO, "skipped compile because no input files have changed");
      job.setCompileStrategy(CompileStrategy.SKIPPED);
      return true;
    }
    // Force a recompile if we don't succeed.
    forceNextRecompile();

    job.onProgress("Compiling");

    CompilerOptions runOptions = new CompilerOptionsImpl(compileDir, newModuleName, options);
    compilerContext = compilerContextBuilder.options(runOptions).build();

    MinimalRebuildCache minimalRebuildCache = new NullRebuildCache();
    if (options.isIncrementalCompileEnabled()) {
      // Returns a copy of the intended cache, which is safe to modify in this compile.
      minimalRebuildCache =
          minimalRebuildCacheManager.getCache(inputModuleName, permutationDescription);
    }
    job.setCompileStrategy(minimalRebuildCache.isPopulated() ? CompileStrategy.INCREMENTAL
        : CompileStrategy.FULL);

    boolean success = Compiler.compile(compileLogger, runOptions, minimalRebuildCache, module);
    if (success) {
      publishedCompileDir = compileDir;
      previousInputSummary = inputSummary;
      if (options.isIncrementalCompileEnabled()) {
        minimalRebuildCacheManager.putCache(inputModuleName, permutationDescription,
            minimalRebuildCache);
      }
      String moduleName = outputModuleName.get();
      writeRecompileNoCacheJs(new File(publishedCompileDir.getWarDir(), moduleName), moduleName,
          recompileJs, compileLogger);
      if (launcherDir != null) {
        launcherDir.update(module, compileDir, compileLogger);
      }
    }

    return success;
  }

  private static void writeRecompileNoCacheJs(File outputDir, String moduleName, String content,
      TreeLogger compileLogger) throws UnableToCompleteException {
    try {
      Files.write(content,
          new File(outputDir.getCanonicalPath() + "/" + moduleName + ".recompile.nocache.js"),
          Charsets.UTF_8);
    } catch (IOException e) {
      compileLogger.log(Type.ERROR, "Can not write recompile.nocache.js", e);
      throw new UnableToCompleteException();
    }
  }

  /**
   * Returns the log from the last compile. (It may be a failed build.)
   */
  File getLastLog() {
    return lastBuild.get().getLogFile();
  }

  /**
   * The module name that the recompiler passes as input to the GWT compiler (before renaming).
   */
  public String getInputModuleName() {
    return inputModuleName;
  }

  /**
   * The module name that the GWT compiler uses in compiled output (after renaming).
   */
  String getOutputModuleName() {
    return outputModuleName.get();
  }

  ResourceLoader getResourceLoader() {
    return resourceLoader.get();
  }

  private TreeLogger makeCompileLogger(CompileDir compileDir, TreeLogger parent)
      throws UnableToCompleteException {
    try {
      PrintWriterTreeLogger fileLogger =
          new PrintWriterTreeLogger(compileDir.getLogFile());
      fileLogger.setMaxDetail(options.getLogLevel());
      return new CompositeTreeLogger(parent, fileLogger);
    } catch (IOException e) {
      parent.log(TreeLogger.ERROR, "unable to open log file: " + compileDir.getLogFile(), e);
      throw new UnableToCompleteException();
    }
  }

  /**
   * Loads the module and configures it for SuperDevMode. (Does not restrict permutations.)
   */
  private ModuleDef loadModule(TreeLogger logger) throws UnableToCompleteException {

    // make sure we get the latest version of any modified jar
    ZipFileClassPathEntry.clearCache();
    ResourceOracleImpl.clearCache();

    ResourceLoader resources = ResourceLoaders.forClassLoader(Thread.currentThread());
    resources = ResourceLoaders.forPathAndFallback(options.getSourcePath(), resources);
    this.resourceLoader.set(resources);

    // ModuleDefLoader.loadFromResources() checks for modified .gwt.xml files.
    ModuleDef moduleDef = ModuleDefLoader.loadFromResources(
        logger, inputModuleName, resources, true);
    compilerContext = compilerContextBuilder.module(moduleDef).build();

    // Undo all permutation restriction customizations from previous compiles.
    for (BindingProperty bindingProperty : moduleDef.getProperties().getBindingProperties()) {
      bindingProperty.resetGeneratedValues();
    }

    // A snapshot of the module's configuration before we modified it.
    ConfigurationProperties config = new ConfigurationProperties(moduleDef);

    // We need a cross-site linker. Automatically replace the default linker.
    if (IFrameLinker.class.isAssignableFrom(moduleDef.getActivePrimaryLinker())) {
      moduleDef.addLinker("xsiframe");
    }

    // Check that we have a compatible linker.
    Class<? extends Linker> linker = moduleDef.getActivePrimaryLinker();
    if (!CrossSiteIframeLinker.class.isAssignableFrom(linker)) {
      logger.log(TreeLogger.ERROR,
          "linkers other than CrossSiteIFrameLinker aren't supported. Found: " + linker.getName());
      throw new UnableToCompleteException();
    }

    // Deactivate precompress linker.
    if (moduleDef.deactivateLinker("precompress")) {
      logger.log(TreeLogger.WARN, "Deactivated PrecompressLinker");
    }

    // Print a nice error if the superdevmode hook isn't present
    if (config.getStrings("devModeRedirectEnabled").isEmpty()) {
      throw new RuntimeException("devModeRedirectEnabled isn't set for module: " +
          moduleDef.getName());
    }

    // Disable the redirect hook here to make sure we don't have an infinite loop.
    // (There is another check in the JavaScript, but just in case.)
    overrideConfig(moduleDef, "devModeRedirectEnabled", "false");

    // Turn off "installCode" if it's on because it makes debugging harder.
    // (If it's already off, don't change anything.)
    if (config.getBoolean("installCode", true)) {
      overrideConfig(moduleDef, "installCode", "false");
      // Make sure installScriptJs is set to the default for compiling without installCode.
      overrideConfig(moduleDef, "installScriptJs",
          "com/google/gwt/core/ext/linker/impl/installScriptDirect.js");
    }

    // override computeScriptBase.js to enable the "Compile" button
    overrideConfig(moduleDef, "computeScriptBaseJs",
        "com/google/gwt/dev/codeserver/computeScriptBase.js");
    // Fix bug with SDM and Chrome 24+ where //@ sourceURL directives cause X-SourceMap header to be ignored
    // Frustratingly, Chrome won't canonicalize a relative URL
    overrideConfig(moduleDef, "includeSourceMapUrl", "http://" + serverPrefix +
        SourceHandler.sourceMapLocationTemplate(moduleDef.getName()));

    // If present, set some config properties back to defaults.
    // (Needed for Google's server-side linker.)
    maybeOverrideConfig(moduleDef, "includeBootstrapInPrimaryFragment", "false");
    maybeOverrideConfig(moduleDef, "permutationsJs",
        "com/google/gwt/core/ext/linker/impl/permutations.js");
    maybeOverrideConfig(moduleDef, "propertiesJs",
        "com/google/gwt/core/ext/linker/impl/properties.js");

    if (options.isIncrementalCompileEnabled()) {
      // CSSResourceGenerator needs to produce stable, unique naming for its input.
      // Currently on default settings CssResourceGenerator's obfuscation depends on
      // whole world knowledge and thus will produce collision in obfuscated mode, since in
      // incremental compiles that information is not available.
      //
      // TODO(dankurka): Once we do proper stable hashing of classes in CssResourceGenerator, we
      // can probably replace / remove this.
      maybeOverrideConfig(moduleDef, "CssResource.style", "stable");
    }

    overrideBinding(moduleDef, "compiler.useSourceMaps", "true");
    overrideBinding(moduleDef, "compiler.useSymbolMaps", "false");
    overrideBinding(moduleDef, "superdevmode", "on");

    return moduleDef;
  }

  /**
   * Restricts the compiled permutations by applying the given binding properties, if possible.
   * In some cases, a different binding may be chosen instead.
   */
  private Map<String, String> restrictPermutations(TreeLogger logger, ModuleDef moduleDef,
      Map<String, String> bindingProperties) {

    Map<String, String> chosenProps = Maps.newHashMap();

    for (Map.Entry<String, String> entry : bindingProperties.entrySet()) {
      String propName = entry.getKey();
      String propValue = entry.getValue();
      String actual = maybeSetBinding(logger, moduleDef, propName, propValue);
      if (actual != null) {
        chosenProps.put(propName, actual);
      }
    }

    return chosenProps;
  }

  /**
   * Attempts to set a binding property to the given value.
   * If the value is not allowed, see if we can find a value that will work.
   * There is a special case for "locale".
   * @return the value actually set, or null if unable to set the property
   */
  private static String maybeSetBinding(TreeLogger logger, ModuleDef module, String propName,
      String newValue) {

    logger = logger.branch(TreeLogger.Type.INFO, "binding: " + propName + "=" + newValue);

    BindingProperty binding = module.getProperties().findBindingProp(propName);
    if (binding == null) {
      logger.log(TreeLogger.Type.WARN, "undefined property: '" + propName + "'");
      return null;
    }

    if (!binding.isAllowedValue(newValue)) {

      String[] allowedValues = binding.getAllowedValues(binding.getRootCondition());
      logger.log(TreeLogger.Type.WARN, "property '" + propName +
          "' cannot be set to '" + newValue + "'");
      logger.log(TreeLogger.Type.INFO, "allowed values: " +
          Joiner.on(", ").join(allowedValues));

      // See if we can fall back on a reasonable default.
      if (allowedValues.length == 1) {
        // There is only one possibility, so use it.
        newValue = allowedValues[0];
      } else if (binding.getName().equals("locale")) {
        // TODO: come up with a more general solution. Perhaps fail
        // the compile and give the user a way to override the property?
        newValue = chooseDefault(binding, "default", "en", "en_US");
      } else {
        // There is more than one. Continue and possibly compile multiple permutations.
        logger.log(TreeLogger.Type.INFO, "continuing without " + propName +
            ". Sourcemaps may not work.");
        return null;
      }

      logger.log(TreeLogger.Type.INFO, "recovered with " + propName + "=" + newValue);
    }

    binding.setRootGeneratedValues(newValue);
    return newValue;
  }

  private static String chooseDefault(BindingProperty property, String... candidates) {
    for (String candidate : candidates) {
      if (property.isAllowedValue(candidate)) {
        return candidate;
      }
    }
    return property.getFirstAllowedValue();
  }

  /**
   * Sets a binding even if it's set to a different value in the GWT application.
   */
  private static void overrideBinding(ModuleDef module, String propName, String newValue) {
    BindingProperty binding = module.getProperties().findBindingProp(propName);
    if (binding != null) {
      // This sets both allowed and generated values, which is needed since the module
      // might have explicitly disallowed the value.
      // It persists over multiple compiles but that's okay since we set it the same way
      // every time.
      binding.setValues(binding.getRootCondition(), newValue);
    }
  }

  private static boolean maybeOverrideConfig(ModuleDef module, String propName, String newValue) {
    ConfigurationProperty config = module.getProperties().findConfigProp(propName);
    if (config != null) {
      config.setValue(newValue);
      return true;
    }
    return false;
  }

  private static void overrideConfig(ModuleDef module, String propName, String newValue) {
    if (!maybeOverrideConfig(module, propName, newValue)) {
      throw new RuntimeException("not found: " + propName);
    }
  }

  /**
   * Summarizes the inputs to a GWT compile. (Immutable.)
   * Two summaries should be equal if the compiler's inputs are equal (with high probability).
   */
  private static class InputSummary {
    private final ImmutableMap<String, String> bindingProperties;
    private final long moduleLastModified;
    private final long resourcesLastModified;
    private final long filenameHash;

    InputSummary(Map<String, String> bindingProperties, ModuleDef module) {
      this.bindingProperties = ImmutableMap.copyOf(bindingProperties);
      this.moduleLastModified = module.lastModified();
      this.resourcesLastModified = module.getResourceLastModified();
      this.filenameHash = module.getInputFilenameHash();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof InputSummary) {
        InputSummary other = (InputSummary) obj;
        return bindingProperties.equals(other.bindingProperties) &&
            moduleLastModified == other.moduleLastModified &&
            resourcesLastModified == other.resourcesLastModified &&
            filenameHash == other.filenameHash;
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(bindingProperties, moduleLastModified, resourcesLastModified,
          filenameHash);
    }
  }
}
