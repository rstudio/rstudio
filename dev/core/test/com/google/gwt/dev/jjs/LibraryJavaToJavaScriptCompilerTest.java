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

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.dev.CompilerContext;
import com.google.gwt.dev.cfg.BindingProperty;
import com.google.gwt.dev.cfg.Condition;
import com.google.gwt.dev.cfg.ConditionWhenPropertyIs;
import com.google.gwt.dev.cfg.ConditionWhenTypeIs;
import com.google.gwt.dev.cfg.ConfigurationProperty;
import com.google.gwt.dev.cfg.DeferredBindingQuery;
import com.google.gwt.dev.cfg.LibraryGroup;
import com.google.gwt.dev.cfg.LibraryGroupTest;
import com.google.gwt.dev.cfg.MockLibraryWriter;
import com.google.gwt.dev.cfg.MockModuleDef;
import com.google.gwt.dev.cfg.Properties;
import com.google.gwt.dev.cfg.PropertyProvider;
import com.google.gwt.dev.cfg.RuleFail;
import com.google.gwt.dev.cfg.RuleGenerateWith;
import com.google.gwt.dev.cfg.RuleReplaceWith;
import com.google.gwt.dev.cfg.RuleReplaceWithFallback;
import com.google.gwt.dev.cfg.Rules;
import com.google.gwt.dev.cfg.RuntimeRebindRuleGenerator;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompilationStateBuilder;
import com.google.gwt.dev.javac.StandardGeneratorContext;
import com.google.gwt.dev.jdt.RebindPermutationOracle;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JConstructor;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Sets;
import com.google.gwt.thirdparty.guava.common.util.concurrent.AtomicLongMap;

import junit.framework.TestCase;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.Set;

/**
 * Tests for LibraryJavaToJavaScriptCompiler.
 */
public class LibraryJavaToJavaScriptCompilerTest extends TestCase {

  /**
   * Test Generator that wants to create a FooShim%user.agent% type for every processed FooShim
   * type.
   */
  public static class BrowserShimGenerator extends Generator {

    @Override
    public boolean contentDependsOnTypes() {
      return false;
    }

    @Override
    public String generate(TreeLogger logger, GeneratorContext generatorContext,
        String typeShortName) throws UnableToCompleteException {
      try {
        String userAgentValue = generatorContext.getPropertyOracle()
            .getSelectionProperty(logger, "user.agent").getCurrentValue();
        PrintWriter pw =
            generatorContext.tryCreate(logger, "com.google.gwt", userAgentValue + typeShortName);
        if (pw != null) {
          generatorContext.commit(logger, pw);
        }
        return typeShortName + userAgentValue;
      } catch (BadPropertyValueException e) {
        throw new UnableToCompleteException();
      }
    }

    @Override
    public Set<String> getAccessedPropertyNames() {
      return Sets.newHashSet("user.agent");
    }
  }

  /**
   * Test Generator that wants to create a FooShim%locale% type for every processed FooShim
   * type.
   */
  public static class LocaleMessageGenerator extends Generator {

    @Override
    public boolean contentDependsOnTypes() {
      return false;
    }

    @Override
    public String generate(TreeLogger logger, GeneratorContext generatorContext,
        String typeShortName) throws UnableToCompleteException {
      try {
        String localeValue = generatorContext.getPropertyOracle()
            .getSelectionProperty(logger, "locale").getCurrentValue();
        PrintWriter pw =
            generatorContext.tryCreate(logger, "com.google.gwt", localeValue + typeShortName);
        if (pw != null) {
          generatorContext.commit(logger, pw);
        }
        return typeShortName + localeValue;
      } catch (BadPropertyValueException e) {
        throw new UnableToCompleteException();
      }
    }

    @Override
    public Set<String> getAccessedPropertyNames() {
      return Sets.newHashSet("locale");
    }
  }

  private static class ConditionWhenTypeEndsWith extends Condition {

    private final String suffix;

    public ConditionWhenTypeEndsWith(String suffix) {
      this.suffix = suffix;
    }

    @Override
    public String toSource() {
      return String.format("requestTypeName.endsWith(\"%s\")", suffix);
    }

    @Override
    public String toString() {
      return "<when-type-ends-with class='" + suffix + "'/>";
    }

