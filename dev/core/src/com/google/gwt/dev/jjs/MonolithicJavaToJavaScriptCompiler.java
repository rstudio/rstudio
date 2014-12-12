/*
 * Copyright 2014 Google Inc.
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
import com.google.gwt.core.ext.linker.PrecompilationMetricsArtifact;
import com.google.gwt.core.ext.linker.SyntheticArtifact;
import com.google.gwt.core.ext.soyc.coderef.DependencyGraphRecorder;
import com.google.gwt.core.ext.soyc.impl.DependencyRecorder;
import com.google.gwt.core.linker.SoycReportLinker;
import com.google.gwt.dev.CompilerContext;
import com.google.gwt.dev.Permutation;
import com.google.gwt.dev.cfg.ConfigProps;
import com.google.gwt.dev.cfg.PermProps;
import com.google.gwt.dev.jdt.RebindPermutationOracle;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.impl.ArrayNormalizer;
import com.google.gwt.dev.jjs.impl.CatchBlockNormalizer;
import com.google.gwt.dev.jjs.impl.ComputeCastabilityInformation;
import com.google.gwt.dev.jjs.impl.ComputeExhaustiveCastabilityInformation;
import com.google.gwt.dev.jjs.impl.ComputeInstantiatedJsoInterfaces;
import com.google.gwt.dev.jjs.impl.ControlFlowAnalyzer;
import com.google.gwt.dev.jjs.impl.Devirtualizer;
import com.google.gwt.dev.jjs.impl.EqualityNormalizer;
import com.google.gwt.dev.jjs.impl.HandleCrossFragmentReferences;
import com.google.gwt.dev.jjs.impl.ImplementCastsAndTypeChecks;
import com.google.gwt.dev.jjs.impl.JavaToJavaScriptMap;
import com.google.gwt.dev.jjs.impl.JjsUtils;
import com.google.gwt.dev.jjs.impl.LongCastNormalizer;
import com.google.gwt.dev.jjs.impl.LongEmulationNormalizer;
import com.google.gwt.dev.jjs.impl.PostOptimizationCompoundAssignmentNormalizer;
import com.google.gwt.dev.jjs.impl.Pruner;
import com.google.gwt.dev.jjs.impl.RemoveEmptySuperCalls;
import com.google.gwt.dev.jjs.impl.RemoveSpecializations;
import com.google.gwt.dev.jjs.impl.ReplaceGetClassOverrides;
import com.google.gwt.dev.jjs.impl.ResolveRuntimeTypeReferences;
import com.google.gwt.dev.jjs.impl.ResolveRuntimeTypeReferences.ClosureUniqueIdTypeMapper;
import com.google.gwt.dev.jjs.impl.ResolveRuntimeTypeReferences.IntTypeMapper;
import com.google.gwt.dev.jjs.impl.ResolveRuntimeTypeReferences.StringTypeMapper;
import com.google.gwt.dev.jjs.impl.ResolveRuntimeTypeReferences.TypeMapper;
import com.google.gwt.dev.jjs.impl.ResolveRuntimeTypeReferences.TypeOrder;
import com.google.gwt.dev.jjs.impl.TypeCoercionNormalizer;
import com.google.gwt.dev.jjs.impl.codesplitter.CodeSplitter;
import com.google.gwt.dev.jjs.impl.codesplitter.CodeSplitters;
import com.google.gwt.dev.jjs.impl.codesplitter.MultipleDependencyGraphRecorder;
import com.google.gwt.dev.js.JsDuplicateCaseFolder;
import com.google.gwt.dev.js.JsLiteralInterner;
import com.google.gwt.dev.js.JsNamer.IllegalNameException;
import com.google.gwt.dev.js.JsVerboseNamer;
import com.google.gwt.dev.js.ast.JsLiteral;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsNode;
import com.google.gwt.dev.util.Pair;
import com.google.gwt.dev.util.arg.OptionOptimize;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;

/**
 * Compiles the Java <code>JProgram</code> representation into its corresponding library Js source.
 * <br />
 *
 * Care is taken to ensure that the resulting Js source will be valid for runtime linking, such as
 * performing only local optimizations, running only local stages of Generators, gathering and
 * enqueueing rebind information for runtime usage and outputting Js source with names that are
 * stable across libraries.
 */
public class MonolithicJavaToJavaScriptCompiler extends JavaToJavaScriptCompiler {

  private class MonolithicPermutationCompiler extends PermutationCompiler {

    public MonolithicPermutationCompiler(Permutation permutation) {
      super(permutation);
    }

    @Override
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
          ComputeCastabilityInformation.exec(jprogram, options.isCastCheckingDisabled(),
              !shouldOptimize() /* recordTrivialCasts */);
        }

