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

import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.impl.ResourceGeneratorUtilImpl;
import com.google.gwt.core.ext.linker.PrecompilationMetricsArtifact;
import com.google.gwt.core.ext.linker.SyntheticArtifact;
import com.google.gwt.dev.CompilerContext;
import com.google.gwt.dev.Permutation;
import com.google.gwt.dev.cfg.BindingProperty;
import com.google.gwt.dev.cfg.ConfigurationProperty;
import com.google.gwt.dev.cfg.PropertyProviderRegistratorGenerator;
import com.google.gwt.dev.cfg.Rule;
import com.google.gwt.dev.cfg.RuleGenerateWith;
import com.google.gwt.dev.cfg.RuleReplaceWithFallback;
import com.google.gwt.dev.cfg.Rules;
import com.google.gwt.dev.cfg.RuntimeRebindRegistratorGenerator;
import com.google.gwt.dev.javac.CompilationUnit;
import com.google.gwt.dev.javac.CompiledClass;
import com.google.gwt.dev.javac.StandardGeneratorContext;
import com.google.gwt.dev.jdt.RebindPermutationOracle;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JLiteral;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.impl.ArrayNormalizer;
import com.google.gwt.dev.jjs.impl.CatchBlockNormalizer;
import com.google.gwt.dev.jjs.impl.ComputeExhaustiveCastabilityInformation;
import com.google.gwt.dev.jjs.impl.DeadCodeElimination;
import com.google.gwt.dev.jjs.impl.EqualityNormalizer;
import com.google.gwt.dev.jjs.impl.Finalizer;
import com.google.gwt.dev.jjs.impl.ImplementCastsAndTypeChecks;
import com.google.gwt.dev.jjs.impl.JavaToJavaScriptMap;
import com.google.gwt.dev.jjs.impl.Devirtualizer;
import com.google.gwt.dev.jjs.impl.LongCastNormalizer;
import com.google.gwt.dev.jjs.impl.LongEmulationNormalizer;
import com.google.gwt.dev.jjs.impl.PostOptimizationCompoundAssignmentNormalizer;
import com.google.gwt.dev.jjs.impl.ReboundTypeRecorder;
import com.google.gwt.dev.jjs.impl.ReplaceGetClassOverrides;
import com.google.gwt.dev.jjs.impl.ResolveRuntimeTypeReferences;
import com.google.gwt.dev.jjs.impl.TypeCoercionNormalizer;
import com.google.gwt.dev.jjs.impl.codesplitter.MultipleDependencyGraphRecorder;
import com.google.gwt.dev.js.JsVerboseNamer;
import com.google.gwt.dev.js.ast.JsLiteral;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsNode;
import com.google.gwt.dev.resource.impl.FileResource;
import com.google.gwt.dev.util.Name.BinaryName;
import com.google.gwt.dev.util.Pair;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;
import com.google.gwt.thirdparty.guava.common.base.Predicate;
import com.google.gwt.thirdparty.guava.common.collect.HashMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;
import com.google.gwt.thirdparty.guava.common.collect.SetMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;

import javax.annotation.Nullable;

/**
 * Compiles the Java <code>JProgram</code> representation into its corresponding library Js source.
 * <br />
 *
 * Care is taken to ensure that the resulting Js source will be valid for runtime linking, such as
 * performing only local optimizations, running only local stages of Generators, gathering and
 * enqueueing rebind information for runtime usage and outputting Js source with names that are
 * stable across libraries.
 */
public class LibraryJavaToJavaScriptCompiler extends JavaToJavaScriptCompiler {

  // VisibleForTesting
  class LibraryPermutationCompiler extends PermutationCompiler {

    public LibraryPermutationCompiler(Permutation permutation) {
      super(permutation);
    }

