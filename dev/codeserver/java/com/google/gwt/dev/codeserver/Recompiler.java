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
import com.google.gwt.dev.NullRebuildCache;
import com.google.gwt.dev.cfg.BindingProperty;
import com.google.gwt.dev.cfg.ConfigProps;
import com.google.gwt.dev.cfg.ConfigurationProperty;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.cfg.ResourceLoader;
import com.google.gwt.dev.cfg.ResourceLoaders;
import com.google.gwt.dev.codeserver.Job.Result;
import com.google.gwt.dev.codeserver.JobEvent.CompileStrategy;
import com.google.gwt.dev.javac.UnitCacheSingleton;
import com.google.gwt.dev.resource.impl.ResourceOracleImpl;
import com.google.gwt.dev.resource.impl.ZipFileClassPathEntry;
import com.google.gwt.dev.util.log.CompositeTreeLogger;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.thirdparty.guava.common.base.Joiner;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableMap;
import com.google.gwt.thirdparty.guava.common.collect.Maps;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Recompiles a GWT module on demand.
 */
class Recompiler {

  private final AppSpace appSpace;
  private final String inputModuleName;

  private String serverPrefix;
  private int compilesDone = 0;
  private Map<Map<String, String>, MinimalRebuildCache> minimalRebuildCacheForProperties =
      Maps.newHashMap();

  // after renaming
  private AtomicReference<String> outputModuleName = new AtomicReference<String>(null);

  private final AtomicReference<CompileDir> lastBuild = new AtomicReference<CompileDir>();

  private InputSummary lastBuildInput;

  private CompileDir publishedCompileDir;
  private final AtomicReference<ResourceLoader> resourceLoader =
      new AtomicReference<ResourceLoader>();
  private final CompilerContext.Builder compilerContextBuilder = new CompilerContext.Builder();
  private CompilerContext compilerContext;
  private Options options;

