/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.dev.javac;

import com.google.gwt.core.ext.CachedGeneratorResult;
import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.IncrementalGenerator;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.RebindResult;
import com.google.gwt.core.ext.RebindRuleResolver;
import com.google.gwt.core.ext.SubsetFilteringPropertyOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.Artifact;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.EmittedArtifact.Visibility;
import com.google.gwt.core.ext.linker.GeneratedResource;
import com.google.gwt.core.ext.linker.impl.StandardGeneratedResource;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.CompilerContext;
import com.google.gwt.dev.cfg.RuleGenerateWith;
import com.google.gwt.dev.resource.ResourceOracle;
import com.google.gwt.dev.util.DiskCache;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.collect.HashSet;
import com.google.gwt.dev.util.collect.IdentityHashMap;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;
import com.google.gwt.util.tools.Utility;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;

/**
 * Manages generators and generated units during a single compilation.
 */
public class StandardGeneratorContext implements GeneratorContext {

  /**
   * Extras added to {@link GeneratedUnit}.
   */
  private static interface Generated extends GeneratedUnit {
    void abort();

    void commit(TreeLogger logger);
  }

  /**
   * This generated unit acts as a normal generated unit as well as a buffer
   * into which generators can write their source. A controller should ensure
   * that source isn't requested until the generator has finished writing it.
   * This version is backed by {@link StandardGeneratorContext#diskCache}.
   */
  public static class GeneratedUnitImpl implements Generated {

    /**
     * A token to retrieve this object's bytes from the disk cache.
     */
    protected long sourceToken = -1;

    private long creationTime;

    private String strongHash; // cache so that refreshes work correctly

    private StringWriter sw;

    private final String typeName;

    public GeneratedUnitImpl(StringWriter sw, String typeName) {
      this.typeName = typeName;
      this.sw = sw;
    }

    @Override
    public void abort() {
      sw = null;
    }

    /**
     * Finalizes the source and adds this generated unit to the host.
     */
    @Override
    public void commit(TreeLogger logger) {
      String source = sw.toString();
      strongHash = Util.computeStrongName(Util.getBytes(source));
      sourceToken = diskCache.writeString(source);
      sw = null;
      creationTime = System.currentTimeMillis();
    }

    @Override
    public long creationTime() {
      return creationTime;
    }

    @Override
    public String getSource() {
      if (sw != null) {
        throw new IllegalStateException("source not committed");
      }
      return diskCache.readString(sourceToken);
    }

    @Override
    public String getSourceMapPath() {
      return "gen/" + getTypeName().replace('.', '/') + ".java";
    }

    @Override
    public long getSourceToken() {
      if (sw != null) {
        throw new IllegalStateException("source not committed");
      }
      return sourceToken;
    }

    @Override
    public String getStrongHash() {
      return strongHash;
    }

    @Override
    public String getTypeName() {
      return typeName;
    }

    @Override
    public String optionalFileLocation() {
      return null;
    }
  }

  /**
   * This generated unit acts as a normal generated unit as well as a buffer
   * into which generators can write their source. A controller should ensure
   * that source isn't requested until the generator has finished writing it.
   * This version is backed by an explicit generated file.
   */
  private static class GeneratedUnitWithFile extends GeneratedUnitImpl {
    private final File file;

    public GeneratedUnitWithFile(File file, StringWriter pw, String typeName) {
      super(pw, typeName);
      this.file = file;
    }

    @Override
    public void commit(TreeLogger logger) {
      super.commit(logger);
      FileOutputStream fos = null;
      try {
        fos = new FileOutputStream(file);
        diskCache.transferToStream(sourceToken, fos);
      } catch (IOException e) {
        logger.log(TreeLogger.WARN, "Error writing out generated unit at '"
            + file.getAbsolutePath() + "': " + e);
      } finally {
        Utility.close(fos);
      }
    }

    @Override
    public String optionalFileLocation() {
      return file.exists() ? Util.stripJarPathPrefix(file.getAbsolutePath()) : null;
    }
  }

