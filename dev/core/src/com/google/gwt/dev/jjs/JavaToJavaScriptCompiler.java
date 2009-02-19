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
package com.google.gwt.dev.jjs;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.impl.StandardCompilationAnalysis;
import com.google.gwt.core.ext.soyc.Range;
import com.google.gwt.dev.PermutationResult;
import com.google.gwt.dev.jdt.RebindPermutationOracle;
import com.google.gwt.dev.jdt.WebModeCompilerFrontEnd;
import com.google.gwt.dev.jjs.InternalCompilerException.NodeInfo;
import com.google.gwt.dev.jjs.UnifiedAst.AST;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JGwtCreate;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReboundEntryPoint;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JStatement;
import com.google.gwt.dev.jjs.impl.ArrayNormalizer;
import com.google.gwt.dev.jjs.impl.AssertionNormalizer;
import com.google.gwt.dev.jjs.impl.AssertionRemover;
import com.google.gwt.dev.jjs.impl.BuildTypeMap;
import com.google.gwt.dev.jjs.impl.CastNormalizer;
import com.google.gwt.dev.jjs.impl.CatchBlockNormalizer;
import com.google.gwt.dev.jjs.impl.CodeSplitter;
import com.google.gwt.dev.jjs.impl.DeadCodeElimination;
import com.google.gwt.dev.jjs.impl.EqualityNormalizer;
import com.google.gwt.dev.jjs.impl.Finalizer;
import com.google.gwt.dev.jjs.impl.FixAssignmentToUnbox;
import com.google.gwt.dev.jjs.impl.FragmentLoaderCreator;
import com.google.gwt.dev.jjs.impl.GenerateJavaAST;
import com.google.gwt.dev.jjs.impl.GenerateJavaScriptAST;
import com.google.gwt.dev.jjs.impl.JavaScriptObjectNormalizer;
import com.google.gwt.dev.jjs.impl.JavaToJavaScriptMap;
import com.google.gwt.dev.jjs.impl.JsoDevirtualizer;
import com.google.gwt.dev.jjs.impl.LongCastNormalizer;
import com.google.gwt.dev.jjs.impl.LongEmulationNormalizer;
import com.google.gwt.dev.jjs.impl.MakeCallsStatic;
import com.google.gwt.dev.jjs.impl.MethodCallTightener;
import com.google.gwt.dev.jjs.impl.MethodInliner;
import com.google.gwt.dev.jjs.impl.PostOptimizationCompoundAssignmentNormalizer;
import com.google.gwt.dev.jjs.impl.Pruner;
import com.google.gwt.dev.jjs.impl.RecordRebinds;
import com.google.gwt.dev.jjs.impl.ReplaceRebinds;
import com.google.gwt.dev.jjs.impl.ReplaceRunAsyncs;
import com.google.gwt.dev.jjs.impl.ResolveRebinds;
import com.google.gwt.dev.jjs.impl.TypeMap;
import com.google.gwt.dev.jjs.impl.TypeTightener;
import com.google.gwt.dev.js.EvalFunctionsAtTopScope;
import com.google.gwt.dev.js.JsIEBlockSizeVisitor;
import com.google.gwt.dev.js.JsInliner;
import com.google.gwt.dev.js.JsNormalizer;
import com.google.gwt.dev.js.JsObfuscateNamer;
import com.google.gwt.dev.js.JsPrettyNamer;
import com.google.gwt.dev.js.JsReportGenerationVisitor;
import com.google.gwt.dev.js.JsSourceGenerationVisitor;
import com.google.gwt.dev.js.JsStaticEval;
import com.google.gwt.dev.js.JsStringInterner;
import com.google.gwt.dev.js.JsSymbolResolver;
import com.google.gwt.dev.js.JsUnusedFunctionRemover;
import com.google.gwt.dev.js.JsVerboseNamer;
import com.google.gwt.dev.js.JsReportGenerationVisitor.CountingTextOutput;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.dev.util.PerfLogger;
import com.google.gwt.dev.util.Util;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Compiles the Java <code>JProgram</code> representation into its corresponding
 * JavaScript source.
 */
