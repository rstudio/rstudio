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
package com.google.gwt.dev.javac;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.testing.impl.JavaResourceBase;
import com.google.gwt.dev.resource.Resource;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Utilities for tests that build a type oracle and watch for errors.
 * 
 */
public class TypeOracleTestingUtils {

  public static CompilationState buildCompilationState(TreeLogger logger, Set<Resource> resources,
      Set<GeneratedUnit> generatedUnits) {
    CompilationState state = CompilationStateBuilder.buildFrom(logger, resources);
    state.addGeneratedCompilationUnits(logger, generatedUnits);
    return state;
  }

  public static CompilationState buildStandardCompilationStateWith(TreeLogger logger,
      Resource... resources) {
    return buildCompilationState(logger, standardBuildersPlus(new HashSet<Resource>(Arrays
        .asList(resources))), Collections.<GeneratedUnit> emptySet());
  }

  public static TypeOracleMediator buildMediator(TreeLogger logger, Set<Resource> resources,
      Set<GeneratedUnit> generatedUnits) {
    return buildCompilationState(logger, resources, generatedUnits).getMediator();
  }

  public static TypeOracleMediator buildMediatorWith(TreeLogger logger,
      Set<Resource> resources) {
    return buildMediator(logger, resources,
        Collections.<GeneratedUnit> emptySet());
  }

  public static TypeOracleMediator buildStandardMediatorWith(TreeLogger logger,
      Resource... resources) {
    return buildStandardMediatorWith(logger, new HashSet<Resource>(
        Arrays.asList(resources)));
  }

  public static TypeOracleMediator buildStandardMediatorWith(TreeLogger logger,
      Set<Resource> resources) {
    return buildMediatorWith(logger, standardBuildersPlus(resources));
  }

  public static TypeOracle buildStandardTypeOracleWith(TreeLogger logger,
      Resource... resources) {
    return buildStandardTypeOracleWith(logger, new HashSet<Resource>(
        Arrays.asList(resources)));
  }

  public static TypeOracle buildStandardTypeOracleWith(TreeLogger logger,
      Set<Resource> resources) {
    return buildTypeOracle(logger, standardBuildersPlus(resources));
  }

  public static TypeOracle buildStandardTypeOracleWith(TreeLogger logger,
      Set<Resource> resources, Set<GeneratedUnit> generatedUnits) {
    return buildTypeOracle(logger, standardBuildersPlus(resources),
        generatedUnits);
  }

  public static TypeOracle buildTypeOracle(TreeLogger logger,
      Set<Resource> resources) {
    return buildTypeOracle(logger, resources,
        Collections.<GeneratedUnit> emptySet());
  }

  public static TypeOracle buildTypeOracle(TreeLogger logger,
      Set<Resource> resources, Set<GeneratedUnit> generatedUnits) {
    CompilationState state = CompilationStateBuilder.buildFrom(logger,
        resources);
    state.addGeneratedCompilationUnits(logger, generatedUnits);
    return state.getTypeOracle();
  }

  /**
   * Compilation resources for basic classes like Object and String.
   */
  private static Set<Resource> standardBuildersPlus(Set<Resource> resources) {
    Set<Resource> result = new HashSet<Resource>();
    for (Resource standardResource : JavaResourceBase.getStandardResources()) {
      result.add(standardResource);
    }
    result.addAll(resources);
    return result;
  }
}