  /**
   * Manages a resource that is in the process of being created by a generator.
   */
  private static class PendingResource extends OutputStream {

    private ByteArrayOutputStream baos = new ByteArrayOutputStream();
    private final String partialPath;

    public PendingResource(String partialPath) {
      this.partialPath = partialPath;
    }

    public void abort() {
      baos = null;
    }

    public String getPartialPath() {
      return partialPath;
    }

    public byte[] takeBytes() {
      byte[] result = baos.toByteArray();
      baos = null;
      return result;
    }

    @Override
    public void write(byte[] b) throws IOException {
      if (baos == null) {
        throw new IOException("stream closed");
      }
      baos.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      if (baos == null) {
        throw new IOException("stream closed");
      }
      baos.write(b, off, len);
    }

    @Override
    public void write(int b) throws IOException {
      if (baos == null) {
        throw new IOException("stream closed");
      }
      baos.write(b);
    }
  }

  private static final String GENERATOR_VERSION_ID_KEY = "generator-version-id";

  private static DiskCache diskCache = DiskCache.INSTANCE;

  private static final Map<String, CompilerEventType> eventsByGeneratorType =
      new HashMap<String, CompilerEventType>();
  static {
    eventsByGeneratorType.put(
        "com.google.gwt.resources.rebind.context.InlineClientBundleGenerator",
        CompilerEventType.GENERATOR_CLIENT_BUNDLE);
    eventsByGeneratorType.put("com.google.gwt.i18n.rebind.LocalizableGenerator",
        CompilerEventType.GENERATOR_I18N);
    eventsByGeneratorType.put("com.google.gwt.i18n.rebind.LocaleInfoGenerator",
        CompilerEventType.GENERATOR_I18N);
    eventsByGeneratorType.put("com.google.gwt.i18n.rebind.CurrencyListGenerator",
        CompilerEventType.GENERATOR_I18N);
    eventsByGeneratorType.put("com.google.gwt.i18n.rebind.CustomDateTimeFormatGenerator",
        CompilerEventType.GENERATOR_I18N);
    eventsByGeneratorType.put("com.google.gwt.user.rebind.rpc.ServiceInterfaceProxyGenerator",
        CompilerEventType.GENERATOR_RPC);
    eventsByGeneratorType.put("com.google.gwt.uibinder.rebind.UiBinderGenerator",
        CompilerEventType.GENERATOR_UIBINDER);
    eventsByGeneratorType.put("com.google.gwt.inject.rebind.GinjectorGenerator",
        CompilerEventType.GENERATOR_GIN);
  }

  private final ArtifactSet allGeneratedArtifacts;

  private final Map<String, GeneratedUnit> committedGeneratedCups =
      new HashMap<String, GeneratedUnit>();

  private CompilationState compilationState;

  private Class<? extends Generator> currentGenerator;

  private final File genDir;

  private final Map<Class<? extends Generator>, Generator> generators =
      new IdentityHashMap<Class<? extends Generator>, Generator>();

  private ArtifactSet newlyGeneratedArtifacts = new ArtifactSet();

  private final Set<String> newlyGeneratedTypeNames = new HashSet<String>();

  private final Map<String, PendingResource> pendingResources =
      new HashMap<String, PendingResource>();

  private transient PropertyOracle propertyOracle;

  private RebindRuleResolver rebindRuleResolver;

  private final Map<PrintWriter, Generated> uncommittedGeneratedCupsByPrintWriter =
      new IdentityHashMap<PrintWriter, Generated>();

  private CachedGeneratorResultImpl cachedRebindResult = null;

  private boolean generatorResultCachingEnabled = false;

  private List<String> cachedTypeNamesToReuse = null;

  private boolean isProdMode;

  private CompilerContext compilerContext;