public class JavaToJavaScriptCompiler {

  private static class PermutationResultImpl implements PermutationResult {
    private final ArtifactSet artifacts = new ArtifactSet();
    private final String[] js;

    public PermutationResultImpl(String[] js) {
      this.js = js;
    }

    public ArtifactSet getArtifacts() {
      return artifacts;
    }

    public String[] getJs() {
      return js;
    }
  }

  /**
   * Compiles a particular permutation, based on a precompiled unified AST.
   * 
   * @param logger the logger to use
   * @param unifiedAst the result of a
   *          {@link #precompile(TreeLogger, WebModeCompilerFrontEnd, String[], JJSOptions, boolean)}
   * @param rebindAnswers the set of rebind answers to resolve all outstanding
   *          rebind decisions
   * @return the output JavaScript
   * @throws UnableToCompleteException if an error other than
   *           {@link OutOfMemoryError} occurs
   */
  public static PermutationResult compilePermutation(TreeLogger logger,
      UnifiedAst unifiedAst, Map<String, String> rebindAnswers)
      throws UnableToCompleteException {
    try {
      if (JProgram.isTracingEnabled()) {
        System.out.println("------------------------------------------------------------");
        System.out.println("|                     (new permuation)                     |");
        System.out.println("------------------------------------------------------------");
      }

      AST ast = unifiedAst.getFreshAst();
      JProgram jprogram = ast.getJProgram();
      JsProgram jsProgram = ast.getJsProgram();
      JJSOptions options = unifiedAst.getOptions();

      ResolveRebinds.exec(jprogram, rebindAnswers);

      // (4) Optimize the normalized Java AST for each permutation.
      optimize(options, jprogram);

      // (5) "Normalize" the high-level Java tree into a lower-level tree more
      // suited for JavaScript code generation. Don't go reordering these
      // willy-nilly because there are some subtle interdependencies.
      LongCastNormalizer.exec(jprogram);
      JsoDevirtualizer.exec(jprogram);
      CatchBlockNormalizer.exec(jprogram);
      PostOptimizationCompoundAssignmentNormalizer.exec(jprogram);
      LongEmulationNormalizer.exec(jprogram);
      CastNormalizer.exec(jprogram);
      ArrayNormalizer.exec(jprogram);
      EqualityNormalizer.exec(jprogram);

      // (6) Perform further post-normalization optimizations
      // Prune everything
      Pruner.exec(jprogram, false);

      // (7) Generate a JavaScript code DOM from the Java type declarations
      jprogram.typeOracle.recomputeAfterOptimizations();
      final JavaToJavaScriptMap map = GenerateJavaScriptAST.exec(jprogram,
          jsProgram, options.getOutput());

      // (8) Normalize the JS AST.
      // Fix invalid constructs created during JS AST gen.
      JsNormalizer.exec(jsProgram);
      // Resolve all unresolved JsNameRefs.
      JsSymbolResolver.exec(jsProgram);
      // Move all function definitions to a top-level scope, to reduce weirdness
      EvalFunctionsAtTopScope.exec(jsProgram);

      // (9) Optimize the JS AST.
      if (options.isAggressivelyOptimize()) {
        boolean didChange;
        do {
          if (Thread.interrupted()) {
            throw new InterruptedException();
          }

          didChange = false;
          // Remove unused functions, possible
          didChange = JsStaticEval.exec(jsProgram) || didChange;
          // Inline JavaScript function invocations
          didChange = JsInliner.exec(jsProgram) || didChange;
          // Remove unused functions, possible
          didChange = JsUnusedFunctionRemover.exec(jsProgram) || didChange;
        } while (didChange);
      }

      // (10) Obfuscate
      final Map<JsName, String> stringLiteralMap;
      switch (options.getOutput()) {
        case OBFUSCATED:
          stringLiteralMap = JsStringInterner.exec(jsProgram);
          JsObfuscateNamer.exec(jsProgram);
          break;
        case PRETTY:
          // We don't intern strings in pretty mode to improve readability
          stringLiteralMap = new HashMap<JsName, String>();
          JsPrettyNamer.exec(jsProgram);
          break;
        case DETAILED:
          stringLiteralMap = JsStringInterner.exec(jsProgram);
          JsVerboseNamer.exec(jsProgram);
          break;
        default:
          throw new InternalCompilerException("Unknown output mode");
      }

      JavaToJavaScriptMap postStringInterningMap = addStringLiteralMap(map,
          stringLiteralMap);

      // (10.5) Split up the program into fragments
      if (options.isAggressivelyOptimize() && options.isRunAsyncEnabled()) {
        CodeSplitter.exec(logger, jprogram, jsProgram, postStringInterningMap);
      }

      // (11) Perform any post-obfuscation normalizations.

      // Work around an IE7 bug,
      // http://code.google.com/p/google-web-toolkit/issues/detail?id=1440
      JsIEBlockSizeVisitor.exec(jsProgram);

      // (12) Generate the final output text.
      String[] js = new String[jsProgram.getFragmentCount()];
      List<Map<Range, SourceInfo>> sourceInfoMaps = options.isSoycEnabled()
          ? new ArrayList<Map<Range, SourceInfo>>(jsProgram.getFragmentCount())
          : null;

      for (int i = 0; i < js.length; i++) {
        if (sourceInfoMaps != null) {
          CountingTextOutput out = new CountingTextOutput(
              options.getOutput().shouldMinimize());
          JsReportGenerationVisitor v = new JsReportGenerationVisitor(out);
          v.accept(jsProgram.getFragmentBlock(i));
          js[i] = out.toString();
          sourceInfoMaps.add(v.getSourceInfoMap());
        } else {
          DefaultTextOutput out = new DefaultTextOutput(
              options.getOutput().shouldMinimize());
          JsSourceGenerationVisitor v = new JsSourceGenerationVisitor(out);
          v.accept(jsProgram.getFragmentBlock(i));
          js[i] = out.toString();
        }
      }

      PermutationResult toReturn = new PermutationResultImpl(js);
      if (sourceInfoMaps != null) {
        toReturn.getArtifacts().add(
            new StandardCompilationAnalysis(logger, sourceInfoMaps,
                jprogram.getSplitPointMap()));
      }

      return toReturn;
    } catch (Throwable e) {
      throw logAndTranslateException(logger, e);
    }
  }

