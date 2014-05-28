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
import com.google.gwt.core.ext.soyc.impl.SizeMapRecorder;
import com.google.gwt.core.ext.soyc.impl.SplitPointRecorder;
import com.google.gwt.core.ext.soyc.impl.StoryRecorder;
import com.google.gwt.core.linker.SoycReportLinker;
import com.google.gwt.dev.CompilerContext;
import com.google.gwt.dev.Permutation;
import com.google.gwt.dev.PrecompileTaskOptions;
import com.google.gwt.dev.cfg.ConfigProps;
import com.google.gwt.dev.cfg.EntryMethodHolderGenerator;
import com.google.gwt.dev.cfg.LibraryGroup.CollidingCompilationUnitException;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.PermProps;
import com.google.gwt.dev.javac.CompilationProblemReporter;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.StandardGeneratorContext;
import com.google.gwt.dev.javac.typemodel.JConstructor;
import com.google.gwt.dev.javac.typemodel.TypeOracle;
import com.google.gwt.dev.jdt.RebindPermutationOracle;
import com.google.gwt.dev.jjs.UnifiedAst.AST;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JBlock;
import com.google.gwt.dev.jjs.ast.JCastOperation;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JLiteral;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.impl.AssertionNormalizer;
import com.google.gwt.dev.jjs.impl.AssertionRemover;
import com.google.gwt.dev.jjs.impl.AstDumper;
import com.google.gwt.dev.jjs.impl.DeadCodeElimination;
import com.google.gwt.dev.jjs.impl.EnumOrdinalizer;
import com.google.gwt.dev.jjs.impl.Finalizer;
import com.google.gwt.dev.jjs.impl.FixAssignmentsToUnboxOrCast;
import com.google.gwt.dev.jjs.impl.GenerateJavaScriptAST;
import com.google.gwt.dev.jjs.impl.ImplementClassLiteralsAsFields;
import com.google.gwt.dev.jjs.impl.JavaToJavaScriptMap;
import com.google.gwt.dev.jjs.impl.JsAbstractTextTransformer;
import com.google.gwt.dev.jjs.impl.JsFunctionClusterer;
import com.google.gwt.dev.jjs.impl.MakeCallsStatic;
import com.google.gwt.dev.jjs.impl.MethodCallSpecializer;
import com.google.gwt.dev.jjs.impl.MethodCallTightener;
import com.google.gwt.dev.jjs.impl.MethodInliner;
import com.google.gwt.dev.jjs.impl.OptimizerStats;
import com.google.gwt.dev.jjs.impl.Pruner;
import com.google.gwt.dev.jjs.impl.RecordRebinds;
import com.google.gwt.dev.jjs.impl.ResolveRebinds;
import com.google.gwt.dev.jjs.impl.SameParameterValueOptimizer;
import com.google.gwt.dev.jjs.impl.SourceInfoCorrelator;
import com.google.gwt.dev.jjs.impl.TypeRefDepsChecker;
import com.google.gwt.dev.jjs.impl.TypeTightener;
import com.google.gwt.dev.jjs.impl.UnifyAst;
import com.google.gwt.dev.jjs.impl.codesplitter.CodeSplitters;
import com.google.gwt.dev.jjs.impl.codesplitter.MultipleDependencyGraphRecorder;
import com.google.gwt.dev.jjs.impl.codesplitter.ReplaceRunAsyncs;
import com.google.gwt.dev.jjs.impl.gflow.DataflowOptimizer;
import com.google.gwt.dev.js.BaselineCoverageGatherer;
import com.google.gwt.dev.js.ClosureJsRunner;
import com.google.gwt.dev.js.CoverageInstrumentor;
import com.google.gwt.dev.js.EvalFunctionsAtTopScope;
import com.google.gwt.dev.js.FreshNameGenerator;
import com.google.gwt.dev.js.JsBreakUpLargeVarStatements;
import com.google.gwt.dev.js.JsDuplicateFunctionRemover;
import com.google.gwt.dev.js.JsInliner;
import com.google.gwt.dev.js.JsLiteralInterner;
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
import com.google.gwt.dev.js.SizeBreakdown;
import com.google.gwt.dev.js.ast.JsArrayLiteral;
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
import com.google.gwt.dev.js.ast.JsStringLiteral;
import com.google.gwt.dev.js.ast.JsVars;
import com.google.gwt.dev.js.ast.JsVars.JsVar;
import com.google.gwt.dev.js.ast.JsVisitor;
import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.dev.util.Empty;
import com.google.gwt.dev.util.Memory;
import com.google.gwt.dev.util.Name.SourceName;
import com.google.gwt.dev.util.Pair;
import com.google.gwt.dev.util.TinyCompileSummary;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.arg.OptionOptimize;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;
import com.google.gwt.soyc.SoycDashboard;
import com.google.gwt.soyc.io.ArtifactsOutputDirectory;
import com.google.gwt.thirdparty.guava.common.annotations.VisibleForTesting;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableMap;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;