  /**
   * Normally, the compiler host would be aware of the same types that are
   * available in the supplied type oracle although it isn't strictly required.
   */
  public StandardGeneratorContext(CompilerContext compilerContext,
      CompilationState compilationState, ArtifactSet allGeneratedArtifacts, boolean isProdMode) {
    this.compilerContext = compilerContext;
    this.compilationState = compilationState;
    this.genDir = compilerContext.getOptions().getGenDir();
    this.allGeneratedArtifacts = allGeneratedArtifacts;
    this.isProdMode = isProdMode;
  }

  /**
   * Adds a generated unit to the context if not already present, but will not
   * overwrite an existing unit.
   */
  public void addGeneratedUnit(GeneratedUnit gu) {
    if (!committedGeneratedCups.containsKey(gu.getTypeName())) {
      committedGeneratedCups.put(gu.getTypeName(), gu);
    }
  }

  /**
   * Adds generated units to the context, but will not overwrite any existing
   * units that might already be present.
   */
  public void addGeneratedUnits(Collection<GeneratedUnit> generatedUnits) {
    for (GeneratedUnit gu : generatedUnits) {
      addGeneratedUnit(gu);
    }
  }

  /**
   * Adds all available cached generated units to the context. Existing units
   * for a given type will not be overwritten.
   */
  public void addGeneratedUnitsFromCache() {
    if (cachedRebindResult != null && cachedRebindResult.getGeneratedUnits() != null) {
      addGeneratedUnits(cachedRebindResult.getGeneratedUnits());
    }
  }

  /**
   * Adds cached generated units to the context that have been marked for reuse.
   * Existing units for a given type will not be overwritten.
   */
  public void addGeneratedUnitsMarkedForReuseFromCache() {
    if (cachedTypeNamesToReuse != null && cachedRebindResult != null) {
      for (String typeName : cachedTypeNamesToReuse) {
        GeneratedUnit gu = cachedRebindResult.getGeneratedUnit(typeName);
        if (gu != null) {
          addGeneratedUnit(gu);
        }
      }
    }
  }

  /**
   * Checks whether a rebind rule is available for a given sourceTypeName.
   */
  @Override
  public boolean checkRebindRuleAvailable(String sourceTypeName) {
    if (rebindRuleResolver != null) {
      return rebindRuleResolver.checkRebindRuleResolvable(sourceTypeName);
    } else {
      return false;
    }
  }

  /**
   * Frees memory used up by compilation state.
   */
  public void clear() {
    compilationState = null;
    generators.clear();
  }

  /**
   * Commits a pending generated type.
   */
  @Override
  public final void commit(TreeLogger logger, PrintWriter pw) {
    Generated gcup = uncommittedGeneratedCupsByPrintWriter.get(pw);
    if (gcup == null) {
      logger.log(TreeLogger.WARN, "Generator attempted to commit an unknown PrintWriter", null);
      return;
    }
    gcup.commit(logger);
    uncommittedGeneratedCupsByPrintWriter.remove(pw);
    committedGeneratedCups.put(gcup.getTypeName(), gcup);

    // Write as a source artifact so that a debugger can use it.
    // TODO: if we're not generating sourcemaps then we should probably skip this entirely
    // since the data will be written to the shard's jar file and never read.
    // (But how do we check that?)

    if (currentGenerator == null) {
      return; // probably a test.
    }

    GeneratedResource debuggerSource =
        new StandardGeneratedResource(gcup.getSourceMapPath(), gcup.getSourceToken());
    debuggerSource.setVisibility(Visibility.Source);
    commitArtifact(logger, debuggerSource);
  }

  /**
   * Adds an Artifact to the context's ArtifactSets. This will replace a
   * pre-existing entry in allGeneratedArtifacts, but will not overwrite an
   * entry in the newlyGeneratedArtifacts (since it is assumed by convention
   * that only new entries will ever be inserted here for a given generator
   * run).
   */
  @Override
  public void commitArtifact(TreeLogger logger, Artifact<?> artifact) {
    allGeneratedArtifacts.replace(artifact);
    newlyGeneratedArtifacts.add(artifact);
  }

