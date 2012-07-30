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

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.SelectionProperty;
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
import com.google.gwt.core.ext.soyc.Range;
import com.google.gwt.core.ext.soyc.SourceMapRecorder;
import com.google.gwt.core.ext.soyc.impl.DependencyRecorder;
import com.google.gwt.core.ext.soyc.impl.SizeMapRecorder;
import com.google.gwt.core.ext.soyc.impl.SplitPointRecorder;
import com.google.gwt.core.ext.soyc.impl.StoryRecorder;
import com.google.gwt.core.linker.SoycReportLinker;
import com.google.gwt.dev.Permutation;
import com.google.gwt.dev.cfg.ConfigurationProperty;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.javac.CompilationProblemReporter;
import com.google.gwt.dev.javac.typemodel.TypeOracle;
import com.google.gwt.dev.jdt.RebindPermutationOracle;
import com.google.gwt.dev.jjs.UnifiedAst.AST;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JBlock;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JGwtCreate;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReboundEntryPoint;
import com.google.gwt.dev.jjs.ast.JStatement;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.impl.ArrayNormalizer;
import com.google.gwt.dev.jjs.impl.AssertionNormalizer;
import com.google.gwt.dev.jjs.impl.AssertionRemover;
import com.google.gwt.dev.jjs.impl.AstDumper;
import com.google.gwt.dev.jjs.impl.CastNormalizer;
import com.google.gwt.dev.jjs.impl.CatchBlockNormalizer;
import com.google.gwt.dev.jjs.impl.CodeSplitter;
import com.google.gwt.dev.jjs.impl.CodeSplitter.MultipleDependencyGraphRecorder;
import com.google.gwt.dev.jjs.impl.CodeSplitter2;
import com.google.gwt.dev.jjs.impl.ControlFlowAnalyzer;
import com.google.gwt.dev.jjs.impl.DeadCodeElimination;
import com.google.gwt.dev.jjs.impl.EnumOrdinalizer;
import com.google.gwt.dev.jjs.impl.EqualityNormalizer;
import com.google.gwt.dev.jjs.impl.Finalizer;
import com.google.gwt.dev.jjs.impl.FixAssignmentToUnbox;
import com.google.gwt.dev.jjs.impl.GenerateJavaScriptAST;
import com.google.gwt.dev.jjs.impl.HandleCrossFragmentReferences;
import com.google.gwt.dev.jjs.impl.ImplementClassLiteralsAsFields;
import com.google.gwt.dev.jjs.impl.JavaToJavaScriptMap;
import com.google.gwt.dev.jjs.impl.JsAbstractTextTransformer;
import com.google.gwt.dev.jjs.impl.JsFunctionClusterer;
import com.google.gwt.dev.jjs.impl.JsIEBlockTextTransformer;
import com.google.gwt.dev.jjs.impl.JsoDevirtualizer;
import com.google.gwt.dev.jjs.impl.LongCastNormalizer;
import com.google.gwt.dev.jjs.impl.LongEmulationNormalizer;
import com.google.gwt.dev.jjs.impl.MakeCallsStatic;
import com.google.gwt.dev.jjs.impl.MethodCallTightener;
import com.google.gwt.dev.jjs.impl.MethodInliner;
import com.google.gwt.dev.jjs.impl.OptimizerStats;
import com.google.gwt.dev.jjs.impl.PostOptimizationCompoundAssignmentNormalizer;
import com.google.gwt.dev.jjs.impl.Pruner;
import com.google.gwt.dev.jjs.impl.RecordRebinds;
import com.google.gwt.dev.jjs.impl.RemoveEmptySuperCalls;
import com.google.gwt.dev.jjs.impl.ReplaceGetClassOverrides;
import com.google.gwt.dev.jjs.impl.ReplaceRunAsyncs;
import com.google.gwt.dev.jjs.impl.ResolveRebinds;
import com.google.gwt.dev.jjs.impl.SameParameterValueOptimizer;
import com.google.gwt.dev.jjs.impl.SourceInfoCorrelator;
import com.google.gwt.dev.jjs.impl.TypeTightener;
import com.google.gwt.dev.jjs.impl.UnifyAst;
import com.google.gwt.dev.jjs.impl.VerifySymbolMap;
import com.google.gwt.dev.jjs.impl.gflow.DataflowOptimizer;
import com.google.gwt.dev.js.BaselineCoverageGatherer;
import com.google.gwt.dev.js.ClosureJsRunner;
import com.google.gwt.dev.js.CoverageInstrumentor;
import com.google.gwt.dev.js.EvalFunctionsAtTopScope;
import com.google.gwt.dev.js.JsBreakUpLargeVarStatements;
import com.google.gwt.dev.js.JsCoerceIntShift;
import com.google.gwt.dev.js.JsDuplicateCaseFolder;
import com.google.gwt.dev.js.JsDuplicateFunctionRemover;
import com.google.gwt.dev.js.JsIEBlockSizeVisitor;
import com.google.gwt.dev.js.JsInliner;
import com.google.gwt.dev.js.JsNormalizer;
import com.google.gwt.dev.js.JsObfuscateNamer;
import com.google.gwt.dev.js.JsPrettyNamer;
import com.google.gwt.dev.js.JsReportGenerationVisitor;
import com.google.gwt.dev.js.JsSourceGenerationVisitorWithSizeBreakdown;
import com.google.gwt.dev.js.JsStackEmulator;
import com.google.gwt.dev.js.JsStaticEval;
import com.google.gwt.dev.js.JsStringInterner;
import com.google.gwt.dev.js.JsSymbolResolver;
import com.google.gwt.dev.js.JsUnusedFunctionRemover;
import com.google.gwt.dev.js.JsVerboseNamer;
import com.google.gwt.dev.js.SizeBreakdown;
import com.google.gwt.dev.js.ast.JsBlock;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsForIn;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsLabel;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsNameOf;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsParameter;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsVars;
import com.google.gwt.dev.js.ast.JsVisitor;
import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.dev.util.Empty;
import com.google.gwt.dev.util.Memory;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.arg.OptionOptimize;
import com.google.gwt.dev.util.collect.Lists;
import com.google.gwt.dev.util.collect.Maps;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;
import com.google.gwt.soyc.SoycDashboard;
import com.google.gwt.soyc.io.ArtifactsOutputDirectory;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;

