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
import com.google.gwt.core.ext.TreeLogger.HelpInfo;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompilationUnit;
import com.google.gwt.dev.javac.GWTProblem;
import com.google.gwt.dev.javac.JdtCompiler;
import com.google.gwt.dev.javac.JdtCompiler.CompilationUnitAdapter;
import com.google.gwt.dev.javac.impl.Shared;
import com.google.gwt.dev.util.CharArrayComparator;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * A facade around the JDT compiler to manage on-demand compilation, caching
 * smartly where possible.
 */
public abstract class AbstractCompiler {

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

      doCompilationUnitDeclarationValidation(cud, logger);

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

          HelpInfo helpInfo = null;
          if (error instanceof GWTProblem) {
            GWTProblem gwtProblem = (GWTProblem) error;
            helpInfo = gwtProblem.getHelpInfo();
          }
          branch.log(TreeLogger.ERROR, msgBuf.toString(), null, helpInfo);
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

      if (isPackage(qname)) {
        logger.log(TreeLogger.SPAM, "Found to be a package", null);
        return null;
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
      CompilationUnit unit = findCompilationUnit(qname);
      if (unit != null) {
        logger.log(TreeLogger.SPAM, "Found type in compilation unit: "
            + unit.getDisplayLocation());
        ICompilationUnit icu = new CompilationUnitAdapter(unit);
        NameEnvironmentAnswer out = new NameEnvironmentAnswer(icu, null);
        nameEnvironmentAnswerForTypeName.put(qname, out);
        return out;
      } else {
        ClassLoader classLoader = getClassLoader();
        URL resourceURL = classLoader.getResource(className.replace('.', '/')
            + ".class");
        if (resourceURL != null) {
          /*
           * We know that there is a .class file that matches the name that we
           * are looking for. However, at least on OSX, this lookup is case
           * insensitive so we need to use Class.forName to effectively verify
           * the case.
           */
          if (isBinaryType(classLoader, className)) {
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
      } else if (isPackage(packageName)
          || isPackage(getClassLoader(), packageName)) {
        // Grow our own list to spare calls into the host.
        //
        rememberPackage(packageName);
        return true;
      } else {
        return false;
      }
    }

    private ClassLoader getClassLoader() {
      return Thread.currentThread().getContextClassLoader();
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

    private boolean isPackage(ClassLoader classLoader, String packageName) {
      String packageAsPath = packageName.replace('.', '/');
      return classLoader.getResource(packageAsPath) != null;
    }

    private boolean isPackage(String packageName) {
      return packages.contains(packageName);
    }
  }

  private static final Comparator<CompilationUnitDeclaration> CUD_COMPARATOR = new Comparator<CompilationUnitDeclaration>() {

    public int compare(CompilationUnitDeclaration cud1,
        CompilationUnitDeclaration cud2) {
      ICompilationUnit cu1 = cud1.compilationResult().getCompilationUnit();
      ICompilationUnit cu2 = cud2.compilationResult().getCompilationUnit();
      char[][] package1 = cu1.getPackageName();
      char[][] package2 = cu2.getPackageName();
      for (int i = 0, c = Math.min(package1.length, package2.length); i < c; ++i) {
        int result = CharArrayComparator.INSTANCE.compare(package1[i],
            package2[i]);
        if (result != 0) {
          return result;
        }
      }
      int result = package2.length - package1.length;
      if (result != 0) {
        return result;
      }
      return CharArrayComparator.INSTANCE.compare(cu1.getMainTypeName(),
          cu2.getMainTypeName());
    }
  };

  protected final CompilationState compilationState;

  protected final ThreadLocalTreeLoggerProxy threadLogger = new ThreadLocalTreeLoggerProxy();

  private final CompilerImpl compiler;

  private final boolean doGenerateBytes;

  private final Set<String> knownPackages = new HashSet<String>();

  private final Map<String, NameEnvironmentAnswer> nameEnvironmentAnswerForTypeName = new HashMap<String, NameEnvironmentAnswer>();

  private final Set<String> packages = new HashSet<String>();

  protected AbstractCompiler(CompilationState compilationState,
      boolean doGenerateBytes) {
    this.compilationState = compilationState;
    this.doGenerateBytes = doGenerateBytes;
    rememberPackage("");

    INameEnvironment env = new INameEnvironmentImpl();
    IErrorHandlingPolicy pol = DefaultErrorHandlingPolicies.proceedWithAllProblems();
    IProblemFactory probFact = new DefaultProblemFactory(Locale.getDefault());
    ICompilerRequestor req = new ICompilerRequestorImpl();
    CompilerOptions options = JdtCompiler.getCompilerOptions(true);

    // This is only needed by TypeOracleBuilder to parse metadata.
    options.docCommentSupport = false;

    compiler = new CompilerImpl(env, pol, options, req, probFact);
  }

  protected final CompilationUnitDeclaration[] compile(TreeLogger logger,
      ICompilationUnit[] units) {
    // Initialize the packages list.
    for (CompilationUnit unit : compilationState.getCompilationUnits()) {
      String packageName = Shared.getPackageName(unit.getTypeName());
      addPackages(packageName);
    }

    // Any additional compilation units that are found to be needed will be
    // pulled in while procssing compilation units. See CompilerImpl.process().
    //
    TreeLogger oldLogger = threadLogger.push(logger);
    try {
      Set<CompilationUnitDeclaration> cuds = new TreeSet<CompilationUnitDeclaration>(
          CUD_COMPARATOR);
      compiler.compile(units, cuds);
      int size = cuds.size();
      CompilationUnitDeclaration[] cudArray = new CompilationUnitDeclaration[size];
      return cuds.toArray(cudArray);
    } finally {
      threadLogger.pop(oldLogger);
    }
  }

  @SuppressWarnings("unused")
  // overrider may use unused parameter
  protected void doAcceptResult(CompilationResult result) {
    // Do nothing by default.
    //
  }

  @SuppressWarnings("unused")
  // overrider may use unused parameter
  protected void doCompilationUnitDeclarationValidation(
      CompilationUnitDeclaration cud, TreeLogger logger) {
    // Do nothing by default.
  }

  @SuppressWarnings("unused")
  // overrider may use unused parameter
  protected String[] doFindAdditionalTypesUsingJsni(TreeLogger logger,
      CompilationUnitDeclaration cud) {
    return Empty.STRINGS;
  }

  @SuppressWarnings("unused")
  // overrider may use unused parameter
  protected String[] doFindAdditionalTypesUsingRebinds(TreeLogger logger,
      CompilationUnitDeclaration cud) {
    return Empty.STRINGS;
  }

  protected CompilationUnit findCompilationUnit(String qname) {
    // Build the initial set of compilation units.
    Map<String, CompilationUnit> unitMap = compilationState.getCompilationUnitMap();
    CompilationUnit unit = unitMap.get(qname);
    while (unit == null) {
      int pos = qname.lastIndexOf('.');
      if (pos < 0) {
        return null;
      }
      qname = qname.substring(0, pos);
      unit = unitMap.get(qname);
    }
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

  private void addPackages(String packageName) {
    if (packages.contains(packageName)) {
      return;
    }
    while (true) {
      packages.add(String.valueOf(packageName));
      int pos = packageName.lastIndexOf('.');
      if (pos > 0) {
        packageName = packageName.substring(0, pos);
      } else {
        packages.add("");
        break;
      }
    }
  }
}
