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

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.Generator.RunsLocal;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.RebindResult;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.javac.StandardGeneratorContext;
import com.google.gwt.dev.javac.typemodel.LibraryTypeOracle.UnsupportedTypeOracleAccess;
import com.google.gwt.dev.javac.typemodel.TypeOracle;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.thirdparty.guava.common.annotations.VisibleForTesting;
import com.google.gwt.thirdparty.guava.common.base.Objects;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableSet;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

  private final Set<String> accessedPropertyNames;
  private Generator generator;
  private final Class<? extends Generator> generatorClass;
  private Map<String, String> rebindProperties;
  private String rebindRequestTypeName;
  private String rebindResultTypeName;

  public RuleGenerateWith(Class<? extends Generator> generatorClass) {
    this.generatorClass = generatorClass;
    this.accessedPropertyNames = getAccessedPropertyNames(generatorClass);
  }

  /**
   * Returns whether the output of the Generator being managed by this rule is modified by or
   * whether the rules embedded condition is judging any of the properties whose names have been
   * passed.<br />
   *
   * Makes it possible for external callers to watch the changing property environment and only
   * trigger Generators within Rules whose output might have changed.
   */
  public boolean caresAboutProperties(Set<String> propertyNames) {
    // If this generator cares about all properties.
    if (accessedPropertyNames.equals(ALL_PROPERTIES)) {
      // Then if some properties were supplied, it cares about them.
      return !propertyNames.isEmpty();
    }

    // Otherwise an explicit list of cared about properties was supplied. Return whether any of the
    // supplied properties is cared about.
    return !Sets.intersection(accessedPropertyNames, propertyNames).isEmpty();
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
   * Generate all possible Generator output for the wrapped Generator in combination with the scope
   * of Properties and values known about by the passed GeneratorContext. Additionally generate and
   * gather runtime rebind rules for all corresponding pairs of property values and Generator
   * output.
   */
  public void generate(TreeLogger logger, Properties moduleProperties, GeneratorContext context,
      String typeName) throws UnableToCompleteException {
    Map<Map<String, String>, String> resultTypeNamesByProperties =
        computeResultTypeNamesByProperties(
            logger, moduleProperties, (StandardGeneratorContext) context, typeName);

    rebindRequestTypeName = typeName;
    for (Entry<Map<String, String>, String> entry : resultTypeNamesByProperties.entrySet()) {
      rebindProperties = entry.getKey();
      rebindResultTypeName = entry.getValue();
      runtimeRebindRuleGenerator.generate(
          generateCreateInstanceExpression(), generateMatchesExpression());
    }
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

  public boolean relevantPropertiesAreFinal(Properties currentProperties,
      Properties finalProperties) {
    if (accessedPropertyNames.equals(ALL_PROPERTIES)) {
      // Generator must be assumed to depend on all properties.
      for (BindingProperty finalProperty : finalProperties.getBindingProperties()) {
        Property currentProperty = currentProperties.find(finalProperty.getName());
        if (!Objects.equal(finalProperty, currentProperty)) {
          return false;
        }
      }
      for (ConfigurationProperty finalProperty : finalProperties.getConfigurationProperties()) {
        Property currentProperty = currentProperties.find(finalProperty.getName());
        if (!Objects.equal(finalProperty, currentProperty)) {
          return false;
        }
      }
      return true;
    }

    // Generator defines a limited set of properties it cares about.
    for (String accessedPropertyName : accessedPropertyNames) {
      Property finalProperty = finalProperties.find(accessedPropertyName);
      Property currentProperty = currentProperties.find(accessedPropertyName);
      if (!Objects.equal(finalProperty, currentProperty)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String toString() {
    return "<generate-with class='" + generatorClass.getName() + "'/>";
  }

  @Override
  protected String generateCreateInstanceExpression() {
    return "return @" + rebindResultTypeName + "::new()();";
  }

  @Override
  protected String generateMatchesExpression() {
    ConditionAll rootCondition = new ConditionAll();
    Conditions conditions = rootCondition.getConditions();

    conditions.add(new ConditionWhenTypeIs(rebindRequestTypeName));
    for (Entry<String, String> propertyEntry : rebindProperties.entrySet()) {
      String propertyName = propertyEntry.getKey();
      String propertyValue = propertyEntry.getValue();
      conditions.add(new ConditionWhenPropertyIs(propertyName, propertyValue));
    }

    return "return " + rootCondition.toSource() + ";";
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

  /**
   * Repeatedly run the contained Generator with the maximum range of combinations of Properties and
   * values. Log corresponding pairs of property values and Generator output.
   */
  private Map<Map<String, String>, String> computeResultTypeNamesByProperties(TreeLogger logger,
      Properties moduleProperties, StandardGeneratorContext context, String typeName)
      throws UnableToCompleteException {
    Map<Map<String, String>, String> resultTypeNamesByProperties = Maps.newHashMap();
    DynamicPropertyOracle dynamicPropertyOracle = new DynamicPropertyOracle(moduleProperties);

    // Maybe prime the pump.
    if (!accessedPropertyNames.equals(ALL_PROPERTIES)) {
      for (String accessedPropertyName : accessedPropertyNames) {
        try {
          dynamicPropertyOracle.getSelectionProperty(logger, accessedPropertyName);
        } catch (BadPropertyValueException e) {
          // ignore
        }
      }
    }
    boolean needsAllTypesIfRun = contentDependsOnTypes() && context.isGlobalCompile();
    TypeOracle typeModelTypeOracle =
        (com.google.gwt.dev.javac.typemodel.TypeOracle) context.getTypeOracle();

    context.setPropertyOracle(dynamicPropertyOracle);

    context.setCurrentGenerator(generatorClass);

    do {
      resultTypeNamesByProperties.clear();
      Properties accessedProperties = new Properties();

      List<BindingProperty> accessedPropertiesList =
          new ArrayList<BindingProperty>(dynamicPropertyOracle.getAccessedProperties());
      for (BindingProperty bindingProperty : accessedPropertiesList) {
        accessedProperties.addBindingProperty(bindingProperty);
      }
      PropertyPermutations permutationsOfAccessedProperties =
          new PropertyPermutations(accessedProperties, Sets.<String> newHashSet());

      for (int permutationId = 0; permutationId < permutationsOfAccessedProperties.size();
          permutationId++) {
        String[] orderedPropertyValues =
            permutationsOfAccessedProperties.getOrderedPropertyValues(permutationId);
        BindingProperty[] orderedProperties =
            permutationsOfAccessedProperties.getOrderedProperties();

        dynamicPropertyOracle.reset();
        for (int propertyIndex = 0; propertyIndex < orderedPropertyValues.length; propertyIndex++) {
          dynamicPropertyOracle.prescribePropertyValue(orderedProperties[propertyIndex].getName(),
              orderedPropertyValues[propertyIndex]);
        }

        if (!isApplicable(logger, context, typeName)) {
          continue;
        }
        if (needsAllTypesIfRun) {
          typeModelTypeOracle.ensureAllLoaded();
        }
        String resultTypeName;
        try {
          resultTypeName = getGenerator().generate(logger, context, typeName);
        } catch (UnsupportedTypeOracleAccess e) {
          logger.log(TreeLogger.ERROR, String.format(
              "TypeOracle error when running generator '%s' in an incremental compile: %s",
              getName(), e.getMessage()));
          throw new UnableToCompleteException();
        }
        if (resultTypeName != null) {
          // Some generators only run to create resource artifacts and don't actually participate
          // in the requestType->resultType rebind process.
          resultTypeNamesByProperties.put(dynamicPropertyOracle.getPrescribedPropertyValuesByName(),
              resultTypeName);
        }

        if (dynamicPropertyOracle.haveAccessedPropertiesChanged()) {
          break;
        }
      }
    } while (dynamicPropertyOracle.haveAccessedPropertiesChanged());

    return resultTypeNamesByProperties;
  }
}