  /**
   * Performs a precompilation, returning a unified AST.
   * 
   * @param logger the logger to use
   * @param compilerFrontEnd the compiler front ent
   * @param declEntryPts the set of entry classes
   * @param options the compiler options
   * @param singlePermutation if true, do not pre-optimize the resulting AST or
   *          allow serialization of the result
   * @return the unified AST used to drive permutation compiles
   * @throws UnableToCompleteException if an error other than
   *           {@link OutOfMemoryError} occurs
   */
  public static UnifiedAst precompile(TreeLogger logger,
      WebModeCompilerFrontEnd compilerFrontEnd, String[] declEntryPts,
      JJSOptions options, boolean singlePermutation)
      throws UnableToCompleteException {

    if (declEntryPts.length == 0) {
      throw new IllegalArgumentException("entry point(s) required");
    }

    RebindPermutationOracle rpo = compilerFrontEnd.getRebindPermutationOracle();

    // Find all the possible rebound entry points.
    Set<String> allEntryPoints = new TreeSet<String>();
    for (String element : declEntryPts) {
      String[] all = rpo.getAllPossibleRebindAnswers(logger, element);
      Util.addAll(allEntryPoints, all);
    }
    allEntryPoints.addAll(JProgram.CODEGEN_TYPES_SET);
    allEntryPoints.addAll(JProgram.INDEX_TYPES_SET);
    allEntryPoints.add(FragmentLoaderCreator.ASYNC_FRAGMENT_LOADER);

    // Compile the source and get the compiler so we can get the parse tree
    //
    CompilationUnitDeclaration[] goldenCuds = compilerFrontEnd.getCompilationUnitDeclarations(
        logger, allEntryPoints.toArray(new String[0]));

    // Check for compilation problems. We don't log here because any problems
    // found here will have already been logged by AbstractCompiler.
    //
    checkForErrors(logger, goldenCuds, false);

    PerfLogger.start("Build AST");
    JProgram jprogram = new JProgram(options.isSoycEnabled());
    JsProgram jsProgram = new JsProgram(options.isSoycEnabled());

    try {
      /*
       * (1) Build a flattened map of TypeDeclarations => JType. The resulting
       * map contains entries for all reference types. BuildTypeMap also parses
       * all JSNI.
       */
      TypeMap typeMap = new TypeMap(jprogram);
      TypeDeclaration[] allTypeDeclarations = BuildTypeMap.exec(typeMap,
          goldenCuds, jsProgram);

      // BuildTypeMap can uncover syntactic JSNI errors; report & abort
      checkForErrors(logger, goldenCuds, true);

      // Compute all super type/sub type info
      jprogram.typeOracle.computeBeforeAST();

      // (2) Create our own Java AST from the JDT AST.
      GenerateJavaAST.exec(allTypeDeclarations, typeMap, jprogram, jsProgram,
          options);

      // GenerateJavaAST can uncover semantic JSNI errors; report & abort
      checkForErrors(logger, goldenCuds, true);

      // Allow GC
      goldenCuds = null;
      typeMap = null;
      allTypeDeclarations = null;

      // (3) Perform Java AST normalizations.

      FixAssignmentToUnbox.exec(jprogram);

      /*
       * TODO: If we defer this until later, we could maybe use the results of
       * the assertions to enable more optimizations.
       */
      if (options.isEnableAssertions()) {
        // Turn into assertion checking calls.
        AssertionNormalizer.exec(jprogram);
      } else {
        // Remove all assert statements.
        AssertionRemover.exec(jprogram);
      }

      // Replace GWT.create calls with JGwtCreate nodes.
      ReplaceRebinds.exec(logger, jprogram, rpo);

      // Fix up GWT.runAsync()
      if (options.isAggressivelyOptimize() && options.isRunAsyncEnabled()) {
        ReplaceRunAsyncs.exec(logger, jprogram);
      }

      // Resolve entry points, rebinding non-static entry points.
      findEntryPoints(logger, rpo, declEntryPts, jprogram);

      // Replace references to JSO subtypes with JSO itself.
      JavaScriptObjectNormalizer.exec(jprogram);

      /*
       * (4) Optimize the normalized Java AST for the common AST. By doing
       * optimizations early in the multiple permutation scenario, we're saving
       * work. However, we can't fully optimize because we don't yet know the
       * deferred binding decisions.
       * 
       * Don't bother optimizing early if there's only one permutation.
       */
      if (!singlePermutation) {
        optimize(options, jprogram);
      }

      Set<String> rebindRequests = new HashSet<String>();
      RecordRebinds.exec(jprogram, rebindRequests);

      return new UnifiedAst(options, new AST(jprogram, jsProgram),
          singlePermutation, rebindRequests);
    } catch (Throwable e) {
      throw logAndTranslateException(logger, e);
    } finally {
      PerfLogger.end();
    }
  }