  Recompiler(AppSpace appSpace, String inputModuleName, Options options) {
    this.appSpace = appSpace;
    this.inputModuleName = inputModuleName;
    this.options = options;
    this.serverPrefix = options.getPreferredHost() + ":" + options.getPort();
    compilerContext = compilerContextBuilder.build();
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
      result = new Result(null, e);
    } catch (Throwable error) {
      job.getLogger().log(TreeLogger.Type.WARN, "recompile failed", error);
      result = new Result(null, error);
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
            appSpace.getSpeedTracerLogFile().getAbsolutePath());
      }
      compilerContext = compilerContextBuilder.unitCache(
          UnitCacheSingleton.get(job.getLogger(), appSpace.getUnitCacheDir())).build();
    }

    long startTime = System.currentTimeMillis();
    int compileId = ++compilesDone;
    CompileDir compileDir = makeCompileDir(compileId, job.getLogger());
    TreeLogger compileLogger = makeCompileLogger(compileDir, job.getLogger());

    job.onStarted(compileId, compileDir);

    boolean success = doCompile(compileLogger, compileDir, job);
    if (!success) {
      compileLogger.log(TreeLogger.Type.ERROR, "Compiler returned false");
      throw new UnableToCompleteException();
    }

    long elapsedTime = System.currentTimeMillis() - startTime;
    compileLogger.log(TreeLogger.Type.INFO,
        String.format("%.3fs total -- Compile completed", elapsedTime / 1000d));

    return new Result(publishedCompileDir, null);
  }

  /**
   * Creates a dummy output directory without compiling the module.
   * Either this method or {@link #precompile} should be called first.
   */
  synchronized Job.Result initWithoutPrecompile(TreeLogger logger)
      throws UnableToCompleteException {

    long startTime = System.currentTimeMillis();
    CompileDir compileDir = makeCompileDir(++compilesDone, logger);
    TreeLogger compileLogger = makeCompileLogger(compileDir, logger);

    ModuleDef module = loadModule(compileLogger);

    logger.log(TreeLogger.INFO, "Loading Java files in " + inputModuleName + ".");
    CompilerOptions loadOptions = new CompilerOptionsImpl(compileDir, inputModuleName, options);
    compilerContext = compilerContextBuilder.options(loadOptions).unitCache(
        Compiler.getOrCreateUnitCache(logger, loadOptions)).build();

    // Loads and parses all the Java files in the GWT application using the JDT.
    // (This is warmup to make compiling faster later; we stop at this point to avoid
    // needing to know the binding properties.)
    module.getCompilationState(compileLogger, compilerContext);

    setUpCompileDir(compileDir, module, compileLogger);

    outputModuleName.set(module.getName());
    lastBuild.set(compileDir);


    long elapsedTime = System.currentTimeMillis() - startTime;
    compileLogger.log(TreeLogger.Type.INFO, "Module setup completed in " + elapsedTime + " ms");

    return new Result(compileDir, null);
  }

  /**
   * Prepares a stub compile directory.
   * It will include all "public" resources and a nocache.js file that invokes the compiler.
   */
  private static void setUpCompileDir(CompileDir compileDir, ModuleDef module,
      TreeLogger compileLogger) throws UnableToCompleteException {
    try {
      String outputModuleName = module.getName();

      // Create the directory.
      File outputDir = new File(
          compileDir.getWarDir().getCanonicalPath() + "/" + outputModuleName);
      if (!outputDir.exists()) {
        if (!outputDir.mkdir()) {
          compileLogger.log(Type.WARN, "cannot create directory: " + outputDir);
        }
      }

      // Copy the public resources to the output.
      ResourceOracleImpl publicResources = module.getPublicResourceOracle();
      for (String pathName : publicResources.getPathNames()) {
        File file = new File(outputDir, pathName);
        File parent = file.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) {
          compileLogger.log(Type.ERROR, "cannot create directory: " + parent);
          throw new UnableToCompleteException();
        }
        Files.copy(publicResources.getResourceAsStream(pathName), file.toPath());
      }

      // Create a "module_name.nocache.js" that calculates the permutation and forces a recompile.
      String stubJs = generateStub(module, compileLogger);
      PageUtil.writeFile(outputDir.getCanonicalPath() + "/" + outputModuleName + ".nocache.js",
          stubJs);

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
  private static String generateStub(ModuleDef module, TreeLogger compileLogger)
      throws IOException, UnableToCompleteException {

    String outputModuleName = module.getName();

    String stub = PageUtil.loadResource(Recompiler.class, "nomodule.nocache.js");

    return "(function() {\n"
        + " var moduleName = '" + outputModuleName  + "';\n"
        + PropertiesUtil.generatePropertiesSnippet(module, compileLogger)
        + stub
        + "})();\n";
  }

  private boolean doCompile(TreeLogger compileLogger, CompileDir compileDir, Job job)
      throws UnableToCompleteException {

    job.onProgress("Loading modules");

    CompilerOptions loadOptions = new CompilerOptionsImpl(compileDir, inputModuleName, options);
    compilerContext = compilerContextBuilder.options(loadOptions).build();

    ModuleDef module = loadModule(compileLogger);
    Map<String, String> bindingProperties = restrictPermutations(compileLogger, module,
        job.getBindingProperties());

    // Propagates module rename.
    String newModuleName = module.getName();
    outputModuleName.set(newModuleName);

    // Check if we can skip the compile altogether.
    InputSummary input = new InputSummary(bindingProperties, module);
    if (input.equals(lastBuildInput)) {
      compileLogger.log(Type.INFO, "skipped compile because no input files have changed");
      job.setCompileStrategy(CompileStrategy.SKIPPED);
      return true;
    }


    job.onProgress("Compiling");
    // TODO: use speed tracer to get more compiler events?

    CompilerOptions runOptions = new CompilerOptionsImpl(compileDir, newModuleName, options);
    compilerContext = compilerContextBuilder.options(runOptions).build();

    // Looks up the matching rebuild cache using the final set of overridden binding properties.
    MinimalRebuildCache minimalRebuildCache = getOrCreateMinimalRebuildCache(bindingProperties);

    job.setCompileStrategy(minimalRebuildCache.isPopulated() ? CompileStrategy.INCREMENTAL :
        CompileStrategy.FULL);

    boolean success = new Compiler(runOptions, minimalRebuildCache).run(compileLogger, module);
    if (success) {
      publishedCompileDir = compileDir;
      lastBuildInput = input;
    } else {
      // always recompile after an error
      lastBuildInput = null;
    }
    lastBuild.set(compileDir); // makes compile log available over HTTP

    return success;
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

  private MinimalRebuildCache getOrCreateMinimalRebuildCache(
      Map<String, String> bindingProperties) {
    if (!options.isIncrementalCompileEnabled()) {
      return new NullRebuildCache();
    }

    MinimalRebuildCache minimalRebuildCache =
        minimalRebuildCacheForProperties.get(bindingProperties);
    if (minimalRebuildCache == null) {
      minimalRebuildCache = new MinimalRebuildCache();
      minimalRebuildCacheForProperties.put(bindingProperties, minimalRebuildCache);
    }
    return minimalRebuildCache;
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
        logger, compilerContext, inputModuleName, resources, true);
    compilerContext = compilerContextBuilder.module(moduleDef).build();

    // A snapshot of the module's configuration before we modified it.
    ConfigProps config = new ConfigProps(moduleDef);

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
   * @return a map of the actual properties used.
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

  private CompileDir makeCompileDir(int compileId, TreeLogger logger)
      throws UnableToCompleteException {
    return CompileDir.create(appSpace.getCompileDir(compileId), logger);
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
      return Objects.hash(bindingProperties, moduleLastModified, resourcesLastModified,
          filenameHash);
    }
  }
}