import org.xml.sax.SAXException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Compiles the Java <code>JProgram</code> representation into its corresponding
 * JavaScript source.
 */
public class JavaToJavaScriptCompiler {

  private static class PermutationResultImpl implements PermutationResult {
    private final ArtifactSet artifacts = new ArtifactSet();
    private final byte[][] js;
    private final Permutation permutation;
    private final byte[] serializedSymbolMap;
    private final StatementRanges[] statementRanges;

    public PermutationResultImpl(String[] js, Permutation permutation, SymbolData[] symbolMap,
        StatementRanges[] statementRanges) {
      byte[][] bytes = new byte[js.length][];
      for (int i = 0; i < js.length; ++i) {
        bytes[i] = Util.getBytes(js[i]);
      }
      this.js = bytes;
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

  private static class TreeStatistics extends JVisitor {
    private int nodeCount = 0;

    public int getNodeCount() {
      return nodeCount;
    }

    @Override
    public boolean visit(JNode x, Context ctx) {
      nodeCount++;
      return true;
    }
  }

  private static final String ENUM_NAME_OBFUSCATION_PROPERTY = "compiler.enum.obfuscate.names";

  /**
   * Compiles a particular permutation, based on a precompiled unified AST.
   *
   * @param logger the logger to use
   * @param unifiedAst the result of a
   *          {@link #precompile(TreeLogger, ModuleDef, RebindPermutationOracle, String[], String[], JJSOptions, boolean, PrecompilationMetricsArtifact)}
   * @param permutation the permutation to compile
   * @return the output JavaScript
   * @throws UnableToCompleteException if an error other than
   *           {@link OutOfMemoryError} occurs
   */
  public static PermutationResult compilePermutation(TreeLogger logger, UnifiedAst unifiedAst,
      Permutation permutation) throws UnableToCompleteException {
    JJSOptions options = unifiedAst.getOptions();
    long startTimeMilliseconds = System.currentTimeMillis();

    Event jjsCompilePermutationEvent =
        SpeedTracerLogger.start(CompilerEventType.JJS_COMPILE_PERMUTATION, "name", permutation
            .prettyPrint());

    InternalCompilerException.preload();
    PropertyOracle[] propertyOracles = permutation.getPropertyOracles();
    int permutationId = permutation.getId();
    if (logger.isLoggable(TreeLogger.INFO)) {
      logger.log(TreeLogger.INFO, "Compiling permutation " + permutationId + "...");
    }
    long permStart = System.currentTimeMillis();
    try {
      if (JProgram.isTracingEnabled()) {
        System.out.println("------------------------------------------------------------");
        System.out.println("|                     (new permuation)                     |");
        System.out.println("------------------------------------------------------------");
        System.out.println("Properties: " + permutation.prettyPrint());
      }

      AST ast = unifiedAst.getFreshAst();
      JProgram jprogram = ast.getJProgram();
      JsProgram jsProgram = ast.getJsProgram();

      Map<StandardSymbolData, JsName> symbolTable =
          new TreeMap<StandardSymbolData, JsName>(new SymbolData.ClassIdentComparator());

      ResolveRebinds.exec(jprogram, permutation.getOrderedRebindAnswers());

      // Traverse the AST to figure out which lines are instrumentable for
      // coverage. This has to happen before optimizations because functions might
      // be optimized out; we want those marked as "not executed", not "not
      // instrumentable".
      Multimap<String, Integer> instrumentableLines = null;
      if (System.getProperty("gwt.coverage") != null) {
        instrumentableLines = BaselineCoverageGatherer.exec(jprogram);
      }

      // (4) Optimize the normalized Java AST for each permutation.
      int optimizationLevel = options.getOptimizationLevel();
      if (optimizationLevel == OptionOptimize.OPTIMIZE_LEVEL_DRAFT) {
        draftOptimize(jprogram);
      } else {
        optimize(options, jprogram);
      }

      RemoveEmptySuperCalls.exec(jprogram);

      // (5) "Normalize" the high-level Java tree into a lower-level tree more
      // suited for JavaScript code generation. Don't go reordering these
      // willy-nilly because there are some subtle interdependencies.
      JsoDevirtualizer.exec(jprogram);
      CatchBlockNormalizer.exec(jprogram);
      PostOptimizationCompoundAssignmentNormalizer.exec(jprogram);
      LongCastNormalizer.exec(jprogram);
      LongEmulationNormalizer.exec(jprogram);
      CastNormalizer.exec(jprogram, options.isCastCheckingDisabled());
      ArrayNormalizer.exec(jprogram);
      EqualityNormalizer.exec(jprogram);

      // (6) Perform further post-normalization optimizations
      // Prune everything
      Pruner.exec(jprogram, false);
      // prune all Object.getClass() overrides and replace with inline field ref
      ReplaceGetClassOverrides.exec(jprogram);

      // (7) Generate a JavaScript code DOM from the Java type declarations
      jprogram.typeOracle.recomputeAfterOptimizations();
      JavaToJavaScriptMap jjsmap =
          GenerateJavaScriptAST.exec(jprogram, jsProgram, options.getOutput(), symbolTable,
              propertyOracles);

      // (8) Normalize the JS AST.
      // Fix invalid constructs created during JS AST gen.
      JsNormalizer.exec(jsProgram);

      /*
       * If coverage is enabled, instrument the AST to record location info.
       */
      if (instrumentableLines != null) {
        CoverageInstrumentor.exec(jsProgram, instrumentableLines);
      }

      // Resolve all unresolved JsNameRefs.
      JsSymbolResolver.exec(jsProgram);
      // Move all function definitions to a top-level scope, to reduce weirdness
      EvalFunctionsAtTopScope.exec(jsProgram, jjsmap);

      // (9) Optimize the JS AST.
      if (optimizationLevel > OptionOptimize.OPTIMIZE_LEVEL_DRAFT) {
        optimizeJs(options, jsProgram);

        /*
         * Coalesce redundant labels in switch statements.
         */
        JsDuplicateCaseFolder.exec(jsProgram);
      }

      /*
       * Creates new variables, must run before code splitter and namer.
       */
      JsStackEmulator.exec(jprogram, jsProgram, propertyOracles, jjsmap);

      /*
       * Work around Safari 5 bug by rewriting a >> b as ~~a >> b.
       *
       * No shifts may be generated after this point.
       */
      JsCoerceIntShift.exec(jsProgram, logger, propertyOracles);

      // (10) Split up the program into fragments
      SyntheticArtifact dependencies = null;
    
      if (options.isRunAsyncEnabled()) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        int fragmentsMerge = 0;
        
        int expectedFragmentCount = options.getFragmentCount();
        if (expectedFragmentCount > 0) {
          // + 1 for left over, + 1 for initial gave us the total number
          // of fragments without splitting.
          fragmentsMerge = jprogram.getRunAsyncs().size() + 2 - expectedFragmentCount;
        } else {
          fragmentsMerge = options.getFragmentsMerge();
        }
        
        // Pick and choose which code splitter to use. Only use the experimental
        // one when the user explicitly decides the project needs fragment
        // merging.
        if (fragmentsMerge > 0) {
          CodeSplitter2.exec(logger, jprogram, jsProgram, jjsmap, fragmentsMerge,
              chooseDependencyRecorder(options.isSoycEnabled(), baos));
        } else {
          CodeSplitter.exec(logger, jprogram, jsProgram, jjsmap, chooseDependencyRecorder(options
              .isSoycEnabled(), baos));
        }
        if (baos.size() == 0 && options.isSoycEnabled()) {
          recordNonSplitDependencies(jprogram, baos);
        }
        if (baos.size() > 0) {
          dependencies =
              new SyntheticArtifact(SoycReportLinker.class, "dependencies" + permutationId
                  + ".xml.gz", baos.toByteArray());
        }
      }

      // detect if browser is ie6 or not known
      boolean isIE6orUnknown = findBooleanProperty(propertyOracles, logger, "user.agent", "ie6",
          true, false, true);

      boolean isSourceMapsEnabled = findBooleanProperty(propertyOracles, logger,
          "compiler.useSourceMaps", "true", true, false, false);
      // (10.5) Obfuscate
      Map<JsName, String> obfuscateMap = Maps.create();
      switch (options.getOutput()) {
        case OBFUSCATED:
          obfuscateMap = JsStringInterner.exec(jprogram, jsProgram, isIE6orUnknown);
          JsObfuscateNamer.exec(jsProgram);
          if (options.isAggressivelyOptimize()) {
            if (JsStackEmulator.getStackMode(propertyOracles) == JsStackEmulator.StackMode.STRIP) {
              boolean changed = false;
              for (int i = 0; i < jsProgram.getFragmentCount(); i++) {
                JsBlock fragment = jsProgram.getFragmentBlock(i);
                changed = JsDuplicateFunctionRemover.exec(jsProgram, fragment) || changed;
              }
              if (changed) {
                JsUnusedFunctionRemover.exec(jsProgram);
                // run again
                JsObfuscateNamer.exec(jsProgram);
              }
            }
          }
          break;
        case PRETTY:
          // We don't intern strings in pretty mode to imprmakeSouove readability
          JsPrettyNamer.exec(jsProgram);
          break;
        case DETAILED:
          obfuscateMap = JsStringInterner.exec(jprogram, jsProgram, isIE6orUnknown);
          JsVerboseNamer.exec(jsProgram);
          break;
        default:
          throw new InternalCompilerException("Unknown output mode");
      }

      // (10.8) Handle cross-island references.
      // No new JsNames or references to JSNames can be introduced after this
      // point.
      HandleCrossFragmentReferences.exec(logger, jsProgram, propertyOracles);

      
      // Verify that SymbolMap is somewhat close to being complete.
      VerifySymbolMap.exec(jsProgram, jjsmap, symbolTable);
      
      // (11) Perform any post-obfuscation normalizations.

      // Work around an IE7 bug,
      // http://code.google.com/p/google-web-toolkit/issues/detail?id=1440
      // note, JsIEBlockTextTransformer now handles restructuring top level
      // blocks, this class now handles non-top level blocks only.
      boolean splitBlocks = isIE6orUnknown;

      if (splitBlocks) {
        JsIEBlockSizeVisitor.exec(jsProgram);
      }
      JsBreakUpLargeVarStatements.exec(jsProgram, propertyOracles);

      // (12) Generate the final output text.
      String[] js = new String[jsProgram.getFragmentCount()];
      StatementRanges[] ranges = new StatementRanges[js.length];
      SizeBreakdown[] sizeBreakdowns =
          options.isSoycEnabled() || options.isCompilerMetricsEnabled()
              ? new SizeBreakdown[js.length] : null;
      List<Map<Range, SourceInfo>> sourceInfoMaps = new ArrayList<Map<Range, SourceInfo>>();
      generateJavaScriptCode(options, jprogram, jsProgram, jjsmap, js, ranges,
          sizeBreakdowns, sourceInfoMaps, splitBlocks, isSourceMapsEnabled);

      PermutationResult toReturn =
          new PermutationResultImpl(js, permutation, makeSymbolMap(symbolTable, jsProgram), ranges);
      CompilationMetricsArtifact compilationMetrics = null;
      // TODO: enable this when ClosureCompiler is enabled
      if (!options.isClosureCompilerEnabled() && options.isCompilerMetricsEnabled()) {
        compilationMetrics = new CompilationMetricsArtifact(permutation.getId());
        compilationMetrics.setCompileElapsedMilliseconds(System.currentTimeMillis()
            - startTimeMilliseconds);
        compilationMetrics.setElapsedMilliseconds(System.currentTimeMillis()
            - ManagementFactory.getRuntimeMXBean().getStartTime());
        compilationMetrics.setJsSize(sizeBreakdowns);
        compilationMetrics.setPermutationDescription(permutation.prettyPrint());
        toReturn.addArtifacts(Lists.create(unifiedAst.getModuleMetrics(), unifiedAst
            .getPrecompilationMetrics(), compilationMetrics));
      }

      // TODO: enable this when ClosureCompiler is enabled
      if (!options.isClosureCompilerEnabled()) {
        toReturn.addArtifacts(makeSoycArtifacts(logger, permutationId, jprogram, js, sizeBreakdowns,
            options.isSoycExtra() ? sourceInfoMaps : null, dependencies, jjsmap, obfuscateMap,
            unifiedAst.getModuleMetrics(), unifiedAst.getPrecompilationMetrics(), compilationMetrics,
            options.isSoycHtmlDisabled()));
      }

      // TODO: enable this when ClosureCompiler is enabled
      if (!options.isClosureCompilerEnabled() && isSourceMapsEnabled) {
        logger.log(TreeLogger.INFO, "Source Maps Enabled");
        toReturn.addArtifacts(SourceMapRecorder.makeSourceMapArtifacts(sourceInfoMaps,
            permutationId));
      }

      logTrackingStats(logger);
      if (logger.isLoggable(TreeLogger.TRACE)) {
        logger.log(TreeLogger.TRACE, "Permutation took " + (System.currentTimeMillis() - permStart)
            + " ms");
      }
      return toReturn;
    } catch (Throwable e) {
      throw CompilationProblemReporter.logAndTranslateException(logger, e);
    } finally {
      jjsCompilePermutationEvent.end();
    }
  }