import org.xml.sax.SAXException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
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
public abstract class JavaToJavaScriptCompiler {

  /**
   * Compile a permutation.
   */
  protected abstract class PermutationCompiler {

    private Permutation permutation;

    public PermutationCompiler(Permutation permutation) {
      this.permutation = permutation;
    }

    /**
     * Takes as input an unresolved Java AST (a Java AST wherein all rebind result classes are
     * available and have not yet been pruned down to the set applicable for a particular
     * permutation) that was previously constructed by the Precompiler and from that constructs
     * output Js source code and related information. This Js source and related information is
     * packaged into a Permutation instance and then returned.
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
     * There are some other types of work here (mostly metrics and data gathering) which do not
     * serve the goal of output program construction. This work should really be moved into
     * subclasses or some sort of callback or plugin system so as not to visually pollute the real
     * compile logic.<br />
     *
     * Significant amounts of visitors implementing the intended above stages are triggered here but
     * in the wrong order. They have been noted for future cleanup.
     */
    public PermutationResult compilePermutation(UnifiedAst unifiedAst)
        throws UnableToCompleteException {
      Event jjsCompilePermutationEvent = SpeedTracerLogger.start(
          CompilerEventType.JJS_COMPILE_PERMUTATION, "name", permutation.getProps().prettyPrint()
      );
      /*
       * Do not introduce any new pass here unless it is logically a part of one of the 9 defined
       * stages and is physically located in that stage.
       */

      long permStartMs = System.currentTimeMillis();
      try {
        // (1) Initialize local state.
        long startTimeMs = System.currentTimeMillis();
        PermProps props = permutation.getProps();
        int permutationId = permutation.getId();
        AST ast = unifiedAst.getFreshAst();
        jprogram = ast.getJProgram();
        jsProgram = ast.getJsProgram();
        Map<StandardSymbolData, JsName> symbolTable =
            new TreeMap<StandardSymbolData, JsName>(new SymbolData.ClassIdentComparator());

        // TODO(stalcup): hide metrics gathering in a callback or subclass
        if (compilerContext.shouldCompileMonolithic() && logger.isLoggable(TreeLogger.INFO)) {
          logger.log(TreeLogger.INFO, "Compiling permutation " + permutationId + "...");
        }
        printPermutationTrace(permutation);

        // (2) Transform unresolved Java AST to resolved Java AST
        ResolveRebinds.exec(jprogram, permutation.getGwtCreateAnswers());

        // TODO(stalcup): hide metrics gathering in a callback or subclass
        // This has to happen before optimizations because functions might
        // be optimized out; we want those marked as "not executed", not "not
        // instrumentable".
        Multimap<String, Integer> instrumentableLines = null;
        if (System.getProperty("gwt.coverage") != null) {
          instrumentableLines = BaselineCoverageGatherer.exec(jprogram);
        }

        // TODO(stalcup): move to after normalize.
        // (4) Optimize the resolved Java AST
        optimizeJava();

        // TODO(stalcup): move to before optimize.
        // (3) Normalize the resolved Java AST
        Map<JType, JLiteral> typeIdLiteralsByType = normalizeSemantics();

        // TODO(stalcup): this stage shouldn't exist, move into optimize.
        postNormalizationOptimizeJava();
        jprogram.typeOracle.recomputeAfterOptimizations();

        // (5) Construct the Js AST
        Pair<? extends JavaToJavaScriptMap, Set<JsNode>> jjsMapAndInlineableFunctions =
            GenerateJavaScriptAST.exec(jprogram, jsProgram, compilerContext,
                typeIdLiteralsByType,  symbolTable, props);
        JavaToJavaScriptMap jjsmap = jjsMapAndInlineableFunctions.getLeft();

        // TODO(stalcup): hide metrics gathering in a callback or subclass
        if (System.getProperty("gwt.coverage") != null) {
          CoverageInstrumentor.exec(jsProgram, instrumentableLines);
        }

        // (6) Normalize the Js AST
        JsNormalizer.exec(jsProgram);

        // TODO(stalcup): move to AST construction
        JsSymbolResolver.exec(jsProgram);
        if (options.getNamespace() == JsNamespaceOption.BY_JAVA_PACKAGE) {
          JsNamespaceChooser.exec(jsProgram, jjsmap);
        }

        // TODO(stalcup): move to normalization
        EvalFunctionsAtTopScope.exec(jsProgram, jjsmap);

        // (7) Optimize the JS AST.

        final Set<JsNode> inlinableJsFunctions = jjsMapAndInlineableFunctions.getRight();
        optimizeJs(inlinableJsFunctions);

        // TODO(stalcup): move to normalization
        // Must run before code splitter and namer.
        JsStackEmulator.exec(jprogram, jsProgram, props, jjsmap);

        // TODO(stalcup): move to normalization
        Pair<SyntheticArtifact, MultipleDependencyGraphRecorder> dependenciesAndRecorder =
            splitJsIntoFragments(props, permutationId, jjsmap);

        // TODO(stalcup): move to optimize.
        Map<JsName, JsLiteral> internedLiteralByVariableName = renameJsSymbols(props);

        // TODO(stalcup): move to normalization
        JsBreakUpLargeVarStatements.exec(jsProgram, props.getConfigProps());

        embedBindingProperties(jsProgram, props);

        // (8) Generate Js source
        List<JsSourceMap> sourceInfoMaps = new ArrayList<JsSourceMap>();
        boolean isSourceMapsEnabled = props.isTrueInAnyPermutation("compiler.useSourceMaps");
        String[] jsFragments = new String[jsProgram.getFragmentCount()];
        StatementRanges[] ranges = new StatementRanges[jsFragments.length];
        SizeBreakdown[] sizeBreakdowns = options.isJsonSoycEnabled() || options.isSoycEnabled()
            || options.isCompilerMetricsEnabled() ? new SizeBreakdown[jsFragments.length] : null;
        generateJavaScriptCode(jjsmap, jsFragments, ranges, sizeBreakdowns, sourceInfoMaps,
            isSourceMapsEnabled || options.isJsonSoycEnabled());

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
        logTrackingStats();
        if (logger.isLoggable(TreeLogger.TRACE)) {
          logger.log(TreeLogger.TRACE,
              "Permutation took " + (System.currentTimeMillis() - permStartMs) + " ms");
        }
      }
    }

