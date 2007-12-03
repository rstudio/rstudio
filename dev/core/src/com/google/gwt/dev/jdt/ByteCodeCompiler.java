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
package com.google.gwt.dev.jdt;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;

/**
 * A facade around the JDT compiler to manage on-demand Java source to bytecode
 * compilation, caching compiled bytecode where possible.
 */
public class ByteCodeCompiler extends AbstractCompiler {

  private final CacheManager cacheManager;

  /**
   * Creates a bytecode compiler for use not in hosted mode. All bytecode will
   * be thrown away after reload.
   * 
   * @param sourceOracle used to find the source
   */
  public ByteCodeCompiler(SourceOracle sourceOracle) {
    super(sourceOracle, true);
    this.cacheManager = new CacheManager();
  }

  /**
   * Creates a byte code compiler given the supplied sourceOracle (to find the
   * source) and the supplied cacheManager (to keep the bytecode and other
   * info). If the cacheManager has a cacheDir, it will keep bytecode across
   * reload, and load them from the cacheDir on startup. Otherwise, each reload
   * will clear the cache. In hosted mode, the cacheManager used to create this
   * object should be the same one used to create the typeOracleBuilder.
   * 
   * @param sourceOracle used to find the source
   * @param cacheManager used to keep the cached information
   */
  public ByteCodeCompiler(SourceOracle sourceOracle, CacheManager cacheManager) {
    super(sourceOracle, true);
    this.cacheManager = cacheManager;
  }

  /**
   * Get the bytecode for the specified type.
   * 
   * @param binaryTypeName the binary type name to look up or compile
   */
  public byte[] getClassBytes(TreeLogger logger, String binaryTypeName)
      throws UnableToCompleteException {

    // We use a thread logger proxy because we can't wind the logger through
    // JDT directly.
    //
    String msg = "Getting bytecode for '" + binaryTypeName + "'";
    logger = logger.branch(TreeLogger.SPAM, msg, null);

    TreeLogger oldLogger = threadLogger.push(logger);
    try {

      // Check the bytecode cache in case we've already compiled it.
      //
      ByteCode byteCode = doGetByteCodeFromCache(logger, binaryTypeName);
      if (byteCode != null) {
        // We have it already.
        //
        return byteCode.getBytes();
      }

      // Need to compile it. It could be the case that we have tried before and
      // failed, but on the off chance that it's been fixed since then, we adopt
      // a policy of always trying to recompile if we don't have it cached.
      //
      ICompilationUnit start = getCompilationUnitForType(logger, binaryTypeName);
      compile(logger, new ICompilationUnit[] {start});

      // Check the cache again. If it's there now, we succeeded.
      // If it isn't there now, we've already logged the error.
      //
      byteCode = doGetByteCodeFromCache(logger, binaryTypeName);
      if (byteCode != null) {
        return byteCode.getBytes();
      } else {
        throw new UnableToCompleteException();
      }
    } finally {
      threadLogger.pop(oldLogger);
    }
  }

  /**
   * Prevents the compile process from ever trying to compile these types from
   * source. This is used for special types that would not compile correctly
   * from source.
   * 
   * @param binaryTypeName the binary name of the specified type
   */
  public void putClassBytes(TreeLogger logger, String binaryTypeName,
      byte[] bytes, String location) {

    // We must remember the package name independently in case this is a type
    // the host doesn't actually know about.
    //
    String pkgName = "";
    int lastDot = binaryTypeName.lastIndexOf('.');
    if (lastDot != -1) {
      pkgName = binaryTypeName.substring(0, lastDot);
    }
    rememberPackage(pkgName);

    // Cache the bytes.
    //
    ByteCode byteCode = new ByteCode(binaryTypeName, bytes, location, true);
    cacheManager.acceptIntoCache(logger, binaryTypeName, byteCode);
  }

  /**
   * This method removes the bytecode which is no longer current, or if the
   * cacheManager does not have a cacheDir, all the bytecode.
   * 
   * @param logger used to describe the results to the user
   */
  public void removeStaleByteCode(TreeLogger logger) {
    cacheManager.removeStaleByteCode(logger, this);
  }

  @Override
  protected void doAcceptResult(CompilationResult result) {
    // Take all compiled class files and put them in the byte cache.
    //
    TreeLogger logger = getLogger();
    ClassFile[] classFiles = result.getClassFiles();
    for (int i = 0; i < classFiles.length; i++) {
      ClassFile classFile = classFiles[i];
      char[][] compoundName = classFile.getCompoundName();
      char[] classNameChars = CharOperation.concatWith(compoundName, '.');
      String className = String.valueOf(classNameChars);
      byte bytes[] = classFile.getBytes();
      String loc = String.valueOf(result.compilationUnit.getFileName());
      boolean isTransient = true;
      if (result.compilationUnit instanceof ICompilationUnitAdapter) {
        ICompilationUnitAdapter unit = (ICompilationUnitAdapter) result.compilationUnit;
        isTransient = unit.getCompilationUnitProvider().isTransient();
      }
      ByteCode byteCode = new ByteCode(className, bytes, loc, isTransient);
      if (cacheManager.acceptIntoCache(logger, className, byteCode)) {
        logger.log(TreeLogger.SPAM, "Successfully compiled and cached '"
            + className + "'", null);
      }
    }
  }

  /**
   * Checks the cache for bytecode for the specified binary type name. Silently
   * removes and pretends it didn't see any bytecode that is out-of-date with
   * respect to the compilation unit that provides it.
   */
  @Override
  protected ByteCode doGetByteCodeFromCache(TreeLogger logger,
      String binaryTypeName) {
    return cacheManager.getByteCode(binaryTypeName);
  }
}