  /**
   * Look for a selection property in all property oracles.
   */
  public static boolean findBooleanProperty(PropertyOracle[] propertyOracles, TreeLogger logger,
      String name, String valueToFind, boolean valueIfFound, boolean valueIfNotFound,
      boolean valueIfError) {
    boolean toReturn = valueIfNotFound;
    for (PropertyOracle oracle : propertyOracles) {
      try {
        SelectionProperty property = oracle.getSelectionProperty(logger, name);
        if (valueToFind.equals(property.getCurrentValue())) {
          toReturn = valueIfFound;
          break;
        }
      } catch (BadPropertyValueException e) {
        // unknown value play it safe
        toReturn = valueIfError;
        break;
      }
    }
    return toReturn;
  }

  public static UnifiedAst precompile(TreeLogger logger, ModuleDef module,
      RebindPermutationOracle rpo, String[] declEntryPts, String[] additionalRootTypes,
      JJSOptions options, boolean singlePermutation) throws UnableToCompleteException {
    return precompile(logger, module, rpo, declEntryPts, additionalRootTypes, options,
        singlePermutation, null);
  }

  /**
   * Performs a precompilation, returning a unified AST.
   *
   * @param logger the logger to use
   * @param module the module to compile
   * @param rpo the RebindPermutationOracle
   * @param declEntryPts the set of entry classes declared in a GWT module;
   *          these will be automatically rebound
   * @param additionalRootTypes additional classes that should serve as code
   *          roots; will not be rebound; may be <code>null</code>
   * @param options the compiler options
   * @param singlePermutation if true, do not pre-optimize the resulting AST or
   *          allow serialization of the result
   * @param precompilationMetrics if not null, gather diagnostic information
   *          from this build for a report.
   * @return the unified AST used to drive permutation compiles
   * @throws UnableToCompleteException if an error other than
   *           {@link OutOfMemoryError} occurs
   */
  public static UnifiedAst precompile(TreeLogger logger, ModuleDef module,
      RebindPermutationOracle rpo, String[] declEntryPts, String[] additionalRootTypes,
      JJSOptions options, boolean singlePermutation,
      PrecompilationMetricsArtifact precompilationMetrics) throws UnableToCompleteException {

    InternalCompilerException.preload();

    if (additionalRootTypes == null) {
      additionalRootTypes = Empty.STRINGS;
    }
    if (declEntryPts.length + additionalRootTypes.length == 0) {
      throw new IllegalArgumentException("entry point(s) required");
    }

    Set<String> allRootTypes = new TreeSet<String>();

    // Find all the possible rebinds for declared entry point types.
    for (String element : declEntryPts) {
      String[] all = rpo.getAllPossibleRebindAnswers(logger, element);
      Collections.addAll(allRootTypes, all);
    }
    rpo.getGeneratorContext().finish(logger);
    Collections.addAll(allRootTypes, additionalRootTypes);
    allRootTypes.addAll(JProgram.CODEGEN_TYPES_SET);
    allRootTypes.addAll(JProgram.INDEX_TYPES_SET);
    /*
     * Add all SingleJsoImpl types that we know about. It's likely that the
     * concrete types are never explicitly referenced.
     */
    TypeOracle typeOracle = rpo.getCompilationState().getTypeOracle();
    for (com.google.gwt.core.ext.typeinfo.JClassType singleJsoIntf : typeOracle
        .getSingleJsoImplInterfaces()) {
      allRootTypes.add(typeOracle.getSingleJsoImpl(singleJsoIntf).getQualifiedSourceName());
    }

    Memory.maybeDumpMemory("CompStateBuilt");

    JProgram jprogram = new JProgram();
    JsProgram jsProgram = new JsProgram();

    try {
      // (2) Assemble the Java AST.
      UnifyAst unifyAst = new UnifyAst(jprogram, jsProgram, options, rpo);
      unifyAst.addRootTypes(allRootTypes);
      // TODO: move this into UnifyAst?
      findEntryPoints(logger, rpo, declEntryPts, jprogram);
      unifyAst.exec(logger);

      List<String> finalTypeOracleTypes = Lists.create();
      if (precompilationMetrics != null) {
        for (com.google.gwt.core.ext.typeinfo.JClassType type : typeOracle.getTypes()) {
          finalTypeOracleTypes =
              Lists.add(finalTypeOracleTypes, type.getPackage().getName() + "." + type.getName());
        }
        precompilationMetrics.setFinalTypeOracleTypes(finalTypeOracleTypes);
      }

      // Free up memory.
      rpo.clear();

      if (options.isSoycEnabled()) {
        SourceInfoCorrelator.exec(jprogram);
      }

      // Compute all super type/sub type info
      jprogram.typeOracle.computeBeforeAST();

      Memory.maybeDumpMemory("AstOnly");
      AstDumper.maybeDumpAST(jprogram);

      // See if we should run the EnumNameObfuscator
      if (module != null) {
        ConfigurationProperty enumNameObfuscationProp =
            (ConfigurationProperty) module.getProperties().find(ENUM_NAME_OBFUSCATION_PROPERTY);
        if (enumNameObfuscationProp != null
            && Boolean.parseBoolean(enumNameObfuscationProp.getValue())) {
          EnumNameObfuscator.exec(jprogram, logger);
        }
      }

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

      // Fix up GWT.runAsync()
      if (module != null && options.isRunAsyncEnabled()) {
        ReplaceRunAsyncs.exec(logger, jprogram);
        CodeSplitter2.pickInitialLoadSequence(logger, jprogram, module.getProperties());
      }

      ImplementClassLiteralsAsFields.exec(jprogram);

      /*
       * 4) Possibly optimize some.
       *
       * Don't optimize early if this is a draft compile, or if there's only one
       * permutation.
       */
      if (options.getOptimizationLevel() > OptionOptimize.OPTIMIZE_LEVEL_DRAFT
          && !singlePermutation) {
        if (options.isOptimizePrecompile()) {
          /*
           * Go ahead and optimize early, so that each permutation will run
           * faster. This code path is used by the Compiler entry point. We
           * assume that we will not be able to perfectly parallelize the
           * permutation compiles, so let's optimize as much as possible the
           * common AST. In some cases, this might also have the side benefit of
           * reducing the total permutation count.
           */
          optimize(options, jprogram);
        } else {
          /*
           * Do only minimal early optimizations. This code path is used by the
           * Precompile entry point. The external system might be able to
           * perfectly parallelize the permutation compiles, so let's avoid
           * doing potentially superlinear optimizations on the unified AST.
           */
          optimizeLoop("Early Optimization", jprogram, false);
        }
      }

      Set<String> rebindRequests = new HashSet<String>();
      RecordRebinds.exec(jprogram, rebindRequests);

      if (options.isCompilerMetricsEnabled()) {
        precompilationMetrics.setAstTypes(getReferencedJavaClasses(jprogram));
      }

      logTrackingStats(logger);

      Event createUnifiedAstEvent = SpeedTracerLogger.start(CompilerEventType.CREATE_UNIFIED_AST);
      UnifiedAst result =
          new UnifiedAst(options, new AST(jprogram, jsProgram), singlePermutation, rebindRequests);
      createUnifiedAstEvent.end();
      return result;
    } catch (Throwable e) {
      throw CompilationProblemReporter.logAndTranslateException(logger, e);
    } finally {
    }
  }

