/*
 * Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.jjs;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.Artifact;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.CompilationMetricsArtifact;
import com.google.gwt.core.ext.linker.EmittedArtifact;
import com.google.gwt.core.ext.linker.EmittedArtifact.Visibility;
import com.google.gwt.core.ext.linker.ModuleMetricsArtifact;
import com.google.gwt.core.ext.linker.PrecompilationMetricsArtifact;
import com.google.gwt.core.ext.linker.StatementRanges;
import com.google.gwt.core.ext.linker.SymbolData;
import com.google.gwt.core.ext.linker.SyntheticArtifact;
import com.google.gwt.core.ext.linker.impl.StandardSymbolData;
import com.google.gwt.core.ext.soyc.SourceMapRecorder;
import com.google.gwt.core.ext.soyc.coderef.DependencyGraphRecorder;
import com.google.gwt.core.ext.soyc.coderef.EntityRecorder;
import com.google.gwt.core.ext.soyc.impl.DependencyRecorder;
import com.google.gwt.core.ext.soyc.impl.SizeMapRecorder;
import com.google.gwt.core.ext.soyc.impl.SplitPointRecorder;
import com.google.gwt.core.ext.soyc.impl.StoryRecorder;
import com.google.gwt.core.linker.SoycReportLinker;
import com.google.gwt.dev.CompilerContext;
import com.google.gwt.dev.MinimalRebuildCache;
import com.google.gwt.dev.Permutation;
import com.google.gwt.dev.PrecompileTaskOptions;
import com.google.gwt.dev.cfg.ConfigurationProperties;
import com.google.gwt.dev.cfg.EntryMethodHolderGenerator;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.PermutationProperties;
import com.google.gwt.dev.javac.CompilationProblemReporter;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.StandardGeneratorContext;
import com.google.gwt.dev.javac.typemodel.TypeOracle;
import com.google.gwt.dev.jdt.RebindPermutationOracle;
import com.google.gwt.dev.jjs.UnifiedAst.AST;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JBlock;
import com.google.gwt.dev.jjs.ast.JCastOperation;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JTypeOracle.StandardTypes;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.impl.ArrayNormalizer;
import com.google.gwt.dev.jjs.impl.AssertionNormalizer;
import com.google.gwt.dev.jjs.impl.AssertionRemover;
import com.google.gwt.dev.jjs.impl.AstDumper;
import com.google.gwt.dev.jjs.impl.CatchBlockNormalizer;
import com.google.gwt.dev.jjs.impl.CompileTimeConstantsReplacer;
import com.google.gwt.dev.jjs.impl.ComputeCastabilityInformation;
import com.google.gwt.dev.jjs.impl.ComputeExhaustiveCastabilityInformation;
import com.google.gwt.dev.jjs.impl.ControlFlowAnalyzer;
import com.google.gwt.dev.jjs.impl.ControlFlowRecorder;
import com.google.gwt.dev.jjs.impl.DeadCodeElimination;
import com.google.gwt.dev.jjs.impl.Devirtualizer;
import com.google.gwt.dev.jjs.impl.EnumNameObfuscator;
import com.google.gwt.dev.jjs.impl.EnumOrdinalizer;
import com.google.gwt.dev.jjs.impl.EqualityNormalizer;
import com.google.gwt.dev.jjs.impl.Finalizer;
import com.google.gwt.dev.jjs.impl.FixAssignmentsToUnboxOrCast;
import com.google.gwt.dev.jjs.impl.FullOptimizerContext;
import com.google.gwt.dev.jjs.impl.GenerateJavaScriptAST;
import com.google.gwt.dev.jjs.impl.HandleCrossFragmentReferences;
import com.google.gwt.dev.jjs.impl.ImplementCastsAndTypeChecks;
import com.google.gwt.dev.jjs.impl.ImplementClassLiteralsAsFields;
import com.google.gwt.dev.jjs.impl.ImplementJsVarargs;
import com.google.gwt.dev.jjs.impl.JavaAstVerifier;
import com.google.gwt.dev.jjs.impl.JavaToJavaScriptMap;
import com.google.gwt.dev.jjs.impl.JjsUtils;
import com.google.gwt.dev.jjs.impl.JsAbstractTextTransformer;
import com.google.gwt.dev.jjs.impl.JsFunctionClusterer;
import com.google.gwt.dev.jjs.impl.JsInteropRestrictionChecker;
import com.google.gwt.dev.jjs.impl.JsNoopTransformer;
import com.google.gwt.dev.jjs.impl.JsTypeLinker;
import com.google.gwt.dev.jjs.impl.JsniRestrictionChecker;
import com.google.gwt.dev.jjs.impl.LongCastNormalizer;
import com.google.gwt.dev.jjs.impl.LongEmulationNormalizer;
import com.google.gwt.dev.jjs.impl.MakeCallsStatic;
import com.google.gwt.dev.jjs.impl.MethodCallSpecializer;
import com.google.gwt.dev.jjs.impl.MethodCallTightener;
import com.google.gwt.dev.jjs.impl.MethodInliner;
import com.google.gwt.dev.jjs.impl.OptimizerContext;
import com.google.gwt.dev.jjs.impl.OptimizerStats;
import com.google.gwt.dev.jjs.impl.PostOptimizationCompoundAssignmentNormalizer;
import com.google.gwt.dev.jjs.impl.Pruner;
import com.google.gwt.dev.jjs.impl.RecordRebinds;
import com.google.gwt.dev.jjs.impl.RemoveEmptySuperCalls;
import com.google.gwt.dev.jjs.impl.RemoveSpecializations;
import com.google.gwt.dev.jjs.impl.ReplaceDefenderMethodReferences;
import com.google.gwt.dev.jjs.impl.ReplaceGetClassOverrides;
import com.google.gwt.dev.jjs.impl.ResolvePermutationDependentValues;
import com.google.gwt.dev.jjs.impl.ResolveRuntimeTypeReferences;
import com.google.gwt.dev.jjs.impl.ResolveRuntimeTypeReferences.ClosureUniqueIdTypeMapper;
import com.google.gwt.dev.jjs.impl.ResolveRuntimeTypeReferences.IntTypeMapper;
import com.google.gwt.dev.jjs.impl.ResolveRuntimeTypeReferences.StringTypeMapper;
import com.google.gwt.dev.jjs.impl.ResolveRuntimeTypeReferences.TypeMapper;
import com.google.gwt.dev.jjs.impl.ResolveRuntimeTypeReferences.TypeOrder;
import com.google.gwt.dev.jjs.impl.RewriteConstructorCallsForUnboxedTypes;
import com.google.gwt.dev.jjs.impl.SameParameterValueOptimizer;
import com.google.gwt.dev.jjs.impl.SourceInfoCorrelator;
import com.google.gwt.dev.jjs.impl.TypeCoercionNormalizer;
import com.google.gwt.dev.jjs.impl.TypeReferencesRecorder;
import com.google.gwt.dev.jjs.impl.TypeTightener;
import com.google.gwt.dev.jjs.impl.UnifyAst;
import com.google.gwt.dev.jjs.impl.codesplitter.CodeSplitter;
import com.google.gwt.dev.jjs.impl.codesplitter.CodeSplitters;
import com.google.gwt.dev.jjs.impl.codesplitter.MultipleDependencyGraphRecorder;
import com.google.gwt.dev.jjs.impl.codesplitter.ReplaceRunAsyncs;
import com.google.gwt.dev.jjs.impl.gflow.DataflowOptimizer;
import com.google.gwt.dev.js.BaselineCoverageGatherer;
import com.google.gwt.dev.js.ClosureJsRunner;
import com.google.gwt.dev.js.CoverageInstrumentor;
import com.google.gwt.dev.js.DuplicateClinitRemover;
import com.google.gwt.dev.js.EvalFunctionsAtTopScope;
import com.google.gwt.dev.js.FreshNameGenerator;
import com.google.gwt.dev.js.JsBreakUpLargeVarStatements;
import com.google.gwt.dev.js.JsDuplicateCaseFolder;
import com.google.gwt.dev.js.JsDuplicateFunctionRemover;
import com.google.gwt.dev.js.JsForceInliningChecker;
import com.google.gwt.dev.js.JsIncrementalNamer;
import com.google.gwt.dev.js.JsInliner;
import com.google.gwt.dev.js.JsLiteralInterner;
import com.google.gwt.dev.js.JsNamer.IllegalNameException;
import com.google.gwt.dev.js.JsNamespaceChooser;
import com.google.gwt.dev.js.JsNamespaceOption;
import com.google.gwt.dev.js.JsNormalizer;
import com.google.gwt.dev.js.JsObfuscateNamer;
import com.google.gwt.dev.js.JsPrettyNamer;
import com.google.gwt.dev.js.JsReportGenerationVisitor;
import com.google.gwt.dev.js.JsStackEmulator;
import com.google.gwt.dev.js.JsStaticEval;
import com.google.gwt.dev.js.JsSymbolResolver;
import com.google.gwt.dev.js.JsUnusedFunctionRemover;
import com.google.gwt.dev.js.JsVerboseNamer;
import com.google.gwt.dev.js.SizeBreakdown;
import com.google.gwt.dev.js.ast.JavaScriptVerifier;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsForIn;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsLabel;
import com.google.gwt.dev.js.ast.JsLiteral;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsNameOf;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsNode;
import com.google.gwt.dev.js.ast.JsParameter;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsVars;
import com.google.gwt.dev.js.ast.JsVisitor;
import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.dev.util.Memory;
import com.google.gwt.dev.util.Name.SourceName;
import com.google.gwt.dev.util.Pair;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.arg.OptionJsInteropMode.Mode;
import com.google.gwt.dev.util.arg.OptionOptimize;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;
import com.google.gwt.soyc.SoycDashboard;
import com.google.gwt.soyc.io.ArtifactsOutputDirectory;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableMap;
import com.google.gwt.thirdparty.guava.common.collect.Iterables;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import org.xml.sax.SAXException;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.ParserConfigurationException;

/**
 * A base for classes that compile Java <code>JProgram</code> representations into corresponding Js
 * source.<br />
 *
 * Work is split between a precompile() stage which is only called once and compilePerms() stage
 * which is called once per permutation. This allow build systems the option of distributing and
 * parallelizing some of the work.
 */
