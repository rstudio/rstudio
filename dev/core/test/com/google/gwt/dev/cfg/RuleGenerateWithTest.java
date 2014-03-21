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
package com.google.gwt.dev.cfg;

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.SelectionProperty;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.dev.CompilerContext;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompilationStateBuilder;
import com.google.gwt.dev.javac.StandardGeneratorContext;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import junit.framework.TestCase;

import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;

/**
 * Tests for RuleGenerateWith.
 */
public class RuleGenerateWithTest extends TestCase {

  /**
   * Test Generator that cares about properties Foo and Bar.
   */
  public static class CaresAboutPropertiesGenerator extends Generator {

    @Override
    public boolean contentDependsOnProperties() {
      return true;
    }

    @Override
    public boolean contentDependsOnTypes() {
      return false;
    }

    @Override
    public String generate(TreeLogger logger, GeneratorContext context, String typeName)
        throws UnableToCompleteException {
      return null;
    }

    @Override
    public Set<String> getAccessedPropertyNames() {
      return Sets.newHashSet("Foo", "Bar");
    }
  }

  /**
   * Test Generator that wants to create types for some combination of user.agent and flavor
   * property values.
   */
  public static class FooGenerator extends Generator {

    @Override
    public boolean contentDependsOnProperties() {
      return true;
    }

    @Override
    public boolean contentDependsOnTypes() {
      return false;
    }

    @Override
    public String generate(TreeLogger logger, GeneratorContext context, String typeName)
        throws UnableToCompleteException {
      try {
        SelectionProperty userAgentProperty =
            context.getPropertyOracle().getSelectionProperty(logger, "user.agent");
        String userAgentValue = userAgentProperty.getCurrentValue();

        String className = null;
        if (userAgentValue.equals("webkit")) {
          className = "FooWebkit";
        } else {
          SelectionProperty flavorProperty =
              context.getPropertyOracle().getSelectionProperty(logger, "flavor");
          String flavorValue = flavorProperty.getCurrentValue();
          className = "FooMozilla" + flavorValue;
        }
        if (className != null) {
          PrintWriter pw = context.tryCreate(logger, "com.google.gwt", className);
          if (pw != null) {
            pw.println("package com.google.gwt;");
            pw.println("public class " + className + " {");
            pw.println("}");
            pw.flush();
          }
        }
        return "com.google.gwt." + className;
      } catch (BadPropertyValueException e) {
        throw new UnableToCompleteException();
      }
    }

    @Override
    public Set<String> getAccessedPropertyNames() {
      return Sets.newHashSet("user.agent", "flavor");
    }
  }

  private static class MockGeneratorContext extends StandardGeneratorContext {

    private Set<String> compilationUnitNames = Sets.newHashSet();
    private boolean globalCompile;

    public MockGeneratorContext(CompilerContext compilerContext, CompilationState compilationState,
        ArtifactSet allGeneratedArtifacts, boolean isProdMode, boolean globalCompile) {
      super(compilerContext, compilationState, allGeneratedArtifacts, isProdMode);
      this.globalCompile = globalCompile;
    }

    @Override
    public boolean isGlobalCompile() {
      return globalCompile;
    }

    @Override
    public boolean isProdMode() {
      return true;
    }

    @Override
    public void reset() {
    }

    @Override
    public PrintWriter tryCreate(TreeLogger logger, String packageName, String simpleTypeName) {
      compilationUnitNames.add(simpleTypeName);
      return super.tryCreate(logger, packageName, simpleTypeName);
    }
  }

  public void testCaresAboutProperties() {
    RuleGenerateWith rule = new RuleGenerateWith(CaresAboutPropertiesGenerator.class);

    assertFalse(rule.caresAboutProperties(Sets.<String>newHashSet()));
    assertFalse(rule.caresAboutProperties(Sets.newHashSet("Baz")));
    assertTrue(rule.caresAboutProperties(Sets.newHashSet("Foo")));
    assertTrue(rule.caresAboutProperties(Sets.newHashSet("Bar")));
    assertTrue(rule.caresAboutProperties(Sets.newHashSet("Foo", "Bar")));
    assertTrue(rule.caresAboutProperties(Sets.newHashSet("Foo", "Bar", "Baz")));
  }

