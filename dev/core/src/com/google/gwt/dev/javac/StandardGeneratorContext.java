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

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.Artifact;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.GeneratedResource;
import com.google.gwt.core.ext.linker.impl.StandardGeneratedResource;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.resource.ResourceOracle;
import com.google.gwt.dev.util.DiskCache;
import com.google.gwt.dev.util.PerfLogger;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.collect.HashSet;
import com.google.gwt.dev.util.collect.IdentityHashMap;
import com.google.gwt.util.tools.Utility;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Map.Entry;

/**
 * Manages generators and generated units during a single compilation.
 */
public class StandardGeneratorContext implements GeneratorContext {

  /**
   * Extras added to {@link CompilationUnit}.
   */
  public static interface Generated extends GeneratedUnit {
    void abort();

    void commit();

    /**
     * Returns the strong hash of the source.
     */
    String getStrongHash();

    String getTypeName();
  }

  /**
   * This compilation unit acts as a normal compilation unit as well as a buffer
   * into which generators can write their source. A controller should ensure
   * that source isn't requested until the generator has finished writing it.
   * This version is backed by {@link StandardGeneratorContext#diskCache}.
   */
  private static class GeneratedUnitImpl implements Generated {

    /**
     * A token to retrieve this object's bytes from the disk cache.
     */
    protected long cacheToken;

    private long creationTime;

    private String strongHash; // cache so that refreshes work correctly

    private StringWriter sw;

    private final String typeName;

    public GeneratedUnitImpl(StringWriter sw, String typeName) {
      this.typeName = typeName;
      this.sw = sw;
    }

    public void abort() {
      sw = null;
    }

    /**
     * Finalizes the source and adds this compilation unit to the host.
     */
    public void commit() {
      String source = sw.toString();
      strongHash = Util.computeStrongName(Util.getBytes(source));
      cacheToken = diskCache.writeString(source);
      sw = null;
      creationTime = System.currentTimeMillis();
    }

    public long creationTime() {
      return creationTime;
    }

    public String getSource() {
      if (sw != null) {
        throw new IllegalStateException("source not committed");
      }
      return diskCache.readString(cacheToken);
    }

    public String getStrongHash() {
      return strongHash;
    }

    public String getTypeName() {
      return typeName;
    }

    public String optionalFileLocation() {
      return null;
    }
  }

  /**
   * This compilation unit acts as a normal compilation unit as well as a buffer
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

    public void commit() {
      super.commit();
      FileOutputStream fos = null;
      try {
        fos = new FileOutputStream(file);
        diskCache.transferToStream(cacheToken, fos);
      } catch (IOException e) {
        throw new RuntimeException("Error writing out generated unit at '"
            + file.getAbsolutePath() + "'", e);
      } finally {
        Utility.close(fos);
      }
    }

    public String optionalFileLocation() {
      return file.getAbsolutePath();
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

    public void write(byte[] b) throws IOException {
      if (baos == null) {
        throw new IOException("stream closed");
      }
      baos.write(b);
    }

    public void write(byte[] b, int off, int len) throws IOException {
      if (baos == null) {
        throw new IOException("stream closed");
      }
      baos.write(b, off, len);
    }

    public void write(int b) throws IOException {
      if (baos == null) {
        throw new IOException("stream closed");
      }
      baos.write(b);
    }
  }

  private static DiskCache diskCache = new DiskCache();

  private final ArtifactSet allGeneratedArtifacts;

  private final Set<GeneratedUnit> committedGeneratedCups = new HashSet<GeneratedUnit>();

  private CompilationState compilationState;

  private Class<? extends Generator> currentGenerator;

  private final File genDir;

  private final Map<Class<? extends Generator>, Generator> generators = new IdentityHashMap<Class<? extends Generator>, Generator>();

  private final ModuleDef module;

  private ArtifactSet newlyGeneratedArtifacts = new ArtifactSet();

  private final Set<String> newlyGeneratedTypeNames = new HashSet<String>();

  private final Map<String, PendingResource> pendingResources = new HashMap<String, PendingResource>();

  private transient PropertyOracle propOracle;

  private final Map<PrintWriter, Generated> uncommittedGeneratedCupsByPrintWriter = new IdentityHashMap<PrintWriter, Generated>();

  /**
   * Normally, the compiler host would be aware of the same types that are
   * available in the supplied type oracle although it isn't strictly required.
   */
  public StandardGeneratorContext(CompilationState compilationState,
      ModuleDef module, File genDir, ArtifactSet allGeneratedArtifacts) {
    this.compilationState = compilationState;
    this.module = module;
    this.genDir = genDir;
    this.allGeneratedArtifacts = allGeneratedArtifacts;
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
  public final void commit(TreeLogger logger, PrintWriter pw) {
    Generated gcup = uncommittedGeneratedCupsByPrintWriter.get(pw);
    if (gcup != null) {
      gcup.commit();
      uncommittedGeneratedCupsByPrintWriter.remove(pw);
      committedGeneratedCups.add(gcup);
    } else {
      logger.log(TreeLogger.WARN,
          "Generator attempted to commit an unknown PrintWriter", null);
    }
  }

  /**
   * Adds an Artifact to the ArtifactSet if one has been provided to the
   * context.
   */
  public void commitArtifact(TreeLogger logger, Artifact<?> artifact) {
    allGeneratedArtifacts.replace(artifact);
    newlyGeneratedArtifacts.add(artifact);
  }

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
      logger.log(TreeLogger.WARN,
          "Generator attempted to commit an unknown OutputStream", null);
      throw new UnableToCompleteException();
    }