    @Override
    protected boolean doEval(TreeLogger logger, DeferredBindingQuery query) {
      return query.getTestType().endsWith(suffix);
    }

    @Override
    protected String getEvalAfterMessage(String testType, boolean result) {
      if (result) {
        return "Yes, the requested type ended in " + suffix;
      } else {
        return "Suffix didn't match";
      }
    }

    @Override
    protected String getEvalBeforeMessage(String testType) {
      return toString();
    }
  }

  private static class MockGeneratorContext extends StandardGeneratorContext {

    private boolean dirty = false;
    private boolean globalCompile;
    private Map<String, StringWriter> stringWriterByTypeSourceName = Maps.newHashMap();

    public MockGeneratorContext(CompilerContext compilerContext, CompilationState compilationState,
        ArtifactSet allGeneratedArtifacts, boolean isProdMode, boolean globalCompile) {
      super(compilerContext, compilationState, allGeneratedArtifacts, isProdMode);
      this.globalCompile = globalCompile;
    }

    @Override
    public ArtifactSet finish(TreeLogger logger) throws UnableToCompleteException {
      // Don't actually compile generated source code;
      return new ArtifactSet();
    }

    @Override
    public boolean isDirty() {
      return dirty;
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
      dirty = false;
    }

    @Override
    public PrintWriter tryCreate(TreeLogger logger, String packageName, String typeShortName) {
      if (!stringWriterByTypeSourceName.containsKey(packageName + "." + typeShortName)) {
        StringWriter stringWriter = new StringWriter();
        stringWriterByTypeSourceName.put(packageName + "." + typeShortName, stringWriter);
        dirty = true;
        return new PrintWriter(stringWriter);
      }
      return null;
    }
  }

  private class MockLibraryJavaToJavaScriptCompiler extends LibraryJavaToJavaScriptCompiler {

    private class MockLibraryPrecompiler extends LibraryPrecompiler {

      private Set<String> processedReboundTypeSourceNames = Sets.newHashSet();
      private Set<JDeclaredType> reboundTypes = Sets.<JDeclaredType>newHashSet(
          createInstantiableClassType("com.google.ErrorMessages"),
          createInstantiableClassType("com.google.EventShim"));
      private AtomicLongMap<String> runCountByGeneratorName = AtomicLongMap.create();

      public MockLibraryPrecompiler(RebindPermutationOracle rpo) {
        super(rpo);
      }

      /**
       * Overridden to avoid the complexity of mocking out a LibraryGroupUnitCache.
       */
      @Override
      protected JDeclaredType ensureFullTypeLoaded(JDeclaredType type) {
        return type;
      }

      /**
       * Overridden to avoid the need to mock out DistillerRebindPermutationOracle as well as to
       * dynamically expand the reboundTypes list to cause repeated generator runs.
       */
      @Override
      protected Set<JDeclaredType> gatherReboundTypes(RebindPermutationOracle rpo) {
        return reboundTypes;
      }

      @Override
      protected StandardGeneratorContext getGeneratorContext() {
        return mockGeneratorContext;
      }

      @Override
      protected boolean runGenerator(RuleGenerateWith generatorRule,
          Set<String> reboundTypeSourceNames) throws UnableToCompleteException {
        processedReboundTypeSourceNames.addAll(reboundTypeSourceNames);
        runCountByGeneratorName.incrementAndGet(generatorRule.getName());
        return super.runGenerator(generatorRule, reboundTypeSourceNames);
      }
    }

    private StandardGeneratorContext mockGeneratorContext;

    public MockLibraryJavaToJavaScriptCompiler(TreeLogger logger, CompilerContext compilerContext,
        StandardGeneratorContext generatorContext) {
      super(logger, compilerContext);
      this.mockGeneratorContext = generatorContext;
    }

    private MockLibraryPrecompiler createPrecompiler() {
      return new MockLibraryPrecompiler(null);
    }
  }

  private static JClassType createInstantiableClassType(String typeBinaryName) {
    JClassType instantiableType = new JClassType(SourceOrigin.UNKNOWN, typeBinaryName, false, true);
    JConstructor defaultConstructor = new JConstructor(SourceOrigin.UNKNOWN, instantiableType);
    defaultConstructor.setOriginalTypes(instantiableType, Lists.<JType>newArrayList());
    instantiableType.addMethod(defaultConstructor);
    return instantiableType;
  }