public final class JavaToJavaScriptCompiler {

  /**
   * Ending optimization passes when the rate of change has reached this value results in gaining
   * nearly all of the impact while avoiding the long tail of costly but low-impact passes.
   */
  private static final float EFFICIENT_CHANGE_RATE = 0.01f;
  /**
   * Continuing to apply optimizations till the rate of change reaches this value causes the AST to
   * reach a fixed point.
   */
  private static final int FIXED_POINT_CHANGE_RATE = 0;
  /**
   * Limits the number of optimization passes against the possible danger of an AST that does not
   * converge.
   */
  private static final int MAX_PASSES = 100;

  static {
    // Preload the internal compiler exception just in case we run out of memory?.
    InternalCompilerException.preload();
  }

  private final CompilerContext compilerContext;
  private final TreeLogger logger;
  private final ModuleDef module;
  private final PrecompileTaskOptions options;
  private JsProgram jsProgram;
  private JProgram jprogram;

  public JavaToJavaScriptCompiler(TreeLogger logger, CompilerContext compilerContext) {
    this.logger = logger;
    this.compilerContext = compilerContext;
    this.module = compilerContext.getModule();
    this.options = compilerContext.getOptions();
  }

  public static UnifiedAst precompile(TreeLogger logger, CompilerContext compilerContext,
      PrecompilationContext precompilationContext)
      throws UnableToCompleteException {
    return new JavaToJavaScriptCompiler(logger, compilerContext).precompile(precompilationContext);
  }

  /**
   * Compiles a particular permutation.
   *
   * @param logger the logger to use
   * @param compilerContext shared read only compiler state
   * @param permutation the permutation to compile
   * @return the permutation result
   * @throws UnableToCompleteException if an error other than {@link OutOfMemoryError} occurs
   */
  public static PermutationResult compilePermutation(UnifiedAst unifiedAst,
      TreeLogger logger, CompilerContext compilerContext, Permutation permutation)
      throws UnableToCompleteException {
    JavaToJavaScriptCompiler javaToJavaScriptCompiler =
        new JavaToJavaScriptCompiler(logger, compilerContext);
    return javaToJavaScriptCompiler.compilePermutation(permutation, unifiedAst);
  }

  /**
   * Takes as input an unresolved Java AST (a Java AST wherein all rebind result classes are
   * available and have not yet been pruned down to the set applicable for a particular permutation)
   * that was previously constructed by the Precompiler and from that constructs output Js source
   * code and related information. This Js source and related information is packaged into a
   * Permutation instance and then returned.
   *
   * Permutation compilation is INTENDED to progress as a series of stages:
   *
   * <pre>
   * 1. initialize local state
   * 2. transform unresolved Java AST to resolved Java AST
   * 3. normalize the resolved Java AST
   * 4. optimize the resolved Java AST
   * 5. construct the Js AST
   * 6. normalize the Js AST
   * 7. optimize the Js AST
   * 8. generate Js source
   * 9. construct and return a value
   * </pre>
   *
   * There are some other types of work here (mostly metrics and data gathering) which do not serve
   * the goal of output program construction. This work should really be moved into subclasses or
   * some sort of callback or plugin system so as not to visually pollute the real compile logic.<br
   * />
   *
   * Significant amounts of visitors implementing the intended above stages are triggered here but
   * in the wrong order. They have been noted for future cleanup.
   */
  private PermutationResult compilePermutation(Permutation permutation, UnifiedAst unifiedAst)
      throws UnableToCompleteException {
    Event jjsCompilePermutationEvent = SpeedTracerLogger.start(
        CompilerEventType.JJS_COMPILE_PERMUTATION, "name", permutation.getProperties().prettyPrint()
    );
    /*
     * Do not introduce any new pass here unless it is logically a part of one of the 9 defined
     * stages and is physically located in that stage.
     */

    long permStartMs = System.currentTimeMillis();
    try {
      Event javaEvent = SpeedTracerLogger.start(CompilerEventType.PERMUTATION_JAVA);

      // (1) Initialize local state.
      long startTimeMs = System.currentTimeMillis();
      PermutationProperties properties = permutation.getProperties();
      int permutationId = permutation.getId();
      AST ast = unifiedAst.getFreshAst();
      jprogram = ast.getJProgram();
      jsProgram = ast.getJsProgram();
      Map<StandardSymbolData, JsName> symbolTable =
          new TreeMap<StandardSymbolData, JsName>(new SymbolData.ClassIdentComparator());

      // TODO(stalcup): hide metrics gathering in a callback or subclass
      logger.log(TreeLogger.INFO, "Compiling permutation " + permutationId + "...");

      // Rewrite calls to from boxed constructor types to specialized unboxed methods
      RewriteConstructorCallsForUnboxedTypes.exec(jprogram);

      // (2) Transform unresolved Java AST to resolved Java AST
      ResolvePermutationDependentValues
          .exec(jprogram, properties, permutation.getPropertyAndBindingInfos());

      // TODO(stalcup): hide metrics gathering in a callback or subclass
      // This has to happen before optimizations because functions might
      // be optimized out; we want those marked as "not executed", not "not
      // instrumentable".
      Multimap<String, Integer> instrumentableLines = null;
      if (CoverageInstrumentor.isCoverageEnabled()) {
        instrumentableLines = BaselineCoverageGatherer.exec(jprogram);
      }

      // Record initial set of type->type references.
      // type->type references need to be collected in two phases, 1) before any process to the
      // AST has happened (to record for example reference to types declaring compile-time
      // constants) and 2) after all normalizations to collect synthetic references (e.g. to
      // record references to runtime classes like LongLib).
      maybeRecordReferencesAndControlFlow(false);

      // Replace compile time constants by their values.
      // TODO(rluble): eventually move to normizeSemantics.
      CompileTimeConstantsReplacer.exec(jprogram);

      // TODO(stalcup): move to after normalize.
      // (3) Optimize the resolved Java AST
      optimizeJava();

      // TODO(stalcup): move to before optimize.
      // (4) Normalize the resolved Java AST
      TypeMapper<?> typeMapper = normalizeSemantics();

      // TODO(stalcup): this stage shouldn't exist, move into optimize.
      postNormalizationOptimizeJava();

      // Now that the AST has stopped mutating update with the final references.
      maybeRecordReferencesAndControlFlow(true);

      javaEvent.end();

      Event javaScriptEvent = SpeedTracerLogger.start(CompilerEventType.PERMUTATION_JAVASCRIPT);

      // (5) Construct the Js AST
      Pair<? extends JavaToJavaScriptMap, Set<JsNode>> jjsMapAndInlineableFunctions =
          GenerateJavaScriptAST.exec(logger, jprogram, jsProgram,
              compilerContext, typeMapper, symbolTable, properties);
      JavaToJavaScriptMap jjsmap = jjsMapAndInlineableFunctions.getLeft();

      // TODO(stalcup): hide metrics gathering in a callback or subclass
      if (CoverageInstrumentor.isCoverageEnabled()) {
        CoverageInstrumentor.exec(jprogram, jsProgram, jjsmap, instrumentableLines);
      }

      // (6) Normalize the Js AST
      JsNormalizer.exec(jsProgram);

      // TODO(stalcup): move to AST construction
      JsSymbolResolver.exec(jsProgram);

      if (options.getNamespace() == JsNamespaceOption.PACKAGE) {
        if (!jprogram.getRunAsyncs().isEmpty()) {
          options.setNamespace(JsNamespaceOption.NONE);
          logger.log(TreeLogger.Type.WARN,
              "Namespace option is not compatible with CodeSplitter, turning it off.");
        } else {
          JsNamespaceChooser.exec(jprogram, jsProgram, jjsmap);
        }
      }

      // TODO(stalcup): move to normalization
      Pair<SyntheticArtifact, MultipleDependencyGraphRecorder> dependenciesAndRecorder =
          splitJsIntoFragments(properties, permutationId, jjsmap);

      // TODO(stalcup): move to normalization
      EvalFunctionsAtTopScope.exec(jsProgram, jjsmap);

      // (7) Optimize the JS AST.
      final Set<JsNode> inlinableJsFunctions = jjsMapAndInlineableFunctions.getRight();
      optimizeJs(inlinableJsFunctions);
      if (options.getOptimizationLevel() > OptionOptimize.OPTIMIZE_LEVEL_DRAFT) {
        JsForceInliningChecker.check(logger, jjsmap, jsProgram);
      }

      // TODO(stalcup): move to normalization
      // Must run before code splitter and namer.
      JsStackEmulator.exec(jprogram, jsProgram, properties, jjsmap);

      // TODO(stalcup): move to optimize.
      Map<JsName, JsLiteral> internedLiteralByVariableName = renameJsSymbols(properties, jjsmap);

      // No new JsNames or references to JSNames can be introduced after this
      // point.
      HandleCrossFragmentReferences.exec(jsProgram, properties);

      // TODO(stalcup): move to normalization
      JsBreakUpLargeVarStatements.exec(jsProgram, properties.getConfigurationProperties());

      if (!options.isIncrementalCompileEnabled()) {
        // Verifies consistency between jsProgram and jjsmap if assertions are enabled.
        // TODO(rluble): make it work for incremental compiles.
        JavaScriptVerifier.verify(jsProgram, jjsmap);
      }

      // (8) Generate Js source
      List<JsSourceMap> sourceInfoMaps = new ArrayList<JsSourceMap>();
      boolean isSourceMapsEnabled = properties.isTrueInAnyPermutation("compiler.useSourceMaps");
      String[] jsFragments = new String[jsProgram.getFragmentCount()];
      StatementRanges[] ranges = new StatementRanges[jsFragments.length];
      SizeBreakdown[] sizeBreakdowns = options.isJsonSoycEnabled() || options.isSoycEnabled()
          || options.isCompilerMetricsEnabled() ? new SizeBreakdown[jsFragments.length] : null;
      generateJavaScriptCode(jjsmap, jsFragments, ranges, sizeBreakdowns, sourceInfoMaps,
          isSourceMapsEnabled || options.isJsonSoycEnabled());

      javaScriptEvent.end();

      // (9) Construct and return a value
      PermutationResult permutationResult =
          new PermutationResultImpl(jsFragments, permutation, makeSymbolMap(symbolTable), ranges);

      // TODO(stalcup): hide metrics gathering in a callback or subclass
      addSyntheticArtifacts(unifiedAst, permutation, startTimeMs, permutationId, jjsmap,
          dependenciesAndRecorder, internedLiteralByVariableName, isSourceMapsEnabled, jsFragments,
          sizeBreakdowns, sourceInfoMaps, permutationResult);
      return permutationResult;
    } catch (Throwable e) {
      throw CompilationProblemReporter.logAndTranslateException(logger, e);
    } finally {
      jjsCompilePermutationEvent.end();
      if (logger.isLoggable(TreeLogger.TRACE)) {
        logger.log(TreeLogger.TRACE,
            "Permutation took " + (System.currentTimeMillis() - permStartMs) + " ms");
      }
    }
  }