  /**
   * Perform the minimal amount of optimization to make sure the compile
   * succeeds.
   */
  protected static void draftOptimize(JProgram jprogram) {
    Event draftOptimizeEvent = SpeedTracerLogger.start(CompilerEventType.DRAFT_OPTIMIZE);
    Finalizer.exec(jprogram);
    MakeCallsStatic.exec(jprogram);
    jprogram.typeOracle.recomputeAfterOptimizations();
    // needed for certain libraries that depend on dead stripping to work
    DeadCodeElimination.exec(jprogram);
    Pruner.exec(jprogram, true);
    jprogram.typeOracle.recomputeAfterOptimizations();
    draftOptimizeEvent.end();
  }

  protected static void optimize(JJSOptions options, JProgram jprogram) throws InterruptedException {
    Event optimizeEvent = SpeedTracerLogger.start(CompilerEventType.OPTIMIZE);

    List<OptimizerStats> allOptimizerStats = new ArrayList<OptimizerStats>();
    int counter = 0;
    int optimizationLevel = options.getOptimizationLevel();
    while (true) {
      counter++;
      if (optimizationLevel < OptionOptimize.OPTIMIZE_LEVEL_MAX && counter > optimizationLevel) {
        break;
      }
      if (Thread.interrupted()) {
        optimizeEvent.end();
        throw new InterruptedException();
      }
      AstDumper.maybeDumpAST(jprogram);
      OptimizerStats stats =
          optimizeLoop("Pass " + counter, jprogram, options.isAggressivelyOptimize());
      allOptimizerStats.add(stats);
      if (!stats.didChange()) {
        break;
      }
    }

    if (options.isAggressivelyOptimize()) {
      // Just run it once, because it is very time consuming
      allOptimizerStats.add(DataflowOptimizer.exec(jprogram));
    }

    if (JProgram.isTracingEnabled()) {
      System.out.println("");
      System.out.println("                Java Optimization Stats");
      System.out.println("");
      for (OptimizerStats stats : allOptimizerStats) {
        System.out.println(stats.prettyPrint());
      }
    }

    optimizeEvent.end();
  }

