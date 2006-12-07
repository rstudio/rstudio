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
package com.google.gwt.dev.jjs;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.CompilationUnitProvider;
import com.google.gwt.dev.jdt.ICompilationUnitAdapter;
import com.google.gwt.dev.jdt.RebindOracle;
import com.google.gwt.dev.jdt.RebindPermutationOracle;
import com.google.gwt.dev.jdt.WebModeCompilerFrontEnd;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JExpressionStatement;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNewInstance;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.impl.ArrayNormalizer;
import com.google.gwt.dev.jjs.impl.BuildTypeMap;
import com.google.gwt.dev.jjs.impl.CastNormalizer;
import com.google.gwt.dev.jjs.impl.CastOptimizer;
import com.google.gwt.dev.jjs.impl.CatchBlockNormalizer;
import com.google.gwt.dev.jjs.impl.CompoundAssignmentNormalizer;
import com.google.gwt.dev.jjs.impl.GenerateJavaAST;
import com.google.gwt.dev.jjs.impl.GenerateJavaScriptAST;
import com.google.gwt.dev.jjs.impl.JavaScriptObjectCaster;
import com.google.gwt.dev.jjs.impl.MakeCallsStatic;
import com.google.gwt.dev.jjs.impl.MethodAndClassFinalizer;
import com.google.gwt.dev.jjs.impl.MethodCallTightener;
import com.google.gwt.dev.jjs.impl.MethodInliner;
import com.google.gwt.dev.jjs.impl.Pruner;
import com.google.gwt.dev.jjs.impl.ReplaceRebinds;
import com.google.gwt.dev.jjs.impl.TypeMap;
import com.google.gwt.dev.jjs.impl.TypeTightener;
import com.google.gwt.dev.js.FullNamingStrategy;
import com.google.gwt.dev.js.JsSourceGenerationVisitor;
import com.google.gwt.dev.js.NamingStrategy;
import com.google.gwt.dev.js.ObfuscatedNamingStrategy;
import com.google.gwt.dev.js.PrettyNamingStrategy;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.util.TextOutputOnPrintWriter;
import com.google.gwt.dev.util.Util;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

/**
 * Compiles the Java <code>JProgram</code> representation into its
 * corresponding JavaScript source.
 */
public class JavaToJavaScriptCompiler {

  private static void findEntryPoints(TreeLogger logger,
      String[] mainClassNames, JProgram program)
      throws UnableToCompleteException {
    JMethod bootStrapMethod = program.createMethod("init".toCharArray(), null,
        program.getTypeVoid(), false, true, true, false, false);
    bootStrapMethod.freezeParamTypes();

    for (int i = 0; i < mainClassNames.length; ++i) {
      String mainClassName = mainClassNames[i];
      JReferenceType referenceType = program.getFromTypeMap(mainClassName);

      if (referenceType == null) {
        logger.log(TreeLogger.ERROR,
            "Could not find module entry point class '" + mainClassName + "'",
            null);
        throw new UnableToCompleteException();
      }

      if (!(referenceType instanceof JClassType)) {
        logger.log(TreeLogger.ERROR, "Module entry point class '"
            + mainClassName + "' must be a class", null);
        throw new UnableToCompleteException();
      }

      JClassType mainClass = (JClassType) referenceType;

      JMethod mainMethod = null;
      outer : for (JClassType it = mainClass; it != null; it = it.extnds) {
        for (int j = 0; j < it.methods.size(); ++j) {
          JMethod method = (JMethod) it.methods.get(j);
          if (method.getName().equals("onModuleLoad")) {
            mainMethod = method;
            break outer;
          }
        }
      }

      if (mainMethod == null) {
        logger.log(TreeLogger.ERROR,
            "Could not find entry method 'onModuleLoad' method in entry-point class "
                + mainClassName, null);
        throw new UnableToCompleteException();
      }

      if (mainMethod.params.size() > 0) {
        logger.log(TreeLogger.ERROR,
            "Entry method 'onModuleLoad' in entry-point class " + mainClassName
                + "must take zero arguments", null);
        throw new UnableToCompleteException();
      }

      if (mainMethod.isAbstract()) {
        logger.log(TreeLogger.ERROR,
            "Entry method 'onModuleLoad' in entry-point class " + mainClassName
                + "must not be abstract", null);
        throw new UnableToCompleteException();
      }

      JExpression qualifier = null;
      if (!mainMethod.isStatic()) {
        // Find the appropriate (noArg) constructor
        JMethod noArgCtor = null;
        for (int j = 0; j < mainClass.methods.size(); ++j) {
          JMethod ctor = (JMethod) mainClass.methods.get(j);
          if (ctor.getName().equals(mainClass.getShortName())) {
            if (ctor.params.size() == 0) {
              noArgCtor = ctor;
            }
          }
        }
        if (noArgCtor == null) {
          logger.log(
              TreeLogger.ERROR,
              "No default (zero argument) constructor could be found in entry-point class "
                  + mainClassName
                  + " to qualify a call to non-static entry method 'onModuleLoad'",
              null);
          throw new UnableToCompleteException();
        }

        // Construct a new instance of the class to qualify the non-static call
        JNewInstance newInstance = new JNewInstance(program, mainClass);
        qualifier = new JMethodCall(program, newInstance, noArgCtor);
      }

      JMethodCall onModuleLoadCall = new JMethodCall(program, qualifier,
          mainMethod);
      onModuleLoadCall.setCanBePolymorphic(true);
      bootStrapMethod.body.statements.add(new JExpressionStatement(program,
          onModuleLoadCall));
    }
    program.addEntryMethod(bootStrapMethod);
  }

