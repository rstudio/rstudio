/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.dev.cfg;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.PropertyProviderGenerator;
import com.google.gwt.core.ext.linker.impl.StandardConfigurationProperty;
import com.google.gwt.core.ext.linker.impl.StandardLinkerContext;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Generator used to generate a class unique to the current module which has a register() function
 * that when invoked will register the given property provider implementations with a global
 * registry.<br />
 *
 * The resulting class is expected to be invoked at runtime as part of per module bootstrapping and
 * before anything that might depend on property lookups (like GWT.create() calls).
 */
public class PropertyProviderRegistratorGenerator extends Generator {

  /**
   * The extension for all generated property provider registrator classes. Is exposed publicly so
   * others can filter using the extension.
   */
  public static final String PROPERTY_PROVIDER_REGISTRATOR_SUFFIX =
      "PropertyProviderRegistrator";

  private static final String PACKAGE_PATH = "com.google.gwt.lang";

  private static SortedSet<com.google.gwt.core.ext.linker.ConfigurationProperty> toLinkerStyle(
      SortedSet<ConfigurationProperty> configurationProperties) {
    SortedSet<com.google.gwt.core.ext.linker.ConfigurationProperty> linkerConfigurationProperties =
        new TreeSet<com.google.gwt.core.ext.linker.ConfigurationProperty>(
            StandardLinkerContext.CONFIGURATION_PROPERTY_COMPARATOR);
    for (ConfigurationProperty configurationProperty : configurationProperties) {
      linkerConfigurationProperties.add(new StandardConfigurationProperty(configurationProperty));
    }
    return linkerConfigurationProperties;
  }

  private SortedSet<com.google.gwt.core.ext.linker.ConfigurationProperty> configurationProperties;
  private Collection<BindingProperty> newBindingProperties;
  private Set<String> propertyProviderClassNames = Sets.newHashSet();

  /**
   * Constructs a PropertyProviderRegistratorGenerator that is able to generate a property provider
   * registrator for all of the given a collection of binding properties.<br />
   *
   * The provided binding properties are expected to all have been created by the current module but
   * the provided configuration properties should be all that have been created by the entire
   * current dependency tree.
   */
  public PropertyProviderRegistratorGenerator(Collection<BindingProperty> newBindingProperties,
      SortedSet<ConfigurationProperty> configurationProperties) {
    this.newBindingProperties = newBindingProperties;
    this.configurationProperties = toLinkerStyle(configurationProperties);
  }

  @Override
  public String generate(TreeLogger logger, GeneratorContext context, String moduleName)
      throws UnableToCompleteException {
    String typeName = moduleName.replace(".", "_").replace("-", "_") + "_"
        + PROPERTY_PROVIDER_REGISTRATOR_SUFFIX;
    PrintWriter out = context.tryCreate(logger, PACKAGE_PATH, typeName);

    if (out != null) {
      out.println("package " + PACKAGE_PATH + ";");
      out.println("import com.google.gwt.lang.RuntimePropertyRegistry;");
      out.println(
          "import com.google.gwt.lang.RuntimePropertyRegistry.PropertyValueProvider;");
      out.println("public class " + typeName + " {");

      for (BindingProperty bindingProperty : newBindingProperties) {
        // If nothing about the binding property was set or created in the current module.
        if (bindingProperty.getTargetLibraryDefinedValues().isEmpty()) {
          // Then its previously created property provider is still valid.
          continue;
        }
        createPropertyProviderClass(logger, out, bindingProperty);
      }
      // TODO(stalcup): create configuration property providers.

      out.println("  public static void register() {");
      for (String propertyProviderClassName : propertyProviderClassNames) {
        out.println("    RuntimePropertyRegistry.registerPropertyValueProvider(new "
            + propertyProviderClassName + "());");
      }
      // TODO(stalcup): register configuration property providers.
      out.println("  }");
      out.println("}");

      context.commit(logger, out);
    } else {
      // Must have been a cache hit.
    }

    return PACKAGE_PATH + "." + typeName;
  }

