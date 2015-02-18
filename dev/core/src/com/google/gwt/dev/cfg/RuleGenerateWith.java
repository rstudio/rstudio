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
package com.google.gwt.dev.cfg;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.Generator.RunsLocal;
import com.google.gwt.core.ext.RebindResult;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.javac.StandardGeneratorContext;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.thirdparty.guava.common.annotations.VisibleForTesting;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableSet;

import java.util.Set;

/**
 * A rule to replace the type being rebound with a class whose name is determined by a generator
 * class. Generators usually generate new classes during the deferred binding process, but it is not
 * required.
 */
public class RuleGenerateWith extends Rule {

  public static final Set<String> ALL_PROPERTIES = ImmutableSet.of(RunsLocal.ALL);

  /**
   * Returns a Set of the names of properties that will be accessed by the given Generator.
   */
  public static Set<String> getAccessedPropertyNames(Class<? extends Generator> generatorClass) {
    RunsLocal runsLocal = generatorClass.getAnnotation(RunsLocal.class);
    return runsLocal == null ? ALL_PROPERTIES : ImmutableSet.copyOf(runsLocal.requiresProperties());
  }

  private Generator generator;
  private final Class<? extends Generator> generatorClass;

  public RuleGenerateWith(Class<? extends Generator> generatorClass) {
    this.generatorClass = generatorClass;
  }

  /**
   * Returns whether the output of the Generator being managed by this rule depends on access to the
   * global set of types to be able to run accurately.
   */
  public boolean contentDependsOnTypes() {
    return generatorClass.getAnnotation(RunsLocal.class) == null;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof RuleGenerateWith)) {
      return false;
    }
    RuleGenerateWith other = (RuleGenerateWith) obj;
    if (generatorClass == null) {
      if (other.generatorClass != null) {
        return false;
      }
    } else if (!generatorClass.equals(other.generatorClass)) {
      return false;
    }
    return true;
  }

  /**
   * Returns the name of the class of Generator being managed here.
   */
  public String getName() {
    return generatorClass.getName();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((generatorClass == null) ? 0 : generatorClass.hashCode());
    return result;
  }

  @Override
  public RebindResult realize(TreeLogger logger,
      StandardGeneratorContext context, String typeName)
      throws UnableToCompleteException {
    return context.runGeneratorIncrementally(logger, generatorClass, typeName);
  }

  @Override
  public String toString() {
    return "<generate-with class='" + generatorClass.getName() + "'/>";
  }

  @VisibleForTesting
  protected Generator getGenerator() {
    if (generator == null) {
      try {
        generator = generatorClass.newInstance();
      } catch (InstantiationException e) {
        throw new InternalCompilerException(
            "Could not instantiate generator " + generatorClass.getSimpleName());
      } catch (IllegalAccessException e) {
        throw new InternalCompilerException(
            "Could not instantiate generator " + generatorClass.getSimpleName());
      }
    }
    return generator;
  }
}