  private void maybeRecordReferencesAndControlFlow(boolean onlyUpdate) {
    if (options.isIncrementalCompileEnabled()) {
      // Per file compilation needs the type reference graph to construct the set of reachable
      // types when linking.
      TypeReferencesRecorder.exec(jprogram, getMinimalRebuildCache(), onlyUpdate);
      ControlFlowRecorder.exec(jprogram, getMinimalRebuildCache().getTypeEnvironment(),
          onlyUpdate);
    }
  }

  /**
   * Transform patterns that can't be represented in JS (such as multiple catch blocks) into
   * equivalent but compatible patterns and take JVM semantics (such as numeric casts) that are not
   * explicit in the AST and make them explicit.<br />
   *
   * These passes can not be reordering because of subtle interdependencies.
   */
  protected TypeMapper<?> normalizeSemantics() {
    Event event = SpeedTracerLogger.start(CompilerEventType.JAVA_NORMALIZERS);
    try {
      Devirtualizer.exec(jprogram);
      CatchBlockNormalizer.exec(jprogram);
      PostOptimizationCompoundAssignmentNormalizer.exec(jprogram);
      LongCastNormalizer.exec(jprogram);
      LongEmulationNormalizer.exec(jprogram);
      TypeCoercionNormalizer.exec(jprogram);

      if (options.isIncrementalCompileEnabled()) {
        // Per file compilation reuses type JS even as references (like casts) in other files
        // change, which means all legal casts need to be allowed now before they are actually
        // used later.
        ComputeExhaustiveCastabilityInformation.exec(jprogram);
      } else {
        // If trivial casts are pruned then one can use smaller runtime castmaps.
        ComputeCastabilityInformation.exec(jprogram, !shouldOptimize() /* recordTrivialCasts */);
      }

      ImplementCastsAndTypeChecks.exec(jprogram, shouldOptimize() /* pruneTrivialCasts */);
      ImplementJsVarargs.exec(jprogram);
      ArrayNormalizer.exec(jprogram);
      EqualityNormalizer.exec(jprogram);

      TypeMapper<?> typeMapper = getTypeMapper();
      ResolveRuntimeTypeReferences.exec(jprogram, typeMapper, getTypeOrder());

      return typeMapper;
    } finally {
      event.end();
    }
  }

  private void optimizeJava() throws InterruptedException {
    if (shouldOptimize()) {
      optimizeJavaToFixedPoint();
      RemoveEmptySuperCalls.exec(jprogram);
    }
  }

  private void optimizeJs(Set<JsNode> inlinableJsFunctions) throws InterruptedException {
    if (shouldOptimize()) {
      optimizeJsLoop(inlinableJsFunctions);
      JsDuplicateCaseFolder.exec(jsProgram);
    }
  }

  private void postNormalizationOptimizeJava() {
    Event event = SpeedTracerLogger.start(CompilerEventType.JAVA_POST_NORMALIZER_OPTIMIZERS);
    try {
      if (shouldOptimize()) {
        RemoveSpecializations.exec(jprogram);
        Pruner.exec(jprogram, false);
        // Last Java optimization step, update type oracle accordingly.
        jprogram.typeOracle.recomputeAfterOptimizations(jprogram.getDeclaredTypes());
      }
      ReplaceGetClassOverrides.exec(jprogram);
    } finally {
      event.end();
    }
  }

  private Map<JsName, JsLiteral> runDetailedNamer(ConfigurationProperties config)
      throws IllegalNameException {
    Map<JsName, JsLiteral> internedTextByVariableName =
        maybeInternLiterals(JsLiteralInterner.INTERN_ALL);
    JsVerboseNamer.exec(jsProgram, config);
    return internedTextByVariableName;
  }

  private Map<JsName, JsLiteral> maybeInternLiterals(int interningMask) {
    if (!shouldOptimize()) {
      return null;
    }
    // Only perform the interning optimization when optimizations are enabled.
    if (options.isClosureCompilerFormatEnabled()) {
      // Do no intern strings in closure format as it breaks goog.provides, etc.
      interningMask &= ~JsLiteralInterner.INTERN_STRINGS;
    }
    return JsLiteralInterner.exec(jprogram, jsProgram, interningMask);
  }

  private Pair<SyntheticArtifact, MultipleDependencyGraphRecorder> splitJsIntoFragments(
      PermutationProperties properties, int permutationId, JavaToJavaScriptMap jjsmap) {
    Pair<SyntheticArtifact, MultipleDependencyGraphRecorder> dependenciesAndRecorder;
    MultipleDependencyGraphRecorder dependencyRecorder = null;
    SyntheticArtifact dependencies = null;
    if (options.isRunAsyncEnabled()) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();

      int expectedFragmentCount = options.getFragmentCount();
      // -1 is the default value, we trap 0 just in case (0 is not a legal value in any case)
      if (expectedFragmentCount <= 0) {
        // Fragment count not set check fragments merge.
        int numberOfMerges = options.getFragmentsMerge();
        if (numberOfMerges > 0) {
          // + 1 for left over, + 1 for initial gave us the total number
          // of fragments without splitting.
          expectedFragmentCount =
              Math.max(0, jprogram.getRunAsyncs().size() + 2 - numberOfMerges);
        }
      }

      int minFragmentSize = properties.getConfigurationProperties()
          .getInteger(CodeSplitters.MIN_FRAGMENT_SIZE, 0);

      dependencyRecorder = chooseDependencyRecorder(baos);
      CodeSplitter.exec(logger, jprogram, jsProgram, jjsmap, expectedFragmentCount,
          minFragmentSize, dependencyRecorder);

      if (baos.size() == 0) {
        dependencyRecorder = recordNonSplitDependencies(baos);
      }
      if (baos.size() > 0) {
        dependencies = new SyntheticArtifact(
            SoycReportLinker.class, "dependencies" + permutationId + ".xml.gz",
            baos.toByteArray());
      }
    } else if (options.isSoycEnabled() || options.isJsonSoycEnabled()) {
      dependencyRecorder = recordNonSplitDependencies(new ByteArrayOutputStream());
    }
    dependenciesAndRecorder = Pair.create(dependencies, dependencyRecorder);