    // Add the GeneratedResource to the ArtifactSet
    GeneratedResource toReturn = new StandardGeneratedResource(
        currentGenerator, partialPath, pendingResource.takeBytes());
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
   */
  public final ArtifactSet finish(TreeLogger logger)
      throws UnableToCompleteException {

    abortUncommittedResources(logger);

    // Process pending generated types.
    List<String> genTypeNames = new ArrayList<String>();

    try {
      TreeLogger branch;
      if (!committedGeneratedCups.isEmpty()) {
        // Assimilate the new types into the type oracle.
        //
        String msg = "Assimilating generated source";
        branch = logger.branch(TreeLogger.DEBUG, msg, null);

        TreeLogger subBranch = null;
        if (branch.isLoggable(TreeLogger.DEBUG)) {
          subBranch = branch.branch(TreeLogger.DEBUG,
              "Generated source files...", null);
        }

        for (GeneratedUnit gcup : committedGeneratedCups) {
          String qualifiedTypeName = gcup.getTypeName();
          genTypeNames.add(qualifiedTypeName);
          if (subBranch != null) {
            subBranch.log(TreeLogger.DEBUG, qualifiedTypeName, null);
          }
        }

        compilationState.addGeneratedCompilationUnits(logger,
            committedGeneratedCups);
      }

      // Make sure all generated types can be found in TypeOracle.
      TypeOracle typeOracle = getTypeOracle();
      for (String genTypeName : genTypeNames) {
        if (typeOracle.findType(genTypeName) == null) {
          String msg = "Unable to find recently-generated type '" + genTypeName;
          logger.log(TreeLogger.ERROR, msg, null);
          throw new UnableToCompleteException();
        }
      }
      return newlyGeneratedArtifacts;
    } finally {

      // Remind the user if there uncommitted cups.
      if (!uncommittedGeneratedCupsByPrintWriter.isEmpty()) {
        String msg = "For the following type(s), generated source was never committed (did you forget to call commit()?)";
        logger = logger.branch(TreeLogger.WARN, msg, null);

        for (Generated unit : uncommittedGeneratedCupsByPrintWriter.values()) {
          logger.log(TreeLogger.WARN, unit.getTypeName(), null);
        }
      }

      uncommittedGeneratedCupsByPrintWriter.clear();
      committedGeneratedCups.clear();
      newlyGeneratedTypeNames.clear();
      newlyGeneratedArtifacts = new ArtifactSet();
    }
  }

  public Set<String> getActiveLinkerNames() {
    return module.getActiveLinkerNames();
  }

