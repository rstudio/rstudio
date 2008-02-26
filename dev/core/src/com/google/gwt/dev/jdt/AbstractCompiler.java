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
package com.google.gwt.dev.jdt;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.CompilationUnitProvider;
import com.google.gwt.dev.util.Empty;
import com.google.gwt.dev.util.PerfLogger;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.log.ThreadLocalTreeLoggerProxy;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.Compiler;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.ICompilerRequestor;
import org.eclipse.jdt.internal.compiler.IErrorHandlingPolicy;
import org.eclipse.jdt.internal.compiler.IProblemFactory;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;

import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A facade around the JDT compiler to manage on-demand compilation, caching
 * smartly where possible.
 */
public abstract class AbstractCompiler {

  /**
   * A policy that can be set to affect which
   * {@link org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration
   * CompilationUnitDeclarations} the compiler processes.
   */
  public interface CachePolicy {

    /**
     * Return true if <code>cud</code> should be processed, otherwise false.
     * 
     * @param cud a not <code>null</code> unit
     * @return true iff <code>cud</code> should be fully processed
     */
    boolean shouldProcess(CompilationUnitDeclaration cud);
  }

  /**
   * Adapted to hook the processing of compilation unit declarations so as to be
   * able to add additional compilation units based on the results of
   * previously-compiled ones. Examples of cases where this is useful include
   * classes referenced only from JSNI and <code>GWT.create</code>.
   */
  private class CompilerImpl extends Compiler {

    private Set<CompilationUnitDeclaration> cuds;
    private long jdtProcessNanos;

    public CompilerImpl(INameEnvironment environment,
        IErrorHandlingPolicy policy, CompilerOptions compilerOptions,
        ICompilerRequestor requestor, IProblemFactory problemFactory) {
      super(environment, policy, compilerOptions, requestor, problemFactory);
    }

    @Override
    public void compile(ICompilationUnit[] sourceUnits) {
      jdtProcessNanos = 0;
      super.compile(sourceUnits);
      PerfLogger.log("AbstractCompiler.compile, time spent in JDT process callback: "
          + (jdtProcessNanos / 1000000) + "ms");
      cuds = null;
    }

    @Override
    public void process(CompilationUnitDeclaration cud, int index) {

      long processBeginNanos = System.nanoTime();

      if (!cachePolicy.shouldProcess(cud)) {
        jdtProcessNanos += System.nanoTime() - processBeginNanos;
        return;
      }

      // The following block of code is a copy of super.process(cud, index),
      // with the modification that cud.generateCode is conditionally called
      // based on doGenerateBytes
      {
        this.parser.getMethodBodies(cud);

        // fault in fields & methods
        if (cud.scope != null) {
          cud.scope.faultInTypes();
        }

        // verify inherited methods
        if (cud.scope != null) {
          cud.scope.verifyMethods(lookupEnvironment.methodVerifier());
        }

        // type checking
        cud.resolve();

        // flow analysis
        cud.analyseCode();

        // code generation
        if (doGenerateBytes) {
          cud.generateCode();
        }

        // reference info
        if (options.produceReferenceInfo && cud.scope != null) {
          cud.scope.storeDependencyInfo();
        }

        // refresh the total number of units known at this stage
        cud.compilationResult.totalUnitsKnown = totalUnits;
      }

      ICompilationUnit cu = cud.compilationResult.compilationUnit;
      String loc = String.valueOf(cu.getFileName());
      TreeLogger logger = threadLogger.branch(TreeLogger.SPAM,
          "Scanning for additional dependencies: " + loc, null);

      // Examine the cud for magic types.
      //
      String[] typeNames = doFindAdditionalTypesUsingJsni(logger, cud);

      // Accept each new compilation unit.
      //
      for (int i = 0; i < typeNames.length; i++) {
        String typeName = typeNames[i];
        final String msg = "Need additional type '" + typeName + "'";
        logger.log(TreeLogger.SPAM, msg, null);

        resolvePossiblyNestedType(typeName);
      }

      typeNames = doFindAdditionalTypesUsingRebinds(logger, cud);

      // Accept each new compilation unit, and check for instantiability
      //
      for (int i = 0; i < typeNames.length; i++) {
        String typeName = typeNames[i];
        final String msg = "Need additional type '" + typeName + "'";
        logger.log(TreeLogger.SPAM, msg, null);

        // This causes the compiler to find the additional type, possibly
        // winding its back to ask for the compilation unit from the source
        // oracle.
        //
        resolvePossiblyNestedType(typeName);
      }

      // Optionally remember this cud.
      //
      if (cuds != null) {
        cuds.add(cud);
      }

      jdtProcessNanos += System.nanoTime() - processBeginNanos;
    }

