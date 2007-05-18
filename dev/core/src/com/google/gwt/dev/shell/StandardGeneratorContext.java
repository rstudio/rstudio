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
package com.google.gwt.dev.shell;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.CompilationUnitProvider;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.jdt.CacheManager;
import com.google.gwt.dev.jdt.StaticCompilationUnitProvider;
import com.google.gwt.dev.jdt.TypeOracleBuilder;
import com.google.gwt.dev.jdt.URLCompilationUnitProvider;
import com.google.gwt.dev.util.Util;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An abstract implementation of a generator context in terms of a
 * {@link com.google.gwt.dev.jdt.MutableCompilationServiceHost}, a
 * {@link com.google.gwt.dev.jdt.PropertyOracle}, and a
 * {@link com.google.gwt.core.server.typeinfo.TypeOracle}. The generator
 * interacts with the mutable source oracle by increasing the available
 * compilation units as they are generated.
 */
public class StandardGeneratorContext implements GeneratorContext {

  /**
   * This compilation unit provider acts as a normal compilation unit provider
   * as well as a buffer into which generators can write their source. A
   * controller should ensure that source isn't requested until the generator
   * has finished writing it.
   */
  private static class GeneratedCompilationUnitProvider extends
      StaticCompilationUnitProvider {

    public CharArrayWriter caw;

    public PrintWriter pw;

    public char[] source;

    public GeneratedCompilationUnitProvider(String packageName,
        String simpleTypeName) {
      super(packageName, simpleTypeName, null);
      caw = new CharArrayWriter();
      pw = new PrintWriter(caw, true);
    }

    /**
     * Finalizes the source and adds this compilation unit to the host.
     */
    public void commit() {
      source = caw.toCharArray();
      pw.close();
      pw = null;
      caw.close();
      caw = null;
    }

    public char[] getSource() {
      if (source == null) {
        throw new IllegalStateException("source not committed");
      }
      return source;
    }
  }

  /**
   * {@link CompilationUnitProvider} used to represent generated source code 
   * which is stored on disk.  This class is only used if the -gen flag is
   * specified.
   */
  private static final class GeneratedCUP extends URLCompilationUnitProvider {
    private GeneratedCUP(URL url, String name) {
      super(url, name);
    }

    public long getLastModified() throws UnableToCompleteException {
      // Make it seem really old so it won't cause recompiles.
      //
      return 0L;
    }
    
    public boolean isTransient() {
      return true;
    }
  }

  /**
   * Manages a resource that is in the process of being created by a generator.
   */
  private static class PendingResource {

    private final File pendingFile;
    private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

    public PendingResource(File pendingFile) {
      this.pendingFile = pendingFile;
    }

    public void commit(TreeLogger logger) throws UnableToCompleteException {
      logger = logger.branch(TreeLogger.TRACE, "Writing generated resource '"
          + pendingFile.getAbsolutePath() + "'", null);

      if (pendingFile.exists()) {
        logger.log(TreeLogger.ERROR,
            "The destination file already exists; aborting the commit", null);
        throw new UnableToCompleteException();
      }

      Util.writeBytesToFile(logger, pendingFile, baos.toByteArray());
    }

    public File getFile() {
      return pendingFile;
    }

    public OutputStream getOutputStream() {
      return baos;
    }

    public boolean isSamePath(TreeLogger logger, File other) {
      File failedFile = null;
      try {
        /*
         * We try to convert both files to their canonical form. Either one
         * might throw an exception, so we keep track of the one being converted
         * so that we can say which one failed in the case of an error.
         */
        failedFile = pendingFile;
        File thisFile = pendingFile.getCanonicalFile();
        failedFile = pendingFile;
        File thatFile = other.getCanonicalFile();

        if (thisFile.equals(thatFile)) {
          return true;
        } else {
          return false;
        }
      } catch (IOException e) {
        logger.log(TreeLogger.ERROR,
            "Unable to determine canonical path of pending resource '"
                + failedFile.toString() + "'", e);
      }
      return false;
    }
  }

  private final CacheManager cacheManager;

  private final Set committedGeneratedCups = new HashSet();

  private final File genDir;

  private final Set generatedTypeNames = new HashSet();

  private final File outDir;

  private final Map pendingResourcesByOutputStream = new IdentityHashMap();

  private final PropertyOracle propOracle;

  private final TypeOracle typeOracle;

  private final Map uncommittedGeneratedCupsByPrintWriter = new IdentityHashMap();

  /**
   * Normally, the compiler host would be aware of the same types that are
   * available in the supplied type oracle although it isn't strictly required.
   */
  public StandardGeneratorContext(TypeOracle typeOracle,
      PropertyOracle propOracle, File genDir, File outDir,
      CacheManager cacheManager) {
    this.propOracle = propOracle;
    this.typeOracle = typeOracle;
    this.genDir = genDir;
    this.outDir = outDir;
    this.cacheManager = cacheManager;
  }