  protected static void optimizeJs(JJSOptions options, JsProgram jsProgram)
      throws InterruptedException {
    List<OptimizerStats> allOptimizerStats = new ArrayList<OptimizerStats>();
    int counter = 0;
    while (true) {
      counter++;
      if (Thread.interrupted()) {
        throw new InterruptedException();
      }
      Event optimizeJsEvent = SpeedTracerLogger.start(CompilerEventType.OPTIMIZE_JS);

      OptimizerStats stats = new OptimizerStats("Pass " + counter);

      // Remove unused functions, possible
      stats.add(JsStaticEval.exec(jsProgram));
      // Inline JavaScript function invocations
      stats.add(JsInliner.exec(jsProgram));
      // Remove unused functions, possible
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

    if (JProgram.isTracingEnabled()) {
      System.out.println("");
      System.out.println("               JavaScript Optimization Stats");
      System.out.println("");
      for (OptimizerStats stats : allOptimizerStats) {
        System.out.println(stats.prettyPrint());
      }
    }
  }

  protected static OptimizerStats optimizeLoop(String passName, JProgram jprogram,
      boolean isAggressivelyOptimize) {
    Event optimizeEvent = SpeedTracerLogger.start(CompilerEventType.OPTIMIZE, "phase", "loop");

    // Count the number of nodes in the AST so we can measure the efficiency of
    // the optimizers.
    Event countEvent = SpeedTracerLogger.start(CompilerEventType.OPTIMIZE, "phase", "countNodes");
    TreeStatistics treeStats = new TreeStatistics();
    treeStats.accept(jprogram);
    int numNodes = treeStats.getNodeCount();
    countEvent.end();

    // Recompute clinits each time, they can become empty.
    jprogram.typeOracle.recomputeAfterOptimizations();
    // jprogram.methodOracle =
    // MethodOracleBuilder.buildMethodOracle(jprogram);
    OptimizerStats stats = new OptimizerStats(passName);

    // Remove unreferenced types, fields, methods, [params, locals]
    stats.add(Pruner.exec(jprogram, true).recordVisits(numNodes));

    // finalize locals, params, fields, methods, classes
    stats.add(Finalizer.exec(jprogram).recordVisits(numNodes));

    // rewrite non-polymorphic calls as static calls; update all call sites
    stats.add(MakeCallsStatic.exec(jprogram).recordVisits(numNodes));

    // type flow tightening
    // - fields, locals based on assignment
    // - params based on assignment and call sites
    // - method bodies based on return statements
    // - polymorphic methods based on return types of all implementors
    // - optimize casts and instance of
    stats.add(TypeTightener.exec(jprogram).recordVisits(numNodes));

    // tighten method call bindings
    stats.add(MethodCallTightener.exec(jprogram).recordVisits(numNodes));

    // dead code removal??
    stats.add(DeadCodeElimination.exec(jprogram).recordVisits(numNodes));

    // inlining
    stats.add(MethodInliner.exec(jprogram).recordVisits(numNodes));

    if (isAggressivelyOptimize) {
      // remove same parameters value
      stats.add(SameParameterValueOptimizer.exec(jprogram).recordVisits(numNodes));

      /*
       * Enum ordinalization.
       *
       * TODO(jbrosenberg): graduate this out of the 'isAggressivelyOptimize'
       * block, over time.
       */
      stats.add(EnumOrdinalizer.exec(jprogram).recordVisits(numNodes));
    }

    // prove that any types that have been culled from the main tree are
    // unreferenced due to type tightening?

    optimizeEvent.end();
    return stats;
  }

  private static MultipleDependencyGraphRecorder chooseDependencyRecorder(boolean soycEnabled,
      OutputStream out) {
    MultipleDependencyGraphRecorder dependencyRecorder = CodeSplitter.NULL_RECORDER;
    if (soycEnabled) {
      dependencyRecorder = new DependencyRecorder(out);
    }
    return dependencyRecorder;
  }

  private static JMethodCall createReboundModuleLoad(TreeLogger logger, SourceInfo info,
      JDeclaredType reboundEntryType, String originalMainClassName, JDeclaredType enclosingType)
      throws UnableToCompleteException {
    if (!(reboundEntryType instanceof JClassType)) {
      logger.log(TreeLogger.ERROR, "Module entry point class '" + originalMainClassName
          + "' must be a class", null);
      throw new UnableToCompleteException();
    }

    JClassType entryClass = (JClassType) reboundEntryType;
    if (entryClass.isAbstract()) {
      logger.log(TreeLogger.ERROR, "Module entry point class '" + originalMainClassName
          + "' must not be abstract", null);
      throw new UnableToCompleteException();
    }

    JMethod entryMethod = findMainMethodRecurse(entryClass);
    if (entryMethod == null) {
      logger.log(TreeLogger.ERROR,
          "Could not find entry method 'onModuleLoad()' method in entry point class '"
              + originalMainClassName + "'", null);
      throw new UnableToCompleteException();
    }

    if (entryMethod.isAbstract()) {
      logger.log(TreeLogger.ERROR, "Entry method 'onModuleLoad' in entry point class '"
          + originalMainClassName + "' must not be abstract", null);
      throw new UnableToCompleteException();
    }

    JExpression qualifier = null;
    if (!entryMethod.isStatic()) {
      qualifier = JGwtCreate.createInstantiationExpression(info, entryClass, enclosingType);

      if (qualifier == null) {
        logger.log(TreeLogger.ERROR,
            "No default (zero argument) constructor could be found in entry point class '"
                + originalMainClassName
                + "' to qualify a call to non-static entry method 'onModuleLoad'", null);
        throw new UnableToCompleteException();
      }
    }
    return new JMethodCall(info, qualifier, entryMethod);
  }

  private static void findEntryPoints(TreeLogger logger, RebindPermutationOracle rpo,
      String[] mainClassNames, JProgram program) throws UnableToCompleteException {
    Event findEntryPointsEvent = SpeedTracerLogger.start(CompilerEventType.FIND_ENTRY_POINTS);
    JMethod bootStrapMethod = program.getIndexedMethod("EntryMethodHolder.init");

    JMethodBody body = (JMethodBody) bootStrapMethod.getBody();
    JBlock block = body.getBlock();
    SourceInfo info = block.getSourceInfo().makeChild();

    // Also remember $entry, which we'll handle specially in GenerateJsAst
    JMethod registerEntry = program.getIndexedMethod("Impl.registerEntry");
    program.addEntryMethod(registerEntry);

    for (String mainClassName : mainClassNames) {
      block.addStmt(makeStatsCalls(info, program, mainClassName));
      JDeclaredType mainType = program.getFromTypeMap(mainClassName);

      if (mainType == null) {
        logger.log(TreeLogger.ERROR, "Could not find module entry point class '" + mainClassName
            + "'", null);
        throw new UnableToCompleteException();
      }

      JMethod mainMethod = findMainMethod(mainType);
      if (mainMethod != null && mainMethod.isStatic()) {
        JMethodCall onModuleLoadCall = new JMethodCall(info, null, mainMethod);
        block.addStmt(onModuleLoadCall.makeStatement());
        continue;
      }

      // Couldn't find a static main method; must rebind the class
      String[] resultTypeNames = rpo.getAllPossibleRebindAnswers(logger, mainClassName);
      List<JClassType> resultTypes = new ArrayList<JClassType>();
      List<JExpression> entryCalls = new ArrayList<JExpression>();
      for (String resultTypeName : resultTypeNames) {
        JDeclaredType resultType = program.getFromTypeMap(resultTypeName);
        if (resultType == null) {
          logger.log(TreeLogger.ERROR, "Could not find module entry point class '" + resultTypeName
              + "' after rebinding from '" + mainClassName + "'", null);
          throw new UnableToCompleteException();
        }

        JMethodCall onModuleLoadCall =
            createReboundModuleLoad(logger, info, resultType, mainClassName, bootStrapMethod
                .getEnclosingType());
        resultTypes.add((JClassType) resultType);
        entryCalls.add(onModuleLoadCall);
      }
      if (resultTypes.size() == 1) {
        block.addStmt(entryCalls.get(0).makeStatement());
      } else {
        JReboundEntryPoint reboundEntryPoint =
            new JReboundEntryPoint(info, mainType, resultTypes, entryCalls);
        block.addStmt(reboundEntryPoint);
      }
    }
    program.addEntryMethod(bootStrapMethod);
    findEntryPointsEvent.end();
  }

  private static JMethod findMainMethod(JDeclaredType declaredType) {
    for (JMethod method : declaredType.getMethods()) {
      if (method.getName().equals("onModuleLoad")) {
        if (method.getParams().size() == 0) {
          return method;
        }
      }
    }
    return null;
  }

  private static JMethod findMainMethodRecurse(JDeclaredType declaredType) {
    for (JDeclaredType it = declaredType; it != null; it = it.getSuperClass()) {
      JMethod result = findMainMethod(it);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  /**
   * Generate JavaScript code from the given JavaScript ASTs. Also produces
   * information about that transformation.
   *
   * @param options The options this compiler instance is running with
   * @param jprogram The original Java program AST
   * @param jsProgram The AST to convert to source code
   * @param jjsMap A map between the JavaScript AST and the Java AST it came
*          from
   * @param js An array to hold the output JavaScript
   * @param ranges An array to hold the statement ranges for that JavaScript
   * @param sizeBreakdowns An array to hold the size breakdowns for that
*          JavaScript
   * @param sourceInfoMaps An array to hold the source info maps for that
*          JavaScript
   * @param splitBlocks true if current permutation is for IE6 or unknown
   * @param sourceMapsEnabled
   */
  private static void generateJavaScriptCode(JJSOptions options,
      JProgram jprogram, JsProgram jsProgram,
      JavaToJavaScriptMap jjsMap, String[] js, StatementRanges[] ranges,
      SizeBreakdown[] sizeBreakdowns,
      List<Map<Range, SourceInfo>> sourceInfoMaps,
      boolean splitBlocks, boolean sourceMapsEnabled) {

    boolean useClosureCompiler = options.isClosureCompilerEnabled();
    if (useClosureCompiler) {
      ClosureJsRunner runner = new ClosureJsRunner();
      runner.compile(jprogram, jsProgram, js, options.getOutput());
      return;
    }

    for (int i = 0; i < js.length; i++) {
      DefaultTextOutput out = new DefaultTextOutput(options.getOutput().shouldMinimize());
      JsSourceGenerationVisitorWithSizeBreakdown v;

      if (sourceInfoMaps != null) {
        v = new JsReportGenerationVisitor(out, jjsMap);
      } else {
        v = new JsSourceGenerationVisitorWithSizeBreakdown(out, jjsMap);
      }
      v.accept(jsProgram.getFragmentBlock(i));

      StatementRanges statementRanges = v.getStatementRanges();
      String code = out.toString();
      Map<Range, SourceInfo> infoMap = (sourceInfoMaps != null) ? v.getSourceInfoMap() : null;

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
       * Reorder function decls to improve compression ratios. Also restructures
       * the top level blocks into sub-blocks if they exceed 32767 statements.
       */
      Event functionClusterEvent = SpeedTracerLogger.start(CompilerEventType.FUNCTION_CLUSTER);
      // TODO(cromwellian) move to the Js AST, re-enable sourcemaps + clustering
      if (!sourceMapsEnabled
          && options.isAggressivelyOptimize()
          // only cluster for obfuscated mode
          && options.getOutput() == JsOutputOption.OBFUSCATED) {
        transformer = new JsFunctionClusterer(transformer);
        transformer.exec();
      }
      functionClusterEvent.end();

      // rewrite top-level blocks to limit the number of statements
      if (!sourceMapsEnabled && splitBlocks) {
        transformer = new JsIEBlockTextTransformer(transformer);
        transformer.exec();
      }

      js[i] = transformer.getJs();
      ranges[i] = transformer.getStatementRanges();
      if (sizeBreakdowns != null) {
        sizeBreakdowns[i] = v.getSizeBreakdown();
      }
      if (sourceInfoMaps != null) {
        sourceInfoMaps.add(transformer.getSourceInfoMap());
      }
    }
  }

  /**
   * This method can be used to fetch the list of referenced classs.
   *
   * This method is intended to support compiler metrics in the precompile
   * phase.
   */
  private static String[] getReferencedJavaClasses(JProgram jprogram) {
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

  /*
   * This method is intended as a central location for producing optional
   * tracking output. This will be called after all optimization/normalization
   * passes have completed.
   */
  private static void logTrackingStats(TreeLogger logger) {
    EnumOrdinalizer.Tracker eot = EnumOrdinalizer.getTracker();
    if (eot != null) {
      eot.logResultsDetailed(logger, TreeLogger.WARN);
    }
  }

  private static Collection<? extends Artifact<?>> makeSoycArtifacts(TreeLogger logger,
      int permutationId, JProgram jprogram, String[] js, SizeBreakdown[] sizeBreakdowns,
      List<Map<Range, SourceInfo>> sourceInfoMaps, SyntheticArtifact dependencies,
      JavaToJavaScriptMap jjsmap, Map<JsName, String> obfuscateMap,
      ModuleMetricsArtifact moduleMetricsArtifact,
      PrecompilationMetricsArtifact precompilationMetricsArtifact,
      CompilationMetricsArtifact compilationMetrics, boolean htmlReportsDisabled)
      throws IOException, UnableToCompleteException {
    Memory.maybeDumpMemory("makeSoycArtifactsStart");
    List<SyntheticArtifact> soycArtifacts = new ArrayList<SyntheticArtifact>();

    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    Event soycEvent = SpeedTracerLogger.start(CompilerEventType.MAKE_SOYC_ARTIFACTS);

    Event recordSplitPoints =
        SpeedTracerLogger
            .start(CompilerEventType.MAKE_SOYC_ARTIFACTS, "phase", "recordSplitPoints");
    SplitPointRecorder.recordSplitPoints(jprogram, baos, logger);
    SyntheticArtifact splitPoints =
        new SyntheticArtifact(SoycReportLinker.class, "splitPoints" + permutationId + ".xml.gz",
            baos.toByteArray());
    soycArtifacts.add(splitPoints);
    recordSplitPoints.end();

    SyntheticArtifact sizeMaps = null;
    if (sizeBreakdowns != null) {
      Event recordSizeMap =
          SpeedTracerLogger.start(CompilerEventType.MAKE_SOYC_ARTIFACTS, "phase", "recordSizeMap");
      baos.reset();
      SizeMapRecorder.recordMap(logger, baos, sizeBreakdowns, jjsmap, obfuscateMap);
      sizeMaps =
          new SyntheticArtifact(SoycReportLinker.class, "stories" + permutationId + ".xml.gz", baos
              .toByteArray());
      soycArtifacts.add(sizeMaps);
      recordSizeMap.end();
    }

    if (sourceInfoMaps != null) {
      Event recordStories =
          SpeedTracerLogger.start(CompilerEventType.MAKE_SOYC_ARTIFACTS, "phase", "recordStories");
      baos.reset();
      StoryRecorder.recordStories(logger, baos, sourceInfoMaps, js);
      soycArtifacts.add(new SyntheticArtifact(SoycReportLinker.class, "detailedStories"
          + permutationId + ".xml.gz", baos.toByteArray()));
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
      Event generateCompileReport =
          SpeedTracerLogger.start(CompilerEventType.MAKE_SOYC_ARTIFACTS, "phase",
              "generateCompileReport");
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
        dashboard.generateCompilerMetricsForOnePermuation(moduleMetricsArtifact,
            precompilationMetricsArtifact, compilationMetrics);
      }
      soycArtifacts.addAll(outDir.getArtifacts());
      generateCompileReport.end();
    }

    soycEvent.end();

    return soycArtifacts;
  }

  /**
   * Create a variable assignment to invoke a call to the statistics collector.
   *
   * <pre>
   * Stats.isStatsAvailable() &&
   *   Stats.onModuleStart("mainClassName");
   * </pre>
   */
  private static JStatement makeStatsCalls(SourceInfo info, JProgram program, String mainClassName) {
    JMethod isStatsAvailableMethod = program.getIndexedMethod("Stats.isStatsAvailable");
    JMethod onModuleStartMethod = program.getIndexedMethod("Stats.onModuleStart");

    JMethodCall availableCall = new JMethodCall(info, null, isStatsAvailableMethod);
    JMethodCall onModuleStartCall = new JMethodCall(info, null, onModuleStartMethod);
    onModuleStartCall.addArg(program.getLiteralString(info, mainClassName));

    JBinaryOperation amp =
        new JBinaryOperation(info, program.getTypePrimitiveBoolean(), JBinaryOperator.AND,
            availableCall, onModuleStartCall);

    return amp.makeStatement();
  }

  private static SymbolData[] makeSymbolMap(Map<StandardSymbolData, JsName> symbolTable,
      JsProgram jsProgram) {


    // Keep tracks of a list of referenced name. If it is not used, don't
    // add it to symbol map.
    final Set<String> nameUsed = new HashSet<String>();
    final Map<JsName, Integer> nameToFragment = new HashMap<JsName, Integer>();

    for (int i = 0; i < jsProgram.getFragmentCount(); i++) {
      final Integer fragId = i;
      new JsVisitor() {
        @Override
        public void endVisit(JsFunction x, JsContext ctx) {
            if (x.getName() != null) {
              nameToFragment.put(x.getName(), fragId);
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
        public void endVisit(JsNameOf x, JsContext ctx) {
          if (x.getName() != null) {
            nameUsed.add(x.getName().getIdent());
          }
        }
        
        @Override
        public void endVisit(JsForIn x, JsContext ctx) {
          if (x.getIterVarName() != null) {
            nameUsed.add(x.getIterVarName().getIdent());
          }
        }

        @Override
        public void endVisit(JsLabel x, JsContext ctx) {
          nameUsed.add(x.getName().getIdent());
        };

        @Override
        public void endVisit(JsParameter x, JsContext ctx) {
          nameUsed.add(x.getName().getIdent());
        };

        @Override
        public void endVisit(JsVars.JsVar x, JsContext ctx) {
          nameUsed.add(x.getName().getIdent());
        };

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
      if (nameUsed.contains(entry.getValue().getIdent())) {
        result.add(symbolData);
      }
    }

    return result.toArray(new SymbolData[result.size()]);
  }

  /**
   * Open an emitted artifact and gunzip its contents.
   */
  private static GZIPInputStream openWithGunzip(EmittedArtifact artifact) throws IOException,
      UnableToCompleteException {
    return new GZIPInputStream(artifact.getContents(TreeLogger.NULL));
  }

  /**
   * Dependency information is normally recorded during code splitting, and it
   * results in multiple dependency graphs. If the code splitter doesn't run,
   * then this method can be used instead to record a single dependency graph
   * for the whole program.
   */
  private static void recordNonSplitDependencies(JProgram program, OutputStream out) {
    DependencyRecorder deps = new DependencyRecorder(out);
    deps.open();
    deps.startDependencyGraph("initial", null);

    ControlFlowAnalyzer cfa = new ControlFlowAnalyzer(program);
    cfa.setDependencyRecorder(deps);
    cfa.traverseEntryMethods();
    deps.endDependencyGraph();
    deps.close();
  }
}