  public final PropertyOracle getPropertyOracle() {
    return propOracle;
  }

  public ResourceOracle getResourcesOracle() {
    return module.getResourcesOracle();
  }

  public final TypeOracle getTypeOracle() {
    return compilationState.getTypeOracle();
  }

  public String runGenerator(TreeLogger logger,
      Class<? extends Generator> generatorClass, String typeName)
      throws UnableToCompleteException {
    String msg = "Invoking generator " + generatorClass.getName();
    logger = logger.branch(TreeLogger.DEBUG, msg, null);

    Generator generator = generators.get(generatorClass);
    if (generator == null) {
      try {
        generator = generatorClass.newInstance();
        generators.put(generatorClass, generator);
      } catch (Throwable e) {
        logger.log(TreeLogger.ERROR,
            "Unexpected error trying to instantiate Generator '"
                + generatorClass.getName() + "'", e);
        throw new UnableToCompleteException();
      }
    }

    setCurrentGenerator(generatorClass);

    long before = System.currentTimeMillis();
    PerfLogger.start("Generator '" + generator.getClass().getName()
        + "' produced '" + typeName + "'");
    try {
      String className = generator.generate(logger, this, typeName);
      long after = System.currentTimeMillis();
      if (className == null) {
        msg = "Generator returned null, so the requested type will be used as is";
      } else {
        msg = "Generator returned class '" + className + "'";
      }
      msg += "; in " + (after - before) + " ms";
      logger.log(TreeLogger.DEBUG, msg, null);
      return className;
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
      PerfLogger.end();
    }
  }

  public void setCurrentGenerator(Class<? extends Generator> currentGenerator) {
    this.currentGenerator = currentGenerator;
  }

  /**
   * Sets the current transient property oracle to answer current property
   * questions.
   */
  public void setPropertyOracle(PropertyOracle propOracle) {
    this.propOracle = propOracle;
  }

  public final PrintWriter tryCreate(TreeLogger logger, String packageName,
      String simpleTypeName) {
    String typeName;
    if (packageName.length() == 0) {
      typeName = simpleTypeName;
    } else {
      typeName = packageName + '.' + simpleTypeName;
    }
    // Is type already known to the host?
    JClassType existingType = getTypeOracle().findType(packageName,
        simpleTypeName);
    if (existingType != null) {
      logger.log(TreeLogger.DEBUG, "Type '" + typeName
          + "' already exists and will not be re-created ", null);
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
    PrintWriter pw = new PrintWriter(sw, true);
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

  public OutputStream tryCreateResource(TreeLogger logger, String partialPath)
      throws UnableToCompleteException {

    logger = logger.branch(TreeLogger.DEBUG,
        "Preparing pending output resource '" + partialPath + "'", null);

    // Disallow null or empty names.
    if (partialPath == null || partialPath.trim().equals("")) {
      logger.log(TreeLogger.ERROR,
          "The resource name must be a non-empty string", null);
      throw new UnableToCompleteException();
    }

    // Disallow absolute paths.
    if (new File(partialPath).isAbsolute()) {
      logger.log(
          TreeLogger.ERROR,
          "Resource paths are intended to be relative to the compiled output directory and cannot be absolute",
          null);
      throw new UnableToCompleteException();
    }

    // Disallow backslashes (to promote consistency in calling code).
    if (partialPath.indexOf('\\') >= 0) {
      logger.log(
          TreeLogger.ERROR,
          "Resource paths must contain forward slashes (not backslashes) to denote subdirectories",
          null);
      throw new UnableToCompleteException();
    }

    // Check for public path collision.
    if (module.findPublicFile(partialPath) != null) {
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
      logger.log(TreeLogger.WARN, "The file '" + partialPath
          + "' is already a pending resource", null);
      return null;
    }
    PendingResource pendingResource = new PendingResource(partialPath);
    pendingResources.put(partialPath, pendingResource);
    return pendingResource;
  }

  private void abortUncommittedResources(TreeLogger logger) {
    if (pendingResources.isEmpty()) {
      // Nothing to do.
      return;
    }

    // Warn the user about uncommitted resources.
    logger = logger.branch(
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