  /**
   * Commits a pending generated type.
   */
  public final void commit(TreeLogger logger, PrintWriter pw) {
    GeneratedCompilationUnitProvider gcup = (GeneratedCompilationUnitProvider) uncommittedGeneratedCupsByPrintWriter.get(pw);
    if (gcup != null) {
      gcup.commit();
      uncommittedGeneratedCupsByPrintWriter.remove(pw);
      committedGeneratedCups.add(gcup);
    } else {
      logger.log(TreeLogger.WARN,
          "Generator attempted to commit an unknown PrintWriter", null);
    }
  }

  public void commitResource(TreeLogger logger, OutputStream os)
      throws UnableToCompleteException {

    // Find the pending resource using its output stream as a key.
    PendingResource pendingResource = (PendingResource) pendingResourcesByOutputStream.get(os);
    if (pendingResource != null) {
      // Actually write the bytes to disk.
      pendingResource.commit(logger);

      // The resource is now no longer pending, so remove it from the map.
      // If the commit above throws an exception, it's okay to leave the entry
      // in the map because it will be reported later as not having been
      // committed, which is accurate.
      pendingResourcesByOutputStream.remove(os);
    } else {
      logger.log(TreeLogger.WARN,
          "Generator attempted to commit an unknown OutputStream", null);
      throw new UnableToCompleteException();
    }
  }

  /**
   * Call this whenever generators are known to not be running to clear out
   * uncommitted compilation units and to force committed compilation units to
   * be parsed and added to the type oracle.
   * 
   * @return types generated during this object's lifetime
   */
  public final JClassType[] finish(TreeLogger logger)
      throws UnableToCompleteException {

    abortUncommittedResources(logger);

    // Process pending generated types.
    List genTypeNames = new ArrayList();

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

        assert (cacheManager.getTypeOracle() == typeOracle);
        TypeOracleBuilder builder = new TypeOracleBuilder(cacheManager);
        for (Iterator iter = committedGeneratedCups.iterator(); iter.hasNext();) {
          GeneratedCompilationUnitProvider gcup = (GeneratedCompilationUnitProvider) iter.next();
          String typeName = gcup.getTypeName();
          String genTypeName = gcup.getPackageName() + "." + typeName;
          genTypeNames.add(genTypeName);
          CompilationUnitProvider cup = writeSource(logger, gcup, typeName);
          builder.addCompilationUnit(cup);
          cacheManager.addGeneratedCup(cup);
          
          if (subBranch != null) {
            subBranch.log(TreeLogger.DEBUG, cup.getLocation(), null);
          }
        }
        
        builder.build(branch);
      }