  /**
   * Commits all available cached Artifacts to the context.
   */
  public void commitArtifactsFromCache(TreeLogger logger) {
    if (cachedRebindResult != null && cachedRebindResult.getArtifacts() != null) {
      for (Artifact<?> art : cachedRebindResult.getArtifacts()) {
        commitArtifact(logger, art);
      }
    }
  }

  @Override
  public GeneratedResource commitResource(TreeLogger logger, OutputStream os)
      throws UnableToCompleteException {

    PendingResource pendingResource = null;
    String partialPath = null;
    if (os instanceof PendingResource) {
      pendingResource = (PendingResource) os;
      partialPath = pendingResource.getPartialPath();
      // Make sure it's ours by looking it up in the map.
      if (pendingResource != pendingResources.get(partialPath)) {
        pendingResource = null;
      }
    }
    if (pendingResource == null) {
      logger.log(TreeLogger.WARN, "Generator attempted to commit an unknown OutputStream", null);
      throw new UnableToCompleteException();
    }

    // Add the GeneratedResource to the ArtifactSet
    GeneratedResource toReturn =
        new StandardGeneratedResource(partialPath, pendingResource.takeBytes());
    commitArtifact(logger, toReturn);
    pendingResources.remove(pendingResource.getPartialPath());
    return toReturn;
  }

  /**
   * Call this whenever generators are known to not be running to clear out
   * uncommitted compilation units and to force committed compilation units to
   * be parsed and added to the type oracle.
   *
   * @return any newly generated artifacts since the last call
   *
   * @throw UnableToCompleteException if the compiler aborted (not
   * a normal compile error).</p>
   */
  public ArtifactSet finish(TreeLogger logger) throws UnableToCompleteException {
    abortUncommittedResources(logger);

    try {
      TreeLogger branch;
      if (isDirty()) {
        // Assimilate the new types into the type oracle.
        //
        String msg = "Assimilating generated source";
        branch = logger.branch(TreeLogger.DEBUG, msg, null);

        TreeLogger subBranch = null;
        if (branch.isLoggable(TreeLogger.DEBUG)) {
          subBranch = branch.branch(TreeLogger.DEBUG, "Generated source files...", null);
        }

        for (GeneratedUnit gcup : committedGeneratedCups.values()) {
          String qualifiedTypeName = gcup.getTypeName();
          if (subBranch != null) {
            subBranch.log(TreeLogger.DEBUG, qualifiedTypeName, null);
          }
        }

        compilationState.addGeneratedCompilationUnits(logger, committedGeneratedCups.values());
      }
      return newlyGeneratedArtifacts;
    } finally {

      // Remind the user if there uncommitted cups.
      if (!uncommittedGeneratedCupsByPrintWriter.isEmpty()) {
        String msg =
            "For the following type(s), generated source was never committed (did you forget to call commit()?)";
        logger = logger.branch(TreeLogger.WARN, msg, null);

        for (Generated unit : uncommittedGeneratedCupsByPrintWriter.values()) {
          logger.log(TreeLogger.WARN, unit.getTypeName(), null);
        }
      }

      reset();
    }
  }

  public boolean isDirty() {
    return !committedGeneratedCups.isEmpty();
  }

  /**
   * Clears all accumulated artifacts and state so that the context can be used
   * as if from scratch. Is useful for clearing out undesired changes after
   * having used the context to explore some hypothetical situations, for
   * example to run Generators for the purpose of discovering the properties
   * they depend on.
   */
  public void reset() {
    uncommittedGeneratedCupsByPrintWriter.clear();
    committedGeneratedCups.clear();
    newlyGeneratedTypeNames.clear();
    newlyGeneratedArtifacts = new ArtifactSet();
    cachedTypeNamesToReuse = null;
  }

  public Set<String> getActiveLinkerNames() {
    return compilerContext.getModule().getActiveLinkerNames();
  }

  /**
   * Gets newly committed artifacts.
   */
  public ArtifactSet getArtifacts() {
    return new ArtifactSet(newlyGeneratedArtifacts);
  }