  public void testGenerate() throws UnableToCompleteException {
    // Sets up environment.
    Map<String, String> runtimeRebindRuleSourcesByName =
        RuntimeRebindRuleGenerator.RUNTIME_REBIND_RULE_SOURCES_BY_SHORT_NAME;
    MockModuleDef moduleDef = new MockModuleDef();
    CompilerContext compilerContext = new CompilerContext.Builder().module(moduleDef).build();

    MockGeneratorContext generatorContext = new MockGeneratorContext(
        compilerContext, CompilationStateBuilder.buildFrom(
            TreeLogger.NULL, compilerContext, Sets.<Resource>newHashSet()), null, true, false);
    Properties moduleProperties = new Properties();

    BindingProperty userAgentProperty = moduleProperties.createBinding("user.agent");
    userAgentProperty.addDefinedValue(userAgentProperty.getRootCondition(), "mozilla");
    userAgentProperty.addDefinedValue(userAgentProperty.getRootCondition(), "webkit");

    BindingProperty flavorProperty = moduleProperties.createBinding("flavor");
    flavorProperty.addDefinedValue(flavorProperty.getRootCondition(), "Vanilla");
    flavorProperty.addDefinedValue(flavorProperty.getRootCondition(), "Chocolate");

    RuleGenerateWith rule = new RuleGenerateWith(FooGenerator.class);

    // Triggers generation with the rule.
    rule.generate(TreeLogger.NULL, moduleProperties, generatorContext, "com.google.gwt.Foo");

    // Expects rebind result classes were generated.
    assertEquals(Sets.newHashSet("FooMozillaVanilla", "FooWebkit", "FooMozillaChocolate"),
        generatorContext.compilationUnitNames);

    // Expects rebind rules were created that represent the discovery and accessing of user.agent
    // and flavor binding rules.
    String runtimeRebindRule0 = runtimeRebindRuleSourcesByName.get("RuntimeRebindRule0");
    assertTrue(runtimeRebindRule0.contains("com.google.gwt.FooMozillaVanilla::new()"));
    assertTrue(runtimeRebindRule0.contains(
        "@com.google.gwt.lang.RuntimePropertyRegistry::getPropertyValue(*)"
        + "(\"user.agent\") == \"mozilla\""));
    assertTrue(runtimeRebindRule0.contains(
        "@com.google.gwt.lang.RuntimePropertyRegistry::getPropertyValue(*)"
        + "(\"flavor\") == \"Vanilla\""));

    String runtimeRebindRule1 = runtimeRebindRuleSourcesByName.get("RuntimeRebindRule1");
    assertTrue(runtimeRebindRule1.contains("com.google.gwt.FooWebkit::new()"));
    assertTrue(runtimeRebindRule1.contains(
        "@com.google.gwt.lang.RuntimePropertyRegistry::getPropertyValue(*)"
        + "(\"user.agent\") == \"webkit\""));
    assertTrue(runtimeRebindRule1.contains(
        "@com.google.gwt.lang.RuntimePropertyRegistry::getPropertyValue(*)"
        + "(\"flavor\") == \"Vanilla\""));

    String runtimeRebindRule2 = runtimeRebindRuleSourcesByName.get("RuntimeRebindRule2");
    assertTrue(runtimeRebindRule2.contains("com.google.gwt.FooWebkit::new()"));
    assertTrue(runtimeRebindRule2.contains(
        "@com.google.gwt.lang.RuntimePropertyRegistry::getPropertyValue(*)"
        + "(\"user.agent\") == \"webkit\""));
    assertTrue(runtimeRebindRule2.contains(
        "@com.google.gwt.lang.RuntimePropertyRegistry::getPropertyValue(*)"
        + "(\"flavor\") == \"Chocolate\""));

    String runtimeRebindRule3 = runtimeRebindRuleSourcesByName.get("RuntimeRebindRule3");
    assertTrue(runtimeRebindRule3.contains("com.google.gwt.FooMozillaChocolate::new()"));
    assertTrue(runtimeRebindRule3.contains(
        "@com.google.gwt.lang.RuntimePropertyRegistry::getPropertyValue(*)"
        + "(\"user.agent\") == \"mozilla\""));
    assertTrue(runtimeRebindRule3.contains(
        "@com.google.gwt.lang.RuntimePropertyRegistry::getPropertyValue(*)"
        + "(\"flavor\") == \"Chocolate\""));
  }
}