  private MockLibraryJavaToJavaScriptCompiler compiler;
  private CompilerContext compilerContext;
  private MockGeneratorContext generatorContext;
  private MockLibraryJavaToJavaScriptCompiler.MockLibraryPrecompiler precompiler;

  public void testBuildFallbackRuntimeRebindRules() throws UnableToCompleteException {
    // Sets up environment.
    Map<String, String> runtimeRebindRuleSourcesByShortName =
        RuntimeRebindRuleGenerator.RUNTIME_REBIND_RULE_SOURCES_BY_SHORT_NAME;
    Set<JDeclaredType> reboundTypes =
        Sets.<JDeclaredType>newHashSet(createInstantiableClassType("CanvasElement"));

    // Runs fallback rebind rule creation for rebound types.
    precompiler.buildFallbackRuntimeRebindRules(reboundTypes);

    // Expects a worst case rebind rule was created that will at least attempt to create a
    // CanvasElement when a CanvasElement is requested.
    String runtimeRebindRule0 = runtimeRebindRuleSourcesByShortName.get("RuntimeRebindRule0");
    assertTrue(runtimeRebindRule0.contains("@CanvasElement::new()()"));
    assertTrue(runtimeRebindRule0.contains("requestTypeClass == @CanvasElement::class"));
  }

  public void testBuildLocalRuntimeRebindRules() throws UnableToCompleteException {
    // Sets up environment.
    Set<String> allRootTypes = Sets.newHashSet();
    compiler.jprogram = new JProgram();
    Map<String, String> runtimeRebindRuleSourcesByShortName =
        RuntimeRebindRuleGenerator.RUNTIME_REBIND_RULE_SOURCES_BY_SHORT_NAME;
    Rules rules = new Rules();
    RuleFail ruleFail = new RuleFail();
    ruleFail.getRootCondition().getConditions().add(new ConditionWhenPropertyIs("foo", "bar"));
    rules.prepend(ruleFail);
    rules.prepend(new RuleGenerateWith(Generator.class));
    RuleReplaceWith ruleReplaceCanvas = new RuleReplaceWith("WebkitCanvasElement");
    ruleReplaceCanvas.getRootCondition()
        .getConditions().add(new ConditionWhenTypeIs("CanvasElement"));
    rules.prepend(ruleReplaceCanvas);
    rules.prepend(new RuleReplaceWithFallback("CanvasElement"));

    // Creates rebind rule classes for the non-generator rules in the provided list.
    precompiler.buildSimpleRuntimeRebindRules(rules);

    // Only 3 rebind rules were created because the generator rule was skipped.
    assertEquals(3, runtimeRebindRuleSourcesByShortName.size());

    // Expects to see the created fallback rule first.
    String runtimeRebindRule0 = runtimeRebindRuleSourcesByShortName.get("RuntimeRebindRule0");
    assertTrue(runtimeRebindRule0.contains("@CanvasElement::new()()"));
    assertTrue(runtimeRebindRule0.contains("requestTypeClass == @CanvasElement::class"));

    // Expects to see the created replace with rule second.
    String runtimeRebindRule1 = runtimeRebindRuleSourcesByShortName.get("RuntimeRebindRule1");
    assertTrue(runtimeRebindRule1.contains("@WebkitCanvasElement::new()()"));
    assertTrue(runtimeRebindRule1.contains("requestTypeClass == @CanvasElement::class"));

    // Expects to see the created fail rule third.
    String runtimeRebindRule2 = runtimeRebindRuleSourcesByShortName.get("RuntimeRebindRule2");
    assertTrue(runtimeRebindRule2.contains("Deferred binding request failed for type"));
    assertTrue(runtimeRebindRule2.contains(
        "RuntimePropertyRegistry::getPropertyValue(*)(\"foo\") == \"bar\""));

    // Now that runtime rebind rules have been generated, create a registrator for them.
    precompiler.buildRuntimeRebindRegistrator(allRootTypes);

    // JProgram was informed of the newly created PropertyProviderRegistrator type and its name
    // reflects the name of the module currently being processed.
    assertEquals("com.google.gwt.lang.mock_RuntimeRebindRegistrator",
        compiler.jprogram.getRuntimeRebindRegistratorTypeSourceName());
    // The allRootTypes list was augmented to know about this newly created type.
    assertTrue(
        allRootTypes.contains(compiler.jprogram.getRuntimeRebindRegistratorTypeSourceName()));

    String registratorSource = generatorContext.stringWriterByTypeSourceName.get(
        compiler.jprogram.getRuntimeRebindRegistratorTypeSourceName()).toString();
    // The generated registrator contains all of the RuntimeRebindRule class instantiation,
    // and registrations.
    assertTrue(registratorSource.contains(
        "RuntimeRebinder.registerRuntimeRebindRule(new RuntimeRebindRule0());"));
    assertTrue(registratorSource.contains(
        "RuntimeRebinder.registerRuntimeRebindRule(new RuntimeRebindRule1());"));
    assertTrue(registratorSource.contains(
        "RuntimeRebinder.registerRuntimeRebindRule(new RuntimeRebindRule2());"));
  }

