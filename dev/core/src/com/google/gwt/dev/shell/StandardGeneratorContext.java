/*
 * Copyright 2006 Google Inc.
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

import java.io.CharArrayWriter;
import java.io.File;
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
  public static class GeneratedCompilationUnitProvider extends
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

  private static final class GeneratedCUP extends URLCompilationUnitProvider {
    private GeneratedCUP(URL url, String name) {
      super(url, name);
    }

    public long getLastModified() throws UnableToCompleteException {
      // Make it seem really old so it won't cause recompiles.
      //
      return 0L;
    }
  }

  private final CacheManager cacheManager;

  private final Set committedGeneratedCups = new HashSet();

  private final File genDir;

  private final Set generatedTypeNames = new HashSet();

  private final PropertyOracle propOracle;

  private final TypeOracle typeOracle;

  private final Map uncommittedGeneratedCupsByPrintWriter = new IdentityHashMap();

  /**
   * Normally, the compiler host would be aware of the same types that are
   * available in the supplied type oracle although it isn't strictly required.
   */
  public StandardGeneratorContext(TypeOracle typeOracle,
      PropertyOracle propOracle, File genDir, CacheManager cacheManager) {
    this.propOracle = propOracle;
    this.typeOracle = typeOracle;
    this.genDir = genDir;
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
          "Generator attempted to commit an unknown stream", null);
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

          if (subBranch != null) {
            subBranch.log(TreeLogger.DEBUG, cup.getLocation(), null);
          }
        }
        cacheManager.markVolatileFiles(committedGeneratedCups);
        builder.build(branch);
      }

      // Return the generated types.
      //
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
      //
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
    //
    JClassType existingType = typeOracle.findType(packageName, simpleTypeName);
    if (existingType != null) {
      logger.log(TreeLogger.DEBUG, "Type '" + typeName
          + "' already exists and will not be re-created ", null);
      return null;
    }

    // Has anybody tried to create this type during this iteraion?
    //
    if (generatedTypeNames.contains(typeName)) {
      final String msg = "A request to create type '"
          + typeName
          + "' was received while the type itself was being created; this might be a generator or configuration bug";
      logger.log(TreeLogger.WARN, msg, null);
      return null;
    }

    // The type isn't there, so we can let the caller create it. Remember that
    // it is pending so another attempt to create the same type will fail.
    //
    GeneratedCompilationUnitProvider gcup = new GeneratedCompilationUnitProvider(
        packageName, simpleTypeName);
    uncommittedGeneratedCupsByPrintWriter.put(gcup.pw, gcup);
    generatedTypeNames.add(typeName);

    return gcup.pw;
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
    //
    String typeName = cup.getPackageName() + "." + simpleTypeName;
    String relativePath = typeName.replace('.', '/') + ".java";
    File srcFile = new File(genDir, relativePath);
    Util.writeCharsAsFile(logger, srcFile, cup.getSource());

    // Update the location of the cup
    //
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