  /**
   * Gets the previously cached rebind result for the current generator.
   */
  @Override
  public CachedGeneratorResult getCachedGeneratorResult() {
    return cachedRebindResult;
  }

  public GeneratorContext getCanonicalContext() {
    return this;
  }

  public CompilationState getCompilationState() {
    return compilationState;
  }

  /**
   * Gets all committed Java units.
   */
  public Map<String, GeneratedUnit> getGeneratedUnitMap() {
    return committedGeneratedCups;
  }

  @Override
  public final PropertyOracle getPropertyOracle() {
    return propertyOracle;
  }

  /**
   * Returns whether the current compile and generator passes are executing in
   * the global phase of a compile, as opposed to further down in the dependency
   * tree.
   */
  public boolean isGlobalCompile() {
    return compilerContext.getOptions().shouldLink();
  }

  @Override
  public ResourceOracle getResourcesOracle() {
    return compilerContext.getBuildResourceOracle();
  }

  @Override
  public final TypeOracle getTypeOracle() {
    return compilationState.getTypeOracle();
  }

  @Override
  public boolean isGeneratorResultCachingEnabled() {
    return generatorResultCachingEnabled;
  }

  @Override
  public boolean isProdMode() {
    return isProdMode;
  }

  /**
   * This method is maintained for backwards compatibility.
   * {@link #runGeneratorIncrementally} should be used instead.
   */
  public String runGenerator(TreeLogger logger, Class<? extends Generator> generatorClass,
      String typeName) throws UnableToCompleteException {

    RebindResult result = runGeneratorIncrementally(logger, generatorClass, typeName);

    return result.getResultTypeName();
  }

  /**
   * Runs a generator incrementally, with support for managing the returned
   * {@link RebindResult} object, which can contain cached results. This is a
   * replacement for the {@link #runGenerator} method.
   * <p>
   * If the passed in generatorClass is an instance of
   * {@link IncrementalGenerator}, it's
   * {@link IncrementalGenerator#generateIncrementally} method will be called.
   * <p>
   * Otherwise, for backwards compatibility, the generatorClass will be wrapped
   * in a {@link IncrementalGenerator} instance, and it's
   * {@link Generator#generate} method will be called.
   *
   * @param logger
   * @param generatorClass
   * @param typeName
   * @return a RebindResult
   * @throws UnableToCompleteException
   */
  public RebindResult runGeneratorIncrementally(TreeLogger logger,
      Class<? extends Generator> generatorClass, String typeName) throws UnableToCompleteException {
    String msg = "Invoking generator " + generatorClass.getName();
    logger = logger.branch(TreeLogger.DEBUG, msg, null);

    Generator generator = generators.get(generatorClass);
    if (generator == null) {
      try {
        generator = generatorClass.newInstance();
        generators.put(generatorClass, generator);
      } catch (Throwable e) {
        logger.log(TreeLogger.ERROR, "Unexpected error trying to instantiate Generator '"
            + generatorClass.getName() + "'", e);
        throw new UnableToCompleteException();
      }
    }

    setCurrentGenerator(generatorClass);

    // Avoid call to System.currentTimeMillis() if not logging DEBUG level
    boolean loggable = logger.isLoggable(TreeLogger.DEBUG);
    long before = loggable ? System.currentTimeMillis() : 0L;

    String generatorClassName = generator.getClass().getName();
    CompilerEventType type = eventsByGeneratorType.get(generatorClassName);

    if (type == null) {
      type = CompilerEventType.GENERATOR_OTHER;
    }

    Event generatorEvent =
        SpeedTracerLogger.start(type, "class", generatorClassName, "type", typeName);

    PropertyOracle originalPropertyOracle = propertyOracle;
    try {
      RebindResult result;
      // TODO(stalcup): refactor the Generator/PropertyOracle system (in a potentially backwards
      // incompatible way) so that all Generators are forced to accurately declare the names of
      // properties they care about.
      propertyOracle = new SubsetFilteringPropertyOracle(
          RuleGenerateWith.getAccessedPropertyNames(generator.getClass()), originalPropertyOracle,
          generatorClassName + "'s RunsLocal annotation may need to be updated.");
      if (generator instanceof IncrementalGenerator) {
        IncrementalGenerator incGenerator = (IncrementalGenerator) generator;

        // check version id for any previously cached rebind result
        if (cachedRebindResult != null) {
          Long cachedVersionId = (Long) cachedRebindResult.getClientData(GENERATOR_VERSION_ID_KEY);
          if (cachedVersionId != null && cachedVersionId != incGenerator.getVersionId()) {
            // remove from context
            if (logger.isLoggable(TreeLogger.TRACE)) {
              logger.log(TreeLogger.TRACE, "Got version mismatch with cached generator result for "
                  + typeName + ", invalidating cached result");
            }
            cachedRebindResult = null;
          }
        }

        // run the generator
        result = incGenerator.generateIncrementally(logger, this, typeName);

        // add version id to the returned result
        result.putClientData(GENERATOR_VERSION_ID_KEY, incGenerator.getVersionId());
      } else {
        // run a non-incremental generator
        result = IncrementalGenerator.generateNonIncrementally(logger, generator, this, typeName);
      }

      if (loggable) {
        long after = System.currentTimeMillis();
        msg =
            "Generator returned type '" + result.getResultTypeName() + "; mode "
                + result.getRebindMode() + "; in " + (after - before) + " ms";
        logger.log(TreeLogger.DEBUG, msg, null);
      }
      return result;
    } catch (AssertionError e) {
      // Catch and log the assertion as a convenience to the developer
      logger.log(TreeLogger.ERROR, "Generator '" + generatorClass.getName()
          + "' failed an assertion while rebinding '" + typeName + "'", e);
      throw new UnableToCompleteException();
    } catch (RuntimeException e) {
      logger.log(TreeLogger.ERROR, "Generator '" + generatorClass.getName()
          + "' threw an exception while rebinding '" + typeName + "'", e);
      throw new UnableToCompleteException();
    } finally {
      propertyOracle = originalPropertyOracle;
      generatorEvent.end();
    }
  }

