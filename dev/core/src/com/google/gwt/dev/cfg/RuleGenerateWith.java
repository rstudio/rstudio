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
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.RebindResult;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.javac.StandardGeneratorContext;
import com.google.gwt.dev.javac.typemodel.TypeOracle;
import com.google.gwt.dev.jjs.InternalCompilerException;
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

  private Generator generator;
  private Class<? extends Generator> generatorClass;
  private Map<String, String> rebindProperties;
  private String rebindRequestTypeName;
  private String rebindResultTypeName;

  public RuleGenerateWith(Class<? extends Generator> generatorClass) {
    this.generatorClass = generatorClass;
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
    Set<String> generatorPropertyNames = getGenerator().getAccessedPropertyNames();

    if (generatorPropertyNames == null) {
      return !propertyNames.isEmpty();
    }

    if (!Sets.intersection(generatorPropertyNames, propertyNames).isEmpty()) {
      return true;
    }

    Set<String> conditionalPropertyNames = getRootCondition().getRequiredProperties();
    if (!Sets.intersection(conditionalPropertyNames, propertyNames).isEmpty()) {
      return true;
    }

    return false;
  }

  public boolean contentDependsOnProperties() {
    return getGenerator().contentDependsOnProperties();
  }

  public boolean contentDependsOnTypes() {
    return getGenerator().contentDependsOnTypes();
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
  public void generate(
      TreeLogger logger, Properties moduleProperties, GeneratorContext context, String typeName) {
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

  // VisibleForTesting
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
      Properties moduleProperties, StandardGeneratorContext context, String typeName) {
    try {
      Map<Map<String, String>, String> resultTypeNamesByProperties = Maps.newHashMap();
      DynamicPropertyOracle dynamicPropertyOracle =
          new DynamicPropertyOracle(moduleProperties);

      // Maybe prime the pump.
      if (getGenerator().getAccessedPropertyNames() != null) {
        for (String accessedPropertyName : getGenerator().getAccessedPropertyNames()) {
          try {
            dynamicPropertyOracle.getSelectionProperty(logger, accessedPropertyName);
          } catch (BadPropertyValueException e) {
            // ignore
          }
        }
      }
      boolean needsAllTypesIfRun =
          getGenerator().contentDependsOnTypes() && context.isGlobalCompile();
      TypeOracle typeModelTypeOracle =
          (com.google.gwt.dev.javac.typemodel.TypeOracle) context.getTypeOracle();

      context.reset();
      context.setPropertyOracle(dynamicPropertyOracle);

      context.setCurrentGenerator(generatorClass);

      do {
        resultTypeNamesByProperties.clear();
        context.reset();
        Properties accessedProperties = new Properties();

        List<BindingProperty> accessedPropertiesList =
            new ArrayList<BindingProperty>(dynamicPropertyOracle.getAccessedProperties());
        for (BindingProperty bindingProperty : accessedPropertiesList) {
          accessedProperties.addBindingProperty(bindingProperty);
        }
        PropertyPermutations permutationsOfAccessedProperties =
            new PropertyPermutations(accessedProperties, Sets.<String>newHashSet());

        for (int permutationId = 0; permutationId < permutationsOfAccessedProperties.size();
            permutationId++) {
          String[] orderedPropertyValues =
              permutationsOfAccessedProperties.getOrderedPropertyValues(permutationId);
          BindingProperty[] orderedProperties =
              permutationsOfAccessedProperties.getOrderedProperties();

          dynamicPropertyOracle.reset();
          for (int propertyIndex = 0; propertyIndex < orderedPropertyValues.length;
              propertyIndex++) {
            dynamicPropertyOracle.prescribePropertyValue(
                orderedProperties[propertyIndex].getName(), orderedPropertyValues[propertyIndex]);
          }

          if (!isApplicable(logger, context, typeName)) {
            continue;
          }
          if (needsAllTypesIfRun) {
            typeModelTypeOracle.ensureAllLoaded();
          }
          String resultTypeName = getGenerator().generate(logger, context, typeName);
          if (resultTypeName != null) {
            // Some generators only run to create resource artifacts and don't actually participate
            // in the requestType->resultType rebind process.
            resultTypeNamesByProperties.put(
                dynamicPropertyOracle.getPrescribedPropertyValuesByName(), resultTypeName);
          }

          if (dynamicPropertyOracle.haveAccessedPropertiesChanged()) {
            break;
          }
        }
      } while (dynamicPropertyOracle.haveAccessedPropertiesChanged());

      return resultTypeNamesByProperties;
    } catch (UnableToCompleteException e) {
      throw new InternalCompilerException(e.getMessage());
    }
  }
}