    protected abstract  void optimizeJs(Set<JsNode> inlinableJsFunctions)
        throws InterruptedException;

    protected abstract void optimizeJava() throws InterruptedException;

    protected abstract void postNormalizationOptimizeJava();

    protected abstract Map<JsName, JsLiteral> runDetailedNamer(ConfigProps config);

    protected abstract Pair<SyntheticArtifact, MultipleDependencyGraphRecorder> splitJsIntoFragments(
        PermProps props, int permutationId, JavaToJavaScriptMap jjsmap);

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
          compilationMetrics.setPermutationDescription(permutation.getProps().prettyPrint());
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
      CompilationMetricsArtifact compilationMetrics = addCompilerMetricsArtifact(
          unifiedAst, permutation, startTimeMs, sizeBreakdowns, permutationResult);
      addSoycArtifacts(unifiedAst, permutationId, jjsmap, dependenciesAndRecorder,
          internedLiteralByVariableName, jsFragments, sizeBreakdowns, sourceInfoMaps,
          permutationResult, compilationMetrics);
      addSourceMapArtifacts(permutationId, jjsmap, dependenciesAndRecorder, isSourceMapsEnabled,
          sizeBreakdowns, sourceInfoMaps, permutationResult);
    }

    /**
     * Embeds properties into $permProps for easy access from JavaScript.
     */
    private void embedBindingProperties(JsProgram jsProgram, PermProps props) {

      // Generates a list of lists of pairs: [[["key", "value"], ...], ...]
      // The outermost list is indexed by soft permutation id. Each item represents
      // a map from binding properties to their values, but is stored as a list of pairs
      // for easy iteration.
      JsArrayLiteral permProps = new JsArrayLiteral(SourceOrigin.UNKNOWN);
      for (ImmutableMap<String, String> propMap : props.findEmbeddedProperties(logger)) {
        JsArrayLiteral entryList = new JsArrayLiteral(SourceOrigin.UNKNOWN);
        for (Entry<String, String> entry : propMap.entrySet()) {
          JsArrayLiteral pair = new JsArrayLiteral(SourceOrigin.UNKNOWN);
          pair.getExpressions().add(new JsStringLiteral(SourceOrigin.UNKNOWN, entry.getKey()));
          pair.getExpressions().add(new JsStringLiteral(SourceOrigin.UNKNOWN, entry.getValue()));
          entryList.getExpressions().add(pair);
        }
        permProps.getExpressions().add(entryList);
      }

      // Generate: var $permProps = ...;
      JsVar var = new JsVar(SourceOrigin.UNKNOWN,
          jsProgram.getScope().findExistingUnobfuscatableName("$permProps"));
      var.setInitExpr(permProps);
      JsVars vars = new JsVars(SourceOrigin.UNKNOWN);
      vars.add(var);

      // Put it at the beginning for easy reference.
      jsProgram.getGlobalBlock().getStatements().add(0, vars);
    }