  protected static void optimize(JJSOptions options, JProgram jprogram)
      throws InterruptedException {
    /*
     * Record the beginning of optimizations; this turns on certain checks that
     * guard against problematic late construction of things like class
     * literals.
     */
    jprogram.beginOptimizations();

    boolean didChange;
    do {
      if (Thread.interrupted()) {
        throw new InterruptedException();
      }

      // Recompute clinits each time, they can become empty.
      jprogram.typeOracle.recomputeAfterOptimizations();
      didChange = false;

      // Remove unreferenced types, fields, methods, [params, locals]
      didChange = Pruner.exec(jprogram, true) || didChange;
      // finalize locals, params, fields, methods, classes
      didChange = Finalizer.exec(jprogram) || didChange;
      // rewrite non-polymorphic calls as static calls; update all call sites
      didChange = MakeCallsStatic.exec(jprogram) || didChange;

      // type flow tightening
      // - fields, locals based on assignment
      // - params based on assignment and call sites
      // - method bodies based on return statements
      // - polymorphic methods based on return types of all implementors
      // - optimize casts and instance of
      didChange = TypeTightener.exec(jprogram) || didChange;

      // tighten method call bindings
      didChange = MethodCallTightener.exec(jprogram) || didChange;

      // dead code removal??
      didChange = DeadCodeElimination.exec(jprogram) || didChange;

      if (options.isAggressivelyOptimize()) {
        // inlining
        didChange = MethodInliner.exec(jprogram) || didChange;
      }
      // prove that any types that have been culled from the main tree are
      // unreferenced due to type tightening?
    } while (didChange && !options.isDraftCompile());

    if (options.isDraftCompile()) {
      /*
       * Ensure that references to dead clinits are removed. Otherwise, the
       * application won't run reliably.
       */
      jprogram.typeOracle.recomputeAfterOptimizations();
      DeadCodeElimination.exec(jprogram);
    }
  }