    private void compile(ICompilationUnit[] units,
        Set<CompilationUnitDeclaration> cuds) {
      this.cuds = cuds;
      compile(units);
    }

    private ReferenceBinding resolvePossiblyNestedType(String typeName) {
      ReferenceBinding type = null;

      int p = typeName.indexOf('$');
      if (p > 0) {
        // resolve an outer type before trying to get the cached inner
        String cupName = typeName.substring(0, p);
        char[][] chars = CharOperation.splitOn('.', cupName.toCharArray());
        if (lookupEnvironment.getType(chars) != null) {
          // outer class was found
          chars = CharOperation.splitOn('.', typeName.toCharArray());
          type = lookupEnvironment.getCachedType(chars);
          if (type == null) {
            // no inner type; this is a pure failure
            return null;
          }
        }
      } else {
        // just resolve the type straight out
        char[][] chars = CharOperation.splitOn('.', typeName.toCharArray());
        type = lookupEnvironment.getType(chars);
      }

      if (type != null) {
        // found it
        return type;
      }

      // Assume that the last '.' should be '$' and try again.
      //
      p = typeName.lastIndexOf('.');
      if (p >= 0) {
        typeName = typeName.substring(0, p) + "$" + typeName.substring(p + 1);
        return resolvePossiblyNestedType(typeName);
      }

      return null;
    }
  }

  private class ICompilerRequestorImpl implements ICompilerRequestor {

    public ICompilerRequestorImpl() {
    }

    public void acceptResult(CompilationResult result) {
      // Handle compilation errors.
      //
      IProblem[] errors = result.getErrors();

      if (errors != null && errors.length > 0) {
        // Dump it to disk.
        //
        String fn = String.valueOf(result.compilationUnit.getFileName());
        String msg = "Errors in '" + fn + "'";
        TreeLogger branch = getLogger().branch(TreeLogger.ERROR, msg, null);

        for (int i = 0; i < errors.length; i++) {
          IProblem error = errors[i];

          // Strip the initial code from each error.
          //
          msg = error.toString();
          msg = msg.substring(msg.indexOf(' '));

          // Append 'Line #: msg' to the error message.
          //
          StringBuffer msgBuf = new StringBuffer();
          int line = error.getSourceLineNumber();
          if (line > 0) {
            msgBuf.append("Line ");
            msgBuf.append(line);
            msgBuf.append(": ");
          }
          msgBuf.append(msg);
          branch.log(TreeLogger.ERROR, msgBuf.toString(), null);
        }
      }

      // Let the subclass do something with this if it wants to.
      //
      doAcceptResult(result);
    }
  }

  private class INameEnvironmentImpl implements INameEnvironment {

    public INameEnvironmentImpl() {
    }

    public void cleanup() {
      // intentionally blank
    }

    public NameEnvironmentAnswer findType(char[] type, char[][] pkg) {
      return findType(CharOperation.arrayConcat(pkg, type));
    }