        ComputeInstantiatedJsoInterfaces.exec(jprogram);
        ImplementCastsAndTypeChecks.exec(jprogram, options.isCastCheckingDisabled(),
            shouldOptimize() /* pruneTrivialCasts */);
        ArrayNormalizer.exec(jprogram, options.isCastCheckingDisabled());
        EqualityNormalizer.exec(jprogram);

        TypeMapper<?> typeMapper = getTypeMapper();
        ResolveRuntimeTypeReferences.exec(jprogram, typeMapper, getTypeOrder());
        return typeMapper;
      } finally {
        event.end();
      }
    }

    @Override
    protected void optimizeJava() throws InterruptedException {
      if (shouldOptimize()) {
        optimizeJavaToFixedPoint();
        RemoveEmptySuperCalls.exec(jprogram);
      }
    }

    @Override
    protected void optimizeJs(Set<JsNode> inlinableJsFunctions) throws InterruptedException {
      if (shouldOptimize()) {
        optimizeJsLoop(inlinableJsFunctions);
        JsDuplicateCaseFolder.exec(jsProgram);
      }
    }

    @Override
    protected void postNormalizationOptimizeJava() {
      Event event = SpeedTracerLogger.start(CompilerEventType.JAVA_POST_NORMALIZER_OPTIMIZERS);
      try {
        if (shouldOptimize()) {
          RemoveSpecializations.exec(jprogram);
          Pruner.exec(jprogram, false);
        }
        ReplaceGetClassOverrides.exec(jprogram);
      } finally {
        event.end();
      }
    }

    @Override
    protected Map<JsName, JsLiteral> runDetailedNamer(ConfigProps config)
        throws IllegalNameException {
      Map<JsName, JsLiteral> internedTextByVariableName = null;
      if (shouldOptimize()) {
        // Only perform the interning optimization when optimizations are enabled.
        internedTextByVariableName =
            JsLiteralInterner.exec(jprogram, jsProgram, (byte) (JsLiteralInterner.INTERN_ALL
                & (byte) (jprogram.typeOracle.isJsInteropEnabled()
                ? ~JsLiteralInterner.INTERN_STRINGS : ~0)));
      }
      JsVerboseNamer.exec(jsProgram, config);
      return internedTextByVariableName;
    }

    @Override
    protected Pair<SyntheticArtifact, MultipleDependencyGraphRecorder> splitJsIntoFragments(
        PermProps props, int permutationId, JavaToJavaScriptMap jjsmap) {
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

        int minFragmentSize = props.getConfigProps().getInteger(CodeSplitters.MIN_FRAGMENT_SIZE, 0);

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
      dependenciesAndRecorder = Pair.<SyntheticArtifact, MultipleDependencyGraphRecorder> create(
          dependencies, dependencyRecorder);

      // No new JsNames or references to JSNames can be introduced after this
      // point.
      HandleCrossFragmentReferences.exec(jsProgram, props);

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
  }

  private class MonolithicPrecompiler extends Precompiler {

    public MonolithicPrecompiler(RebindPermutationOracle rpo, String[] entryPointTypeNames) {
      super(rpo, entryPointTypeNames);
    }

    @Override
    protected void beforeUnifyAst(Set<String> allRootTypes)
        throws UnableToCompleteException {
    }

    @Override
    protected void checkEntryPoints(String[] additionalRootTypes) {
      if (entryPointTypeNames.length + additionalRootTypes.length == 0) {
        throw new IllegalArgumentException("entry point(s) required");
      }
    }

    @Override
    protected void createJProgram(CompilerContext compilerContext) {
      jprogram = new JProgram(compilerContext.getMinimalRebuildCache());
    }
  }

  /**
   * Constructs a JavaToJavaScriptCompiler with customizations for compiling independent libraries.
   */
  public MonolithicJavaToJavaScriptCompiler(TreeLogger logger, CompilerContext compilerContext) {
    super(logger, compilerContext);
  }

  @Override
  public PermutationResult compilePermutation(UnifiedAst unifiedAst, Permutation permutation)
      throws UnableToCompleteException {
    return new MonolithicPermutationCompiler(permutation).compilePermutation(unifiedAst);
  }

  @Override
  public UnifiedAst precompile(RebindPermutationOracle rpo, String[] entryPointTypeNames,
      String[] additionalRootTypes, boolean singlePermutation,
      PrecompilationMetricsArtifact precompilationMetrics) throws UnableToCompleteException {
    return new MonolithicPrecompiler(rpo, entryPointTypeNames).precompile(additionalRootTypes,
        singlePermutation, precompilationMetrics);
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
      return new StringTypeMapper();
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
    return this.options.isIncrementalCompileEnabled() ? TypeOrder.ALPHABETICAL : TypeOrder.FREQUENCY;
  }
}