  public void testBuildPropertyProviderRegistrator() throws UnableToCompleteException {
    // Sets up environment.
    Set<String> allRootTypes = Sets.newHashSet();
    Properties properties = new Properties();
    BindingProperty userAgentProperty = properties.createBinding("user.agent");
    userAgentProperty.setProvider(new PropertyProvider("return navigator.userAgent;"));
    userAgentProperty.addDefinedValue(userAgentProperty.getRootCondition(), "mozilla");
    userAgentProperty.addDefinedValue(userAgentProperty.getRootCondition(), "webkit");
    BindingProperty flavorProperty = properties.createBinding("flavor");
    flavorProperty.setProvider(new PropertyProvider("return window.properties.flavor;"));
    flavorProperty.addDefinedValue(flavorProperty.getRootCondition(), "Vanilla");
    flavorProperty.addDefinedValue(flavorProperty.getRootCondition(), "Chocolate");
    ConfigurationProperty emulateStackProperty =
        properties.createConfiguration("emulateStack", false);
    emulateStackProperty.setValue("TRUE");
    compiler.jprogram = new JProgram();

    // Builds property provider classes and a property provider registrator to register them.
    precompiler.buildPropertyProviderRegistrator(allRootTypes,
        Sets.newTreeSet(Lists.newArrayList(userAgentProperty, flavorProperty)),
        Sets.newTreeSet(Lists.newArrayList(emulateStackProperty)));

    // JProgram was informed of the newly created PropertyProviderRegistrator type and its source
    // name reflects the name of the module currently being processed.
    assertEquals("com.google.gwt.lang.mock_PropertyProviderRegistrator",
        compiler.jprogram.getPropertyProviderRegistratorTypeSourceName());
    // The allRootTypes list was augmented to know about this newly created type.
    assertTrue(
        allRootTypes.contains(compiler.jprogram.getPropertyProviderRegistratorTypeSourceName()));

    String registratorSource = generatorContext.stringWriterByTypeSourceName.get(
        compiler.jprogram.getPropertyProviderRegistratorTypeSourceName()).toString();
    // The generated registrator contains PropertyValueProvider class definitions, instantiation,
    // and registration for each binding property.
    assertTrue(registratorSource.contains("class PropertyValueProvider0"));
    assertTrue(registratorSource.contains("\"flavor\""));
    assertTrue(registratorSource.contains("/*-return window.properties.flavor;-*/"));
    assertTrue(registratorSource.contains(
        "registerPropertyValueProvider(" + "new PropertyValueProvider0())"));
    assertTrue(registratorSource.contains("class PropertyValueProvider1"));
    assertTrue(registratorSource.contains("\"user.agent\""));
    assertTrue(registratorSource.contains("/*-return navigator.userAgent;-*/;"));
    assertTrue(registratorSource.contains(
        "registerPropertyValueProvider(" + "new PropertyValueProvider1())"));
  }