    @Override
    protected Map<JType, JLiteral> normalizeSemantics() {
      Devirtualizer.exec(jprogram);
      CatchBlockNormalizer.exec(jprogram);
      PostOptimizationCompoundAssignmentNormalizer.exec(jprogram);
      LongCastNormalizer.exec(jprogram);
      LongEmulationNormalizer.exec(jprogram);
      TypeCoercionNormalizer.exec(jprogram);
      ComputeExhaustiveCastabilityInformation.exec(jprogram, options.isCastCheckingDisabled());
      ImplementCastsAndTypeChecks.exec(jprogram, options.isCastCheckingDisabled());
      ArrayNormalizer.exec(jprogram, options.isCastCheckingDisabled());
      EqualityNormalizer.exec(jprogram);
      return ResolveRuntimeTypeReferences.IntoStringLiterals.exec(jprogram);
    }

    @Override
    protected void optimizeJava() {
      Event draftOptimizeEvent = SpeedTracerLogger.start(CompilerEventType.DRAFT_OPTIMIZE);
      Finalizer.exec(jprogram);
      jprogram.typeOracle.recomputeAfterOptimizations();
      // Certain libraries depend on dead stripping.
      DeadCodeElimination.exec(jprogram);
      jprogram.typeOracle.recomputeAfterOptimizations();
      draftOptimizeEvent.end();
    }

    @Override
    protected void optimizeJs(Set<JsNode> inlinableJsFunctions)throws InterruptedException {
    }

    @Override
    protected void postNormalizationOptimizeJava() {
      // Does not prune types and functions when constructing a library since final runtime usage
      // can not be predicted.
      ReplaceGetClassOverrides.exec(jprogram);
    }

    @Override
    protected Map<JsName, JsLiteral> runDetailedNamer(PropertyOracle[] propertyOracles) {
      JsVerboseNamer.exec(jsProgram, propertyOracles);
      return null;
    }

    @Override
    protected Pair<SyntheticArtifact, MultipleDependencyGraphRecorder> splitJsIntoFragments(
        PropertyOracle[] propertyOracles, int permutationId, JavaToJavaScriptMap jjsmap) {
      // Local control flow knowledge and the local list of RunAsyncs is not enough information to
      // be able to accurately split program fragments.
      return Pair.<SyntheticArtifact, MultipleDependencyGraphRecorder>create(null, null);
    }
  }

  // VisibleForTesting
  class LibraryPrecompiler extends Precompiler {

    private Set<String> badRebindCombinations = Sets.newHashSet();
    private SetMultimap<String, String> generatorNamesByPreviouslyReboundTypeName =
        HashMultimap.create();
    private Set<String> previouslyReboundTypeNames = Sets.newHashSet();

    public LibraryPrecompiler(RebindPermutationOracle rpo) {
      super(rpo);
    }

    @Override
    protected void beforeUnifyAst(Set<String> allRootTypes)
        throws UnableToCompleteException {
      runGeneratorsToFixedPoint(rpo);

      Set<JDeclaredType> reboundTypes = gatherReboundTypes(rpo);
      buildFallbackRuntimeRebindRules(reboundTypes);
      buildSimpleRuntimeRebindRules(module.getRules());

      buildRuntimeRebindRegistrator(allRootTypes);
      buildPropertyProviderRegistrator(allRootTypes, module.getProperties().getBindingProperties(),
          module.getProperties().getConfigurationProperties());
    }

    @Override
    protected void checkEntryPoints(String[] entryPointTypeNames, String[] additionalRootTypes) {
      // Library construction does not need to care whether their are or are not any entry points.
    }

    @Override
    protected void createJProgram() {
      jprogram = new JProgram(false);
    }

    // VisibleForTesting
    // TODO(stalcup): performs much the same load logic as UnifyAst.findType(), but is necessary
    // much earlier. Replace with some single mechanism. This logic only exists to support the
    // ability to analyze whether a type is instantiable prior to creating a rebind rule that
    // attempts to instantiate it. It would be very nice to not be duplicating an instantiability
    // check here that JDT already does quite well during its own compile.
    protected JDeclaredType ensureFullTypeLoaded(JDeclaredType type) {
      String internalName = BinaryName.toInternalName(type.getName());
      Map<String, CompiledClass> compiledClassesByInternalName =
          rpo.getCompilationState().getClassFileMap();
      // If the type is available as compiled source.
      if (compiledClassesByInternalName.containsKey(internalName)) {
        // Get and return it.
        CompiledClass compiledClass = compiledClassesByInternalName.get(internalName);
        return compiledClass.getUnit().getTypeByName(type.getName());
      }
      // Otherwise if the type is available in a loaded library.
      CompilationUnit compilationUnit =
          compilerContext.getLibraryGroup().getCompilationUnitByTypeBinaryName(type.getName());
      if (compilationUnit != null) {
        // Get and return it.
        compilerContext.getUnitCache().add(compilationUnit);
        return compilationUnit.getTypeByName(type.getName());
      }
      return type;
    }