  private final Set/* <IProblem> */problemSet = new HashSet/* <IProblem> */();

  private final String[] declEntryPoints;

  private final CompilationUnitDeclaration[] goldenCuds;

  private long lastModified;

  private final boolean obfuscate;

  private final boolean prettyNames;

  public JavaToJavaScriptCompiler(final TreeLogger logger,
      final WebModeCompilerFrontEnd compiler, final String[] declEntryPts)
      throws UnableToCompleteException {
    this(logger, compiler, declEntryPts, true, false);
  }

  public JavaToJavaScriptCompiler(final TreeLogger logger,
      final WebModeCompilerFrontEnd compiler, final String[] declEntryPts,
      boolean obfuscate, boolean prettyNames) throws UnableToCompleteException {

    if (declEntryPts.length == 0) {
      throw new IllegalArgumentException("entry point(s) required");
    }

    // Remember these for subsequent compiles.
    //
    this.declEntryPoints = declEntryPts;

    // Should we obfuscate or, if not, use pretty names?
    //
    this.obfuscate = obfuscate;
    this.prettyNames = prettyNames;

    // Find all the possible rebound entry points.
    //
    RebindPermutationOracle rpo = compiler.getRebindPermutationOracle();
    Set allEntryPoints = new HashSet();
    for (int i = 0; i < declEntryPts.length; i++) {
      String[] all = rpo.getAllPossibleRebindAnswers(logger, declEntryPts[i]);
      Util.addAll(allEntryPoints, all);
    }
    String[] entryPts = Util.toStringArray(allEntryPoints);

    // Add intrinsics needed for code gen.
    //
    int k = entryPts.length;
    String[] seedTypeNames = new String[k + 3];
    System.arraycopy(entryPts, 0, seedTypeNames, 0, k);
    seedTypeNames[k++] = "com.google.gwt.lang.Array";
    seedTypeNames[k++] = "com.google.gwt.lang.Cast";
    seedTypeNames[k++] = "com.google.gwt.lang.Exceptions";

    // Compile the source and get the compiler so we can get the parse tree
    //
    goldenCuds = compiler.getCompilationUnitDeclarations(logger, seedTypeNames);

    // See if there are none. If so, then we had problems.
    //
    if (goldenCuds.length == 0) {
      logger.log(TreeLogger.ERROR, "Cannot proceed due to previous errors",
          null);
      throw new UnableToCompleteException();
    }

    // Find the newest of all these.
    //
    lastModified = 0;
    CompilationUnitProvider newestCup = null;
    for (int i = 0; i < goldenCuds.length; i++) {
      CompilationUnitDeclaration cud = goldenCuds[i];
      ICompilationUnitAdapter icua = (ICompilationUnitAdapter) cud.compilationResult.compilationUnit;
      CompilationUnitProvider cup = icua.getCompilationUnitProvider();
      long cupLastModified = cup.getLastModified();
      if (cupLastModified > lastModified) {
        newestCup = cup;
        lastModified = cupLastModified;
      }
    }
    if (newestCup != null) {
      String loc = newestCup.getLocation();
      String msg = "Newest compilation unit is '" + loc + "'";
      logger.log(TreeLogger.DEBUG, msg, null);
    }

    // Check for errors in the returned compilation units
    //
    if (checkForErrors(logger)) {
      throw new UnableToCompleteException();
    }
  }

