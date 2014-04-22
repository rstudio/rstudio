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
import com.google.gwt.dev.cfg.RuntimeRebindRuleGenerator;
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
import com.google.gwt.dev.jjs.impl.ComputeInstantiatedJsoInterfaces;
import com.google.gwt.dev.jjs.impl.Devirtualizer;
import com.google.gwt.dev.jjs.impl.EqualityNormalizer;
import com.google.gwt.dev.jjs.impl.ImplementCastsAndTypeChecks;
import com.google.gwt.dev.jjs.impl.JavaToJavaScriptMap;
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
import com.google.gwt.thirdparty.guava.common.annotations.VisibleForTesting;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;

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

  @VisibleForTesting
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
      ComputeInstantiatedJsoInterfaces.exec(jprogram);
      ImplementCastsAndTypeChecks.exec(jprogram, options.isCastCheckingDisabled());
      ArrayNormalizer.exec(jprogram, options.isCastCheckingDisabled());
      EqualityNormalizer.exec(jprogram);
      return ResolveRuntimeTypeReferences.IntoStringLiterals.exec(jprogram);
    }

    @Override
    protected void optimizeJava() {
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

  @VisibleForTesting
  class LibraryPrecompiler extends Precompiler {

    public LibraryPrecompiler(RebindPermutationOracle rpo, String[] entryPointTypeNames) {
      super(rpo, entryPointTypeNames);
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
    protected void checkEntryPoints(String[] additionalRootTypes) {
      // Library construction does not need to care whether their are or are not any entry points.
    }

    @Override
    protected void createJProgram() {
      jprogram = new JProgram(false);
    }

    @VisibleForTesting
    protected JDeclaredType ensureFullTypeLoaded(JDeclaredType type) {
      return findTypeBySourceName(BinaryName.toSourceName(type.getName()));
    }

    private JDeclaredType findType(List<JDeclaredType> types, String typeName) {
      for (JDeclaredType type : types) {
        if (BinaryName.toSourceName(type.getName()).equals(typeName)) {
          return type;
        }
      }
      return null;
    }

    // TODO(stalcup): performs much the same load logic as UnifyAst.findType(), but is necessary
    // much earlier. Replace with some single mechanism. This logic only exists to support the
    // ability to analyze whether a type is instantiable prior to creating a rebind rule that
    // attempts to instantiate it. It would be very nice to not be duplicating an instantiability
    // check here that JDT already does quite well during its own compile.
    private JDeclaredType findTypeBySourceName(String sourceTypeName) {
      Map<String, CompiledClass> compiledClassesBySourceName =
          rpo.getCompilationState().getClassFileMapBySource();
      // If the type is available as compiled source.
      if (compiledClassesBySourceName.containsKey(sourceTypeName)) {
        // Get and return it.
        CompiledClass compiledClass = compiledClassesBySourceName.get(sourceTypeName);
        return findType(compiledClass.getUnit().getTypes(), sourceTypeName);
      }
      // Otherwise if the type is available in a loaded library.
      CompilationUnit compilationUnit =
          compilerContext.getLibraryGroup().getCompilationUnitByTypeSourceName(sourceTypeName);
      if (compilationUnit != null) {
        // Get and return it.
        compilerContext.getUnitCache().add(compilationUnit);
        return findType(compilationUnit.getTypes(), sourceTypeName);
      }
      return null;
    }

    @VisibleForTesting
    protected Set<JDeclaredType> gatherReboundTypes(RebindPermutationOracle rpo) {
      Collection<CompilationUnit> compilationUnits =
          rpo.getCompilationState().getCompilationUnits();
      Set<JDeclaredType> reboundTypes = Sets.newLinkedHashSet();
      // EntryPoints are rebound but the rebind synthetisation has not occurred yet. Gather them as
      // rebound types anyway.
      for (String entryPointTypeName : entryPointTypeNames) {
        reboundTypes.add(findTypeBySourceName(entryPointTypeName));
      }

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

    @VisibleForTesting
    protected Set<String> getTypeNames(Set<JDeclaredType> types) {
      Set<String> typeNames = Sets.newHashSet();
      for (JDeclaredType type : types) {
        typeNames.add(type.getName());
      }
      return typeNames;
    }

    /**
     * Runs a particular generator on the provided set of rebound types. Takes care to guard against
     * duplicate work during reruns as generation approaches a fixed point.
     */
    @VisibleForTesting
    protected void runGenerator(RuleGenerateWith generatorRule, Set<String> reboundTypeNames)
        throws UnableToCompleteException {
      for (String reboundTypeName : reboundTypeNames) {
        generatorRule.generate(logger, module.getProperties(), getGeneratorContext(),
            BinaryName.toSourceName(reboundTypeName));
      }
    }

    @VisibleForTesting
    protected void runGeneratorsToFixedPoint(RebindPermutationOracle rpo)
        throws UnableToCompleteException {
      boolean fixedPoint;
      do {
        compilerContext.getLibraryWriter()
            .markReboundTypesProcessed(getTypeNames(gatherReboundTypes(rpo)));

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

    @VisibleForTesting
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

    @VisibleForTesting
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

    @VisibleForTesting
    void buildRuntimeRebindRegistrator(Set<String> allRootTypes) throws UnableToCompleteException {
      // If no runtime rebind rules were created for this library.
      if (RuntimeRebindRuleGenerator.RUNTIME_REBIND_RULE_SOURCES_BY_SHORT_NAME.isEmpty()) {
        // Then there's no need to generate a registrator to attach them to the runtime registry.
        return;
      }

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

    @VisibleForTesting
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

    /**
     * Figures out which generators should run based on the current state and runs them. Generator
     * execution can create new opportunities for further generator execution so this function
     * should be invoked repeatedly till a fixed point is reached.<br />
     *
     * Returns whether a fixed point was reached.
     */
    private boolean runGenerators() throws UnableToCompleteException {
      boolean globalCompile = compilerContext.getOptions().shouldLink();
      Set<Rule> generatorRules = Sets.newHashSet(module.getGeneratorRules());

      TreeLogger branch = logger.branch(TreeLogger.SPAM, "running generators");

      for (Rule rule : generatorRules) {
        RuleGenerateWith generatorRule = (RuleGenerateWith) rule;
        String generatorName = generatorRule.getName();

        if (generatorRule.contentDependsOnTypes() && !globalCompile) {
          // Type unstable generators can only be safely run in the global phase.
          // TODO(stalcup): modify type unstable generators such that their output is no longer
          // unstable.
          branch.log(TreeLogger.SPAM,
              "skipping generator " + generatorName + " since it can only run in the global phase");
          continue;
        }

        if (!generatorRule.relevantPropertiesAreFinal(module.getProperties(),
            options.getFinalProperties())) {
          // Some property(s) that this generator cares about have not yet reached their final
          // value. Running the generator now would be wasted effort as it would just need to be run
          // again later anyway.
          branch.log(TreeLogger.SPAM, "skipping generator " + generatorName
              + " since properties it cares about have not reached their final values.");
          continue;
        }

        Set<String> reboundTypes = Sets.newHashSet(compilerContext.getReboundTypeSourceNames());
        Set<String> processedReboundTypeSourceNamesForGenerator =
            compilerContext.getProcessedReboundTypeSourceNames(generatorName);

        Set<String> unprocessedReboundTypeSourceNames = Sets.newHashSet(reboundTypes);
        unprocessedReboundTypeSourceNames.removeAll(processedReboundTypeSourceNamesForGenerator);
        if (unprocessedReboundTypeSourceNames.isEmpty()) {
          // All the requested rebound types have already been processed by this generator.
          branch.log(TreeLogger.SPAM, "skipping generator " + generatorName
              + " since it has already processed all requested rebound types.");
          continue;
        }

        branch.log(TreeLogger.SPAM, "running generator " + generatorName + " on "
            + unprocessedReboundTypeSourceNames.size() + " not yet processed rebound types");
        runGenerator(generatorRule, unprocessedReboundTypeSourceNames);

        // Marks the previously unprocessed types as processed.
        for (String unprocessedReboundTypeSourceName : unprocessedReboundTypeSourceNames) {
          compilerContext.getLibraryWriter().markReboundTypeProcessed(
              unprocessedReboundTypeSourceName, generatorName);
        }
      }

      // If there is output.
      if (getGeneratorContext().isDirty()) {
        // Compile and assimilate it.
        getGeneratorContext().finish(logger);
        return false;
      }

      return true;
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
    return new LibraryPrecompiler(rpo, entryPointTypeNames).precompile(
        additionalRootTypes, singlePermutation, precompilationMetrics);
  }
}