  /**
   * Set previously cached rebind result for currently active generator.
   */
  public void setCachedGeneratorResult(CachedGeneratorResult cachedRebindResult) {
    this.cachedRebindResult = (CachedGeneratorResultImpl) cachedRebindResult;
  }

  public void setCurrentGenerator(Class<? extends Generator> currentGenerator) {
    this.currentGenerator = currentGenerator;
  }

  public void setGeneratorResultCachingEnabled(boolean enabled) {
    this.generatorResultCachingEnabled = enabled;
  }

  /**
   * Sets the current transient property oracle to answer current property
   * questions.
   */
  public void setPropertyOracle(PropertyOracle propertyOracle) {
    this.propertyOracle = propertyOracle;
  }

  public void setRebindRuleResolver(RebindRuleResolver resolver) {
    this.rebindRuleResolver = resolver;
  }

  @Override
  public PrintWriter tryCreate(TreeLogger logger, String packageName, String simpleTypeName) {
    String typeName;
    if (packageName.length() == 0) {
      typeName = simpleTypeName;
    } else {
      typeName = packageName + '.' + simpleTypeName;
    }
    // Is type already known to the host?
    JClassType existingType = getTypeOracle().findType(packageName, simpleTypeName);
    if (existingType != null) {
      if (logger.isLoggable(TreeLogger.DEBUG)) {
        logger.log(TreeLogger.DEBUG, "Type '" + typeName
            + "' already exists and will not be re-created ", null);
      }
      return null;
    }

    // Type recently generated?
    if (newlyGeneratedTypeNames.contains(typeName)) {
      return null;
    }

    // The type isn't there, so we can let the caller create it. Remember that
    // it is pending so another attempt to create the same type will fail.
    Generated gcup;
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw, true) {
      /**
       * Overridden to force unix-style line endings for consistent behavior
       * across platforms.
       */
      @Override
      public void println() {
        super.print('\n');
        super.flush();
      }
    };
    if (this.genDir == null) {
      gcup = new GeneratedUnitImpl(sw, typeName);
    } else {
      File dir = new File(genDir, packageName.replace('.', File.separatorChar));
      // No need to check mkdirs result because an IOException will occur anyway
      dir.mkdirs();
      File srcFile = new File(dir, simpleTypeName + ".java");
      if (srcFile.exists()) {
        srcFile.delete();
      }
      gcup = new GeneratedUnitWithFile(srcFile, sw, typeName);
    }
    uncommittedGeneratedCupsByPrintWriter.put(pw, gcup);
    newlyGeneratedTypeNames.add(typeName);
    return pw;
  }

  @Override
  public OutputStream tryCreateResource(TreeLogger logger, String partialPath)
      throws UnableToCompleteException {

    logger =
        logger.branch(TreeLogger.DEBUG, "Preparing pending output resource '" + partialPath + "'",
            null);

    // Disallow null or empty names.
    if (partialPath == null || partialPath.trim().equals("")) {
      logger.log(TreeLogger.ERROR, "The resource name must be a non-empty string", null);
      throw new UnableToCompleteException();
    }

    // Disallow absolute paths.
    if (new File(partialPath).isAbsolute()) {
      logger
          .log(
              TreeLogger.ERROR,
              "Resource paths are intended to be relative to the compiled output directory and cannot be absolute",
              null);
      throw new UnableToCompleteException();
    }

    // Disallow backslashes (to promote consistency in calling code).
    if (partialPath.indexOf('\\') >= 0) {
      logger.log(TreeLogger.ERROR,
          "Resource paths must contain forward slashes (not backslashes) to denote subdirectories",
          null);
      throw new UnableToCompleteException();
    }

    // Check for public path collision.
    if (compilerContext.getPublicResourceOracle().getResourceMap().containsKey(partialPath)) {
      logger.log(TreeLogger.WARN, "Cannot create resource '" + partialPath
          + "' because it already exists on the public path", null);
      return null;
    }

    // See if the file is already committed.
    SortedSet<GeneratedResource> resources = allGeneratedArtifacts.find(GeneratedResource.class);
    for (GeneratedResource resource : resources) {
      if (partialPath.equals(resource.getPartialPath())) {
        return null;
      }
    }

    // See if the file is pending.
    if (pendingResources.containsKey(partialPath)) {
      // It is already pending.
      logger.log(TreeLogger.WARN, "The file '" + partialPath + "' is already a pending resource",
          null);
      return null;
    }
    PendingResource pendingResource = new PendingResource(partialPath);
    pendingResources.put(partialPath, pendingResource);
    return pendingResource;
  }

  /**
   * Adds a type name to the list of types to be reused from cache, if
   * available.
   *
   * @param typeName The fully qualified name of a type.
   *
   * @return true, if the type is available in the cache and was successfully
   *         added to the list for reuse, false otherwise.
   */
  @Override
  public boolean tryReuseTypeFromCache(String typeName) {
    if (!isGeneratorResultCachingEnabled() || cachedRebindResult == null
        || !cachedRebindResult.isTypeCached(typeName)) {
      return false;
    }

    if (cachedTypeNamesToReuse == null) {
      cachedTypeNamesToReuse = new ArrayList<String>();
    }
    cachedTypeNamesToReuse.add(typeName);
    return true;
  }

  private void abortUncommittedResources(TreeLogger logger) {
    if (pendingResources.isEmpty()) {
      // Nothing to do.
      return;
    }

    // Warn the user about uncommitted resources.
    logger =
        logger
            .branch(
                TreeLogger.WARN,
                "The following resources will not be created because they were never committed (did you forget to call commit()?)",
                null);

    for (Entry<String, PendingResource> entry : pendingResources.entrySet()) {
      logger.log(TreeLogger.WARN, entry.getKey());
      entry.getValue().abort();
    }
    pendingResources.clear();
  }
}