  private void createConditionTreeGetter(PrintWriter out, BindingProperty bindingProperty) {
    List<Entry<Condition, SortedSet<String>>> entries = new ArrayList<
        Entry<Condition, SortedSet<String>>>(bindingProperty.getConditionalValues().entrySet());
    List<Entry<Condition, SortedSet<String>>> prioritizedEntries = Lists.reverse(entries);

    out.println("  public native String getValue() /*-{");
    boolean alwaysReturnsAValue = false;
    for (Entry<Condition, SortedSet<String>> entry : prioritizedEntries) {
      Condition condition = entry.getKey();
      SortedSet<String> propertyValue = entry.getValue();

      String conditionSource = condition.toSource();
      if (!conditionSource.isEmpty() && !conditionSource.equals("true")) {
        out.println("    if (" + conditionSource + ") {");
        out.println("      return \"" + propertyValue.iterator().next() + "\";");
        out.println("    }");
      } else {
        alwaysReturnsAValue = true;
        out.println("    return \"" + propertyValue.iterator().next() + "\";");
      }
    }
    if (!alwaysReturnsAValue) {
      out.println("    throw @java.lang.RuntimeException::new(Ljava/lang/String;)"
          + "(\"No known value for property " + bindingProperty.getName() + "\");");
    }
    out.println("  }-*/;");
  }

  private void createConstrainedValueGetter(PrintWriter out, BindingProperty bindingProperty) {
    out.println("  public String getValue() {");
    out.println("    return \"" + bindingProperty.getConstrainedValue() + "\";");
    out.println("  }");
  }

  private void createPropertyProviderClass(
      TreeLogger logger, PrintWriter out, BindingProperty bindingProperty) {
    String bindingPropertyClassName = "PropertyValueProvider" + propertyProviderClassNames.size();
    propertyProviderClassNames.add(bindingPropertyClassName);

    out.println(
        "  private static class " + bindingPropertyClassName + " extends PropertyValueProvider {");
    out.println("  public String getName() {");
    out.println("    return \"" + bindingProperty.getName() + "\";");
    out.println("  }");

    // There are four different ways that modules can register the runtime provider for binding
    // properties and the order of precedence is very particular and important.
    if (bindingProperty.getConstrainedValue() != null) {
      createConstrainedValueGetter(out, bindingProperty);
    } else if (bindingProperty.isDerived()) {
      createConditionTreeGetter(out, bindingProperty);
    } else if (bindingProperty.getProviderGenerator() != null) {
      createPropertyProviderGeneratorGetter(logger, out, bindingProperty);
    } else if (bindingProperty.getProvider() != null) {
      createProviderGetter(out, bindingProperty);
    } else {
      throw new InternalCompilerException("Failed to locate the runtime provider for "
          + "binding property '" + bindingProperty.getName() + "'");
    }
    out.println("  }");
  }

  private void createPropertyProviderGeneratorGetter(
      TreeLogger logger, PrintWriter out, BindingProperty bindingProperty) {
    out.print("  public native String getValue() /*-");
    out.print(generateValue(logger, bindingProperty).trim());
    out.println("-*/;");
  }

  private void createProviderGetter(PrintWriter out, BindingProperty bindingProperty) {
    out.print("  public native String getValue() /*-");
    out.print(bindingProperty.getProvider().getBody().trim());
    out.print("-*/;");
  }

  private String generateValue(TreeLogger logger, BindingProperty bindingProperty) {
    PropertyProviderGenerator propertyProviderGenerator;
    try {
      propertyProviderGenerator = bindingProperty.getProviderGenerator().newInstance();
    } catch (IllegalAccessException e) {
      throw new InternalCompilerException("Failed to instantiate property provider generator "
          + bindingProperty.getProviderGenerator());
    } catch (InstantiationException e) {
      throw new InternalCompilerException("Failed to instantiate property provider generator "
          + bindingProperty.getProviderGenerator());
    }

    try {
      return propertyProviderGenerator.generate(logger,
          Sets.newTreeSet(Arrays.asList(bindingProperty.getDefinedValues())),
          bindingProperty.getFallback(), configurationProperties);

    } catch (UnableToCompleteException e) {
      throw new InternalCompilerException("Failed to run property provider generator "
          + bindingProperty.getProviderGenerator());
    }
  }
}
