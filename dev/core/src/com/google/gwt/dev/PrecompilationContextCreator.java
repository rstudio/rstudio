/*
 * Copyright 2015 Google Inc.
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
package com.google.gwt.dev;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.PrecompilationMetricsArtifact;
import com.google.gwt.dev.cfg.BindingProperties;
import com.google.gwt.dev.cfg.BindingProperty;
import com.google.gwt.dev.cfg.ConfigurationProperties;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.PropertyCombinations;
import com.google.gwt.dev.cfg.Rule;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.StandardGeneratorContext;
import com.google.gwt.dev.jdt.RebindOracle;
import com.google.gwt.dev.jdt.RebindPermutationOracle;
import com.google.gwt.dev.jjs.PrecompilationContext;
import com.google.gwt.dev.shell.StandardRebindOracle;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.Deque;
import java.util.Set;

/**
 * Creates the context encapsulating necessary data for precompile.
 */
class PrecompilationContextCreator {
  static PrecompilationContext create(CompilerContext compilerContext,
      final CompilationState compilationState, PropertyCombinations propertyCombinations,
      String[] entryPoints, String[] additionalRootTypes,
      PrecompilationMetricsArtifact precompilationMetricsArtifact) {

    ModuleDef module = compilerContext.getModule();
    final ArtifactSet generatorArtifacts = new ArtifactSet();
    final Permutation[] permutations = new Permutation[propertyCombinations.size()];
    final RebindOracle[] rebindOracles = new RebindOracle[propertyCombinations.size()];
    final StandardGeneratorContext generatorContext =
        new StandardGeneratorContext(compilerContext, compilationState, generatorArtifacts, true);
    BindingProperty[] orderedProperties = propertyCombinations.getOrderedProperties();
    ConfigurationProperties configurationProperties = new ConfigurationProperties(module);
    Deque<Rule> rules = module.getRules();
    for (int i = 0; i < propertyCombinations.size(); ++i) {
      BindingProperties bindingProperties = new BindingProperties(orderedProperties,
          propertyCombinations.getOrderedPropertyValues(i), configurationProperties);
      rebindOracles[i] =
          new StandardRebindOracle(bindingProperties.toPropertyOracle(), rules, generatorContext);
      permutations[i] = new Permutation(i, bindingProperties);
    }

    RebindPermutationOracle rebindPermutationOracle =
        new RebindPermutationOracle() {
          @Override
          public void clear() {
            generatorContext.clear();
          }

          @Override
          public String[] getAllPossibleRebindAnswers(TreeLogger logger, String requestTypeName)
              throws UnableToCompleteException {
            String msg = "Computing all possible rebind results for '"
                + requestTypeName + "'";
            logger = logger.branch(TreeLogger.DEBUG, msg, null);

            Set<String> answers = Sets.newHashSet();
            Event getAllRebindsEvent = SpeedTracerLogger.start(CompilerEventType.GET_ALL_REBINDS);
            for (int i = 0; i < permutations.length; ++i) {
              String resultTypeName = rebindOracles[i].rebind(logger, requestTypeName);
              answers.add(resultTypeName);
              // Record the correct answer into each permutation.
              permutations[i].putRebindAnswer(requestTypeName, resultTypeName);
            }
            String[] result = Util.toArray(String.class, answers);
            getAllRebindsEvent.end();
            return result;
          }

          @Override
          public CompilationState getCompilationState() {
            return compilationState;
          }

          @Override
          public StandardGeneratorContext getGeneratorContext() {
            return generatorContext;
          }
        };

    return new PrecompilationContext(rebindPermutationOracle, entryPoints, additionalRootTypes,
        permutations, generatorArtifacts, precompilationMetricsArtifact);
  }
}