    // VisibleForTesting
    protected Set<JDeclaredType> gatherReboundTypes(RebindPermutationOracle rpo) {
      Collection<CompilationUnit> compilationUnits =
          rpo.getCompilationState().getCompilationUnits();
      Set<JDeclaredType> reboundTypes = Sets.newLinkedHashSet();
      for (CompilationUnit compilationUnit : compilationUnits) {
        for (JDeclaredType type : compilationUnit.getTypes()) {
          ReboundTypeRecorder.exec(type, reboundTypes);
        }
      }
      return reboundTypes;
    }

    protected StandardGeneratorContext getGeneratorContext() {
      return rpo.getGeneratorContext();
    }

    // VisibleForTesting
    protected Set<String> getTypeNames(Set<JDeclaredType> types) {
      Set<String> typeNames = Sets.newHashSet();
      for (JDeclaredType type : types) {
        typeNames.add(type.getName());
      }
      return typeNames;
    }

    @Override
    protected void populateEntryPointRootTypes(
        String[] entryPointTypeNames, Set<String> allRootTypes) {
      Collections.addAll(allRootTypes, entryPointTypeNames);
    }

    /**
     * Runs a particular generator on the provided set of rebound types. Takes care to guard against
     * duplicate work during reruns as generation approaches a fixed point.
     *
     * @return whether a fixed point was reached.
     */
    // VisibleForTesting
    protected boolean runGenerator(RuleGenerateWith generatorRule, Set<String> reboundTypeNames)
        throws UnableToCompleteException {
      boolean fixedPoint = true;
      StandardGeneratorContext generatorContext = getGeneratorContext();
      removePreviouslyReboundCombinations(generatorRule.getName(), reboundTypeNames);
      reboundTypeNames.removeAll(previouslyReboundTypeNames);

      for (String reboundTypeName : reboundTypeNames) {
        if (badRebindCombinations.contains(generatorRule.getName() + "-" + reboundTypeName)) {
          continue;
        }
        generatorNamesByPreviouslyReboundTypeName.put(reboundTypeName, generatorRule.getName());
        reboundTypeName = reboundTypeName.replace("$", ".");
        generatorRule.generate(logger, module.getProperties(), generatorContext, reboundTypeName);

        if (generatorContext.isDirty()) {
          fixedPoint = false;
          previouslyReboundTypeNames.add(reboundTypeName);
          // Ensure that cascading generations rerun properly.
          for (String generatedTypeName : generatorContext.getGeneratedUnitMap().keySet()) {
            generatorNamesByPreviouslyReboundTypeName.removeAll(generatedTypeName);
          }
          generatorContext.finish(logger);
        } else {
          badRebindCombinations.add(generatorRule.getName() + "-" + reboundTypeName);
        }
      }

      return fixedPoint;
    }

    // VisibleForTesting
    protected void runGeneratorsToFixedPoint(RebindPermutationOracle rpo)
        throws UnableToCompleteException {
      boolean fixedPoint;
      do {
        compilerContext.getLibraryWriter()
            .setReboundTypeSourceNames(getTypeNames(gatherReboundTypes(rpo)));

        fixedPoint = runGenerators();
      } while (!fixedPoint);

      // This is a horribly dirty hack to work around the fact that CssResourceGenerator uses a
      // completely nonstandard resource creation and caching mechanism that ignores the
      // GeneratorContext infrastructure. It and GenerateCssAst need to be fixed.
      for (Entry<String, File> entry :
          ResourceGeneratorUtilImpl.getGeneratedFilesByName().entrySet()) {
        String resourcePath = entry.getKey();
        File resourceFile = entry.getValue();
        compilerContext.getLibraryWriter()
            .addBuildResource(new FileResource(null, resourcePath, resourceFile));
      }
    }