  private static JavaToJavaScriptMap addStringLiteralMap(
      final JavaToJavaScriptMap map, final Map<JsName, String> stringLiteralMap) {
    JavaToJavaScriptMap postStringInterningMap = new JavaToJavaScriptMap() {
      public JsName nameForMethod(JMethod method) {
        return map.nameForMethod(method);
      }

      public JsName nameForType(JReferenceType type) {
        return map.nameForType(type);
      }

      public JField nameToField(JsName name) {
        return map.nameToField(name);
      }

      public JMethod nameToMethod(JsName name) {
        return map.nameToMethod(name);
      }

      public String stringLiteralForName(JsName name) {
        return stringLiteralMap.get(name);
      }

      public JReferenceType typeForStatement(JsStatement stat) {
        return map.typeForStatement(stat);
      }

      public JMethod vtableInitToMethod(JsStatement stat) {
        return map.vtableInitToMethod(stat);
      }
    };
    return postStringInterningMap;
  }

  private static void checkForErrors(TreeLogger logger,
      CompilationUnitDeclaration[] cuds, boolean itemizeErrors)
      throws UnableToCompleteException {
    boolean compilationFailed = false;
    if (cuds.length == 0) {
      compilationFailed = true;
    }
    Set<IProblem> problemSet = new HashSet<IProblem>();
    for (CompilationUnitDeclaration cud : cuds) {
      CompilationResult result = cud.compilationResult();
      if (result.hasErrors()) {
        compilationFailed = true;
        // Early out if we don't need to itemize.
        if (!itemizeErrors) {
          break;
        }
        TreeLogger branch = logger.branch(TreeLogger.ERROR, "Errors in "
            + String.valueOf(result.getFileName()), null);
        IProblem[] errors = result.getErrors();
        for (IProblem problem : errors) {
          if (problemSet.contains(problem)) {
            continue;
          }

          problemSet.add(problem);

          // Strip the initial code from each error.
          //
          String msg = problem.toString();
          msg = msg.substring(msg.indexOf(' '));

          // Append 'file (line): msg' to the error message.
          //
          int line = problem.getSourceLineNumber();
          StringBuffer msgBuf = new StringBuffer();
          msgBuf.append("Line ");
          msgBuf.append(line);
          msgBuf.append(": ");
          msgBuf.append(msg);
          branch.log(TreeLogger.ERROR, msgBuf.toString(), null);
        }
      }
    }
    if (compilationFailed) {
      logger.log(TreeLogger.ERROR, "Cannot proceed due to previous errors",
          null);
      throw new UnableToCompleteException();
    }
  }