  /**
   * Creates finished JavaScript source code from the specified Java compilation
   * units.
   */
  public String compile(TreeLogger logger, RebindOracle rebindOracle)
      throws UnableToCompleteException {

    try {

      // (1) Build a flattened map of TypeDeclarations => JType.
      //

      // Note that all reference types (even nested and local ones) are in the
      // resulting type map. BuildTypeMap also parses all JSNI.
      //
      JProgram jprogram = new JProgram(logger, rebindOracle);
      TypeMap typeMap = new TypeMap(jprogram);
      JsProgram jsProgram = new JsProgram();
      TypeDeclaration[] allTypeDeclarations = BuildTypeMap.exec(typeMap,
          goldenCuds, jsProgram);

      // BuildTypeMap can uncover syntactic JSNI errors; report & abort
      // 
      if (checkForErrors(logger)) {
        throw new UnableToCompleteException();
      }

      // Compute all supertype/subtype info
      jprogram.typeOracle.computeBeforeAST();

      // (3) Create a normalized Java AST using our own notation.
      //

      // Create the tree from JDT
      GenerateJavaAST.exec(allTypeDeclarations, typeMap, jprogram);

      // GenerateJavaAST can uncover semantic JSNI errors; report & abort
      // 
      if (checkForErrors(logger)) {
        throw new UnableToCompleteException();
      }

      // Compute which classes have clinits
      jprogram.typeOracle.computeAfterAST();

      // Fix up GWT.create() into new operations
      ReplaceRebinds.exec(jprogram);

      // Rebind each entry point.
      //
      String[] actualEntryPoints = new String[declEntryPoints.length];
      for (int i = 0; i < declEntryPoints.length; i++) {
        actualEntryPoints[i] = rebindOracle.rebind(logger, declEntryPoints[i]);
      }
      findEntryPoints(logger, actualEntryPoints, jprogram);

      // (4) Optimize the normalized Java AST
      boolean didChange;
      do {
        didChange = false;
        // Remove unreferenced types, fields, methods, [params, locals]
        didChange = Pruner.exec(jprogram, true) || didChange;
        // finalize locals, params, fields, methods, classes
        didChange = MethodAndClassFinalizer.exec(jprogram) || didChange;
        // rewrite non-poly calls as static calls; update all call sites
        didChange = MakeCallsStatic.exec(jprogram) || didChange;

        // type flow tightening
        // - fields, locals based on assignment
        // - params based on assignment and call sites
        // - method bodies based on return statements
        // - polymorphic methods based on return types of all implementors
        didChange = TypeTightener.exec(jprogram) || didChange;

        // tighten method call bindings
        didChange = MethodCallTightener.exec(jprogram) || didChange;

        // remove unnecessary casts / optimize instanceof
        didChange = CastOptimizer.exec(jprogram) || didChange;

        // dead code removal??

        // inlining
        didChange = MethodInliner.exec(jprogram) || didChange;

        // prove that any types that have been culled from the main tree are
        // unreferenced due to type tightening?
      } while (didChange);

      // (5) "Normalize" the high-level Java tree into a lower-level tree more
      // suited for JavaScript code gen. Don't go reordering these willy-nilly
      // because there are some subtle interdependencies.
      CatchBlockNormalizer.exec(jprogram);
      CompoundAssignmentNormalizer.exec(jprogram);
      JavaScriptObjectCaster.exec(jprogram);
      CastNormalizer.exec(jprogram);
      ArrayNormalizer.exec(jprogram);

      // (6) Perform furthur post-normalization optimizations
      // Prune everything
      Pruner.exec(jprogram, false);

      // (7) Generate a JavaScript code DOM from the Java type declarations
      GenerateJavaScriptAST.exec(jprogram, jsProgram);

      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw, true);
      TextOutputOnPrintWriter out = new TextOutputOnPrintWriter(pw, obfuscate);
      NamingStrategy ns;
      if (obfuscate) {
        ns = new ObfuscatedNamingStrategy();
      } else if (prettyNames) {
        ns = new PrettyNamingStrategy();
      } else {
        ns = new FullNamingStrategy();
      }
      JsSourceGenerationVisitor v = new JsSourceGenerationVisitor(out, ns);
      jsProgram.traverse(v);

      return sw.toString();
    } catch (UnableToCompleteException e) {
      // just rethrow
      throw e;
    } catch (Throwable e) {
      logger.log(TreeLogger.ERROR, "Unexpected internal compiler error", e);
      throw new UnableToCompleteException();
    }
  }

  public long getLastModifiedTimeOfNewestCompilationUnit() {
    return lastModified;
  }

  private boolean checkForErrors(final TreeLogger logger) {
    boolean compilationFailed = false;
    for (int iCud = 0; iCud < goldenCuds.length; iCud++) {
      CompilationUnitDeclaration cud = goldenCuds[iCud];
      CompilationResult result = cud.compilationResult();
      if (result.hasErrors()) {
        compilationFailed = true;
        TreeLogger branch = logger.branch(TreeLogger.TRACE, "Errors in "
            + String.valueOf(result.getFileName()), null);
        IProblem[] errors = result.getErrors();
        for (int i = 0; i < errors.length; i++) {
          IProblem problem = errors[i];
          if (problemSet.contains(problem)) {
            continue;
          }

          problemSet.add(problem);

          // Strip the initial code from each error.
          //
          String msg = problem.toString();
          msg = msg.substring(msg.indexOf(' '));

          // Append 'file (line,pos): msg' to the error message.
          //
          int line = problem.getSourceLineNumber();
          // int sourceStart = problem.getSourceStart();
          // int lineStart = (line > 1) ? result.lineSeparatorPositions[line -
          // 2] : 0;
          // int charPos = sourceStart - lineStart;
          StringBuffer msgBuf = new StringBuffer();
          msgBuf.append("Line ");
          msgBuf.append(line);
          // msgBuf.append(" (pos ");
          // msgBuf.append(charPos);
          msgBuf.append(": ");
          msgBuf.append(msg);
          branch.log(problem.isError() ? TreeLogger.ERROR : TreeLogger.TRACE,
              msgBuf.toString(), null);
        }
      }
    }
    return compilationFailed;
  }
}