    // VisibleForTesting
    void buildFallbackRuntimeRebindRules(Set<JDeclaredType> reboundTypes)
        throws UnableToCompleteException {
      // Create fallback rebinds.
      for (JDeclaredType reboundType : reboundTypes) {
        // It's possible for module A to declare rebind rules about types that were defined in
        // module B. While processing module A these types might not be loaded in their full form,
        // which would cause their instantiability analysis to be wrong. So, make sure the full
        // version of each such type has been loaded.
        // TODO(stalcup) find a way to check if a type is instantiable without having to have the
        // full version of the type loaded.
        reboundType = ensureFullTypeLoaded(reboundType);
        if (!reboundType.isInstantiable()) {
          continue;
        }
        RuleReplaceWithFallback fallbackRule =
            new RuleReplaceWithFallback(reboundType.getName().replace("$", "."));
        fallbackRule.generateRuntimeRebindClasses(logger, module, getGeneratorContext());
      }
    }

    // VisibleForTesting
    void buildPropertyProviderRegistrator(Set<String> allRootTypes,
        SortedSet<BindingProperty> bindingProperties,
        SortedSet<ConfigurationProperty> configurationProperties) throws UnableToCompleteException {
      PropertyProviderRegistratorGenerator propertyProviderRegistratorGenerator =
          new PropertyProviderRegistratorGenerator(bindingProperties, configurationProperties);
      StandardGeneratorContext generatorContext = getGeneratorContext();
      // Name based on module canonical name, to avoid collisions resulting from multiple modules
      // with the same rename.
      String propertyProviderRegistratorTypeName = propertyProviderRegistratorGenerator.generate(
          logger, generatorContext, module.getCanonicalName());
      // Ensures that unification traverses and keeps the class.
      allRootTypes.add(propertyProviderRegistratorTypeName);
      // Ensures that JProgram knows to index this class's methods so that later bootstrap
      // construction code is able to locate the FooPropertyProviderRegistrator.register() function.
      jprogram.addIndexedTypeName(propertyProviderRegistratorTypeName);
      jprogram.setPropertyProviderRegistratorTypeSourceName(propertyProviderRegistratorTypeName);
      generatorContext.finish(logger);
    }

    // VisibleForTesting
    void buildRuntimeRebindRegistrator(Set<String> allRootTypes) throws UnableToCompleteException {
      RuntimeRebindRegistratorGenerator runtimeRebindRegistratorGenerator =
          new RuntimeRebindRegistratorGenerator();
      StandardGeneratorContext generatorContext = getGeneratorContext();
      // Name based on module canonical name, to avoid collisions resulting from multiple modules
      // with the same rename.
      String runtimeRebindRegistratorTypeName = runtimeRebindRegistratorGenerator.generate(logger,
          generatorContext, module.getCanonicalName());
      // Ensures that unification traverses and keeps the class.
      allRootTypes.add(runtimeRebindRegistratorTypeName);
      // Ensures that JProgram knows to index this class's methods so that later bootstrap
      // construction code is able to locate the FooRuntimeRebindRegistrator.register() function.
      jprogram.addIndexedTypeName(runtimeRebindRegistratorTypeName);
      jprogram.setRuntimeRebindRegistratorTypeName(runtimeRebindRegistratorTypeName);
      generatorContext.finish(logger);
    }

    // VisibleForTesting
    void buildSimpleRuntimeRebindRules(Rules rules) throws UnableToCompleteException {
      // Create rebinders for rules specified in the module.
      Iterator<Rule> iterator = rules.iterator();
      while (iterator.hasNext()) {
        Rule rule = iterator.next();
        if (rule instanceof RuleGenerateWith) {
          continue;
        }
        rule.generateRuntimeRebindClasses(logger, module, getGeneratorContext());
      }
    }