  private static JMethodCall createReboundModuleLoad(TreeLogger logger,
      JProgram program, JReferenceType reboundEntryType,
      String originalMainClassName) throws UnableToCompleteException {
    if (!(reboundEntryType instanceof JClassType)) {
      logger.log(TreeLogger.ERROR, "Module entry point class '"
          + originalMainClassName + "' must be a class", null);
      throw new UnableToCompleteException();
    }

    JClassType entryClass = (JClassType) reboundEntryType;
    if (entryClass.isAbstract()) {
      logger.log(TreeLogger.ERROR, "Module entry point class '"
          + originalMainClassName + "' must not be abstract", null);
      throw new UnableToCompleteException();
    }

    JMethod entryMethod = findMainMethodRecurse(reboundEntryType);
    if (entryMethod == null) {
      logger.log(TreeLogger.ERROR,
          "Could not find entry method 'onModuleLoad()' method in entry point class '"
              + originalMainClassName + "'", null);
      throw new UnableToCompleteException();
    }

    if (entryMethod.isAbstract()) {
      logger.log(TreeLogger.ERROR,
          "Entry method 'onModuleLoad' in entry point class '"
              + originalMainClassName + "' must not be abstract", null);
      throw new UnableToCompleteException();
    }
    SourceInfo sourceInfo = reboundEntryType.getSourceInfo().makeChild(
        JavaToJavaScriptCompiler.class, "Rebound entry point");

    JExpression qualifier = null;
    if (!entryMethod.isStatic()) {
      qualifier = JGwtCreate.createInstantiationExpression(program, sourceInfo,
          entryClass);

      if (qualifier == null) {
        logger.log(
            TreeLogger.ERROR,
            "No default (zero argument) constructor could be found in entry point class '"
                + originalMainClassName
                + "' to qualify a call to non-static entry method 'onModuleLoad'",
            null);
        throw new UnableToCompleteException();
      }
    }
    return new JMethodCall(program, sourceInfo, qualifier, entryMethod);
  }

  private static void findEntryPoints(TreeLogger logger,
      RebindPermutationOracle rpo, String[] mainClassNames, JProgram program)
      throws UnableToCompleteException {
    SourceInfo sourceInfo = program.createSourceInfoSynthetic(
        JavaToJavaScriptCompiler.class, "Bootstrap method");
    JMethod bootStrapMethod = program.createMethod(sourceInfo,
        "init".toCharArray(), program.getIndexedType("EntryMethodHolder"),
        program.getTypeVoid(), false, true, true, false, false);
    bootStrapMethod.freezeParamTypes();

    JMethodBody body = (JMethodBody) bootStrapMethod.getBody();
    List<JStatement> statements = body.getStatements();
    for (String mainClassName : mainClassNames) {
      statements.add(makeStatsCalls(program, mainClassName));
      JReferenceType mainType = program.getFromTypeMap(mainClassName);

      if (mainType == null) {
        logger.log(TreeLogger.ERROR,
            "Could not find module entry point class '" + mainClassName + "'",
            null);
        throw new UnableToCompleteException();
      }

      JMethod mainMethod = findMainMethod(mainType);
      if (mainMethod != null && mainMethod.isStatic()) {
        JMethodCall onModuleLoadCall = new JMethodCall(program, null, null,
            mainMethod);
        statements.add(onModuleLoadCall.makeStatement());
        continue;
      }

      // Couldn't find a static main method; must rebind the class
      String[] resultTypeNames = rpo.getAllPossibleRebindAnswers(logger,
          mainClassName);
      List<JClassType> resultTypes = new ArrayList<JClassType>();
      List<JExpression> entryCalls = new ArrayList<JExpression>();
      for (String resultTypeName : resultTypeNames) {
        JReferenceType resultType = program.getFromTypeMap(resultTypeName);
        if (resultType == null) {
          logger.log(TreeLogger.ERROR,
              "Could not find module entry point class '" + resultTypeName
                  + "' after rebinding from '" + mainClassName + "'", null);
          throw new UnableToCompleteException();
        }

        JMethodCall onModuleLoadCall = createReboundModuleLoad(logger, program,
            resultType, mainClassName);
        resultTypes.add((JClassType) resultType);
        entryCalls.add(onModuleLoadCall);
      }
      if (resultTypes.size() == 1) {
        statements.add(entryCalls.get(0).makeStatement());
      } else {
        JReboundEntryPoint reboundEntryPoint = new JReboundEntryPoint(program,
            null, mainType, resultTypes, entryCalls);
        statements.add(reboundEntryPoint);
      }
    }
    program.addEntryMethod(bootStrapMethod);
  }