    public NameEnvironmentAnswer findType(char[][] compoundTypeName) {

      // Cache the answers to findType to prevent the creation of more
      // CompilationUnitDeclarations than needed.
      String qname = CharOperation.toString(compoundTypeName);
      if (nameEnvironmentAnswerForTypeName.containsKey(qname)) {
        return (nameEnvironmentAnswerForTypeName.get(qname));
      }
      TreeLogger logger = threadLogger.branch(TreeLogger.SPAM,
          "Compiler is asking about '" + qname + "'", null);

      if (sourceOracle.isPackage(qname)) {
        logger.log(TreeLogger.SPAM, "Found to be a package", null);
        return null;
      }

      // Try to find the compiled type in the cache.
      //
      ByteCode byteCode = doGetByteCodeFromCache(logger, qname);
      if (byteCode != null) {
        // Return it as a binary type to JDT.
        //
        byte[] classBytes = byteCode.getBytes();
        char[] loc = byteCode.getLocation().toCharArray();
        try {
          logger.log(TreeLogger.SPAM, "Found cached bytes", null);
          ClassFileReader cfr = new ClassFileReader(classBytes, loc);
          NameEnvironmentAnswer out = new NameEnvironmentAnswer(cfr, null);
          nameEnvironmentAnswerForTypeName.put(qname, out);
          return out;
        } catch (ClassFormatException e) {
          // Bad bytecode in the cache. Remove it from the cache.
          //
          String msg = "Bad bytecode for '" + qname + "'";
          compiler.problemReporter.abortDueToInternalError(msg);
          return null;
        }
      }

      // Didn't find it in the cache, so let's compile from source.
      // Strip off the inner types, if any
      //
      String className = qname;
      int pos = qname.indexOf('$');
      if (pos >= 0) {
        qname = qname.substring(0, pos);
        // Recheck the cache for the outer type.
        if (nameEnvironmentAnswerForTypeName.containsKey(qname)) {
          return (nameEnvironmentAnswerForTypeName.get(qname));
        }
      }
      CompilationUnitProvider cup;
      try {
        cup = sourceOracle.findCompilationUnit(logger, qname);
        if (cup != null) {
          logger.log(TreeLogger.SPAM, "Found type in compilation unit: "
              + cup.getLocation(), null);
          ICompilationUnitAdapter unit = new ICompilationUnitAdapter(cup);
          NameEnvironmentAnswer out = new NameEnvironmentAnswer(unit, null);
          nameEnvironmentAnswerForTypeName.put(qname, out);
          return out;
        } else {
          ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
          if (isBinaryType(contextClassLoader, className)) {
            URL resourceURL = contextClassLoader.getResource(className.replace(
                '.', '/')
                + ".class");
            if (resourceURL != null) {
              byte[] classBytes = Util.readURLAsBytes(resourceURL);
              ClassFileReader cfr;
              try {
                cfr = new ClassFileReader(classBytes, null);
                NameEnvironmentAnswer out = new NameEnvironmentAnswer(cfr, null);
                nameEnvironmentAnswerForTypeName.put(qname, out);
                return out;
              } catch (ClassFormatException e) {
                // Ignored.
              }
            }
          }

          logger.log(TreeLogger.SPAM, "Not a known type", null);
          return null;
        }
      } catch (UnableToCompleteException e) {
        // It was found, but something went really wrong trying to get it.
        //
        String msg = "Error acquiring source for '" + qname + "'";
        compiler.problemReporter.abortDueToInternalError(msg);
        return null;
      }
    }

    public boolean isPackage(char[][] parentPkg, char[] pkg) {
      // In special cases where class bytes are asserted from the outside,
      // a package can exist that the host doesn't know about. We have to
      // do a special check for these cases.
      //
      final char[] pathChars = CharOperation.concatWith(parentPkg, pkg, '.');
      String packageName = String.valueOf(pathChars);
      if (knownPackages.contains(packageName)) {
        return true;
      } else if (sourceOracle.isPackage(packageName)) {
        // Grow our own list to spare calls into the host.
        //
        rememberPackage(packageName);
        return true;
      } else {
        return false;
      }
    }