    private boolean relevantPropertiesHaveChanged(RuleGenerateWith generatorRule) {
      // Gather binding and configuration property values that have been changed in the part of
      // the library dependency tree on which this generator has not yet run.
      Multimap<String, String> newConfigurationPropertyValues =
          compilerContext.gatherNewConfigurationPropertyValuesForGenerator(generatorRule.getName());
      Multimap<String, String> newBindingPropertyValues =
          compilerContext.gatherNewBindingPropertyValuesForGenerator(generatorRule.getName());

      return generatorRule.caresAboutProperties(newConfigurationPropertyValues.keySet())
          || generatorRule.caresAboutProperties(newBindingPropertyValues.keySet());
    }

    /**
     * Generator output can create opportunities for further generator execution, so runGenerators()
     * is repeated to a fixed point. But previously handled generator/reboundType pairs should be
     * ignored.
     */
    private void removePreviouslyReboundCombinations(
        final String generatorName, Set<String> newReboundTypeNames) {
      newReboundTypeNames.removeAll(
          Sets.newHashSet(Sets.filter(newReboundTypeNames, new Predicate<String>() {
            @Override
            public boolean apply(@Nullable String newReboundTypeName) {
              return generatorNamesByPreviouslyReboundTypeName.containsEntry(
                  newReboundTypeName, generatorName);
            }
          })));
    }

    /**
     * Figures out which generators should run based on the current state and runs them. Generator
     * execution can create new opportunities for further generator execution so this function
     * should be invoked repeatedly till a fixed point is reached.<br />
     *
     * Returns whether a fixed point was reached.
     */
    private boolean runGenerators() throws UnableToCompleteException {
      boolean fixedPoint = true;
      boolean globalCompile = compilerContext.getOptions().shouldLink();
      Set<Rule> generatorRules = Sets.newHashSet(module.getGeneratorRules());

      for (Rule rule : generatorRules) {
        RuleGenerateWith generatorRule = (RuleGenerateWith) rule;
        String generatorName = generatorRule.getName();

        if (generatorRule.contentDependsOnTypes() && !globalCompile) {
          // Type unstable generators can only be safely run in the global phase.
          // TODO(stalcup): modify type unstable generators such that their output is no longer
          // unstable.
          continue;
        }

        // Run generator for new rebound types.
        Set<String> newReboundTypeNames =
            compilerContext.gatherNewReboundTypeNamesForGenerator(generatorName);
        fixedPoint &= runGenerator(generatorRule, newReboundTypeNames);

        // If the content of generator output varies when some relevant properties change and some
        // relevant properties have changed.
        if (generatorRule.contentDependsOnProperties()
            && relevantPropertiesHaveChanged(generatorRule)) {
          // Rerun the generator on old rebound types to replace old stale output.
          Set<String> oldReboundTypeNames =
              compilerContext.gatherOldReboundTypeNamesForGenerator(generatorName);
          fixedPoint &= runGenerator(generatorRule, oldReboundTypeNames);
        }

        compilerContext.getLibraryWriter().addRanGeneratorName(generatorName);
      }

      return fixedPoint;
    }
  }

  /**
   * Constructs a JavaToJavaScriptCompiler with customizations for compiling independent libraries.
   */
  public LibraryJavaToJavaScriptCompiler(TreeLogger logger, CompilerContext compilerContext) {
    super(logger, compilerContext);
  }

  @Override
  public PermutationResult compilePermutation(UnifiedAst unifiedAst, Permutation permutation)
      throws UnableToCompleteException {
    return new LibraryPermutationCompiler(permutation).compilePermutation(unifiedAst);
  }

  @Override
  public UnifiedAst precompile(RebindPermutationOracle rpo, String[] entryPointTypeNames,
      String[] additionalRootTypes, boolean singlePermutation,
      PrecompilationMetricsArtifact precompilationMetrics) throws UnableToCompleteException {
    return new LibraryPrecompiler(rpo).precompile(
        entryPointTypeNames, additionalRootTypes, singlePermutation, precompilationMetrics);
  }
}