  private static JMethod findMainMethod(JReferenceType referenceType) {
    for (int j = 0; j < referenceType.methods.size(); ++j) {
      JMethod method = referenceType.methods.get(j);
      if (method.getName().equals("onModuleLoad")) {
        if (method.params.size() == 0) {
          return method;
        }
      }
    }
    return null;
  }

  private static JMethod findMainMethodRecurse(JReferenceType referenceType) {
    for (JReferenceType it = referenceType; it != null; it = it.extnds) {
      JMethod result = findMainMethod(it);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  private static UnableToCompleteException logAndTranslateException(
      TreeLogger logger, Throwable e) {
    if (e instanceof UnableToCompleteException) {
      // just rethrow
      return (UnableToCompleteException) e;
    } else if (e instanceof InternalCompilerException) {
      TreeLogger topBranch = logger.branch(TreeLogger.ERROR,
          "An internal compiler exception occurred", e);
      List<NodeInfo> nodeTrace = ((InternalCompilerException) e).getNodeTrace();
      for (NodeInfo nodeInfo : nodeTrace) {
        SourceInfo info = nodeInfo.getSourceInfo();
        String msg;
        if (info != null) {
          String fileName = info.getFileName();
          fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
          fileName = fileName.substring(fileName.lastIndexOf('\\') + 1);
          msg = "at " + fileName + "(" + info.getStartLine() + "): ";
        } else {
          msg = "<no source info>: ";
        }

        String description = nodeInfo.getDescription();
        if (description != null) {
          msg += description;
        } else {
          msg += "<no description available>";
        }
        TreeLogger nodeBranch = topBranch.branch(TreeLogger.ERROR, msg, null);
        String className = nodeInfo.getClassName();
        if (className != null) {
          nodeBranch.log(TreeLogger.INFO, className, null);
        }
      }
      return new UnableToCompleteException();
    } else if (e instanceof OutOfMemoryError) {
      // Rethrow the original exception so the caller can deal with it.
      throw (OutOfMemoryError) e;
    } else {
      logger.log(TreeLogger.ERROR, "Unexpected internal compiler error", e);
      return new UnableToCompleteException();
    }
  }

  /**
   * Create a variable assignment to invoke a call to the statistics collector.
   * 
   * <pre>
   * Stats.isStatsAvailable() &&
   *   Stats.onModuleStart("mainClassName");
   * </pre>
   */
  private static JStatement makeStatsCalls(JProgram program,
      String mainClassName) {
    SourceInfo sourceInfo = program.createSourceInfoSynthetic(
        JavaToJavaScriptCompiler.class, "onModuleStart() stats call");
    JMethod isStatsAvailableMethod = program.getIndexedMethod("Stats.isStatsAvailable");
    JMethod onModuleStartMethod = program.getIndexedMethod("Stats.onModuleStart");

    JMethodCall availableCall = new JMethodCall(program, sourceInfo, null,
        isStatsAvailableMethod);
    JMethodCall onModuleStartCall = new JMethodCall(program, sourceInfo, null,
        onModuleStartMethod);
    onModuleStartCall.getArgs().add(
        program.getLiteralString(sourceInfo, mainClassName));

    JBinaryOperation amp = new JBinaryOperation(program, sourceInfo,
        program.getTypePrimitiveBoolean(), JBinaryOperator.AND, availableCall,
        onModuleStartCall);

    return amp.makeStatement();
  }

}