    return dependenciesAndRecorder;
  }

  private MultipleDependencyGraphRecorder chooseDependencyRecorder(OutputStream out) {
    MultipleDependencyGraphRecorder dependencyRecorder =
        MultipleDependencyGraphRecorder.NULL_RECORDER;
    if (options.isSoycEnabled() && options.isJsonSoycEnabled()) {
      dependencyRecorder = new DependencyGraphRecorder(out, jprogram);
    } else if (options.isSoycEnabled()) {
      dependencyRecorder = new DependencyRecorder(out);
    } else if (options.isJsonSoycEnabled()) {
      dependencyRecorder = new DependencyGraphRecorder(out, jprogram);
    }
    return dependencyRecorder;
  }

  /**
   * Dependency information is normally recorded during code splitting, and it results in multiple
   * dependency graphs. If the code splitter doesn't run, then this method can be used instead to
   * record a single dependency graph for the whole program.
   */
  private DependencyRecorder recordNonSplitDependencies(OutputStream out) {
    DependencyRecorder deps;
    if (options.isSoycEnabled() && options.isJsonSoycEnabled()) {
      deps = new DependencyGraphRecorder(out, jprogram);
    } else if (options.isSoycEnabled()) {
      deps = new DependencyRecorder(out);
    } else if (options.isJsonSoycEnabled()) {
      deps = new DependencyGraphRecorder(out, jprogram);
    } else {
      return null;
    }
    deps.open();
    deps.startDependencyGraph("initial", null);

    ControlFlowAnalyzer cfa = new ControlFlowAnalyzer(jprogram);
    cfa.setDependencyRecorder(deps);
    cfa.traverseEntryMethods();
    deps.endDependencyGraph();
    deps.close();
    return deps;
  }

  private CompilationMetricsArtifact addCompilerMetricsArtifact(UnifiedAst unifiedAst,
      Permutation permutation, long startTimeMs, SizeBreakdown[] sizeBreakdowns,
      PermutationResult permutationResult) {
    CompilationMetricsArtifact compilationMetrics = null;
    // TODO: enable this when ClosureCompiler is enabled
    if (options.isCompilerMetricsEnabled()) {
      if (options.isClosureCompilerEnabled()) {
        logger.log(TreeLogger.WARN, "Incompatible options: -XenableClosureCompiler and "
            + "-XcompilerMetric; ignoring -XcompilerMetric.");
      } else {
        compilationMetrics = new CompilationMetricsArtifact(permutation.getId());
        compilationMetrics.setCompileElapsedMilliseconds(
            System.currentTimeMillis() - startTimeMs);
        compilationMetrics.setElapsedMilliseconds(
            System.currentTimeMillis() - ManagementFactory.getRuntimeMXBean().getStartTime());
        compilationMetrics.setJsSize(sizeBreakdowns);
        compilationMetrics.setPermutationDescription(permutation.getProperties().prettyPrint());
        permutationResult.addArtifacts(Lists.newArrayList(
            unifiedAst.getModuleMetrics(), unifiedAst.getPrecompilationMetrics(),
            compilationMetrics));
      }
    }
    return compilationMetrics;
  }

  private void addSourceMapArtifacts(int permutationId, JavaToJavaScriptMap jjsmap,
      Pair<SyntheticArtifact, MultipleDependencyGraphRecorder> dependenciesAndRecorder,
      boolean isSourceMapsEnabled, SizeBreakdown[] sizeBreakdowns,
      List<JsSourceMap> sourceInfoMaps, PermutationResult permutationResult) {
    if (options.isJsonSoycEnabled()) {
      // TODO: enable this when ClosureCompiler is enabled
      if (options.isClosureCompilerEnabled()) {
        logger.log(TreeLogger.WARN, "Incompatible options: -XenableClosureCompiler and "
            + "-XjsonSoyc; ignoring -XjsonSoyc.");
      } else {
        // Is a super set of SourceMapRecorder.makeSourceMapArtifacts().
        permutationResult.addArtifacts(EntityRecorder.makeSoycArtifacts(
            permutationId, sourceInfoMaps, options.getSourceMapFilePrefix(),
            jjsmap, sizeBreakdowns,
            ((DependencyGraphRecorder) dependenciesAndRecorder.getRight()), jprogram));
      }
    } else if (isSourceMapsEnabled) {
      // TODO: enable this when ClosureCompiler is enabled
      if (options.isClosureCompilerEnabled()) {
        logger.log(TreeLogger.WARN, "Incompatible options: -XenableClosureCompiler and "
            + "compiler.useSourceMaps=true; ignoring compiler.useSourceMaps=true.");
      } else {
        logger.log(TreeLogger.INFO, "Source Maps Enabled");
        permutationResult.addArtifacts(SourceMapRecorder.exec(permutationId, sourceInfoMaps,
            options.getSourceMapFilePrefix()));
      }
    }
  }

  /**
   * Adds generated artifacts from previous compiles when doing per-file compiles. <p> All
   * generators are run on first compile but only some very small subset are rerun on recompiles.
   * Care must be taken to ensure that all generated artifacts (such as png/html/css files) are
   * still registered for output even when no generators are run in the current compile.
   */
  private void maybeAddGeneratedArtifacts(PermutationResult permutationResult) {
    if (options.isIncrementalCompileEnabled()) {
      permutationResult.addArtifacts(
          compilerContext.getMinimalRebuildCache().getGeneratedArtifacts());
    }
  }

  private void addSoycArtifacts(UnifiedAst unifiedAst, int permutationId,
      JavaToJavaScriptMap jjsmap,
      Pair<SyntheticArtifact, MultipleDependencyGraphRecorder> dependenciesAndRecorder,
      Map<JsName, JsLiteral> internedLiteralByVariableName, String[] js,
      SizeBreakdown[] sizeBreakdowns,
      List<JsSourceMap> sourceInfoMaps, PermutationResult permutationResult,
      CompilationMetricsArtifact compilationMetrics)
      throws IOException, UnableToCompleteException {
    // TODO: enable this when ClosureCompiler is enabled
    if (options.isClosureCompilerEnabled()) {
      if (options.isSoycEnabled()) {
        logger.log(TreeLogger.WARN, "Incompatible options: -XenableClosureCompiler and "
            + "-compileReport; ignoring -compileReport.");
      }
    } else {
      permutationResult.addArtifacts(makeSoycArtifacts(permutationId, js, sizeBreakdowns,
          options.isSoycExtra() ? sourceInfoMaps : null, dependenciesAndRecorder.getLeft(),
          jjsmap, internedLiteralByVariableName, unifiedAst.getModuleMetrics(),
          unifiedAst.getPrecompilationMetrics(), compilationMetrics,
          options.isSoycHtmlDisabled()));
    }
  }

  private void addSyntheticArtifacts(UnifiedAst unifiedAst, Permutation permutation,
      long startTimeMs, int permutationId, JavaToJavaScriptMap jjsmap,
      Pair<SyntheticArtifact, MultipleDependencyGraphRecorder> dependenciesAndRecorder,
      Map<JsName, JsLiteral> internedLiteralByVariableName, boolean isSourceMapsEnabled,
      String[] jsFragments, SizeBreakdown[] sizeBreakdowns,
      List<JsSourceMap> sourceInfoMaps, PermutationResult permutationResult)
      throws IOException, UnableToCompleteException {

    assert internedLiteralByVariableName != null;

    Event event = SpeedTracerLogger.start(CompilerEventType.PERMUTATION_ARTIFACTS);

    CompilationMetricsArtifact compilationMetrics = addCompilerMetricsArtifact(
        unifiedAst, permutation, startTimeMs, sizeBreakdowns, permutationResult);
    addSoycArtifacts(unifiedAst, permutationId, jjsmap, dependenciesAndRecorder,
        internedLiteralByVariableName, jsFragments, sizeBreakdowns, sourceInfoMaps,
        permutationResult, compilationMetrics);
    addSourceMapArtifacts(permutationId, jjsmap, dependenciesAndRecorder, isSourceMapsEnabled,
        sizeBreakdowns, sourceInfoMaps, permutationResult);
    maybeAddGeneratedArtifacts(permutationResult);

    event.end();
  }

  /**
   * Generate Js code from the given Js ASTs. Also produces information about that transformation.
   */
  private void generateJavaScriptCode(JavaToJavaScriptMap jjsMap, String[] jsFragments,
      StatementRanges[] ranges, SizeBreakdown[] sizeBreakdowns,
      List<JsSourceMap> sourceInfoMaps, boolean sourceMapsEnabled) {

    Event generateJavascriptEvent =
        SpeedTracerLogger.start(CompilerEventType.GENERATE_JAVASCRIPT);

    boolean useClosureCompiler = options.isClosureCompilerEnabled();
    if (useClosureCompiler) {
      ClosureJsRunner runner = new ClosureJsRunner();
      runner.compile(jprogram, jsProgram, jsFragments, options.getOutput());
      generateJavascriptEvent.end();
      return;
    }

    for (int i = 0; i < jsFragments.length; i++) {
      DefaultTextOutput out = new DefaultTextOutput(!options.isIncrementalCompileEnabled() &&
          options.getOutput().shouldMinimize());
      JsReportGenerationVisitor v = new JsReportGenerationVisitor(out, jjsMap,
          options.isJsonSoycEnabled());
      v.accept(jsProgram.getFragmentBlock(i));

      StatementRanges statementRanges = v.getStatementRanges();
      String code = out.toString();
      JsSourceMap infoMap = (sourceInfoMaps != null) ? v.getSourceInfoMap() : null;

      JsAbstractTextTransformer transformer =
          new JsNoopTransformer(code, statementRanges, infoMap);

      /**
       * Cut generated JS up on class boundaries and re-link the source (possibly making use of
       * source from previous compiles, thus making it possible to perform partial recompiles).
       */
      if (options.isIncrementalCompileEnabled()) {
        transformer = new JsTypeLinker(logger, transformer, v.getClassRanges(),
            v.getProgramClassRange(), getMinimalRebuildCache(), jprogram.typeOracle);
        transformer.exec();
      }

      /**
       * Reorder function decls to improve compression ratios. Also restructures the top level
       * blocks into sub-blocks if they exceed 32767 statements.
       */
      Event functionClusterEvent = SpeedTracerLogger.start(CompilerEventType.FUNCTION_CLUSTER);
      // TODO(cromwellian) move to the Js AST optimization, re-enable sourcemaps + clustering
      if (!sourceMapsEnabled && !options.isClosureCompilerFormatEnabled()
          && options.shouldClusterSimilarFunctions()
          && options.getNamespace() == JsNamespaceOption.NONE
          && options.getOutput() == JsOutputOption.OBFUSCATED) {
        transformer = new JsFunctionClusterer(transformer);
        transformer.exec();
      }
      functionClusterEvent.end();

      jsFragments[i] = transformer.getJs();
      ranges[i] = transformer.getStatementRanges();
      if (sizeBreakdowns != null) {
        sizeBreakdowns[i] = v.getSizeBreakdown();
      }
      if (sourceInfoMaps != null) {
        sourceInfoMaps.add(transformer.getSourceInfoMap());
      }
    }

    generateJavascriptEvent.end();
  }

  private Collection<? extends Artifact<?>> makeSoycArtifacts(int permutationId, String[] js,
      SizeBreakdown[] sizeBreakdowns, List<JsSourceMap> sourceInfoMaps,
      SyntheticArtifact dependencies, JavaToJavaScriptMap jjsmap,
      Map<JsName, JsLiteral> internedLiteralByVariableName,
      ModuleMetricsArtifact moduleMetricsArtifact,
      PrecompilationMetricsArtifact precompilationMetricsArtifact,
      CompilationMetricsArtifact compilationMetrics, boolean htmlReportsDisabled)
      throws IOException, UnableToCompleteException {
    Memory.maybeDumpMemory("makeSoycArtifactsStart");
    List<SyntheticArtifact> soycArtifacts = new ArrayList<SyntheticArtifact>();

    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    Event soycEvent = SpeedTracerLogger.start(CompilerEventType.MAKE_SOYC_ARTIFACTS);

    Event recordSplitPoints = SpeedTracerLogger.start(
        CompilerEventType.MAKE_SOYC_ARTIFACTS, "phase", "recordSplitPoints");
    SplitPointRecorder.recordSplitPoints(jprogram, baos, logger);
    SyntheticArtifact splitPoints = new SyntheticArtifact(
        SoycReportLinker.class, "splitPoints" + permutationId + ".xml.gz", baos.toByteArray());
    soycArtifacts.add(splitPoints);
    recordSplitPoints.end();

    SyntheticArtifact sizeMaps = null;
    if (sizeBreakdowns != null) {
      Event recordSizeMap = SpeedTracerLogger.start(
          CompilerEventType.MAKE_SOYC_ARTIFACTS, "phase", "recordSizeMap");
      baos.reset();
      SizeMapRecorder.recordMap(logger, baos, sizeBreakdowns, jjsmap,
          internedLiteralByVariableName);
      sizeMaps = new SyntheticArtifact(
          SoycReportLinker.class, "stories" + permutationId + ".xml.gz", baos.toByteArray());
      soycArtifacts.add(sizeMaps);
      recordSizeMap.end();
    }

    if (sourceInfoMaps != null) {
      Event recordStories = SpeedTracerLogger.start(
          CompilerEventType.MAKE_SOYC_ARTIFACTS, "phase", "recordStories");
      baos.reset();
      StoryRecorder.recordStories(logger, baos, sourceInfoMaps, js);
      soycArtifacts.add(new SyntheticArtifact(
          SoycReportLinker.class, "detailedStories" + permutationId + ".xml.gz",
          baos.toByteArray()));
      recordStories.end();
    }

    if (dependencies != null) {
      soycArtifacts.add(dependencies);
    }

    // Set all of the main SOYC artifacts private.
    for (SyntheticArtifact soycArtifact : soycArtifacts) {
      soycArtifact.setVisibility(Visibility.Private);
    }

    if (!htmlReportsDisabled && sizeBreakdowns != null) {
      Event generateCompileReport = SpeedTracerLogger.start(
          CompilerEventType.MAKE_SOYC_ARTIFACTS, "phase", "generateCompileReport");
      ArtifactsOutputDirectory outDir = new ArtifactsOutputDirectory();
      SoycDashboard dashboard = new SoycDashboard(outDir);
      dashboard.startNewPermutation(Integer.toString(permutationId));
      try {
        dashboard.readSplitPoints(openWithGunzip(splitPoints));
        if (sizeMaps != null) {
          dashboard.readSizeMaps(openWithGunzip(sizeMaps));
        }
        if (dependencies != null) {
          dashboard.readDependencies(openWithGunzip(dependencies));
        }
        Memory.maybeDumpMemory("soycReadDependenciesEnd");
      } catch (ParserConfigurationException e) {
        throw new InternalCompilerException(
            "Error reading compile report information that was just generated", e);
      } catch (SAXException e) {
        throw new InternalCompilerException(
            "Error reading compile report information that was just generated", e);
      }
      dashboard.generateForOnePermutation();
      if (moduleMetricsArtifact != null && precompilationMetricsArtifact != null
          && compilationMetrics != null) {
        dashboard.generateCompilerMetricsForOnePermutation(
            moduleMetricsArtifact, precompilationMetricsArtifact, compilationMetrics);
      }
      soycArtifacts.addAll(outDir.getArtifacts());
      generateCompileReport.end();
    }

    soycEvent.end();

    return soycArtifacts;
  }

  private SymbolData[] makeSymbolMap(Map<StandardSymbolData, JsName> symbolTable) {
    // Keep tracks of a list of referenced name. If it is not used, don't
    // add it to symbol map.
    final Set<String> nameUsed = new HashSet<String>();
    final Map<JsName, Integer> nameToFragment = new HashMap<JsName, Integer>();

    for (int i = 0; i < jsProgram.getFragmentCount(); i++) {
      final Integer fragId = i;
      new JsVisitor() {
        @Override
        public void endVisit(JsForIn x, JsContext ctx) {
          if (x.getIterVarName() != null) {
            nameUsed.add(x.getIterVarName().getIdent());
          }
        }

        @Override
        public void endVisit(JsFunction x, JsContext ctx) {
          if (x.getName() != null) {
            nameToFragment.put(x.getName(), fragId);
            nameUsed.add(x.getName().getIdent());
          }
        }

        @Override
        public void endVisit(JsLabel x, JsContext ctx) {
          nameUsed.add(x.getName().getIdent());
        }

        @Override
        public void endVisit(JsNameOf x, JsContext ctx) {
          if (x.getName() != null) {
            nameUsed.add(x.getName().getIdent());
          }
        }

        @Override
        public void endVisit(JsNameRef x, JsContext ctx) {
          // Obviously this isn't even that accurate. Some of them are
          // variable names, some of the are property. At least this
          // this give us a safe approximation. Ideally we need
          // the code removal passes to remove stuff in the scope objects.
          if (x.isResolved()) {
            nameUsed.add(x.getName().getIdent());
          }
        }

        @Override
        public void endVisit(JsParameter x, JsContext ctx) {
          nameUsed.add(x.getName().getIdent());
        }

        @Override

        public void endVisit(JsVars.JsVar x, JsContext ctx) {
          nameUsed.add(x.getName().getIdent());
        }
      }.accept(jsProgram.getFragmentBlock(i));
    }

    // TODO(acleung): This is a temp fix. Once we know this is safe. We
    // new to rewrite it to avoid extra ArrayList creations.
    // Or we should just consider serializing it as an ArrayList if
    // it is that much trouble to determine the true size.
    List<SymbolData> result = new ArrayList<SymbolData>();

    for (Map.Entry<StandardSymbolData, JsName> entry : symbolTable.entrySet()) {
      StandardSymbolData symbolData = entry.getKey();
      symbolData.setSymbolName(entry.getValue().getShortIdent());
      Integer fragNum = nameToFragment.get(entry.getValue());
      if (fragNum != null) {
        symbolData.setFragmentNumber(fragNum);
      }
      if (nameUsed.contains(entry.getValue().getIdent()) || entry.getKey().isClass()) {
        result.add(symbolData);
      }
    }

    return result.toArray(new SymbolData[result.size()]);
  }

  /**
   * Open an emitted artifact and gunzip its contents.
   */
  private InputStream openWithGunzip(EmittedArtifact artifact)
      throws IOException, UnableToCompleteException {
    return new BufferedInputStream(new GZIPInputStream(artifact.getContents(TreeLogger.NULL)));
  }

  private void optimizeJsLoop(Collection<JsNode> toInline) throws InterruptedException {
    int optimizationLevel = options.getOptimizationLevel();
    List<OptimizerStats> allOptimizerStats = Lists.newArrayList();
    int counter = 0;
    while (true) {
      counter++;
      if (Thread.interrupted()) {
        throw new InterruptedException();
      }
      Event optimizeJsEvent = SpeedTracerLogger.start(CompilerEventType.OPTIMIZE_JS);

      OptimizerStats stats = new OptimizerStats("Pass " + counter);

      // Remove unused functions if possible.
      stats.add(JsStaticEval.exec(jsProgram));
      // Inline Js function invocations
      stats.add(JsInliner.exec(jsProgram, toInline));
      // Remove unused functions if possible.
      stats.add(JsUnusedFunctionRemover.exec(jsProgram));

      // Save the stats to print out after optimizers finish.
      allOptimizerStats.add(stats);

      optimizeJsEvent.end();
      if ((optimizationLevel < OptionOptimize.OPTIMIZE_LEVEL_MAX && counter > optimizationLevel)
          || !stats.didChange()) {
        break;
      }
    }

    if (optimizationLevel > OptionOptimize.OPTIMIZE_LEVEL_DRAFT) {
      DuplicateClinitRemover.exec(jsProgram);
    }
  }

  private Map<JsName, JsLiteral> renameJsSymbols(PermutationProperties properties,
      JavaToJavaScriptMap jjsmap) throws UnableToCompleteException {
    Map<JsName, JsLiteral> internedLiteralByVariableName = null;
    try {
      switch (options.getOutput()) {
        case OBFUSCATED:
          internedLiteralByVariableName = runObfuscateNamer(options, properties, jjsmap);
          break;
        case PRETTY:
          internedLiteralByVariableName = runPrettyNamer(options, properties, jjsmap);
          break;
        case DETAILED:
          internedLiteralByVariableName = runDetailedNamer(properties.getConfigurationProperties());
          break;
        default:
          throw new InternalCompilerException("Unknown output mode");
      }
    } catch (IllegalNameException e) {
      logger.log(TreeLogger.ERROR, e.getMessage(), e);
      throw new UnableToCompleteException();
    }
    return internedLiteralByVariableName == null ?
        ImmutableMap.<JsName, JsLiteral>of() : internedLiteralByVariableName;
  }

  private Map<JsName, JsLiteral> runObfuscateNamer(JJSOptions options,
      PermutationProperties properties, JavaToJavaScriptMap jjsmap)
      throws IllegalNameException {
    if (options.isIncrementalCompileEnabled()) {
      runIncrementalNamer(options, properties.getConfigurationProperties(), jjsmap);
      return null;
    }

    Map<JsName, JsLiteral> internedLiteralByVariableName =
        maybeInternLiterals(JsLiteralInterner.INTERN_ALL);
    FreshNameGenerator freshNameGenerator =
        JsObfuscateNamer.exec(jsProgram, properties.getConfigurationProperties());
    if (options.shouldRemoveDuplicateFunctions()
        && JsStackEmulator.getStackMode(properties) == JsStackEmulator.StackMode.STRIP) {
      JsDuplicateFunctionRemover.exec(jsProgram, freshNameGenerator);
    }
    return internedLiteralByVariableName;
  }

  private Map<JsName, JsLiteral> runPrettyNamer(JJSOptions options,
      PermutationProperties properties, JavaToJavaScriptMap jjsmap)
      throws IllegalNameException {
    if (options.isIncrementalCompileEnabled()) {
      runIncrementalNamer(options, properties.getConfigurationProperties(), jjsmap);
      return null;
    }
    // We don't intern strings in pretty mode to improve readability
    Map<JsName, JsLiteral> internedLiteralByVariableName =
        maybeInternLiterals(JsLiteralInterner.INTERN_ALL & ~JsLiteralInterner.INTERN_STRINGS);

    JsPrettyNamer.exec(jsProgram, properties.getConfigurationProperties());
    return internedLiteralByVariableName;
  }

  private void runIncrementalNamer(JJSOptions options,
      ConfigurationProperties configurationProperties, JavaToJavaScriptMap jjsmap)
    throws IllegalNameException {
    JsIncrementalNamer.exec(jsProgram, configurationProperties,
        compilerContext.getMinimalRebuildCache().getPersistentPrettyNamerState(), jjsmap,
        options.getOutput() == JsOutputOption.OBFUSCATED);
  }

  /**
   * Takes as input a CompilationState and transforms that into a unified by not yet resolved Java
   * AST (a Java AST wherein cross-class references have been connected and all rebind result
   * classes are available and have not yet been pruned down to the set applicable for a particular
   * permutation). This AST is packaged into a UnifiedAst instance and then returned.
   *
   * Precompilation is INTENDED to progress as a series of stages:
   *
   * <pre>
   * 1. initialize local state
   * 2. assert preconditions
   * 3. construct and unify the unresolved Java AST
   * 4. normalize the unresolved Java AST  // arguably should be removed
   * 5. optimize the unresolved Java AST  // arguably should be removed
   * 6. construct and return a value
   * </pre>
   *
   * There are some other types of work here (mostly metrics and data gathering) which do not serve
   * the goal of output program construction. This work should really be moved into subclasses or
   * some sort of callback or plugin system so as not to visually pollute the real compile logic.<br
   * />
   *
   * Significant amounts of visitors implementing the intended above stages are triggered here but
   * in the wrong order. They have been noted for future cleanup.
   */
  private UnifiedAst precompile(PrecompilationContext precompilationContext)
      throws UnableToCompleteException {
    try {

      // (0) Assert preconditions
      if (precompilationContext.getEntryPoints().length +
          precompilationContext.getAdditionalRootTypes().length == 0) {
        throw new IllegalArgumentException("entry point(s) required");
      }

      boolean singlePermutation = precompilationContext.getPermutations().length == 1;
      PrecompilationMetricsArtifact precompilationMetrics =
          precompilationContext.getPrecompilationMetricsArtifact();
      /*
       * Do not introduce any new pass here unless it is logically a part of one of the 6 defined
       * stages and is physically located in that stage.
       */

      // (1) Initialize local state
      boolean legacyJsInterop = compilerContext.getOptions().getJsInteropMode() == Mode.JS;
      jprogram = new JProgram(compilerContext.getMinimalRebuildCache(), legacyJsInterop);
      // Synchronize JTypeOracle with compile optimization behavior.
      jprogram.typeOracle.setOptimize(
          options.getOptimizationLevel() > OptionOptimize.OPTIMIZE_LEVEL_DRAFT);

      jsProgram = new JsProgram();

      // (2) Construct and unify the unresolved Java AST
      CompilationState compilationState =
          constructJavaAst(precompilationContext);

      // TODO(stalcup): hide metrics gathering in a callback or subclass
      JsniRestrictionChecker.exec(logger, jprogram);
      JsInteropRestrictionChecker.exec(logger, jprogram, getMinimalRebuildCache());
      logTypeOracleMetrics(precompilationMetrics, compilationState);
      Memory.maybeDumpMemory("AstOnly");
      AstDumper.maybeDumpAST(jprogram);

      // TODO(stalcup): is in wrong place, move to optimization stage
      ConfigurationProperties configurationProperties = new ConfigurationProperties(module);
      EnumNameObfuscator.exec(jprogram, logger, configurationProperties, options);

      // (3) Normalize the unresolved Java AST
      // Replace defender method references
      ReplaceDefenderMethodReferences.exec(jprogram);

      FixAssignmentsToUnboxOrCast.exec(jprogram);
      if (options.isEnableAssertions()) {
        AssertionNormalizer.exec(jprogram);
      } else {
        AssertionRemover.exec(jprogram);
      }
      if (module != null && options.isRunAsyncEnabled()) {
        ReplaceRunAsyncs.exec(logger, jprogram);
        ConfigurationProperties config = new ConfigurationProperties(module);
        CodeSplitters.pickInitialLoadSequence(logger, jprogram, config);
      }
      ImplementClassLiteralsAsFields.exec(jprogram, shouldOptimize());

      // TODO(stalcup): hide metrics gathering in a callback or subclass
      logAstTypeMetrics(precompilationMetrics);

      // (4) Construct and return a value.
      Event createUnifiedAstEvent = SpeedTracerLogger.start(CompilerEventType.CREATE_UNIFIED_AST);
      UnifiedAst result = new UnifiedAst(
          options, new AST(jprogram, jsProgram), singlePermutation, RecordRebinds.exec(jprogram));
      createUnifiedAstEvent.end();
      return result;
    } catch (Throwable e) {
      throw CompilationProblemReporter.logAndTranslateException(logger, e);
    }
  }

  /**
   * Creates (and returns the name for) a new class to serve as the container for the invocation of
   * registered entry point methods as part of module bootstrapping.<br />
   *
   * The resulting class will be invoked during bootstrapping like FooEntryMethodHolder.init(). By
   * generating the class on the fly and naming it to match the current module, the resulting holder
   * class can work in both monolithic and separate compilation schemes.
   */
  private String buildEntryMethodHolder(StandardGeneratorContext context,
      String[] entryPointTypeNames, Set<String> allRootTypes)
      throws UnableToCompleteException {
    // If there are no entry points.
    if (entryPointTypeNames.length == 0) {
      // Then there's no need to generate an EntryMethodHolder class to launch them.
      return null;
    }

    EntryMethodHolderGenerator entryMethodHolderGenerator = new EntryMethodHolderGenerator();
    String entryMethodHolderTypeName =
        entryMethodHolderGenerator.generate(logger, context, module.getCanonicalName());
    context.finish(logger);
    // Ensures that unification traverses and keeps the class.
    allRootTypes.add(entryMethodHolderTypeName);
    // Ensures that JProgram knows to index this class's methods so that later bootstrap
    // construction code is able to locate the FooEntryMethodHolder.init() function.
    jprogram.addIndexedTypeName(entryMethodHolderTypeName);
    return entryMethodHolderTypeName;
  }

  private CompilationState constructJavaAst(PrecompilationContext precompilationContext)
      throws UnableToCompleteException {
    RebindPermutationOracle rpo = precompilationContext.getRebindPermutationOracle();

    CompilationState compilationState = rpo.getCompilationState();
    Memory.maybeDumpMemory("CompStateBuilt");
    recordJsoTypes(compilationState.getTypeOracle());
    unifyJavaAst(precompilationContext);
    if (options.isSoycEnabled() || options.isJsonSoycEnabled()) {
      SourceInfoCorrelator.exec(jprogram);
    }

    // Free up memory.
    rpo.clear();
    Set<String> deletedTypeNames = options.isIncrementalCompileEnabled()
        ? getMinimalRebuildCache().computeDeletedTypeNames() : Sets.<String>newHashSet();
    jprogram.typeOracle.computeBeforeAST(StandardTypes.createFrom(jprogram),
        jprogram.getDeclaredTypes(), jprogram.getModuleDeclaredTypes(), deletedTypeNames);
    return compilationState;
  }

  /**
   * This method can be used to fetch the list of referenced class.
   *
   * This method is intended to support compiler metrics.
   */
  private String[] getReferencedJavaClasses() {
    class ClassNameVisitor extends JVisitor {
      List<String> classNames = new ArrayList<String>();

      @Override
      public boolean visit(JClassType x, Context ctx) {
        classNames.add(x.getName());
        return true;
      }
    }
    ClassNameVisitor v = new ClassNameVisitor();
    v.accept(jprogram);
    return v.classNames.toArray(new String[v.classNames.size()]);
  }

  private void logAstTypeMetrics(PrecompilationMetricsArtifact precompilationMetrics) {
    if (options.isCompilerMetricsEnabled()) {
      precompilationMetrics.setAstTypes(getReferencedJavaClasses());
    }
  }

  private void logTypeOracleMetrics(
      PrecompilationMetricsArtifact precompilationMetrics, CompilationState compilationState) {
    if (precompilationMetrics != null) {
      List<String> finalTypeOracleTypes = Lists.newArrayList();
      for (com.google.gwt.core.ext.typeinfo.JClassType type :
          compilationState.getTypeOracle().getTypes()) {
        finalTypeOracleTypes.add(type.getPackage().getName() + "." + type.getName());
      }
      precompilationMetrics.setFinalTypeOracleTypes(finalTypeOracleTypes);
    }
  }

  private Set<String> computeRootTypes(String[] entryPointTypeNames,
      String[] additionalRootTypes, CompilationState compilationState) {

    Set<String> allRootTypes = Sets.newTreeSet();
    Iterables.addAll(allRootTypes, compilationState.getQualifiedJsInteropRootTypesNames());
    Collections.addAll(allRootTypes, entryPointTypeNames);
    Collections.addAll(allRootTypes, additionalRootTypes);
    allRootTypes.addAll(JProgram.CODEGEN_TYPES_SET);
    allRootTypes.addAll(jprogram.getTypeNamesToIndex());
    /*
     * Add all SingleJsoImpl types that we know about. It's likely that the concrete types are
     * never explicitly referenced.
     */
    TypeOracle typeOracle = compilationState.getTypeOracle();
    for (com.google.gwt.core.ext.typeinfo.JClassType singleJsoIntf :
        typeOracle.getSingleJsoImplInterfaces()) {
      allRootTypes.add(typeOracle.getSingleJsoImpl(singleJsoIntf).getQualifiedSourceName());
    }
    return allRootTypes;
  }

  private void recordJsoTypes(TypeOracle typeOracle) {
    if (!options.isIncrementalCompileEnabled()) {
      return;
    }

    // Add names of JSO subtypes.
    Set<String> jsoTypeNames = Sets.newHashSet();
    for (com.google.gwt.dev.javac.typemodel.JClassType subtype :
        typeOracle.getJavaScriptObject().getSubtypes()) {
      jsoTypeNames.add(subtype.getQualifiedBinaryName());
    }

    // Add names of interfaces that are always of a JSO (aka there are no non-JSO implementors).
    Set<String> singleJsoImplInterfaceNames = Sets.newHashSet();
    for (com.google.gwt.core.ext.typeinfo.JClassType singleJsoImplInterface :
        typeOracle.getSingleJsoImplInterfaces()) {
      singleJsoImplInterfaceNames.add(singleJsoImplInterface.getQualifiedBinaryName());
    }

    // Add names of interfaces that are only sometimes a JSO (aka there are both JSO and non-JSO
    // imlementors).
    Set<String> dualJsoImplInterfaceNames = Sets.newHashSet();
    for (com.google.gwt.core.ext.typeinfo.JClassType dualJsoImplInterface :
        typeOracle.getDualJsoImplInterfaces()) {
      dualJsoImplInterfaceNames.add(dualJsoImplInterface.getQualifiedBinaryName());
    }

    compilerContext.getMinimalRebuildCache().setJsoTypeNames(jsoTypeNames,
        singleJsoImplInterfaceNames, dualJsoImplInterfaceNames);
  }

  private void synthesizeEntryMethodHolderInit(UnifyAst unifyAst, String[] entryPointTypeNames,
      String entryMethodHolderTypeName) throws UnableToCompleteException {
    // Get type references.
    JDeclaredType entryMethodHolderType =
        unifyAst.findType(entryMethodHolderTypeName, unifyAst.getSourceNameBasedTypeLocator());
    JDeclaredType gwtType = unifyAst.findType("com.google.gwt.core.client.GWT",
        unifyAst.getSourceNameBasedTypeLocator());
    JDeclaredType entryPointType = unifyAst.findType("com.google.gwt.core.client.EntryPoint",
        unifyAst.getSourceNameBasedTypeLocator());

    // Get method references.
    JMethod initMethod = entryMethodHolderType.findMethod("init()V", false);
    JMethod gwtCreateMethod =
        gwtType.findMethod("create(Ljava/lang/Class;)Ljava/lang/Object;", false);

    // Synthesize all onModuleLoad() calls.
    JBlock initMethodBlock = ((JMethodBody) initMethod.getBody()).getBlock();
    SourceInfo origin = initMethodBlock.getSourceInfo().makeChild();
    for (String entryPointTypeName : entryPointTypeNames) {
      // Get type and onModuleLoad function for the current entryPointTypeName.
      JDeclaredType specificEntryPointType =
          unifyAst.findType(entryPointTypeName, unifyAst.getSourceNameBasedTypeLocator());
      if (specificEntryPointType == null) {
        logger.log(TreeLogger.ERROR,
            "Could not find module entry point class '" + entryPointTypeName + "'", null);
        throw new UnableToCompleteException();
      }
      JMethod onModuleLoadMethod =
          entryPointType.findMethod("onModuleLoad()V", true);
      JMethod specificOnModuleLoadMethod =
          specificEntryPointType.findMethod("onModuleLoad()V", true);

      if (specificOnModuleLoadMethod != null && specificOnModuleLoadMethod.isStatic()) {
        // Synthesize a static invocation FooEntryPoint.onModuleLoad(); call.
        JMethodCall staticOnModuleLoadCall =
            new JMethodCall(origin, null, specificOnModuleLoadMethod);
        initMethodBlock.addStmt(staticOnModuleLoadCall.makeStatement());
      } else {
        // Synthesize ((EntryPoint)GWT.create(FooEntryPoint.class)).onModuleLoad();
        JClassLiteral entryPointTypeClassLiteral =
            new JClassLiteral(origin, specificEntryPointType);
        JMethodCall createInstanceCall =
            new JMethodCall(origin, null, gwtCreateMethod, entryPointTypeClassLiteral);
        JCastOperation castToEntryPoint =
            new JCastOperation(origin, entryPointType, createInstanceCall);
        JMethodCall instanceOnModuleLoadCall =
            new JMethodCall(origin, castToEntryPoint, onModuleLoadMethod);
        initMethodBlock.addStmt(instanceOnModuleLoadCall.makeStatement());
      }
    }
  }

  private void unifyJavaAst(PrecompilationContext precompilationContext)
      throws UnableToCompleteException {

    Event event = SpeedTracerLogger.start(CompilerEventType.UNIFY_AST);

    RebindPermutationOracle rpo = precompilationContext.getRebindPermutationOracle();
    String[] entryPointTypeNames = precompilationContext.getEntryPoints();
    String[] additionalRootTypes = precompilationContext.getAdditionalRootTypes();

    Set<String> allRootTypes = computeRootTypes(entryPointTypeNames, additionalRootTypes,
        rpo.getCompilationState());

    String entryMethodHolderTypeName =
        buildEntryMethodHolder(rpo.getGeneratorContext(), entryPointTypeNames, allRootTypes);

    UnifyAst unifyAst =
        new UnifyAst(logger, compilerContext, jprogram, jsProgram, precompilationContext);
    // Makes JProgram aware of these types so they can be accessed via index.
    unifyAst.addRootTypes(allRootTypes);
    // Must synthesize entryPoint.onModuleLoad() calls because some EntryPoint classes are
    // private.
    if (entryMethodHolderTypeName != null) {
      // Only synthesize the init method in the EntryMethodHolder class, if there is an
      // EntryMethodHolder class.
      synthesizeEntryMethodHolderInit(unifyAst, entryPointTypeNames, entryMethodHolderTypeName);
    }
    if (entryMethodHolderTypeName != null) {
      // Only register the init method in the EntryMethodHolder class as an entry method, if there
      // is an EntryMethodHolder class.
      jprogram.addEntryMethod(jprogram.getIndexedMethod(
          SourceName.getShortClassName(entryMethodHolderTypeName) + ".init"));
    }
    unifyAst.exec();

    event.end();
  }

  private void optimizeJavaToFixedPoint() throws InterruptedException {
    Event optimizeEvent = SpeedTracerLogger.start(CompilerEventType.OPTIMIZE);

    List<OptimizerStats> allOptimizerStats = Lists.newArrayList();
    int passCount = 0;
    int nodeCount = jprogram.getNodeCount();
    int lastNodeCount;

    boolean atMaxLevel = options.getOptimizationLevel() == OptionOptimize.OPTIMIZE_LEVEL_MAX;
    int passLimit = atMaxLevel ? MAX_PASSES : options.getOptimizationLevel();
    float minChangeRate = atMaxLevel ? FIXED_POINT_CHANGE_RATE : EFFICIENT_CHANGE_RATE;
    OptimizerContext optimizerCtx = new FullOptimizerContext(jprogram);
    while (true) {
      passCount++;
      if (passCount > passLimit) {
        break;
      }
      if (Thread.interrupted()) {
        optimizeEvent.end();
        throw new InterruptedException();
      }
      AstDumper.maybeDumpAST(jprogram);
      OptimizerStats stats = optimizeJavaOneTime("Pass " + passCount, nodeCount, optimizerCtx);
      allOptimizerStats.add(stats);
      lastNodeCount = nodeCount;
      nodeCount = jprogram.getNodeCount();

      float nodeChangeRate = stats.getNumMods() / (float) lastNodeCount;
      float sizeChangeRate = (lastNodeCount - nodeCount) / (float) lastNodeCount;
      if (nodeChangeRate <= minChangeRate && sizeChangeRate <= minChangeRate) {
        break;
      }
    }

    if (options.shouldOptimizeDataflow()) {
      // Just run it once, because it is very time consuming
      allOptimizerStats.add(DataflowOptimizer.exec(jprogram));
    }

    optimizeEvent.end();
  }

  private boolean shouldOptimize() {
    return options.getOptimizationLevel() > OptionOptimize.OPTIMIZE_LEVEL_DRAFT;
  }

  private TypeMapper getTypeMapper() {

    // Used to stabilize output for DeltaJS
    if (JjsUtils.closureStyleLiteralsNeeded(this.options)) {
      return new ClosureUniqueIdTypeMapper(jprogram);
    }

    if (this.options.useDetailedTypeIds()) {
      return new StringTypeMapper(jprogram);
    }
    return this.options.isIncrementalCompileEnabled() ?
        compilerContext.getMinimalRebuildCache().getTypeMapper() :
        new IntTypeMapper();
  }

  private TypeOrder getTypeOrder() {

    // Used to stabilize output for DeltaJS
    if (JjsUtils.closureStyleLiteralsNeeded(this.options)) {
      return TypeOrder.ALPHABETICAL;
    }

    if (this.options.useDetailedTypeIds()) {
      return TypeOrder.NONE;
    }
    return this.options.isIncrementalCompileEnabled() ? TypeOrder.ALPHABETICAL
        : TypeOrder.FREQUENCY;
  }

  private OptimizerStats optimizeJavaOneTime(String passName, int numNodes,
      OptimizerContext optimizerCtx) {
    Event optimizeEvent = SpeedTracerLogger.start(CompilerEventType.OPTIMIZE, "phase", "loop");
    // Clinits might have become empty become empty.
    jprogram.typeOracle.recomputeAfterOptimizations(jprogram.getDeclaredTypes());
    OptimizerStats stats = new OptimizerStats(passName);
    JavaAstVerifier.assertProgramIsConsistent(jprogram);
    stats.add(Pruner.exec(jprogram, true, optimizerCtx).recordVisits(numNodes));
    stats.add(Finalizer.exec(jprogram, optimizerCtx).recordVisits(numNodes));
    stats.add(MakeCallsStatic.exec(jprogram, options.shouldAddRuntimeChecks(), optimizerCtx)
        .recordVisits(numNodes));
    stats.add(TypeTightener.exec(jprogram, optimizerCtx).recordVisits(numNodes));
    stats.add(MethodCallTightener.exec(jprogram, optimizerCtx).recordVisits(numNodes));
    // Note: Specialization should be done before inlining.
    stats.add(MethodCallSpecializer.exec(jprogram, optimizerCtx).recordVisits(numNodes));
    stats.add(DeadCodeElimination.exec(jprogram, optimizerCtx).recordVisits(numNodes));
    stats.add(MethodInliner.exec(jprogram, optimizerCtx).recordVisits(numNodes));
    if (options.shouldInlineLiteralParameters()) {
      stats.add(SameParameterValueOptimizer.exec(jprogram, optimizerCtx).recordVisits(numNodes));
    }
    if (options.shouldOrdinalizeEnums()) {
      stats.add(EnumOrdinalizer.exec(jprogram, optimizerCtx).recordVisits(numNodes));
    }
    optimizeEvent.end();
    return stats;
  }

  private MinimalRebuildCache getMinimalRebuildCache() {
    return compilerContext.getMinimalRebuildCache();
  }

  private static class PermutationResultImpl implements PermutationResult {

    private final ArtifactSet artifacts = new ArtifactSet();
    private final byte[][] js;
    private final String jsStrongName;
    private final Permutation permutation;
    private final byte[] serializedSymbolMap;
    private final StatementRanges[] statementRanges;

    public PermutationResultImpl(String[] jsFragments, Permutation permutation,
        SymbolData[] symbolMap, StatementRanges[] statementRanges) {
      byte[][] bytes = new byte[jsFragments.length][];
      for (int i = 0; i < jsFragments.length; ++i) {
        bytes[i] = Util.getBytes(jsFragments[i]);
      }
      this.js = bytes;
      this.jsStrongName = Util.computeStrongName(bytes);
      this.permutation = permutation;
      try {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Util.writeObjectToStream(baos, (Object) symbolMap);
        this.serializedSymbolMap = baos.toByteArray();
      } catch (IOException e) {
        throw new RuntimeException("Should never happen with in-memory stream", e);
      }
      this.statementRanges = statementRanges;
    }

    @Override
    public void addArtifacts(Collection<? extends Artifact<?>> newArtifacts) {
      this.artifacts.addAll(newArtifacts);
    }

    @Override
    public ArtifactSet getArtifacts() {
      return artifacts;
    }

    @Override
    public byte[][] getJs() {
      return js;
    }

    @Override
    public String getJsStrongName() {
      return jsStrongName;
    }

    @Override
    public Permutation getPermutation() {
      return permutation;
    }

    @Override
    public byte[] getSerializedSymbolMap() {
      return serializedSymbolMap;
    }

    @Override
    public StatementRanges[] getStatementRanges() {
      return statementRanges;
    }
  }
}