    private boolean isBinaryType(ClassLoader classLoader, String typeName) {
      try {
        Class.forName(typeName, false, classLoader);
        return true;
      } catch (ClassNotFoundException e) {
        // Ignored.
      } catch (LinkageError e) {
        // Ignored.
      }

      // Assume that it is not a binary type.
      return false;
    }
  }

  private static final CachePolicy DEFAULT_POLICY = new CachePolicy() {
    public boolean shouldProcess(CompilationUnitDeclaration cud) {
      return true;
    }
  };

  protected final ThreadLocalTreeLoggerProxy threadLogger = new ThreadLocalTreeLoggerProxy();

  private CachePolicy cachePolicy = DEFAULT_POLICY;

  private final CompilerImpl compiler;

  private final boolean doGenerateBytes;

  private final Set<String> knownPackages = new HashSet<String>();

  private final Map<String, NameEnvironmentAnswer> nameEnvironmentAnswerForTypeName = new HashMap<String, NameEnvironmentAnswer>();

  private final SourceOracle sourceOracle;

  private final Map<String, ICompilationUnit> unitsByTypeName = new HashMap<String, ICompilationUnit>();

  protected AbstractCompiler(SourceOracle sourceOracle, boolean doGenerateBytes) {
    this.sourceOracle = sourceOracle;
    this.doGenerateBytes = doGenerateBytes;
    rememberPackage("");

    INameEnvironment env = new INameEnvironmentImpl();
    IErrorHandlingPolicy pol = DefaultErrorHandlingPolicies.proceedWithAllProblems();
    IProblemFactory probFact = new DefaultProblemFactory(Locale.getDefault());
    ICompilerRequestor req = new ICompilerRequestorImpl();
    Map<String, String> settings = new HashMap<String, String>();
    settings.put(CompilerOptions.OPTION_LineNumberAttribute,
        CompilerOptions.GENERATE);
    settings.put(CompilerOptions.OPTION_SourceFileAttribute,
        CompilerOptions.GENERATE);
    /*
     * Tricks like "boolean stopHere = true;" depend on this setting to work in
     * hosted mode. In web mode, our compiler should optimize them out once we
     * do real data flow.
     */
    settings.put(CompilerOptions.OPTION_PreserveUnusedLocal,
        CompilerOptions.PRESERVE);
    settings.put(CompilerOptions.OPTION_ReportDeprecation,
        CompilerOptions.IGNORE);
    settings.put(CompilerOptions.OPTION_LocalVariableAttribute,
        CompilerOptions.GENERATE);
    settings.put(CompilerOptions.OPTION_Compliance, CompilerOptions.VERSION_1_5);
    settings.put(CompilerOptions.OPTION_Source, CompilerOptions.VERSION_1_5);
    settings.put(CompilerOptions.OPTION_TargetPlatform,
        CompilerOptions.VERSION_1_5);

    // This is needed by TypeOracleBuilder to parse metadata.
    settings.put(CompilerOptions.OPTION_DocCommentSupport,
        CompilerOptions.ENABLED);

    compiler = new CompilerImpl(env, pol, new CompilerOptions(settings), req,
        probFact);
  }

  public CachePolicy getCachePolicy() {
    return cachePolicy;
  }

  public void invalidateUnitsInFiles(Set<String> fileNames,
      Set<String> typeNames) {
    // StandardSourceOracle has its own cache that needs to be cleared
    // out. Short of modifying the interface SourceOracle to have an
    // invalidateCups, this check is needed.
    if (sourceOracle instanceof StandardSourceOracle) {
      StandardSourceOracle sso = (StandardSourceOracle) sourceOracle;
      sso.invalidateCups(typeNames);
    }
    for (String qname : typeNames) {
      unitsByTypeName.remove(qname);
      nameEnvironmentAnswerForTypeName.remove(qname);
    }
  }

  public void setCachePolicy(CachePolicy policy) {
    this.cachePolicy = policy;
  }