  public void testRunGeneratorsToFixedPoint() throws UnableToCompleteException {
    // Sets up environment.
    Map<String, String> runtimeRebindRuleSourcesByShortName =
        RuntimeRebindRuleGenerator.RUNTIME_REBIND_RULE_SOURCES_BY_SHORT_NAME;
    MockLibraryWriter libraryWriter = new MockLibraryWriter();
    // A library group with a varied
    // user.agent/locale/BrowserShimGenerator/LocaleMessageGenerator configuration of properties
    // and generators.
    MockModuleDef module = new MockModuleDef();
    BindingProperty userAgentProperty = module.getProperties().createBinding("user.agent");
    userAgentProperty.addDefinedValue(userAgentProperty.getRootCondition(), "mozilla");
    userAgentProperty.addDefinedValue(userAgentProperty.getRootCondition(), "webkit");
    userAgentProperty.addDefinedValue(userAgentProperty.getRootCondition(), "ie");
    userAgentProperty.addDefinedValue(userAgentProperty.getRootCondition(), "webkit_phone");
    userAgentProperty.addDefinedValue(userAgentProperty.getRootCondition(), "webkit_tablet");
    BindingProperty flavorProperty = module.getProperties().createBinding("locale");
    flavorProperty.addDefinedValue(flavorProperty.getRootCondition(), "en");
    flavorProperty.addDefinedValue(flavorProperty.getRootCondition(), "fr");
    flavorProperty.addDefinedValue(flavorProperty.getRootCondition(), "ru");
    RuleGenerateWith browserShimGenerateRule = new RuleGenerateWith(BrowserShimGenerator.class);
    browserShimGenerateRule.getRootCondition()
        .getConditions().add(new ConditionWhenTypeEndsWith("Shim"));
    module.addRule(browserShimGenerateRule);
    RuleGenerateWith localeMessageGenerateRule = new RuleGenerateWith(LocaleMessageGenerator.class);
    localeMessageGenerateRule.getRootCondition()
        .getConditions().add(new ConditionWhenTypeEndsWith("Messages"));
    module.addRule(localeMessageGenerateRule);
    LibraryGroup libraryGroup = LibraryGroupTest.buildVariedPropertyGeneratorLibraryGroup(
        "com.google.gwt.dev.jjs.LibraryJavaToJavaScriptCompilerTest.BrowserShimGenerator",
        Sets.newHashSet("com.google.ChromeMessages"),
        "com.google.gwt.dev.jjs.LibraryJavaToJavaScriptCompilerTest.LocaleMessageGenerator",
        Sets.newHashSet("com.google.WindowShim"));
    compilerContext = new CompilerContext.Builder().libraryGroup(libraryGroup)
        .libraryWriter(libraryWriter).module(module).build();
    finishSetUpWithCompilerContext();

    // Analyzes properties and generators in the library group and watches output in the generator
    // context to figure out which generators to run and how many times.
    precompiler.runGeneratorsToFixedPoint(null);

    // Shows that rebinds processed by generators were not just the ones explicitly rebound in this
    // module but also ones previously processed in dependency libraries but reprocessed now because
    // of new property value changes.
    assertEquals(Sets.newHashSet("com.google.EventShim", // Explicitly rebound at top level
        "com.google.WindowShim", // Old lib rebind, reprocessed because of new property values.
        "com.google.ChromeMessages", // Old lib rebind, reprocessed because of new property values.
        "com.google.ErrorMessages" // Explicitly rebound at top level
    ), precompiler.processedReboundTypeSourceNames);
    // Rebinds for 3 locales * 2 locale rebound files + 5 user agents * 2 user agent rebound files.
    assertEquals(16, runtimeRebindRuleSourcesByShortName.size());
  }

  protected void finishSetUpWithCompilerContext() throws UnableToCompleteException {
    generatorContext = new MockGeneratorContext(compilerContext, CompilationStateBuilder.buildFrom(
        TreeLogger.NULL, compilerContext, Sets.<Resource>newHashSet()), null, true, false);
    compiler =
        new MockLibraryJavaToJavaScriptCompiler(TreeLogger.NULL, compilerContext, generatorContext);
    precompiler = compiler.createPrecompiler();
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    RuntimeRebindRuleGenerator.RUNTIME_REBIND_RULE_SOURCES_BY_SHORT_NAME.clear();
    RuntimeRebindRuleGenerator.runtimeRebindRuleCount = 0;
    compilerContext = new CompilerContext.Builder().module(new MockModuleDef()).build();
    finishSetUpWithCompilerContext();
  }
}