      // Return the generated types.
      JClassType[] genTypes = new JClassType[genTypeNames.size()];
      int next = 0;
      for (Iterator iter = genTypeNames.iterator(); iter.hasNext();) {
        String genTypeName = (String) iter.next();
        try {
          genTypes[next++] = typeOracle.getType(genTypeName);
        } catch (NotFoundException e) {
          String msg = "Unable to find recently-generated type '" + genTypeName;
          logger.log(TreeLogger.ERROR, msg, null);
          throw new UnableToCompleteException();
        }
      }
      return genTypes;
    } finally {

      // Remind the user if there uncommitted cups.
      if (!uncommittedGeneratedCupsByPrintWriter.isEmpty()) {
        String msg = "For the following type(s), generated source was never committed (did you forget to call commit()?)";
        logger = logger.branch(TreeLogger.WARN, msg, null);

        for (Iterator iter = uncommittedGeneratedCupsByPrintWriter.values().iterator(); iter.hasNext();) {
          StaticCompilationUnitProvider cup = (StaticCompilationUnitProvider) iter.next();
          String typeName = cup.getPackageName() + "." + cup.getTypeName();
          logger.log(TreeLogger.WARN, typeName, null);
        }
      }

      uncommittedGeneratedCupsByPrintWriter.clear();
      committedGeneratedCups.clear();
      generatedTypeNames.clear();
    }
  }

  public File getOutputDir() {
    return outDir;
  }

  public final PropertyOracle getPropertyOracle() {
    return propOracle;
  }

  public final TypeOracle getTypeOracle() {
    return typeOracle;
  }

  public final PrintWriter tryCreate(TreeLogger logger, String packageName,
      String simpleTypeName) {
    String typeName = packageName + "." + simpleTypeName;

    // Is type already known to the host?
    JClassType existingType = typeOracle.findType(packageName, simpleTypeName);
    if (existingType != null) {
      logger.log(TreeLogger.DEBUG, "Type '" + typeName
          + "' already exists and will not be re-created ", null);
      return null;
    }

    // Has anybody tried to create this type during this iteration?
    if (generatedTypeNames.contains(typeName)) {
      final String msg = "A request to create type '"
          + typeName
          + "' was received while the type itself was being created; this might be a generator or configuration bug";
      logger.log(TreeLogger.WARN, msg, null);
      return null;
    }

    // The type isn't there, so we can let the caller create it. Remember that
    // it is pending so another attempt to create the same type will fail.
    GeneratedCompilationUnitProvider gcup = new GeneratedCompilationUnitProvider(
        packageName, simpleTypeName);
    uncommittedGeneratedCupsByPrintWriter.put(gcup.pw, gcup);
    generatedTypeNames.add(typeName);

    return gcup.pw;
  }

  public OutputStream tryCreateResource(TreeLogger logger, String name)
      throws UnableToCompleteException {

    logger = logger.branch(TreeLogger.DEBUG,
        "Preparing pending output resource '" + name + "'", null);

    // Disallow null or empty names.
    if (name == null || name.trim().equals("")) {
      logger.log(TreeLogger.ERROR,
          "The resource name must be a non-empty string", null);
      throw new UnableToCompleteException();
    }

    // Disallow absolute paths.
    File f = new File(name);
    if (f.isAbsolute()) {
      logger.log(
          TreeLogger.ERROR,
          "Resource paths are intended to be relative to the compiled output directory and cannot be absolute",
          null);
      throw new UnableToCompleteException();
    }

    // Disallow backslashes (to promote consistency in calling code).
    if (name.indexOf('\\') >= 0) {
      logger.log(
          TreeLogger.ERROR,
          "Resource paths must contain forward slashes (not backslashes) to denote subdirectories",
          null);
      throw new UnableToCompleteException();
    }

    // Compute the final path.
    File pendingFile = new File(outDir, name);

    // See if this file is already pending.
    for (Iterator iter = pendingResourcesByOutputStream.values().iterator(); iter.hasNext();) {
      PendingResource pendingResource = (PendingResource) iter.next();
      if (pendingResource.isSamePath(logger, pendingFile)) {
        // It is already pending.
        logger.log(TreeLogger.WARN, "The file is already a pending resource",
            null);
        return null;
      }
    }

    // If this file already exists, we won't overwrite it.
    if (pendingFile.exists()) {
      logger.log(TreeLogger.TRACE, "File already exists", null);
      return null;
    }

    // Record that this file is pending.
    PendingResource pendingResource = new PendingResource(pendingFile);
    OutputStream os = pendingResource.getOutputStream();
    pendingResourcesByOutputStream.put(os, pendingResource);

    return os;
  }

  private void abortUncommittedResources(TreeLogger logger) {
    if (pendingResourcesByOutputStream.isEmpty()) {
      // Nothing to do.
      return;
    }

    // Warn the user about uncommitted resources.
    logger = logger.branch(
        TreeLogger.WARN,
        "The following resources will not be created because they were never committed (did you forget to call commit()?)",
        null);

    try {
      for (Iterator iter = pendingResourcesByOutputStream.values().iterator(); iter.hasNext();) {
        PendingResource pendingResource = (PendingResource) iter.next();
        logger.log(TreeLogger.WARN,
            pendingResource.getFile().getAbsolutePath(), null);
      }
    } finally {
      pendingResourcesByOutputStream.clear();
    }
  }

  /**
   * Writes the source of the specified compilation unit to disk if a gen
   * directory is specified.
   * 
   * @param cup the compilation unit whose contents might need to be written
   * @param simpleTypeName the fully-qualified type name
   * @return a wrapper for the existing cup with a proper location
   */
  private CompilationUnitProvider writeSource(TreeLogger logger,
      CompilationUnitProvider cup, String simpleTypeName)
      throws UnableToCompleteException {

    if (genDir == null) {
      // No place to write it.
      return cup;
    }

    if (Util.isCompilationUnitOnDisk(cup.getLocation())) {
      // Already on disk.
      return cup;
    }

    // Let's do write it.
    String typeName = cup.getPackageName() + "." + simpleTypeName;
    String relativePath = typeName.replace('.', '/') + ".java";
    File srcFile = new File(genDir, relativePath);
    Util.writeCharsAsFile(logger, srcFile, cup.getSource());

    // Update the location of the cup
    Throwable caught = null;
    try {
      URL fileURL = srcFile.toURL();
      URLCompilationUnitProvider fileBaseCup = new GeneratedCUP(fileURL,
          cup.getPackageName());
      return fileBaseCup;
    } catch (MalformedURLException e) {
      caught = e;
    }
    logger.log(TreeLogger.ERROR,
        "Internal error: cannot build URL from synthesized file name '"
            + srcFile.getAbsolutePath() + "'", caught);
    throw new UnableToCompleteException();
  }
}