  protected final CompilationUnitDeclaration[] compile(TreeLogger logger,
      ICompilationUnit[] units) {
    // Any additional compilation units that are found to be needed will be
    // pulled in while procssing compilation units. See CompilerImpl.process().
    //
    TreeLogger oldLogger = threadLogger.push(logger);
    try {
      Set<CompilationUnitDeclaration> cuds = new HashSet<CompilationUnitDeclaration>();
      compiler.compile(units, cuds);
      int size = cuds.size();
      CompilationUnitDeclaration[] cudArray = new CompilationUnitDeclaration[size];
      return cuds.toArray(cudArray);
    } finally {
      threadLogger.pop(oldLogger);
    }
  }

  protected void doAcceptResult(CompilationResult result) {
    // Do nothing by default.
    //
  }

  protected String[] doFindAdditionalTypesUsingJsni(TreeLogger logger,
      CompilationUnitDeclaration cud) {
    return Empty.STRINGS;
  }

  protected String[] doFindAdditionalTypesUsingRebinds(TreeLogger logger,
      CompilationUnitDeclaration cud) {
    return Empty.STRINGS;
  }

  /**
   * Checks to see if we already have the bytecode definition of the requested
   * type. By default we compile everything from source, so we never have it
   * unless a subclass overrides this method.
   */
  protected ByteCode doGetByteCodeFromCache(TreeLogger logger,
      String binaryTypeName) {
    return null;
  }

  /**
   * Finds a compilation unit for the given type. This is often used to
   * bootstrap compiles since during compiles, the compiler will directly ask
   * the name environment internally, bypassing this call.
   */
  protected ICompilationUnit getCompilationUnitForType(TreeLogger logger,
      String binaryTypeName) throws UnableToCompleteException {

    // We really look for the topmost type rather than a nested type.
    //
    String top = stripNestedTypeNames(binaryTypeName);

    // Check the cache.
    //
    ICompilationUnit unit = unitsByTypeName.get(top);
    if (unit != null) {
      return unit;
    }

    // Not cached, so actually look for it.
    //
    CompilationUnitProvider cup = sourceOracle.findCompilationUnit(logger, top);
    if (cup == null) {
      // Could not find the starting type.
      //
      String s = "Unable to find compilation unit for type '" + top + "'";
      logger.log(TreeLogger.WARN, s, null);
      throw new UnableToCompleteException();
    }

    // Create a cup adapter and cache it.
    //
    unit = new ICompilationUnitAdapter(cup);
    unitsByTypeName.put(top, unit);

    return unit;
  }

  protected TreeLogger getLogger() {
    return threadLogger;
  }

  /**
   * Causes the compilation service itself to recognize the specified package
   * name (and all its parent packages), avoiding a call back into the host.
   * This is useful as an optimization, but more importantly, it is useful to
   * compile against bytecode that was pre-compiled to which we don't have the
   * source. This ability is crucial bridging the gap between user-level and
   * "dev" code in hosted mode for classes such as JavaScriptHost and
   * ShellJavaScriptHost.
   */
  protected void rememberPackage(String packageName) {
    int i = packageName.lastIndexOf('.');
    if (i != -1) {
      // Ensure the parent package is also created.
      //
      rememberPackage(packageName.substring(0, i));
    }
    knownPackages.add(packageName);
  }

  protected ReferenceBinding resolvePossiblyNestedType(String typeName) {
    return compiler.resolvePossiblyNestedType(typeName);
  }

  SourceOracle getSourceOracle() {
    return sourceOracle;
  }

  private String stripNestedTypeNames(String binaryTypeName) {
    int i = binaryTypeName.lastIndexOf('.');
    if (i == -1) {
      i = 0;
    }
    int j = binaryTypeName.indexOf('$', i);
    if (j != -1) {
      return binaryTypeName.substring(0, j);
    } else {
      return binaryTypeName;
    }
  }
}