    /**
     * Generate Js code from the given Js ASTs. Also produces information about that transformation.
     */
    private void generateJavaScriptCode(JavaToJavaScriptMap jjsMap, String[] jsFragments,
        StatementRanges[] ranges, SizeBreakdown[] sizeBreakdowns,
        List<JsSourceMap> sourceInfoMaps, boolean sourceMapsEnabled) {

      boolean useClosureCompiler = options.isClosureCompilerEnabled();
      if (useClosureCompiler) {
        ClosureJsRunner runner = new ClosureJsRunner();
        runner.compile(jprogram, jsProgram, jsFragments, options.getOutput());
        return;
      }

      for (int i = 0; i < jsFragments.length; i++) {
        DefaultTextOutput out = new DefaultTextOutput(options.getOutput().shouldMinimize());
        JsReportGenerationVisitor v = new JsReportGenerationVisitor(out, jjsMap,
            options.isJsonSoycEnabled());
        v.accept(jsProgram.getFragmentBlock(i));

        StatementRanges statementRanges = v.getStatementRanges();
        String code = out.toString();
        JsSourceMap infoMap = (sourceInfoMaps != null) ? v.getSourceInfoMap() : null;

        JsAbstractTextTransformer transformer =
            new JsAbstractTextTransformer(code, statementRanges, infoMap) {
                @Override
              public void exec() {
              }

                @Override
              protected void updateSourceInfoMap() {
              }
            };

        /**
         * Reorder function decls to improve compression ratios. Also restructures the top level
         * blocks into sub-blocks if they exceed 32767 statements.
         */
        Event functionClusterEvent = SpeedTracerLogger.start(CompilerEventType.FUNCTION_CLUSTER);
        // TODO(cromwellian) move to the Js AST optimization, re-enable sourcemaps + clustering
        if (!sourceMapsEnabled && options.shouldClusterSimilarFunctions()
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
     * Transform patterns that can't be represented in JS (such as multiple catch blocks) into
     * equivalent but compatible patterns and take JVM semantics (such as numeric casts) that are
     * not explicit in the AST and make them explicit.<br />
     *
     * These passes can not be reordering because of subtle interdependencies.
     */
    protected abstract Map<JType, JLiteral> normalizeSemantics();

    /**
     * Open an emitted artifact and gunzip its contents.
     */
    private GZIPInputStream openWithGunzip(EmittedArtifact artifact)
        throws IOException, UnableToCompleteException {
      return new GZIPInputStream(artifact.getContents(TreeLogger.NULL));
    }

    protected void optimizeJsLoop(Collection<JsNode> toInline) throws InterruptedException {
      List<OptimizerStats> allOptimizerStats = new ArrayList<OptimizerStats>();
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
        int optimizationLevel = options.getOptimizationLevel();
        if ((optimizationLevel < OptionOptimize.OPTIMIZE_LEVEL_MAX && counter > optimizationLevel)
            || !stats.didChange()) {
          break;
        }
      }

      printJsOptimizeTrace(allOptimizerStats);
    }

    private void printJsOptimizeTrace(List<OptimizerStats> allOptimizerStats) {
      if (JProgram.isTracingEnabled()) {
        System.out.println("");
        System.out.println("               Js Optimization Stats");
        System.out.println("");
        for (OptimizerStats stats : allOptimizerStats) {
          System.out.println(stats.prettyPrint());
        }
      }
    }

    private void printPermutationTrace(Permutation permutation) {
      if (JProgram.isTracingEnabled()) {
        System.out.println("-------------------------------------------------------------");
        System.out.println("|                     (new permutation)                     |");
        System.out.println("-------------------------------------------------------------");
        System.out.println("Properties: " + permutation.getProps().prettyPrint());
      }
    }

    private Map<JsName, JsLiteral> renameJsSymbols(PermProps props) {
      Map<JsName, JsLiteral> internedLiteralByVariableName;
      switch (options.getOutput()) {
        case OBFUSCATED:
          internedLiteralByVariableName = runObfuscateNamer(props);
          break;
        case PRETTY:
          internedLiteralByVariableName = runPrettyNamer(props.getConfigProps());
          break;
        case DETAILED:
          internedLiteralByVariableName = runDetailedNamer(props.getConfigProps());
          break;
        default:
          throw new InternalCompilerException("Unknown output mode");
      }
      return internedLiteralByVariableName;
    }

    private Map<JsName, JsLiteral> runObfuscateNamer(PermProps props) {
      Map<JsName, JsLiteral> internedLiteralByVariableName =
          JsLiteralInterner.exec(jprogram, jsProgram, JsLiteralInterner.INTERN_ALL);
      FreshNameGenerator freshNameGenerator = JsObfuscateNamer.exec(jsProgram,
          props.getConfigProps());
      if (options.shouldRemoveDuplicateFunctions()
          && JsStackEmulator.getStackMode(props) == JsStackEmulator.StackMode.STRIP) {
        JsDuplicateFunctionRemover.exec(jsProgram, freshNameGenerator);
      }
      return internedLiteralByVariableName;
    }

    private Map<JsName, JsLiteral> runPrettyNamer(ConfigProps config) {
      // We don't intern strings in pretty mode to improve readability
      Map<JsName, JsLiteral> internedLiteralByVariableName = JsLiteralInterner.exec(
          jprogram, jsProgram,
          (byte) (JsLiteralInterner.INTERN_ALL & ~JsLiteralInterner.INTERN_STRINGS));

      JsPrettyNamer.exec(jsProgram, config);
      return internedLiteralByVariableName;
    }
  }

  /**
   * Performs precompilation.
   */
  protected abstract class Precompiler {

    protected RebindPermutationOracle rpo;
    protected String[] entryPointTypeNames;

    public Precompiler(RebindPermutationOracle rpo, String[] entryPointTypeNames) {
      this.rpo = rpo;
      this.entryPointTypeNames = entryPointTypeNames;
    }

    protected abstract void beforeUnifyAst(Set<String> allRootTypes)
        throws UnableToCompleteException;

    protected abstract void checkEntryPoints(String[] additionalRootTypes);

    protected abstract void createJProgram();

    /**
     * Takes as input a CompilationState and transforms that into a unified by not yet resolved Java
     * AST (a Java AST wherein cross-class references have been connected and all rebind result
     * classes are available and have not yet been pruned down to the set applicable for a
     * particular permutation). This AST is packaged into a UnifiedAst instance and then returned.
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
     * There are some other types of work here (mostly metrics and data gathering) which do not
     * serve the goal of output program construction. This work should really be moved into
     * subclasses or some sort of callback or plugin system so as not to visually pollute the real
     * compile logic.<br />
     *
     * Significant amounts of visitors implementing the intended above stages are triggered here but
     * in the wrong order. They have been noted for future cleanup.
     */
    protected final UnifiedAst precompile(String[] additionalRootTypes, boolean singlePermutation,
        PrecompilationMetricsArtifact precompilationMetrics) throws UnableToCompleteException {
      try {
        /*
         * Do not introduce any new pass here unless it is logically a part of one of the 6 defined
         * stages and is physically located in that stage.
         */

        // (1) Initialize local state
        createJProgram();
        jsProgram = new JsProgram();
        if (additionalRootTypes == null) {
          additionalRootTypes = Empty.STRINGS;
        }

        // (2) Assert preconditions
        checkEntryPoints(additionalRootTypes);

        // (3) Construct and unify the unresolved Java AST
        CompilationState compilationState = constructJavaAst(additionalRootTypes);

        // TODO(stalcup): hide metrics gathering in a callback or subclass
        TypeRefDepsChecker.exec(logger, jprogram, module, options.warnMissingDeps(),
            options.getMissingDepsFile());
        logTypeOracleMetrics(precompilationMetrics, compilationState);
        Memory.maybeDumpMemory("AstOnly");
        AstDumper.maybeDumpAST(jprogram);

        // TODO(stalcup): is in wrong place, move to optimization stage
        obfuscateEnums();

        // (4) Normalize the unresolved Java AST
        FixAssignmentsToUnboxOrCast.exec(jprogram);
        if (options.isEnableAssertions()) {
          AssertionNormalizer.exec(jprogram);
        } else {
          AssertionRemover.exec(jprogram);
        }
        if (module != null && options.isRunAsyncEnabled()) {
          ReplaceRunAsyncs.exec(logger, jprogram);
          ConfigProps config = new ConfigProps(module);
          CodeSplitters.pickInitialLoadSequence(logger, jprogram, config);
        }
        ImplementClassLiteralsAsFields.exec(jprogram);

        // (5) Optimize the unresolved Java AST
        optimizeJava(singlePermutation);

        // TODO(stalcup): hide metrics gathering in a callback or subclass
        logAstTypeMetrics(precompilationMetrics);

        // (6) Construct and return a value.
        Event createUnifiedAstEvent = SpeedTracerLogger.start(CompilerEventType.CREATE_UNIFIED_AST);
        UnifiedAst result = new UnifiedAst(
            options, new AST(jprogram, jsProgram), singlePermutation, RecordRebinds.exec(jprogram));
        createUnifiedAstEvent.end();
        return result;
      } catch (Throwable e) {
        throw CompilationProblemReporter.logAndTranslateException(logger, e);
      } finally {
        logTrackingStats();
      }
    }

    /**
     * Creates (and returns the name for) a new class to serve as the container for the invocation
     * of registered entry point methods as part of module bootstrapping.<br />
     *
     * The resulting class will be invoked during bootstrapping like FooEntryMethodHolder.init(). By
     * generating the class on the fly and naming it to match the current module, the resulting
     * holder class can work in both monolithic and separate compilation schemes.
     */
    private String buildEntryMethodHolder(StandardGeneratorContext context,
        Set<String> allRootTypes) throws UnableToCompleteException {
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

    private CompilationState constructJavaAst(String[] additionalRootTypes)
        throws UnableToCompleteException {
      Set<String> allRootTypes = new TreeSet<String>();
      CompilationState compilationState = rpo.getCompilationState();
      Memory.maybeDumpMemory("CompStateBuilt");
      populateRootTypes(allRootTypes, additionalRootTypes, compilationState.getTypeOracle());
      String entryMethodHolderTypeName =
          buildEntryMethodHolder(rpo.getGeneratorContext(), allRootTypes);
      beforeUnifyAst(allRootTypes);
      unifyJavaAst(allRootTypes, entryMethodHolderTypeName);
      if (options.isSoycEnabled() || options.isJsonSoycEnabled()) {
        SourceInfoCorrelator.exec(jprogram);
      }

      // Gathers simple metrics that can highlight overly-large modules in an incremental compile.
      TinyCompileSummary tinyCompileSummary = compilerContext.getTinyCompileSummary();
      tinyCompileSummary.setTypesForGeneratorsCount(
          rpo.getGeneratorContext().getTypeOracle().getTypes().length);
      tinyCompileSummary.setTypesForAstCount(jprogram.getDeclaredTypes().size());
      tinyCompileSummary.setStaticSourceFilesCount(compilationState.getStaticSourceCount());
      tinyCompileSummary.setGeneratedSourceFilesCount(compilationState.getGeneratedSourceCount());
      tinyCompileSummary.setCachedStaticSourceFilesCount(
          compilationState.getCachedStaticSourceCount());
      tinyCompileSummary.setCachedGeneratedSourceFilesCount(
          compilationState.getCachedGeneratedSourceCount());

      // Free up memory.
      rpo.clear();
      jprogram.typeOracle.computeBeforeAST();
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

    private void obfuscateEnums() {
      // See if we should run the EnumNameObfuscator
      if (module != null) {
        ConfigProps config = new ConfigProps(module);
        if (config.getBoolean(ENUM_NAME_OBFUSCATION_PROPERTY, false)) {
          EnumNameObfuscator.exec(jprogram, logger);
        }
      }
    }

    private void optimizeJava(boolean singlePermutation) throws InterruptedException {
      if (options.getOptimizationLevel() > OptionOptimize.OPTIMIZE_LEVEL_DRAFT
          && !singlePermutation) {
        if (options.isOptimizePrecompile()) {
          /*
           * Go ahead and optimize early, so that each permutation will run faster. This code path
           * is used by the Compiler entry point. We assume that we will not be able to perfectly
           * parallelize the permutation compiles, so let's optimize as much as possible the common
           * AST. In some cases, this might also have the side benefit of reducing the total
           * permutation count.
           */
          optimizeJavaToFixedPoint();
        } else {
          /*
           * Do only minimal early optimizations. This code path is used by the Precompile entry
           * point. The external system might be able to perfectly parallelize the permutation
           * compiles, so let's avoid doing potentially superlinear optimizations on the unified
           * AST.
           */
          optimizeJavaOneTime("Early Optimization", jprogram.getNodeCount());
        }
      }
    }

    private void populateRootTypes(Set<String> allRootTypes, String[] additionalRootTypes,
        TypeOracle typeOracle) {
      Collections.addAll(allRootTypes, entryPointTypeNames);
      Collections.addAll(allRootTypes, additionalRootTypes);
      allRootTypes.addAll(JProgram.CODEGEN_TYPES_SET);
      allRootTypes.addAll(jprogram.getTypeNamesToIndex());
      /*
       * Add all SingleJsoImpl types that we know about. It's likely that the concrete types are
       * never explicitly referenced.
       */
      for (com.google.gwt.core.ext.typeinfo.JClassType singleJsoIntf :
          typeOracle.getSingleJsoImplInterfaces()) {
        allRootTypes.add(typeOracle.getSingleJsoImpl(singleJsoIntf).getQualifiedSourceName());
      }

      // find any types with @JsExport could be entry points as well
      String jsExportAnn = "com.google.gwt.core.client.js.JsExport";
      nextType: for (com.google.gwt.dev.javac.typemodel.JClassType type :
          typeOracle.getTypes()) {
        for (com.google.gwt.dev.javac.typemodel.JMethod meth : type.getMethods()) {
          for (Annotation ann : meth.getAnnotations()) {
            if (ann.annotationType().getName().equals(jsExportAnn)) {
              allRootTypes.add(type.getQualifiedSourceName());
              continue nextType;
            }
          }
        }
        for (JConstructor meth : type.getConstructors()) {
          for (Annotation ann : meth.getAnnotations()) {
            if (ann.annotationType().getName().equals(jsExportAnn)) {
              allRootTypes.add(type.getQualifiedSourceName());
              continue nextType;
            }
          }
        }
      }
    }

    private void synthesizeEntryMethodHolderInit(UnifyAst unifyAst,
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

    private void unifyJavaAst(Set<String> allRootTypes, String entryMethodHolderTypeName)
        throws UnableToCompleteException {
      UnifyAst unifyAst;
      try {
        unifyAst = new UnifyAst(logger, compilerContext, jprogram, jsProgram, rpo);
      } catch (CollidingCompilationUnitException e) {
        logger.log(TreeLogger.ERROR, e.getMessage());
        throw new UnableToCompleteException();
      }
      // Makes JProgram aware of these types so they can be accessed via index.
      unifyAst.addRootTypes(allRootTypes);
      // Must synthesize entryPoint.onModuleLoad() calls because some EntryPoint classes are
      // private.
      if (entryMethodHolderTypeName != null) {
        // Only synthesize the init method in the EntryMethodHolder class, if there is an
        // EntryMethodHolder class.
        synthesizeEntryMethodHolderInit(unifyAst, entryMethodHolderTypeName);
      }
      // Ensures that unification traversal starts from these methods.
      jprogram.addEntryMethod(jprogram.getIndexedMethod("Impl.registerEntry"));
      if (entryMethodHolderTypeName != null) {
        // Only register the init method in the EntryMethodHolder class as an entry method, if there
        // is an EntryMethodHolder class.
        jprogram.addEntryMethod(jprogram.getIndexedMethod(
            SourceName.getShortClassName(entryMethodHolderTypeName) + ".init"));
      }
      unifyAst.exec();
    }
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

  /**
   * Ending optimization passes when the rate of change has reached this value results in gaining
   * nearly all of the impact while avoiding the long tail of costly but low-impact passes.
   */
  private static final float EFFICIENT_CHANGE_RATE = 0.01f;

  private static final String ENUM_NAME_OBFUSCATION_PROPERTY = "compiler.enum.obfuscate.names";

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
    InternalCompilerException.preload();
  }

  protected final CompilerContext compilerContext;

  protected JsProgram jsProgram;

  protected final TreeLogger logger;

  protected final ModuleDef module;

  protected final PrecompileTaskOptions options;

  @VisibleForTesting
  JProgram jprogram;

  public JavaToJavaScriptCompiler(TreeLogger logger, CompilerContext compilerContext) {
    this.logger = logger;
    this.compilerContext = compilerContext;
    this.module = compilerContext.getModule();
    this.options = compilerContext.getOptions();
  }

  /**
   * Compiles and returns a particular permutation, based on a precompiled unified AST.
   */
  public abstract PermutationResult compilePermutation(
      UnifiedAst unifiedAst, Permutation permutation) throws UnableToCompleteException;

  /**
   * Performs a precompilation, returning a unified AST.
   */
  public UnifiedAst precompile(RebindPermutationOracle rpo, String[] entryPointTypeNames,
      String[] additionalRootTypes, boolean singlePermutation) throws UnableToCompleteException {
    return precompile(rpo, entryPointTypeNames, additionalRootTypes, singlePermutation, null);
  }

  /**
   * Performs a precompilation, returning a unified AST.
   */
  public abstract UnifiedAst precompile(RebindPermutationOracle rpo, String[] entryPointTypeNames,
      String[] additionalRootTypes, boolean singlePermutation,
      PrecompilationMetricsArtifact precompilationMetrics) throws UnableToCompleteException;

  protected final void optimizeJavaToFixedPoint() throws InterruptedException {
    Event optimizeEvent = SpeedTracerLogger.start(CompilerEventType.OPTIMIZE);

    List<OptimizerStats> allOptimizerStats = new ArrayList<OptimizerStats>();
    int passCount = 0;
    int nodeCount = jprogram.getNodeCount();
    int lastNodeCount;

    boolean atMaxLevel = options.getOptimizationLevel() == OptionOptimize.OPTIMIZE_LEVEL_MAX;
    int passLimit = atMaxLevel ? MAX_PASSES : options.getOptimizationLevel();
    float minChangeRate = atMaxLevel ? FIXED_POINT_CHANGE_RATE : EFFICIENT_CHANGE_RATE;
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
      OptimizerStats stats = optimizeJavaOneTime("Pass " + passCount, nodeCount);
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

    printJavaOptimizeTrace(allOptimizerStats);

    optimizeEvent.end();
  }

  /*
   * This method is intended as a central location for producing optional tracking output. This will
   * be called after all optimization/normalization passes have completed.
   */
  private void logTrackingStats() {
    EnumOrdinalizer.Tracker eot = EnumOrdinalizer.getTracker();
    if (eot != null) {
      eot.logResultsDetailed(logger, TreeLogger.WARN);
    }
  }

  private OptimizerStats optimizeJavaOneTime(String passName, int numNodes) {
    Event optimizeEvent = SpeedTracerLogger.start(CompilerEventType.OPTIMIZE, "phase", "loop");
    // Clinits might have become empty become empty.
    jprogram.typeOracle.recomputeAfterOptimizations();
    OptimizerStats stats = new OptimizerStats(passName);
    stats.add(Pruner.exec(jprogram, true).recordVisits(numNodes));
    stats.add(Finalizer.exec(jprogram).recordVisits(numNodes));
    stats.add(MakeCallsStatic.exec(options, jprogram).recordVisits(numNodes));
    stats.add(TypeTightener.exec(jprogram).recordVisits(numNodes));
    stats.add(MethodCallTightener.exec(jprogram).recordVisits(numNodes));
    stats.add(MethodCallSpecializer.exec(jprogram).recordVisits(numNodes));
    stats.add(DeadCodeElimination.exec(jprogram).recordVisits(numNodes));
    stats.add(MethodInliner.exec(jprogram).recordVisits(numNodes));
    if (options.shouldInlineLiteralParameters()) {
      stats.add(SameParameterValueOptimizer.exec(jprogram).recordVisits(numNodes));
    }
    if (options.shouldOrdinalizeEnums()) {
      stats.add(EnumOrdinalizer.exec(jprogram).recordVisits(numNodes));
    }
    optimizeEvent.end();
    return stats;
  }

  private void printJavaOptimizeTrace(List<OptimizerStats> allOptimizerStats) {
    if (JProgram.isTracingEnabled()) {
      System.out.println("");
      System.out.println("                Java Optimization Stats");
      System.out.println("");
      for (OptimizerStats stats : allOptimizerStats) {
        System.out.println(stats.prettyPrint());
      }
    }
  }
}
